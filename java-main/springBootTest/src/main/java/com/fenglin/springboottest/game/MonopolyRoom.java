package com.fenglin.springboottest.game;

import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.repository.UserRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 大富翁·灵石夺宝 —— 牌桌单例（线程安全，参考拍卖行 AuctionSession 模式）。
 *
 * 规则（与前端契约一致）：
 *   - 报名阶段（WAITING）：真实玩家点击「报名」即扣除固定 ENTRY_FEE 灵石进入奖池，
 *     座位按报名顺序分配（0-3），满 4 人可开局；开局前可「退出」退回灵石。
 *   - 开局（PLAYING）：4 人按座位轮流掷骰，服务器权威移动 + 触发格子事件。
 *   - 棋盘为 20 格环形平面（含起点/灵矿/坊市/秘境/天劫/灵石税/机遇/闭关/洞府），
 *     经过起点额外 +START_BONUS。
 *   - 核心玩法（占领）：灵矿(MINE)为可占领地块。首次抵达未占领的灵矿时，服务器暂停回合，
 *     弹出「是否占领」选择（decide: occupy/skip），占领需花费该矿 price 灵石；此后他人落上需支付
 *     租金 baseRent × level 给矿主，矿主自己再次落上可「升级」(decide: upgrade/skip)，升级花费
 *     upgradeCost，租金随等级提升，最高 MAX_PROPERTY_LEVEL 级。
 *   - 闭关格：下次轮到自己时跳过（不掷骰）。
 *   - 总回合数达到 MAX_ROLLS（4 人 × ROUNDS_PER_PLAYER）后结束（ENDED）；
 *     任意玩家可发起「结束投票」，过半数（> 半数）赞成才提前结算，超时或全员否决则继续。
 *   - 结算：按局内灵石排名，奖池按 [50%,30%,15%,5%] 发放至真实账号；
 *     玩家净变化 = 发放 - 报名费。
 *
 * 防卡死：当前玩家超过 IDLE_MS 未掷骰/未作选择，定时任务自动代掷或自动跳过选择。
 *
 * 本类不再由 Spring 单例托管（去掉 @Component），而是由 MonopolyRoomManager 按房间号
 * 实例化多个对象，manager 统一用 @Scheduled 驱动所有房间的 tick，并按用户名路由请求，
 * 从而支持「多局并行」：满 4 人的房间继续玩，其余人可另开新局。
 */
public class MonopolyRoom {

    /** 房间唯一标识（由 manager 分配，如 R1、R2…） */
    private final String roomId;

    /** 报名费（灵石），报名即真实扣减进入奖池 */
    public static final int ENTRY_FEE = 10;
    /** 棋盘格数 */
    public static final int BOARD_SIZE = 20;
    /** 经过起点奖励 */
    public static final int START_BONUS = 500;
    /** 每人可掷骰回合数 */
    public static final int ROUNDS_PER_PLAYER = 12;
    /** 总掷骰次数上限 = 4 × ROUNDS_PER_PLAYER */
    public static final int MAX_ROLLS = 4 * ROUNDS_PER_PLAYER;
    /** 当前玩家未操作超时（毫秒），超时自动代掷/自动跳过 */
    public static final long IDLE_MS = 90_000L;
    /** 结束投票有效时长（毫秒），超时未达多数则撤销投票继续游戏 */
    public static final long VOTE_MS = 60_000L;
    /** 灵矿最高等级 */
    public static final int MAX_PROPERTY_LEVEL = 4;

    public enum State { WAITING, PLAYING, ENDED }

    private final UserRepository userRepository;
    private final Random rng = new Random();

    public MonopolyRoom(String roomId, UserRepository userRepository) {
        this.roomId = roomId;
        this.userRepository = userRepository;
    }

    public String getRoomId() { return roomId; }

    /** 该用户是否已报名入此桌 */
    public boolean isMember(String username) {
        return joined.contains(username);
    }

    /** 该用户是否可加入此桌（等待阶段且未满且未重复） */
    public boolean canJoin(String username) {
        return state == State.WAITING && seats.size() < 4 && !joined.contains(username);
    }

    // ===== 共享状态（全部在 synchronized 块内访问） =====
    private State state = State.WAITING;
    private final List<Seat> seats = new ArrayList<>();          // 0-3，按报名顺序
    private int currentSeat = 0;
    private int rollsDone = 0;
    private int pool = 0;                                         // 奖池（真实灵石累计）
    private long turnStartedAt = 0;
    private int lastDice = 0;
    private String lastDiceBy = null;
    private LastEvent lastEvent = null;
    private int eventSeq = 0;
    private List<LogEntry> log = new ArrayList<>();
    private List<ResultRow> result = null;

