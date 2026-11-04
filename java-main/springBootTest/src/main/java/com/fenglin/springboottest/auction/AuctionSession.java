package com.fenglin.springboottest.auction;

import com.fenglin.springboottest.entity.Manual;
import com.fenglin.springboottest.entity.Treasure;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.repository.ManualRepository;
import com.fenglin.springboottest.repository.TreasureRepository;
import com.fenglin.springboottest.repository.UserRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 拍卖行共享会话（单例，线程安全）。
 *
 * 规则（与前端契约一致）：
 *   - 每 AUCTION_INTERVAL_MS 自动开启一场，通知所有在线修士（前端轮询到 RUNNING 时弹 Toast）。
 *   - 每场取 auction=0 的功法(t_manual)与法宝(t_treasure)，按品阶升序（黄<玄<地<天，最低级先拍），取前 LOT_COUNT 件。
 *   - 每件起拍价 START_PRICE（100 灵石）；每次加价档位 +10/+50/+100/+500；每次加价后倒计时重置为 LOT_MS（5 秒）。
 *   - 倒计时归零且无人再加价，当前最高出价者得：扣灵石 + 装备为当前主修/随身法宝 + 标记 auction=1（移出拍卖池）。
 *   - 无人出价则流拍，保持 auction=0（下一场可再次参与）。
 *
 * 状态由 @Scheduled 每秒驱动（tick）；出价入口 placeBid 同步，保证并发安全。
 */
@Component
public class AuctionSession {

    /** 拍卖间隔：每 10 分钟开启一场 */
    private static final long AUCTION_INTERVAL_MS = 10 * 60 * 1000L;
    /** 首次开拍延迟：服务器启动后 10 分钟开启第一场（与间隔一致） */
    private static final long FIRST_OPEN_DELAY_MS = 10 * 60 * 1000L;
    /** 单件拍品倒计时：加价后停留 5 秒 */
    private static final long LOT_MS = 5000L;
    /** 每场拍品数量 */
    private static final int LOT_COUNT = 5;
    /** 起拍价（灵石） */
    private static final int START_PRICE = 100;
    /** 允许的加价档位 */
    private static final int[] INCREMENTS = { 10, 50, 100, 500 };
    /** 品阶排序：黄 < 玄 < 地 < 天（最低级先拍） */
    private static final List<String> GRADE_ORDER = Arrays.asList("黄", "玄", "地", "天");

    private final ManualRepository manualRepository;
    private final TreasureRepository treasureRepository;
    private final UserRepository userRepository;

    public AuctionSession(ManualRepository manualRepository,
                          TreasureRepository treasureRepository,
                          UserRepository userRepository) {
        this.manualRepository = manualRepository;
        this.treasureRepository = treasureRepository;
        this.userRepository = userRepository;
        this.nextOpenAt = System.currentTimeMillis() + FIRST_OPEN_DELAY_MS;
    }

    public enum State { WAITING, RUNNING, ENDED }

    // ===== 共享状态（全部在 synchronized 块内访问） =====
    private State state = State.WAITING;
    private long nextOpenAt;
    private long lastOpenAt = 0;
    private int openCount = 0;

    private List<LotRef> lots = new ArrayList<>();
    private int currentIndex = 0;
    private int currentPrice = START_PRICE;
    private String highestBidder = null;
    private long lotEndsAt = 0;
    private List<BidLogEntry> bidLog = new ArrayList<>();

    /** 已点击「参与本场」的修士（开拍时会自动跳转+通知）。线程安全集合。 */
    private final Set<String> participants = ConcurrentHashMap.newKeySet();

    /** 最近一件拍品的成交结果（供前端 Toast） */
    private LastResult lastResult = null;
    /** 最近一次成交结果是否已推送给前端（避免重复弹窗） */
    private int lastResultSeq = 0;

    // ===== 调度：每秒驱动一次 =====
    @Scheduled(fixedRate = 1000)
    public void tick() {
        long now = System.currentTimeMillis();
        synchronized (this) {
            // 注意：拍卖结束后进入 ENDED，必须允许从 ENDED 重新开拍，否则只会开一场就停（历史 bug）
            if ((state == State.WAITING || state == State.ENDED) && now >= nextOpenAt) {
                openAuction(now);
            }
            if (state == State.RUNNING && now >= lotEndsAt) {
                settleAndAdvance(now);
            }
        }
    }

