package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.config.SectRankEnum;
import com.fenglin.springboottest.config.TokenStore;
import com.fenglin.springboottest.entity.Sect;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.repository.SectRepository;
import com.fenglin.springboottest.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 门派接口（契约见前端 src/api/sects.js）：
 *   GET  /api/sects?camp=&q=        -> { items:[...] } 门派列表（可筛选/搜索）
 *   GET  /api/sects/facets          -> { camps:[...] }
 *   GET  /api/sects/{id}            -> 单派详情（含三场所/弟子级别/月俸，附当前用户加入状态）
 *   GET  /api/sects/my              -> 当前用户门派信息（未加入返回 joined:false）
 *   POST /api/sects/join/{id}       -> 加入门派（已加入需先退出？此处允许直接换门派）
 *   POST /api/sects/salary          -> 领取月俸（30 天冷却，附剩余秒数）
 *   POST /api/sects/leave           -> 退出当前门派（散修）
 *
 * 鉴权：除列表/详情外，my/join/salary/leave 均需 Bearer token。
 */
@RestController
@RequestMapping("/api/sects")
public class SectController {

    /** 阵营固定顺序 */
    private static final List<String> CAMP_ORDER = Arrays.asList("正道", "魔道", "中立", "散修");

    private final SectRepository sectRepository;
    private final UserRepository userRepository;
    private final TokenStore tokenStore;

    public SectController(SectRepository sectRepository,
                          UserRepository userRepository,
                          TokenStore tokenStore) {
        this.sectRepository = sectRepository;
        this.userRepository = userRepository;
        this.tokenStore = tokenStore;
    }

