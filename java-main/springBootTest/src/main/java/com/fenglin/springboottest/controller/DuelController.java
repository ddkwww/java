package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.config.RealmEnum;
import com.fenglin.springboottest.config.TokenStore;
import com.fenglin.springboottest.entity.Manual;
import com.fenglin.springboottest.entity.Treasure;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.repository.ManualRepository;
import com.fenglin.springboottest.repository.TreasureRepository;
import com.fenglin.springboottest.repository.UserRepository;
import com.fenglin.springboottest.service.CharacterStatsService;
import com.fenglin.springboottest.service.DuelService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 对战系统接口（鉴权复用共享 TokenStore）：
 *   GET  /api/duel/online        -> { users:[{username,realm,totalLevel,spiritRoot,physique}] }  当前在线用户（排除自己）
 *   GET  /api/duel/profile?user= -> 某人完整属性面板（灵根/体质/境界/五维/灵石/门派/功法/法宝）
 *   POST /api/duel/challenge     {target} -> {duelId, status}
 *   GET  /api/duel/pending       -> { duel:{duelId,from,fromRealm,fromTotalLevel,fromSpiritRoot,status} | null }
 *   POST /api/duel/respond       {duelId, accept} -> {status, winner?}
 *   GET  /api/duel/status?duelId=-> {status, from, to, winner?}  挑战方轮询结果
 */
@RestController
@RequestMapping("/api/duel")
public class DuelController {

    /** 发起对战消耗的灵石，胜利方获得的灵石 */
    private static final int DUEL_COST = 10;

    private final UserRepository userRepository;
    private final ManualRepository manualRepository;
    private final TreasureRepository treasureRepository;
    private final TokenStore tokenStore;
    private final DuelService duelService;

    public DuelController(UserRepository userRepository,
                          ManualRepository manualRepository,
                          TreasureRepository treasureRepository,
                          TokenStore tokenStore,
                          DuelService duelService) {
        this.userRepository = userRepository;
        this.manualRepository = manualRepository;
        this.treasureRepository = treasureRepository;
        this.tokenStore = tokenStore;
        this.duelService = duelService;
    }

    /** 当前在线用户列表（排除自己） */
    @GetMapping("/online")
    public ResponseEntity<?> online(@RequestHeader(value = "Authorization", required = false) String auth) {
        User me = resolveUser(auth);
        if (me == null) return unauthorized();
        List<String> names = tokenStore.getOnline(me.getUsername(), 30_000L);
        List<Map<String, Object>> list = new ArrayList<>();
        for (String name : names) {
            User u = userRepository.findByUsername(name).orElse(null);
            if (u == null) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("username", u.getUsername());
            m.put("realm", u.getRealm());
            m.put("totalLevel", u.getTotalLevel());
            m.put("spiritRoot", u.getSpiritRoot());
            m.put("physique", u.getPhysique());
            list.add(m);
        }
        list.sort((a, b) -> Integer.compare(
                (Integer) (b.get("totalLevel") != null ? b.get("totalLevel") : 0),
                (Integer) (a.get("totalLevel") != null ? a.get("totalLevel") : 0)));
        return ResponseEntity.ok(Map.of("users", list));
    }