    private void openAuction(long now) {
        List<LotRef> pool = new ArrayList<>();
        for (Manual m : manualRepository.findByAuction(0)) {
            pool.add(LotRef.fromManual(m));
        }
        for (Treasure t : treasureRepository.findByAuction(0)) {
            pool.add(LotRef.fromTreasure(t));
        }
        if (pool.isEmpty()) {
            // 没有可拍物品，顺延到下一周期
            nextOpenAt = now + AUCTION_INTERVAL_MS;
            return;
        }
        // 按品阶升序：最低级先拍；同品阶按类型(功法<法宝)再按 id 稳定排序
        pool.sort(Comparator.comparingInt((LotRef l) -> gradeRank(l.grade))
                .thenComparing(l -> "manual".equals(l.type) ? 0 : 1)
                .thenComparingLong(l -> l.id));
        lots = new ArrayList<>(pool.subList(0, Math.min(LOT_COUNT, pool.size())));
        currentIndex = 0;
        lastOpenAt = now;
        openCount++;
        state = State.RUNNING;
        startLot(now);
    }

    private void startLot(long now) {
        currentPrice = START_PRICE;
        highestBidder = null;
        lotEndsAt = now + LOT_MS;
        bidLog = new ArrayList<>();
    }

    private void settleAndAdvance(long now) {
        LotRef lot = lots.get(currentIndex);
        String winner = highestBidder;
        int price = currentPrice;
        if (winner == null) {
            // 流拍：保持未拍卖
            lastResult = new LastResult(lot.name, null, price, false, true);
        } else {
            // 赢家扣灵石 + 装备 + 标记已拍卖
            User u = userRepository.findByUsername(winner).orElse(null);
            boolean ok = false;
            if (u != null) {
                int stones = u.getSpiritStone() != null ? u.getSpiritStone() : 0;
                if (stones >= price) {
                    u.setSpiritStone(stones - price);
                    if ("manual".equals(lot.type)) {
                        u.setInitialGongfa(lot.name);
                    } else {
                        u.setInitialTreasure(lot.name);
                    }
                    userRepository.save(u);
                    ok = true;
                }
            }
            // 无论赢家是否成功扣款，都视为已拍出，标记 auction=1 移出拍卖池
            if ("manual".equals(lot.type)) {
                manualRepository.findById(lot.id).ifPresent(m -> { m.setAuction(1); manualRepository.save(m); });
            } else {
                treasureRepository.findById(lot.id).ifPresent(t -> { t.setAuction(1); treasureRepository.save(t); });
            }
            lastResult = new LastResult(lot.name, winner, price, ok, false);
        }
        lastResultSeq++;

        currentIndex++;
        if (currentIndex < lots.size()) {
            startLot(now);
        } else {
            state = State.ENDED;
            nextOpenAt = lastOpenAt + AUCTION_INTERVAL_MS;
            lots = new ArrayList<>();
            // 本轮结束，清空报名者，下一轮开始时可重新「参与本场」
            participants.clear();
        }
    }

    /**
     * 处理出价。返回 null 表示成功，否则返回错误原因。
     */
    public synchronized String placeBid(String username, String type, long id, int amount) {
        long now = System.currentTimeMillis();
        if (state != State.RUNNING) return "拍卖尚未开始";
        if (currentIndex >= lots.size()) return "当前无拍品";
        LotRef lot = lots.get(currentIndex);
        if (!lot.type.equals(type) || lot.id != id) return "这不是当前拍品";
        if (username.equals(highestBidder)) return "您已是最高出价者，无需再加价";
        // 校验加价档位：amount 必须严格高于当前价，且增量 ∈ {10,50,100,500}
        int inc = amount - currentPrice;
        boolean valid = amount > currentPrice;
        if (valid) {
            valid = false;
            for (int step : INCREMENTS) {
                if (inc == step) { valid = true; break; }
            }
        }
        if (!valid) return "加价必须为 +10 / +50 / +100 / +500 灵石";
        currentPrice = amount;
        highestBidder = username;
        lotEndsAt = now + LOT_MS; // 重置 5 秒倒计时
        bidLog.add(new BidLogEntry(username, amount, inc, now));
        return null;
    }

    /** 报名/取消参与下一场拍卖。join=true 报名，false 取消。 */
    public synchronized void setParticipate(String username, boolean join) {
        if (join) participants.add(username);
        else participants.remove(username);
    }

    public synchronized int participantCount() {
        return participants.size();
    }

    public synchronized boolean hasParticipated(String username) {
        return participants.contains(username);
    }

    private static int gradeRank(String g) {
        int i = GRADE_ORDER.indexOf(g);
        return i < 0 ? 9 : i;
    }

    /** 生成状态快照（供 Controller 返回前端） */
    public synchronized Map<String, Object> snapshot(long now, int yourStone, String username) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("state", state.name());
        out.put("nextOpenAt", nextOpenAt);
        out.put("openCount", openCount);
        out.put("yourStone", yourStone);
        out.put("participated", username != null && participants.contains(username));
        out.put("participantCount", participants.size());

