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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 数值系统接口（鉴权复用共享 TokenStore）：
 *   GET  /api/stats/me
 *       Header Authorization: Bearer <token>
 *       -> { fullRealm, totalLevel, realm, realmLevel, subLevel, stage,
 *            lifespanCap, total:{hp,lifespan,atk,def,spd}, breakdown:{...} }
 *   POST /api/stats/breakthrough
 *       Header Authorization: Bearer <token>
 *       -> 修为总层数 +1（最高 81 层，渡劫后期九层为上限），返回最新属性面板
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final UserRepository userRepository;
    private final ManualRepository manualRepository;
    private final TreasureRepository treasureRepository;
    private final TokenStore tokenStore;

    public StatsController(UserRepository userRepository,
                           ManualRepository manualRepository,
                           TreasureRepository treasureRepository,
                           TokenStore tokenStore) {
        this.userRepository = userRepository;
        this.manualRepository = manualRepository;
        this.treasureRepository = treasureRepository;
        this.tokenStore = tokenStore;
    }

    /** 查询当前角色属性面板 */
    @GetMapping("/me")
    public ResponseEntity<?> myStats(@RequestHeader(value = "Authorization", required = false) String auth) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();
        return ResponseEntity.ok(buildPanel(u));
    }

    /** 提升一个小境界（修为总层数 +1，消耗灵石 + 灵气 + 天数），返回最新属性面板 */
    @PostMapping("/breakthrough")
    public ResponseEntity<?> breakthrough(@RequestHeader(value = "Authorization", required = false) String auth) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();

        int cur = u.getTotalLevel() != null ? u.getTotalLevel() : 1;
        if (cur >= RealmEnum.MAX_TOTAL_LEVEL) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "已至渡劫后期九层，修为圆满，无法再提升"));
        }

        int cost = RealmEnum.breakthroughCost(cur);
        int qiCost = RealmEnum.breakthroughQiCost(cur);
        int dayCost = RealmEnum.breakthroughDayCost(cur);
        int stones = u.getSpiritStone() != null ? u.getSpiritStone() : 0;
        int qi = u.getSpiritQi() != null ? u.getSpiritQi() : 0;

        if (stones < cost) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "灵石不足，突破需 " + cost + " 灵石（当前仅 " + stones + "）"));
        }
        if (qi < qiCost) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "灵气不足，突破需 " + qiCost + " 灵气（当前仅 " + qi + "）"));
        }
        // 寿元检查：闭关天数不得导致坐化
        int ageUsed = u.getAgeUsed() != null ? u.getAgeUsed() : 0;
        if (ageUsed + dayCost > CharacterStatsService.remainingLifespan(u) * 365L) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "寿元将尽，无力再闭关突破"));
        }

        int next = cur + 1;
        u.setTotalLevel(next);
        u.setSpiritStone(stones - cost);
        u.setSpiritQi(qi - qiCost);
        u.setAgeUsed(ageUsed + dayCost);
        // 突破后刷新持久化的 lifespan（境界提升可能增加寿元上限）
        CharacterStatsService.refreshLifespan(u);
        // 同步可读境界名，保持与历史字段一致
        u.setRealm(RealmEnum.fullName(next));
        userRepository.save(u);

        Map<String, Object> panel = buildPanel(u);
        panel.put("levelUp", true);
        panel.put("costPaid", cost);
        panel.put("qiCostPaid", qiCost);
        panel.put("dayCostPaid", dayCost);
        return ResponseEntity.ok(panel);
    }

    /** 从 Authorization 头解析并加载用户，失败返回 null */
    private User resolveUser(String auth) {
        String token = parseToken(auth);
        if (token == null || !tokenStore.contains(token)) return null;
        String username = tokenStore.getUsername(token);
        return userRepository.findByUsername(username).orElse(null);
    }

    /** 根据用户当前配置（功法/法宝名称）装配完整属性面板 */
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
        // 经济系统：灵石余额 + 灵气 + 已耗天数 + 剩余寿元 + 下一层突破费用
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

        // 五维「寿元」与剩余寿元均来自数据库 lifespan 字段（compute 中已同步），无需额外覆盖
        panel.putAll(stats);

        // 资源获取冷却剩余秒数（供前端刷新后恢复倒计时，不丢）
        panel.put("meditateCd", cooldownRemaining(u.getLastMeditateAt(), 60));
        panel.put("huntCd", cooldownRemaining(u.getLastHuntAt(), 300));
        panel.put("mineCd", cooldownRemaining(u.getLastMineAt(), 300));
        return panel;
    }

    /** 计算某操作剩余的冷却秒数（从未操作过返回 0） */
    private long cooldownRemaining(LocalDateTime lastAt, int cooldownSec) {
        if (lastAt == null) return 0;
        long elapsed = Duration.between(lastAt, LocalDateTime.now()).getSeconds();
        return Math.max(0, cooldownSec - elapsed);
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
}