    /** 心跳：仅用于刷新自身在线时间戳（由 OnlineFilter 处理），返回 ok */
    @GetMapping("/heartbeat")
    public ResponseEntity<?> heartbeat(@RequestHeader(value = "Authorization", required = false) String auth) {
        User me = resolveUser(auth);
        if (me == null) return unauthorized();
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** 某人完整属性面板（详情抽屉用） */
    @GetMapping("/profile")
    public ResponseEntity<?> profile(@RequestHeader(value = "Authorization", required = false) String auth,
                                    @RequestParam String user) {
        User me = resolveUser(auth);
        if (me == null) return unauthorized();
        User u = userRepository.findByUsername(user).orElse(null);
        if (u == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "用户不存在"));
        }
        return ResponseEntity.ok(buildPanel(u));
    }

    /** 发起对战：消耗 10 灵石（不足则拒绝） */
    @PostMapping("/challenge")
    public ResponseEntity<?> challenge(@RequestHeader(value = "Authorization", required = false) String auth,
                                      @RequestBody Map<String, String> body) {
        User me = resolveUser(auth);
        if (me == null) return unauthorized();
        String target = body.get("target");
        if (target == null || target.isBlank()) return bad("请指定对战目标");
        if (target.equals(me.getUsername())) return bad("不能与自己切磋");
        if (userRepository.findByUsername(target).isEmpty()) return bad("目标用户不存在");

        int stones = me.getSpiritStone() != null ? me.getSpiritStone() : 0;
        if (stones < DUEL_COST) return bad("灵石不足，发起对战需 " + DUEL_COST + " 灵石");

        DuelService.Duel d = duelService.challenge(me.getUsername(), target);
        if (d == null) return bad("无法发起对战");

        // 发起即扣费并持久化
        me.setSpiritStone(stones - DUEL_COST);
        userRepository.save(me);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("duelId", d.id);
        out.put("status", d.status);
        out.put("spiritStone", me.getSpiritStone());
        return ResponseEntity.ok(out);
    }

    /** 被挑战方轮询：待处理请求 */
    @GetMapping("/pending")
    public ResponseEntity<?> pending(@RequestHeader(value = "Authorization", required = false) String auth) {
        User me = resolveUser(auth);
        if (me == null) return unauthorized();
        DuelService.Duel d = duelService.pendingFor(me.getUsername());
        if (d == null) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("duel", null);
            return ResponseEntity.ok(empty);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("duelId", d.id);
        m.put("from", d.from);
        User fu = userRepository.findByUsername(d.from).orElse(null);
        if (fu != null) {
            m.put("fromRealm", fu.getRealm());
            m.put("fromTotalLevel", fu.getTotalLevel());
            m.put("fromSpiritRoot", fu.getSpiritRoot());
        }
        m.put("status", d.status);
        return ResponseEntity.ok(Map.of("duel", m));
    }

    /** 被挑战方响应（同意/拒绝） */
    @PostMapping("/respond")
    public ResponseEntity<?> respond(@RequestHeader(value = "Authorization", required = false) String auth,
                                    @RequestBody Map<String, Object> body) {
        User me = resolveUser(auth);
        if (me == null) return unauthorized();
        String duelId = body.get("duelId") != null ? body.get("duelId").toString() : null;
        boolean accept = Boolean.TRUE.equals(body.get("accept"));
        if (duelId == null || duelId.isBlank()) return bad("缺少对战 ID");

        DuelService.Duel d = duelService.statusOf(duelId);
        if (d == null) return bad("对战不存在或已过期");

        User fu = userRepository.findByUsername(d.from).orElse(null);
        User tu = userRepository.findByUsername(d.to).orElse(null);

        DuelService.Duel result;
        if (accept) {
            // 同意：用双方真实属性模拟整场回合制战斗，产出战报
            Map<String, Object> fromPanel = fu != null ? buildPanel(fu) : new LinkedHashMap<>();
            Map<String, Object> toPanel = tu != null ? buildPanel(tu) : new LinkedHashMap<>();
            int fromLevel = fu != null && fu.getTotalLevel() != null ? fu.getTotalLevel() : 1;
            int toLevel = tu != null && tu.getTotalLevel() != null ? tu.getTotalLevel() : 1;
            DuelService.BattleResult battle =
                    duelService.simulate(d.from, fromPanel, fromLevel, d.to, toPanel, toLevel);
            result = duelService.applyResult(duelId, true, me.getUsername(), battle);

            // 胜者获得 10 灵石（持久化）
            if (result != null && result.winner != null) {
                User winner = userRepository.findByUsername(result.winner).orElse(null);
                if (winner != null) {
                    int ws = winner.getSpiritStone() != null ? winner.getSpiritStone() : 0;
                    winner.setSpiritStone(ws + DUEL_COST);
                    userRepository.save(winner);
                }
            }
        } else {
            result = duelService.applyResult(duelId, false, me.getUsername(), null);
        }
        if (result == null) return bad("非被挑战方或无待处理对战");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", result.status);
        if (result.winner != null) out.put("winner", result.winner);
        if (result.log != null) out.put("log", result.log);
        out.put("reward", result.winner != null ? DUEL_COST : 0);
        return ResponseEntity.ok(out);
    }

    /** 挑战方轮询：对战状态 */
    @GetMapping("/status")
    public ResponseEntity<?> status(@RequestHeader(value = "Authorization", required = false) String auth,
                                   @RequestParam String duelId) {
        User me = resolveUser(auth);
        if (me == null) return unauthorized();
        DuelService.Duel d = duelService.statusOf(duelId);
        if (d == null) return ResponseEntity.ok(Map.of("status", "EXPIRED"));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("status", d.status);
        out.put("from", d.from);
        out.put("to", d.to);
        if (d.winner != null) out.put("winner", d.winner);
        if (d.log != null) out.put("log", d.log);
        return ResponseEntity.ok(out);
    }

    // ===== 鉴权与面板（复用 StatsController 同套逻辑） =====

    private User resolveUser(String auth) {
        String token = parseToken(auth);
        if (token == null || !tokenStore.contains(token)) return null;
        String username = tokenStore.getUsername(token);
        return userRepository.findByUsername(username).orElse(null);
    }

    private String parseToken(String auth) {
        if (auth == null || !auth.toLowerCase().startsWith("bearer ")) return null;
        String token = auth.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "未登录或登录已失效"));
    }

    private ResponseEntity<?> bad(String msg) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", msg));
    }

    /** 装配完整属性面板（与 StatsController.buildPanel 一致） */
    private Map<String, Object> buildPanel(User u) {
        Manual manual = null;
        Treasure treasure = null;
        if (u.getInitialGongfa() != null && !u.getInitialGongfa().isBlank()) {
            manual = manualRepository.findFirstByName(u.getInitialGongfa()).orElse(null);
        }
        if (u.getInitialTreasure() != null && !u.getInitialTreasure().isBlank()) {
            treasure = treasureRepository.findFirstByName(u.getInitialTreasure()).orElse(null);
        }

        Map<String, Object> stats = CharacterStatsService.compute(u, manual, treasure);

        Map<String, Object> panel = new LinkedHashMap<>();
        panel.put("username", u.getUsername());
        panel.put("physique", u.getPhysique());
        panel.put("spiritRoot", u.getSpiritRoot());
        panel.put("manual", u.getInitialGongfa());
        panel.put("treasure", u.getInitialTreasure());
        panel.put("sect", u.getSect());
        int stones = u.getSpiritStone() != null ? u.getSpiritStone() : 0;
        panel.put("spiritStone", stones);
        panel.put("spiritQi", u.getSpiritQi() != null ? u.getSpiritQi() : 0);
        panel.put("age", u.getAge() != null ? u.getAge() : 0);
        panel.put("ageUsed", u.getAgeUsed() != null ? u.getAgeUsed() : 0);
        int remaining = CharacterStatsService.remainingLifespan(u);
        panel.put("remainingLifespan", remaining);
        panel.put("breakthroughCost", RealmEnum.breakthroughCost(
                u.getTotalLevel() != null ? u.getTotalLevel() : 1));
        panel.put("breakthroughQiCost", RealmEnum.breakthroughQiCost(
                u.getTotalLevel() != null ? u.getTotalLevel() : 1));
        panel.put("breakthroughDayCost", RealmEnum.breakthroughDayCost(
                u.getTotalLevel() != null ? u.getTotalLevel() : 1));

        panel.putAll(stats);

        panel.put("meditateCd", cooldownRemaining(u.getLastMeditateAt(), 60));
        panel.put("huntCd", cooldownRemaining(u.getLastHuntAt(), 300));
        panel.put("mineCd", cooldownRemaining(u.getLastMineAt(), 300));
        return panel;
    }

    private long cooldownRemaining(LocalDateTime lastAt, int cooldownSec) {
        if (lastAt == null) return 0;
        long elapsed = Duration.between(lastAt, LocalDateTime.now()).getSeconds();
        return Math.max(0, cooldownSec - elapsed);
    }
}