    /** 已报名用户名集合（快速判重 + 防止重复进桌） */
    private final Set<String> joined = ConcurrentHashMap.newKeySet();

    /** 地块归属：tileIndex -> 归属/等级 */
    private final Map<Integer, PropState> props = new HashMap<>();

    /** 待处理选择（占领/升级）：非 null 表示当前座位需先作出选择才能推进 */
    private String pendingType = null;      // "OCCUPY" | "UPGRADE"
    private int pendingTile = -1;
    private String pendingUsername = null;
    private long pendingSince = 0;

    // ===== 结束投票（多数同意才提前结算） =====
    private boolean voteEndActive = false;
    private final Set<String> voteEndYes = ConcurrentHashMap.newKeySet();
    private final Set<String> voteEndNo = ConcurrentHashMap.newKeySet();
    private String voteEndInitiator = null;
    private long voteEndSince = 0;

    // ===== 内部数据结构 =====
    public static class Seat {
        public String username;
        public String realm;
        public int position;          // 棋盘位置 0..BOARD_SIZE-1
        public boolean skipNext;      // 闭关：下次跳过
        public boolean left;          // 报名后退出（仅 WAITING 阶段会发生）
        public boolean out;           // 灵石耗尽已出局（PLAYING 阶段发生，座位保留用于展示）
    }

    public static class PropState {
        public String owner;
        public int level;             // 1..MAX_PROPERTY_LEVEL，1=已占领
        public PropState(String owner, int level) {
            this.owner = owner; this.level = level;
        }
    }

    public static class LastEvent {
        public final String username;
        public final String text;
        public final int delta;
        public final int tileIndex;
        public LastEvent(String username, String text, int delta, int tileIndex) {
            this.username = username; this.text = text; this.delta = delta; this.tileIndex = tileIndex;
        }
    }

    public static class LogEntry {
        public final String username;
        public final String text;
        public final long timeMs;
        public LogEntry(String username, String text, long timeMs) {
            this.username = username; this.text = text; this.timeMs = timeMs;
        }
    }

    public static class ResultRow {
        public final String username;
        public final int gameStone;
        public final int rank;
        public final int payout;
        public final int net;
        public ResultRow(String username, int gameStone, int rank, int payout, int net) {
            this.username = username; this.gameStone = gameStone;
            this.rank = rank; this.payout = payout; this.net = net;
        }
    }

    /** 地块定义（静态部分；owner/level 为动态，见 props） */
    public static class Tile {
        public final int index;
        public final String name;
        public final String type;   // START/MINE/FORTUNE/REST/MARKET/THUNDER/TRIBUTE/SECRET/PAUSE
        public final String desc;
        public final boolean property;   // 是否可占领（灵矿）
        public final int price;          // 占领价
        public final int baseRent;       // 基础租金（实际租金 = baseRent × level）
        Tile(int index, String name, String type, String desc) {
            this(index, name, type, desc, false, 0, 0);
        }
        Tile(int index, String name, String type, String desc, int price, int baseRent) {
            this(index, name, type, desc, true, price, baseRent);
        }
        Tile(int index, String name, String type, String desc, boolean property, int price, int baseRent) {
            this.index = index; this.name = name; this.type = type; this.desc = desc;
            this.property = property; this.price = price; this.baseRent = baseRent;
        }
    }

    private static final Tile[] BOARD = buildBoard();

