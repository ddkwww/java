package com.fenglin.springboottest.controller;

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
 * 经济系统接口（鉴权复用共享 TokenStore）：
 *   POST /api/economy/buy/manual/{id}
 *       Header Authorization: Bearer <token>
 *       -> 扣除灵石，将该功法设为当前主修（装备），返回余额
 *   POST /api/economy/buy/treasure/{id}
 *       Header Authorization: Bearer <token>
 *       -> 扣除灵石，将该法宝设为当前法宝（装备），返回余额
 *   POST /api/economy/earn/meditate
 *       -> 打坐修炼：+灵气，消耗 15 天，冷却 60 秒（与寿元挂钩）
 *   POST /api/economy/earn/hunt
 *       -> 斩妖除魔：+灵气 +灵石，消耗 60 天（2 个月），冷却 5 分钟（与寿元挂钩）
 *   POST /api/economy/earn/mine
 *       -> 挖矿：+灵石，消耗 30 天（1 个月），冷却 5 分钟（与寿元挂钩）
 *
 * 购买规则：灵石不足返回 400；物品不存在返回 404；未登录返回 401。
 * 修炼规则：寿元耗尽返回 400；冷却未到返回 400（附剩余秒数）。
 */
@RestController
@RequestMapping("/api/economy")
public class EconomyController {

    /** 打坐修炼：每次灵气产出 + 消耗天数 + 冷却（秒） */
    private static final int MEDITATE_QI_REWARD = 20;
    private static final int MEDITATE_DAY_COST = 15;
    private static final int MEDITATE_COOLDOWN_SEC = 60;
    /** 斩妖除魔：每次灵气/灵石产出 + 消耗天数 + 冷却（秒） */
    private static final int HUNT_QI_REWARD = 30;
    private static final int HUNT_STONE_REWARD = 100;
    private static final int HUNT_DAY_COST = 60; // 2 个月
    private static final int HUNT_COOLDOWN_SEC = 300;
    /** 挖矿：每次灵石产出 + 消耗天数 + 冷却（秒） */
    private static final int MINE_STONE_REWARD = 150;
    private static final int MINE_DAY_COST = 30; // 1 个月
    private static final int MINE_COOLDOWN_SEC = 300;

    private final UserRepository userRepository;
    private final ManualRepository manualRepository;
    private final TreasureRepository treasureRepository;
    private final TokenStore tokenStore;

    public EconomyController(UserRepository userRepository,
                             ManualRepository manualRepository,
                             TreasureRepository treasureRepository,
                             TokenStore tokenStore) {
        this.userRepository = userRepository;
        this.manualRepository = manualRepository;
        this.treasureRepository = treasureRepository;
        this.tokenStore = tokenStore;
    }

    /** 购买并装备功法 */
    @PostMapping("/buy/manual/{id}")
    public ResponseEntity<?> buyManual(@PathVariable Long id,
                                       @RequestHeader(value = "Authorization", required = false) String auth) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();