        if (lastResult != null) {
            Map<String, Object> lr = new LinkedHashMap<>();
            lr.put("lotName", lastResult.lotName);
            lr.put("winner", lastResult.winner == null ? "" : lastResult.winner);
            lr.put("price", lastResult.price);
            lr.put("success", lastResult.success);
            lr.put("passed", lastResult.passed);
            lr.put("seq", lastResultSeq);
            out.put("lastResult", lr);
        }

        if (state == State.RUNNING && currentIndex < lots.size()) {
            LotRef lot = lots.get(currentIndex);
            Map<String, Object> cur = lot.toMap();
            cur.put("currentPrice", currentPrice);
            cur.put("highestBidder", highestBidder == null ? "" : highestBidder);
            cur.put("lotEndsAt", lotEndsAt);
            List<Map<String, Object>> logs = bidLog.stream().map(e -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("bidder", e.bidder);
                m.put("amount", e.amount);
                m.put("increment", e.increment);
                m.put("time", e.timeMs);
                return m;
            }).collect(Collectors.toList());
            cur.put("bidLog", logs);
            out.put("currentLot", cur);

            List<Map<String, Object>> queue = new ArrayList<>();
            for (int i = currentIndex + 1; i < lots.size(); i++) {
                queue.add(lots.get(i).toMap());
            }
            out.put("queue", queue);
            out.put("remaining", lots.size() - currentIndex);
        } else {
            out.put("currentLot", null);
            out.put("queue", new ArrayList<>());
            out.put("remaining", 0);
        }
        return out;
    }

    // ===== 内部数据结构 =====

    public static class LotRef {
        public String type; // manual / treasure
        public long id;
        public String name;
        public String category;
        public String grade;
        public String realm;
        public String image;
        public int basePrice;
        public String desc;
        public String origin;
        public Integer bonusHp, bonusAtk, bonusDef, bonusSpd, bonusLifespan;

        static LotRef fromManual(Manual m) {
            LotRef l = new LotRef();
            l.type = "manual";
            l.id = m.getId();
            l.name = m.getName();
            l.category = m.getCategory();
            l.grade = m.getGrade();
            l.realm = m.getRealm();
            l.image = m.getImage();
            l.basePrice = m.getPrice() != null ? m.getPrice() : 0;
            l.desc = m.getDesc();
            l.origin = m.getOrigin();
            l.bonusHp = m.getBonusHp();
            l.bonusAtk = m.getBonusAtk();
            l.bonusDef = m.getBonusDef();
            l.bonusSpd = m.getBonusSpd();
            l.bonusLifespan = m.getBonusLifespan();
            return l;
        }

        static LotRef fromTreasure(Treasure t) {
            LotRef l = new LotRef();
            l.type = "treasure";
            l.id = t.getId();
            l.name = t.getName();
            l.category = t.getCategory();
            l.grade = t.getGrade();
            l.realm = t.getRealm();
            l.image = t.getImage();
            l.basePrice = t.getPrice() != null ? t.getPrice() : 0;
            l.desc = t.getDesc();
            l.origin = t.getOrigin();
            l.bonusHp = t.getBonusHp();
            l.bonusAtk = t.getBonusAtk();
            l.bonusDef = t.getBonusDef();
            l.bonusSpd = t.getBonusSpd();
            l.bonusLifespan = t.getBonusLifespan();
            return l;
        }

        Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("type", type);
            m.put("id", id);
            m.put("name", name);
            m.put("category", category);
            m.put("grade", grade);
            m.put("realm", realm);
            m.put("image", image);
            m.put("price", basePrice);
            m.put("desc", desc);
            m.put("origin", origin);
            m.put("bonusHp", bonusHp);
            m.put("bonusAtk", bonusAtk);
            m.put("bonusDef", bonusDef);
            m.put("bonusSpd", bonusSpd);
            m.put("bonusLifespan", bonusLifespan);
            return m;
        }
    }

    public static class BidLogEntry {
        public final String bidder;
        public final int amount;
        public final int increment;
        public final long timeMs;
        public BidLogEntry(String bidder, int amount, int increment, long timeMs) {
            this.bidder = bidder;
            this.amount = amount;
            this.increment = increment;
            this.timeMs = timeMs;
        }
    }

    public static class LastResult {
        public final String lotName;
        public final String winner;
        public final int price;
        public final boolean success;
        public final boolean passed;
        public LastResult(String lotName, String winner, int price, boolean success, boolean passed) {
            this.lotName = lotName;
            this.winner = winner;
            this.price = price;
            this.success = success;
            this.passed = passed;
        }
    }
}