    private static Tile[] buildBoard() {
        Tile[] b = new Tile[BOARD_SIZE];
        b[0]  = new Tile(0,  "起点", "START",    "灵石夺宝的起点，经过或停留皆获灵石犒赏");
        b[1]  = new Tile(1,  "灵矿", "MINE",     "可占领的灵矿，占领后他人落上需付租金，可升级", 200, 40);
        b[2]  = new Tile(2,  "机遇", "FORTUNE",  "天降机缘，灵石或有盈亏");
        b[3]  = new Tile(3,  "洞府", "REST",     "清静洞府，安然无恙");
        b[4]  = new Tile(4,  "坊市", "MARKET",    "坊市交易，灵石或有盈亏");
        b[5]  = new Tile(5,  "天劫", "THUNDER",  "天劫临头，局内灵石大损");
        b[6]  = new Tile(6,  "灵矿", "MINE",     "可占领的灵矿，占领后他人落上需付租金，可升级", 350, 70);
        b[7]  = new Tile(7,  "灵石税", "TRIBUTE", "上缴灵石税，按局内灵石比例扣除");
        b[8]  = new Tile(8,  "秘境", "SECRET",   "误入秘境，抽取机缘卡");
        b[9]  = new Tile(9,  "洞府", "REST",     "清静洞府，安然无恙");
        b[10] = new Tile(10, "机遇", "FORTUNE",  "天降机缘，灵石或有盈亏");
        b[11] = new Tile(11, "坊市", "MARKET",    "坊市交易，灵石或有盈亏");
        b[12] = new Tile(12, "灵矿", "MINE",     "可占领的灵矿，占领后他人落上需付租金，可升级", 500, 100);
        b[13] = new Tile(13, "闭关", "PAUSE",    "入定闭关，下次轮到自己时跳过一回合");
        b[14] = new Tile(14, "天劫", "THUNDER",  "天劫临头，局内灵石大损");
        b[15] = new Tile(15, "灵石税", "TRIBUTE", "上缴灵石税，按局内灵石比例扣除");
        b[16] = new Tile(16, "秘境", "SECRET",   "误入秘境，抽取机缘卡");
        b[17] = new Tile(17, "灵矿", "MINE",     "可占领的灵矿，占领后他人落上需付租金，可升级", 650, 130);
        b[18] = new Tile(18, "机遇", "FORTUNE",  "天降机缘，灵石或有盈亏");
        b[19] = new Tile(19, "坊市", "MARKET",    "坊市交易，灵石或有盈亏");
        return b;
    }

    // ===== 调度：由 MonopolyRoomManager 的 @Scheduled 每秒驱动一次 =====
    public void tick() {
        long now = System.currentTimeMillis();
        synchronized (this) {
            if (state != State.PLAYING) return;
            // 结束投票超时：撤销并继续游戏
            if (voteEndActive && voteEndSince > 0 && now - voteEndSince > VOTE_MS) {
                voteEndActive = false;
                voteEndYes.clear(); voteEndNo.clear();
                pushLog("系统", "结束投票超时，继续游戏");
            }
            if (pendingUsername != null) {
                if (pendingSince > 0 && now - pendingSince > IDLE_MS) {
                    autoSkip(now);
                }
                return;
            }
            if (turnStartedAt > 0 && now - turnStartedAt > IDLE_MS) {
                Seat s = seats.get(currentSeat);
                if (s != null && !s.left && !s.out) {
                    autoRoll(s.username, now);
                }
            }
        }
    }

    // ===== 报名 / 退出 =====
    public synchronized String join(String username, User u) {
        if (state != State.WAITING) return "牌桌已开始，无法报名";
        if (joined.contains(username)) return "您已报名";
        if (seats.size() >= 4) return "牌桌已满 4 人";
        int stones = u.getSpiritStone() != null ? u.getSpiritStone() : 0;
        if (stones < ENTRY_FEE) return "灵石不足，报名需 " + ENTRY_FEE + " 灵石（当前仅 " + stones + "）";
        u.setSpiritStone(stones - ENTRY_FEE);
        userRepository.save(u);
        pool += ENTRY_FEE;
        Seat seat = new Seat();
        seat.username = username;
        seat.realm = u.getRealm() != null ? u.getRealm() : "炼气期";
        seat.position = 0;
        seats.add(seat);
        joined.add(username);
        pushLog(username, "报名入桌，缴纳 " + ENTRY_FEE + " 灵石进入奖池");
        return null;
    }

    public synchronized String leave(String username, User u) {
        if (state != State.WAITING) return "牌桌已开始，无法退出";
        if (!joined.contains(username)) return "您尚未报名";
        int stones = u.getSpiritStone() != null ? u.getSpiritStone() : 0;
        u.setSpiritStone(stones + ENTRY_FEE);
        userRepository.save(u);
        pool -= ENTRY_FEE;
        joined.remove(username);
        seats.removeIf(s -> s.username.equals(username));
        pushLog(username, "退出牌桌，退回 " + ENTRY_FEE + " 灵石");
        if (seats.isEmpty()) {
            state = State.WAITING;
        }
        return null;
    }