        Manual m = manualRepository.findById(id).orElse(null);
        if (m == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "未寻得此卷功法"));
        }

        int price = m.getPrice() != null ? m.getPrice() : 0;
        int stones = u.getSpiritStone() != null ? u.getSpiritStone() : 0;
        if (stones < price) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "灵石不足，购买《" + m.getName() + "》需 " + price + " 灵石（当前仅 " + stones + "）"));
        }

        u.setSpiritStone(stones - price);
        u.setInitialGongfa(m.getName());
        userRepository.save(u);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "已购得《" + m.getName() + "》并装备为主修功法");
        result.put("item", m.getName());
        result.put("price", price);
        result.put("spiritStone", u.getSpiritStone());
        return ResponseEntity.ok(result);
    }

    /** 购买并装备法宝 */
    @PostMapping("/buy/treasure/{id}")
    public ResponseEntity<?> buyTreasure(@PathVariable Long id,
                                         @RequestHeader(value = "Authorization", required = false) String auth) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();

        Treasure t = treasureRepository.findById(id).orElse(null);
        if (t == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "未寻得此件法宝"));
        }

        int price = t.getPrice() != null ? t.getPrice() : 0;
        int stones = u.getSpiritStone() != null ? u.getSpiritStone() : 0;
        if (stones < price) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "灵石不足，购买「" + t.getName() + "」需 " + price + " 灵石（当前仅 " + stones + "）"));
        }

        u.setSpiritStone(stones - price);
        u.setInitialTreasure(t.getName());
        userRepository.save(u);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("message", "已购得「" + t.getName() + "」并装备为随身法宝");
        result.put("item", t.getName());
        result.put("price", price);
        result.put("spiritStone", u.getSpiritStone());
        return ResponseEntity.ok(result);
    }

    /** 打坐修炼：+灵气，消耗 15 天，冷却 60 秒 */
    @PostMapping("/earn/meditate")
    public ResponseEntity<?> meditate(@RequestHeader(value = "Authorization", required = false) String auth) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();

        long remaining = cooldownRemaining(u.getLastMeditateAt(), MEDITATE_COOLDOWN_SEC);
        if (remaining > 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "打坐需静心，尚需 " + remaining + " 秒方可再次运功",
                            "cooldown", remaining));
        }

        // 寿元检查：消耗天数后不得坐化
        if (!canAffordDays(u, MEDITATE_DAY_COST)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "寿元将尽，无力再闭关打坐"));
        }

        int qi = u.getSpiritQi() != null ? u.getSpiritQi() : 0;
        u.setSpiritQi(qi + MEDITATE_QI_REWARD);
        u.setAgeUsed(nz(u.getAgeUsed()) + MEDITATE_DAY_COST);
        u.setLastMeditateAt(LocalDateTime.now());
        userRepository.save(u);

        Map<String, Object> result = baseResult();
        result.put("message", "打坐运功一周天，获得 " + MEDITATE_QI_REWARD + " 灵气，耗 " + MEDITATE_DAY_COST + " 天");
        result.put("qiReward", MEDITATE_QI_REWARD);
        result.put("dayCost", MEDITATE_DAY_COST);
        result.put("spiritQi", u.getSpiritQi());
        result.put("ageUsed", u.getAgeUsed());
        result.put("remainingLifespan", CharacterStatsService.remainingLifespan(u));
        result.put("cooldown", MEDITATE_COOLDOWN_SEC);
        return ResponseEntity.ok(result);
    }

    /** 斩妖除魔：+灵气 +灵石，消耗 60 天（2 个月），冷却 5 分钟 */
    @PostMapping("/earn/hunt")
    public ResponseEntity<?> hunt(@RequestHeader(value = "Authorization", required = false) String auth) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();

        long remaining = cooldownRemaining(u.getLastHuntAt(), HUNT_COOLDOWN_SEC);
        if (remaining > 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "妖魔尚未现身，尚需 " + remaining + " 秒方可再次除妖",
                            "cooldown", remaining));
        }

        if (!canAffordDays(u, HUNT_DAY_COST)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "寿元将尽，无力再赴除妖"));
        }

        int qi = u.getSpiritQi() != null ? u.getSpiritQi() : 0;
        int stones = u.getSpiritStone() != null ? u.getSpiritStone() : 0;
        u.setSpiritQi(qi + HUNT_QI_REWARD);
        u.setSpiritStone(stones + HUNT_STONE_REWARD);
        u.setAgeUsed(nz(u.getAgeUsed()) + HUNT_DAY_COST);
        u.setLastHuntAt(LocalDateTime.now());
        userRepository.save(u);

        Map<String, Object> result = baseResult();
        result.put("message", "斩妖除魔，获得 " + HUNT_QI_REWARD + " 灵气、" + HUNT_STONE_REWARD + " 灵石，耗 " + HUNT_DAY_COST + " 天（2 个月）");
        result.put("qiReward", HUNT_QI_REWARD);
        result.put("stoneReward", HUNT_STONE_REWARD);
        result.put("dayCost", HUNT_DAY_COST);
        result.put("spiritQi", u.getSpiritQi());
        result.put("spiritStone", u.getSpiritStone());
        result.put("ageUsed", u.getAgeUsed());
        result.put("remainingLifespan", CharacterStatsService.remainingLifespan(u));
        result.put("cooldown", HUNT_COOLDOWN_SEC);
        return ResponseEntity.ok(result);
    }

    /** 挖矿：+灵石，消耗 30 天（1 个月），冷却 5 分钟 */
    @PostMapping("/earn/mine")
    public ResponseEntity<?> mine(@RequestHeader(value = "Authorization", required = false) String auth) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();

        long remaining = cooldownRemaining(u.getLastMineAt(), MINE_COOLDOWN_SEC);
        if (remaining > 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "矿脉尚未重凝，尚需 " + remaining + " 秒方可再次开采",
                            "cooldown", remaining));
        }

        if (!canAffordDays(u, MINE_DAY_COST)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "寿元将尽，无力再入矿脉"));
        }

        int stones = u.getSpiritStone() != null ? u.getSpiritStone() : 0;
        u.setSpiritStone(stones + MINE_STONE_REWARD);
        u.setAgeUsed(nz(u.getAgeUsed()) + MINE_DAY_COST);
        u.setLastMineAt(LocalDateTime.now());
        userRepository.save(u);

        Map<String, Object> result = baseResult();
        result.put("message", "深入矿脉，采得 " + MINE_STONE_REWARD + " 灵石，耗 " + MINE_DAY_COST + " 天（1 个月）");
        result.put("stoneReward", MINE_STONE_REWARD);
        result.put("dayCost", MINE_DAY_COST);
        result.put("spiritStone", u.getSpiritStone());
        result.put("ageUsed", u.getAgeUsed());
        result.put("remainingLifespan", CharacterStatsService.remainingLifespan(u));
        result.put("cooldown", MINE_COOLDOWN_SEC);
        return ResponseEntity.ok(result);
    }

    /** 检查消耗指定天数后寿元是否仍充足（不会坐化） */
    private boolean canAffordDays(User u, int days) {
        int ageUsed = nz(u.getAgeUsed()) + days;
        int lifespanYears = CharacterStatsService.remainingLifespan(u);
        // 剩余寿元（年）折算为天数上限；消耗后不能超过上限
        return ageUsed <= lifespanYears * 365L;
    }

    /** 构造包含通用资源的返回结果（附剩余寿元与已耗天数） */
    private Map<String, Object> baseResult() {
        Map<String, Object> map = new LinkedHashMap<>();
        return map;
    }

    /** 计算某操作剩余的冷却秒数（从未操作过返回 0） */
    private long cooldownRemaining(LocalDateTime lastAt, int cooldownSec) {
        if (lastAt == null) return 0;
        long elapsed = Duration.between(lastAt, LocalDateTime.now()).getSeconds();
        return Math.max(0, cooldownSec - elapsed);
    }

    private int nz(Integer v) {
        return v == null ? 0 : v;
    }

    /** 从 Authorization 头解析并加载用户，失败返回 null */
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
}