    // ===== 门派列表 =====

    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) String camp,
                                    @RequestParam(required = false) String q) {
        String key = (q == null) ? "" : q.trim();
        List<Sect> source = key.isEmpty() ? sectRepository.findAll() : sectRepository.search(key);

        List<Sect> result = source.stream()
                .filter(s -> isBlank(camp) || camp.equals("全部") || camp.equals(s.getCamp()))
                .sorted(Comparator
                        .comparingInt((Sect s) -> campRank(s.getCamp()))
                        .thenComparing(Sect::getName))
                .collect(Collectors.toList());

        Map<String, Object> resp = new LinkedHashMap<>(2);
        resp.put("items", result);
        return resp;
    }

    @GetMapping("/facets")
    public Map<String, Object> facets() {
        List<Sect> all = sectRepository.findAll();
        List<String> camps = all.stream()
                .map(Sect::getCamp)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparingInt(c -> {
                    int i = CAMP_ORDER.indexOf(c);
                    return i < 0 ? 999 : i;
                }))
                .collect(Collectors.toList());
        Map<String, Object> resp = new LinkedHashMap<>(2);
        resp.put("camps", camps);
        return resp;
    }

    /** 单派详情：含三场所 + 弟子级别月俸说明 + 当前用户加入状态 */
    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable Long id,
                                      @RequestHeader(value = "Authorization", required = false) String auth) {
        Sect s = sectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "未找到该门派：id=" + id));

        User u = resolveUser(auth);
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("id", s.getId());
        resp.put("name", s.getName());
        resp.put("camp", s.getCamp());
        resp.put("region", s.getRegion());
        resp.put("desc", s.getDesc());
        resp.put("signature", s.getSignature());
        resp.put("representative", s.getRepresentative());
        resp.put("image", s.getImage());
        // 三场所
        resp.put("places", buildPlaces(s));
        // 加入状态
        boolean joined = u != null && u.getSect() != null && u.getSect().equals(s.getName());
        resp.put("joined", joined);
        if (joined && u != null) {
            SectRankEnum rank = SectRankEnum.ofTotalLevel(u.getTotalLevel() != null ? u.getTotalLevel() : 1);
            resp.put("sectRank", rank.getName());
            resp.put("stoneSalary", rank.getStoneSalary());
            resp.put("qiSalary", rank.getQiSalary());
        }
        return resp;
    }

    // ===== 门派交互（需鉴权） =====

    /** 查询当前用户门派信息 */
    @GetMapping("/my")
    public ResponseEntity<?> my(@RequestHeader(value = "Authorization", required = false) String auth) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();
        return ResponseEntity.ok(buildMySect(u));
    }

    /** 加入门派 */
    @PostMapping("/join/{id}")
    public ResponseEntity<?> join(@PathVariable Long id,
                                  @RequestHeader(value = "Authorization", required = false) String auth) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();

        Sect s = sectRepository.findById(id).orElse(null);
        if (s == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "未寻得此门派"));
        }
        // 散修视为「无门派」，不实际加入
        if ("散修".equals(s.getCamp()) || "散修".equals(s.getName())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "散修无门无派，无需加入"));
        }

        SectRankEnum rank = SectRankEnum.ofTotalLevel(u.getTotalLevel() != null ? u.getTotalLevel() : 1);
        u.setSect(s.getName());
        u.setSectRank(rank.getName());
        userRepository.save(u);

        Map<String, Object> resp = buildMySect(u);
        resp.put("message", "已拜入「" + s.getName() + "」，当前为" + rank.getName());
        return ResponseEntity.ok(resp);
    }

    /** 退出当前门派 */
    @PostMapping("/leave")
    public ResponseEntity<?> leave(@RequestHeader(value = "Authorization", required = false) String auth) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();

        String old = u.getSect();
        u.setSect(null);
        u.setSectRank(null);
        userRepository.save(u);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("message", old != null ? "已退出「" + old + "」，从此无门无派" : "你本无门派");
        resp.put("joined", false);
        return ResponseEntity.ok(resp);
    }

    /** 领取月俸（30 天冷却） */
    @PostMapping("/salary")
    public ResponseEntity<?> salary(@RequestHeader(value = "Authorization", required = false) String auth) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();

        if (u.getSect() == null || u.getSect().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "你尚未加入任何门派，无法领取月俸"));
        }

        long remaining = cooldownRemaining(u.getLastSalaryAt(), SectRankEnum.SALARY_COOLDOWN_SEC);
        if (remaining > 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", "月俸尚未到账，尚需 " + remaining + " 秒方可再次领取",
                            "cooldown", remaining));
        }

        SectRankEnum rank = SectRankEnum.ofTotalLevel(u.getTotalLevel() != null ? u.getTotalLevel() : 1);
        int stones = u.getSpiritStone() != null ? u.getSpiritStone() : 0;
        int qi = u.getSpiritQi() != null ? u.getSpiritQi() : 0;
        u.setSpiritStone(stones + rank.getStoneSalary());
        u.setSpiritQi(qi + rank.getQiSalary());
        u.setLastSalaryAt(LocalDateTime.now());
        userRepository.save(u);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("message", "领取「" + rank.getName() + "」月俸：" + rank.getStoneSalary() + " 灵石、" + rank.getQiSalary() + " 灵气");
        resp.put("stoneSalary", rank.getStoneSalary());
        resp.put("qiSalary", rank.getQiSalary());
        resp.put("spiritStone", u.getSpiritStone());
        resp.put("spiritQi", u.getSpiritQi());
        resp.put("cooldown", SectRankEnum.SALARY_COOLDOWN_SEC);
        return ResponseEntity.ok(resp);
    }

    // ===== 私有工具 =====

    /** 构造当前用户的门派信息视图 */
    private Map<String, Object> buildMySect(User u) {
        Map<String, Object> resp = new LinkedHashMap<>();
        boolean joined = u.getSect() != null && !u.getSect().isBlank();
        resp.put("joined", joined);
        if (!joined) {
            resp.put("sectName", null);
            resp.put("sectRank", null);
            return resp;
        }
        Sect s = sectRepository.findAll().stream()
                .filter(x -> x.getName().equals(u.getSect()))
                .findFirst().orElse(null);
        SectRankEnum rank = SectRankEnum.ofTotalLevel(u.getTotalLevel() != null ? u.getTotalLevel() : 1);
        resp.put("sectName", u.getSect());
        resp.put("sectRank", rank.getName());
        resp.put("stoneSalary", rank.getStoneSalary());
        resp.put("qiSalary", rank.getQiSalary());
        resp.put("salaryCd", cooldownRemaining(u.getLastSalaryAt(), SectRankEnum.SALARY_COOLDOWN_SEC));
        if (s != null) {
            resp.put("camp", s.getCamp());
            resp.put("region", s.getRegion());
            resp.put("desc", s.getDesc());
            resp.put("signature", s.getSignature());
            resp.put("representative", s.getRepresentative());
            resp.put("places", buildPlaces(s));
        }
        return resp;
    }

    /** 构造门派三大场所（藏宝阁 / 丹房 / 练功房） */
    private List<Map<String, String>> buildPlaces(Sect s) {
        List<Map<String, String>> places = new ArrayList<>();

        Map<String, String> treasure = new LinkedHashMap<>();
        treasure.put("key", "treasure");
        treasure.put("name", "藏宝阁");
        treasure.put("icon", "宝");
        treasure.put("desc", "珍藏「" + (s.getSignature() != null ? s.getSignature() : "诸般法宝") + "」等宗门法宝，可前往法宝阁兑换趁手法宝。");
        places.add(treasure);

        Map<String, String> alchemy = new LinkedHashMap<>();
        alchemy.put("key", "alchemy");
        alchemy.put("name", "丹房");
        alchemy.put("icon", "丹");
        alchemy.put("desc", "宗门丹师所驻，炉火不熄，可炼灵丹增补灵气、延年益寿。");
        places.add(alchemy);

        Map<String, String> practice = new LinkedHashMap<>();
        practice.put("key", "practice");
        practice.put("name", "练功房");
        practice.put("icon", "功");
        practice.put("desc", "灵气汇聚之地，弟子可静心打坐，运转" + (s.getSignature() != null ? "《" + s.getSignature() + "》" : "本门心法") + "，稳步精进修为。");
        places.add(practice);

        return places;
    }

    private boolean isBlank(String s) {
        return s == null || s.isEmpty();
    }

    private int campRank(String c) {
        int i = CAMP_ORDER.indexOf(c);
        return i < 0 ? 9 : i;
    }

    /** 计算某操作剩余的冷却秒数（从未操作过返回 0） */
    private long cooldownRemaining(LocalDateTime lastAt, long cooldownSec) {
        if (lastAt == null) return 0;
        long elapsed = Duration.between(lastAt, LocalDateTime.now()).getSeconds();
        return Math.max(0, cooldownSec - elapsed);
    }

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