    public synchronized String start(User starter) {
        if (state != State.WAITING) return "牌桌状态异常";
        if (seats.size() != 4) return "需 4 人报名才能开局（当前 " + seats.size() + " 人）";
        if (starter != null && !joined.contains(starter.getUsername())) return "您未报名，无法开局";
        state = State.PLAYING;
        currentSeat = 0;
        rollsDone = 0;
        turnStartedAt = System.currentTimeMillis();
        props.clear();
        clearPending();
        pushLog("系统", "四人齐聚，灵石夺宝开局！");
        return null;
    }

    // ===== 掷骰（仅当前玩家可操作，且无待处理选择） =====
    public synchronized String roll(String username) {
        if (state != State.PLAYING) return "牌桌未在进行中";
        if (pendingUsername != null) return "请先处理当前地块选择";
        Seat s = seats.get(currentSeat);
        if (s == null) return "当前座位异常";
        if (!s.username.equals(username)) return "还没轮到您，请等待";
        doRoll(now());
        return null;
    }

    private void autoRoll(String username, long now) {
        if (state != State.PLAYING) return;
        Seat s = seats.get(currentSeat);
        if (s == null || !s.username.equals(username)) return;
        pushLog(username, "超时未操作，系统代掷");
        doRoll(now);
    }

    /** 真正执行一次掷骰 + 移动 + 事件/占领/租金，并在必要时暂停等待选择 */
    private void doRoll(long now) {
        Seat s = seats.get(currentSeat);
        if (s == null) return;
        int dice = rng.nextInt(6) + 1;
        lastDice = dice;
        lastDiceBy = s.username;
        int old = s.position;
        int np = (old + dice) % BOARD_SIZE;
        s.position = np;
        boolean passedStart = (old + dice) >= BOARD_SIZE;
        if (passedStart) {
            changeOwn(s, START_BONUS);
            pushLog(s.username, "经过起点，获得 " + START_BONUS + " 灵石");
        }
        Tile t = BOARD[np];
        eventSeq++;

        if (t.property) {
            PropState ps = props.get(np);
            if (ps == null) {
                // 未占领：暂停回合，等玩家选择占领/放弃
                rollsDone++;
                pendingType = "OCCUPY";
                pendingTile = np;
                pendingUsername = s.username;
                pendingSince = now;
                String msg = s.username + " 掷出 " + dice + " 点，抵达「" + t.name + "」，可花费 " + t.price + " 灵石占领";
                lastEvent = new LastEvent(s.username, msg, 0, np);
                pushLog(s.username, "掷出 " + dice + " 点 → " + t.name + "（可占领）");
                return;
            } else if (ps.owner.equals(s.username)) {
                if (ps.level >= MAX_PROPERTY_LEVEL) {
                    rollsDone++;
                    lastEvent = new LastEvent(s.username, s.username + " 抵达自己的「" + t.name + "」（已满级），安然无恙", 0, np);
                    pushLog(s.username, "掷出 " + dice + " 点 → " + t.name + "（满级，无事）");
                    advanceTurn(now);
                    checkEnd();
                } else {
                    rollsDone++;
                    pendingType = "UPGRADE";
                    pendingTile = np;
                    pendingUsername = s.username;
                    pendingSince = now;
                    String msg = s.username + " 掷出 " + dice + " 点，抵达自己的「" + t.name + "」Lv." + ps.level + "，可升级";
                    lastEvent = new LastEvent(s.username, msg, 0, np);
                    pushLog(s.username, "掷出 " + dice + " 点 → " + t.name + "（可升级）");
                    return;
                }
            } else {
                // 他人已占领：自动付租金（用真实灵石，不足则付尽余额并出局）
                int rent = t.baseRent * ps.level;
                int have = realStone(s.username);
                int pay = Math.min(rent, have);
                changeOwn(s, -pay);
                Seat owner = findSeat(ps.owner);
                if (owner != null && pay > 0) changeOwn(owner, pay);
                String msg = s.username + " 掷出 " + dice + " 点，抵达「" + t.name + "」，支付租金 " + pay + " 灵石给 " + ps.owner;
                lastEvent = new LastEvent(s.username, msg, -pay, np);
                pushLog(s.username, "掷出 " + dice + " 点 → " + t.name + "，付租金 " + pay + " 给 " + ps.owner);
                rollsDone++;
                advanceTurn(now);
                checkEnd();
            }
        } else {
            int ev = resolveTile(s, t);
            String msg = s.username + " 掷出 " + dice + " 点，抵达「" + t.name + "」" + t.desc
                    + (ev >= 0 ? "，灵石 +" + ev : "，灵石 " + ev);
            lastEvent = new LastEvent(s.username, msg, ev, np);
            pushLog(s.username, "掷出 " + dice + " 点 → " + t.name + (ev >= 0 ? " (+" + ev + ")" : " (" + ev + ")"));
            rollsDone++;
            advanceTurn(now);
            checkEnd();
        }
    }

    /** 处理玩家的占领/升级选择 */
    public synchronized String decide(String username, String action) {
        if (state != State.PLAYING) return "牌桌未在进行中";
        if (pendingUsername == null) return "当前无需选择";
        if (!pendingUsername.equals(username)) return "还没轮到您选择";
        Seat s = findSeat(username);
        if (s == null) return "座位异常";
        long now = now();
        if ("OCCUPY".equals(pendingType)) {
            Tile t = BOARD[pendingTile];
            if ("occupy".equalsIgnoreCase(action)) {
                if (realStone(username) < t.price) return "灵石不足，占领需 " + t.price;
                changeOwn(s, -t.price);
                props.put(pendingTile, new PropState(username, 1));
                lastEvent = new LastEvent(username, username + " 占领了「" + t.name + "」，成为矿主", -t.price, pendingTile);
                pushLog(username, "占领「" + t.name + "」，花费 " + t.price + " 灵石");
            } else {
                lastEvent = new LastEvent(username, username + " 放弃占领「" + t.name + "」", 0, pendingTile);
                pushLog(username, "放弃占领「" + t.name + "」");
            }
        } else { // UPGRADE
            Tile t = BOARD[pendingTile];
            PropState ps = props.get(pendingTile);
            if (ps == null || !ps.owner.equals(username)) return "状态异常";
            if ("upgrade".equalsIgnoreCase(action)) {
                if (ps.level >= MAX_PROPERTY_LEVEL) return "已达最高等级";
                int cost = upgradeCost(t);
                if (realStone(username) < cost) return "灵石不足，升级需 " + cost;
                changeOwn(s, -cost);
                ps.level++;
                int rent = t.baseRent * ps.level;
                lastEvent = new LastEvent(username, username + " 将「" + t.name + "」升至 Lv." + ps.level + "，租金升至 " + rent, -cost, pendingTile);
                pushLog(username, "升级「" + t.name + "」至 Lv." + ps.level + "，租金升至 " + rent);
            } else {
                lastEvent = new LastEvent(username, username + " 跳过升级「" + t.name + "」", 0, pendingTile);
                pushLog(username, "跳过升级「" + t.name + "」");
            }
        }
        clearPending();
        advanceTurn(now);
        checkEnd();
        return null;
    }

    /** 发起结束投票：任意在场玩家可提议，发起人默认赞成 */
    public synchronized String proposeEnd(String username) {
        if (state != State.PLAYING) return "牌桌未在进行中";
        Seat s0 = findSeat(username);
        if (s0 == null) return "您不在牌桌中";
        if (s0.out) return "您已离场，无法发起投票";
        if (voteEndActive) return "已有结束投票进行中";
        voteEndActive = true;
        voteEndYes.clear();
        voteEndNo.clear();
        voteEndInitiator = username;
        voteEndSince = now();
        voteEndYes.add(username);
        pushLog("系统", username + " 提议提前结束本局，等待众人投票（需过半数赞成）");
        checkVoteEnd();
        return null;
    }

    /** 对结束投票表态：agree=true 赞成，false 反对 */
    public synchronized String voteEnd(String username, boolean agree) {
        if (state != State.PLAYING) return "牌桌未在进行中";
        if (!voteEndActive) return "当前没有进行中的结束投票";
        Seat s0 = findSeat(username);
        if (s0 == null) return "您不在牌桌中";
        if (s0.out) return "您已离场，无法投票";
        if (voteEndYes.contains(username) || voteEndNo.contains(username)) return "您已投过票";
        if (agree) voteEndYes.add(username); else voteEndNo.add(username);
        pushLog(username, "对结束本局投了" + (agree ? "赞成" : "反对") + "票");
        checkVoteEnd();
        return null;
    }

    /** 检查投票是否达成多数（> 半数）或全员投完未达多数（撤销） */
    private void checkVoteEnd() {
        if (!voteEndActive) return;
        int total = activeCount();
        if (total == 0) { voteEndActive = false; return; }
        if (voteEndYes.size() > total / 2) {
            pushLog("系统", "赞成结束票数过半，提前结算本局");
            voteEndActive = false;
            settle(now());
            return;
        }
        if ((voteEndYes.size() + voteEndNo.size()) >= total) {
            pushLog("系统", "结束投票未达多数，继续游戏");
            voteEndActive = false;
            voteEndYes.clear();
            voteEndNo.clear();
        }
    }

    private void autoSkip(long now) {
        if (pendingUsername == null) return;
        Seat s = findSeat(pendingUsername);
        if (s == null) {
            clearPending();
            return;
        }
        if ("OCCUPY".equals(pendingType)) {
            Tile t = BOARD[pendingTile];
            lastEvent = new LastEvent(s.username, s.username + " 超时，放弃占领「" + t.name + "」", 0, pendingTile);
            pushLog(s.username, "超时未选择，放弃占领「" + t.name + "」");
        } else {
            Tile t = BOARD[pendingTile];
            lastEvent = new LastEvent(s.username, s.username + " 超时，跳过升级「" + t.name + "」", 0, pendingTile);
            pushLog(s.username, "超时未选择，跳过升级「" + t.name + "」");
        }
        clearPending();
        advanceTurn(now);
        checkEnd();
    }

    private int upgradeCost(Tile t) {
        return Math.max(10, t.price / 2);
    }

    /** 格子事件结算（仅非占领地块），返回对真实灵石的净增减 */
    private int resolveTile(Seat s, Tile t) {
        switch (t.type) {
            case "FORTUNE": {
                int g = -120 + rng.nextInt(341); // -120..220
                return changeOwn(s, g);
            }
            case "MARKET": {
                int g = -80 + rng.nextInt(261); // -80..180
                return changeOwn(s, g);
            }
            case "THUNDER": {
                int g = -(80 + rng.nextInt(121)); // -80..-200
                return changeOwn(s, g);
            }
            case "TRIBUTE": {
                int tax = Math.max(20, (int)(realStone(s.username) * 0.1));
                return changeOwn(s, -tax);
            }
            case "SECRET": {
                int[] cards = {200, -150, 120, -100, 300, -200};
                int g = cards[rng.nextInt(cards.length)];
                return changeOwn(s, g);
            }
            case "PAUSE": {
                s.skipNext = true; return 0;
            }
            case "REST":
            case "START":
            case "MINE":
            default:
                return 0;
        }
    }

    /** 推进到下一个可行动的座位（跳过闭关 / 已出局） */
    private void advanceTurn(long now) {
        if (state != State.PLAYING) return;
        int next = currentSeat;
        for (int i = 0; i < seats.size(); i++) {
            next = (next + 1) % seats.size();
            Seat ns = seats.get(next);
            if (ns == null || ns.out) continue;
            if (ns.skipNext) {
                ns.skipNext = false;
                pushLog(ns.username, "闭关中，跳过本回合");
                continue;
            }
            currentSeat = next;
            turnStartedAt = now;
            return;
        }
        turnStartedAt = now;
    }

    private void checkEnd() {
        if (state == State.PLAYING && rollsDone >= MAX_ROLLS) {
            settle(now());
        }
    }

    /** 结算：按真实灵石排名（仅未出局者），奖池按人数动态比例发放 */
    private void settle(long now) {
        state = State.ENDED;
        clearPending();
        List<Seat> ranked = new ArrayList<>();
        for (Seat s : seats) if (!s.out) ranked.add(s);
        ranked.sort((a, b) -> Integer.compare(realStone(b.username), realStone(a.username)));
        double[] ratio = payoutRatio(ranked.size());
        List<ResultRow> rows = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            Seat s = ranked.get(i);
            int payout = (int) Math.round(pool * ratio[i]);
            int net = payout - ENTRY_FEE;
            User u = userRepository.findByUsername(s.username).orElse(null);
            if (u != null) {
                int stones = u.getSpiritStone() != null ? u.getSpiritStone() : 0;
                u.setSpiritStone(stones + payout);
                userRepository.save(u);
            }
            rows.add(new ResultRow(s.username, realStone(s.username) + payout, i + 1, payout, net));
        }
        result = rows;
        pushLog("系统", "灵石夺宝结束，奖池 " + pool + " 灵石已按名次发放");
    }

    /** 按在场人数返回奖池分配比例（合计为 1） */
    private static double[] payoutRatio(int n) {
        if (n >= 4) return new double[]{0.5, 0.3, 0.15, 0.05};
        if (n == 3) return new double[]{0.5, 0.3, 0.2};
        if (n == 2) return new double[]{0.6, 0.4};
        return new double[]{1.0};
    }

    // ===== 生成状态快照 =====
    public synchronized Map<String, Object> snapshot(long now, String me) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("state", state.name());
        out.put("entryFee", ENTRY_FEE);
        out.put("pool", pool);
        out.put("currentSeat", currentSeat);
        out.put("rollsDone", rollsDone);
        out.put("maxRolls", MAX_ROLLS);

        // 棋盘（含动态 owner/level/rent）
        List<Map<String, Object>> boardList = new ArrayList<>();
        for (Tile t : BOARD) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("index", t.index);
            m.put("name", t.name);
            m.put("type", t.type);
            m.put("desc", t.desc);
            m.put("property", t.property);
            if (t.property) {
                PropState ps = props.get(t.index);
                m.put("price", t.price);
                m.put("baseRent", t.baseRent);
                m.put("owner", ps != null ? ps.owner : null);
                m.put("level", ps != null ? ps.level : 0);
                m.put("rent", ps != null ? t.baseRent * ps.level : t.baseRent);
            } else {
                m.put("price", 0);
                m.put("baseRent", 0);
                m.put("owner", null);
                m.put("level", 0);
                m.put("rent", 0);
            }
            boardList.add(m);
        }
        out.put("board", boardList);

        // 玩家座位
        List<Map<String, Object>> ps = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            if (i < seats.size()) {
                Seat s = seats.get(i);
                m.put("username", s.username);
                m.put("realm", s.realm);
                m.put("position", s.position);
                m.put("gameStone", realStone(s.username));
                m.put("skipNext", s.skipNext);
                m.put("out", s.out);
                m.put("isYou", me != null && me.equals(s.username));
            } else {
                m.put("username", "");
                m.put("realm", "");
                m.put("position", -1);
                m.put("gameStone", 0);
                m.put("skipNext", false);
                m.put("out", false);
                m.put("isYou", false);
            }
            m.put("seat", i);
            ps.add(m);
        }
        out.put("players", ps);

        out.put("dice", lastDice > 0 ? Map.of("value", lastDice, "by", lastDiceBy == null ? "" : lastDiceBy) : null);
        if (lastEvent != null) {
            Map<String, Object> le = new LinkedHashMap<>();
            le.put("username", lastEvent.username);
            le.put("text", lastEvent.text);
            le.put("delta", lastEvent.delta);
            le.put("tileIndex", lastEvent.tileIndex);
            le.put("seq", eventSeq);
            out.put("lastEvent", le);
        } else {
            out.put("lastEvent", null);
        }
        out.put("log", log.stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("username", e.username);
            m.put("text", e.text);
            m.put("time", e.timeMs);
            return m;
        }).collect(Collectors.toList()));

        // 待处理选择
        if (pendingUsername != null) {
            Tile t = BOARD[pendingTile];
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("type", pendingType);
            p.put("username", pendingUsername);
            p.put("seat", indexOfSeat(pendingUsername));
            p.put("tileIndex", pendingTile);
            p.put("tileName", t.name);
            if ("OCCUPY".equals(pendingType)) {
                p.put("price", t.price);
                p.put("rent", t.baseRent);
            } else {
                PropState prop = props.get(pendingTile);
                int curLevel = prop != null ? prop.level : 1;
                p.put("level", curLevel);
                p.put("cost", upgradeCost(t));
                p.put("rent", t.baseRent * (curLevel + 1));
            }
            p.put("isYou", me != null && me.equals(pendingUsername));
            out.put("pending", p);
        } else {
            out.put("pending", null);
        }

        // 当前玩家状态
        boolean isMyTurn = false;
        boolean joinedFlag = me != null && this.joined.contains(me);
        Seat cur = (state == State.PLAYING && currentSeat < seats.size()) ? seats.get(currentSeat) : null;
        if (cur != null && me != null && me.equals(cur.username)) isMyTurn = true;
        Map<String, Object> you = new LinkedHashMap<>();
        you.put("joined", joinedFlag);
        you.put("seat", me != null ? indexOfSeat(me) : -1);
        you.put("isMyTurn", isMyTurn);
        you.put("canRoll", isMyTurn && state == State.PLAYING && pendingUsername == null);
        you.put("canDecide", state == State.PLAYING && pendingUsername != null && me != null && me.equals(pendingUsername));
        boolean voted = voteEndActive && me != null && (voteEndYes.contains(me) || voteEndNo.contains(me));
        you.put("canVote", voteEndActive && me != null && joinedFlag && !voted);
        you.put("votedYes", voteEndActive && me != null && voteEndYes.contains(me));
        you.put("votedNo", voteEndActive && me != null && voteEndNo.contains(me));
        out.put("you", you);

        // 结束投票状态
        if (voteEndActive) {
            Map<String, Object> v = new LinkedHashMap<>();
            v.put("active", true);
            v.put("initiator", voteEndInitiator);
            v.put("yes", new ArrayList<>(voteEndYes));
            v.put("no", new ArrayList<>(voteEndNo));
            v.put("total", activeCount());
            v.put("need", activeCount() / 2 + 1);
            out.put("vote", v);
        } else {
            out.put("vote", null);
        }

        if (result != null) {
            out.put("result", result.stream().map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("username", r.username);
                m.put("gameStone", r.gameStone);
                m.put("rank", r.rank);
                m.put("payout", r.payout);
                m.put("net", r.net);
                return m;
            }).collect(Collectors.toList()));
        } else {
            out.put("result", null);
        }
        return out;
    }

    /** 重置牌桌（仅 ENDED 后调用） */
    public synchronized String reset() {
        if (state != State.ENDED) return "当前牌桌未结束，无法重置";
        state = State.WAITING;
        seats.clear();
        joined.clear();
        currentSeat = 0;
        rollsDone = 0;
        pool = 0;
        turnStartedAt = 0;
        lastDice = 0;
        lastDiceBy = null;
        lastEvent = null;
        result = null;
        log = new ArrayList<>();
        props.clear();
        clearPending();
        voteEndActive = false;
        voteEndYes.clear();
        voteEndNo.clear();
        voteEndInitiator = null;
        voteEndSince = 0;
        return null;
    }

    // ===== 工具方法 =====
    private Seat findSeat(String username) {
        for (Seat s : seats) {
            if (s.username.equals(username)) return s;
        }
        return null;
    }

    private int indexOfSeat(String username) {
        for (int i = 0; i < seats.size(); i++) {
            if (seats.get(i).username.equals(username)) return i;
        }
        return -1;
    }

    private void clearPending() {
        pendingType = null;
        pendingTile = -1;
        pendingUsername = null;
        pendingSince = 0;
    }

    private void pushLog(String username, String text) {
        log.add(new LogEntry(username, text, System.currentTimeMillis()));
        if (log.size() > 200) log.remove(0);
    }

    private long now() { return System.currentTimeMillis(); }

    /** 读取玩家当前真实灵石余额（实时从账号取，因为游戏外操作也会增减灵石） */
    private int realStone(String username) {
        User u = userRepository.findByUsername(username).orElse(null);
        if (u == null) return 0;
        return u.getSpiritStone() != null ? u.getSpiritStone() : 0;
    }

    /** 调整玩家真实灵石（持久化到账号，最低为 0），返回调整后的真实余额 */
    private int adjustReal(String username, int delta) {
        User u = userRepository.findByUsername(username).orElse(null);
        if (u == null) return 0;
        int cur = u.getSpiritStone() != null ? u.getSpiritStone() : 0;
        int next = cur + delta;
        if (next < 0) next = 0;
        u.setSpiritStone(next);
        userRepository.save(u);
        return next;
    }

    /** 直接对玩家真实灵石记账（唯一账本，无局内/真实之分）：
     *  扣减时若余额不足则扣到 0；返回实际发生的增减额。
     *  若对局中某玩家真实灵石因此降至 0（灵石耗尽），则将其判负出局。 */
    private int changeOwn(Seat s, int delta) {
        int before = realStone(s.username);
        int applied = delta;
        if (before + applied < 0) applied = -before;
        int newReal = adjustReal(s.username, applied);
        if (state == State.PLAYING && !s.out && applied < 0 && newReal <= 0) {
            markOut(s, "灵石耗尽，黯然离场");
        }
        return applied;
    }

    /** 当前仍在场（未出局）的玩家数 */
    private int activeCount() {
        int n = 0;
        for (Seat s : seats) if (!s.out) n++;
        return n;
    }

    /** 玩家灵石耗尽出局：标记 out、清待处理；若在场不足 2 人（含）则直接结算本局 */
    private void markOut(Seat s, String reason) {
        if (s.out) return;
        s.out = true;
        joined.remove(s.username);
        pushLog(s.username, reason);
        if (pendingUsername != null && pendingUsername.equals(s.username)) clearPending();
        if (activeCount() <= 2) {
            pushLog("系统", "在场人数不足，本局提前结束");
            settle(now());
        }
    }
}
