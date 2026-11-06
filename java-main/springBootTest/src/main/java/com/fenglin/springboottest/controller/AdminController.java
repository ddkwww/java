package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.config.TokenStore;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 后台管理系统接口（仅管理员 permission=0 可访问，鉴权复用共享 TokenStore）：
 *   GET  /api/admin/users?keyword=&page=&size=  -> 分页列出用户（按注册时间倒序），支持按用户名模糊搜索
 *   POST /api/admin/users/{id}                  -> 修改用户属性/数值/权限/重置密码
 *
 * 安全约束：管理员不得修改自己的 permission（防止唯一管理员把自己锁在后台外）。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int PW_MIN = 5;
    private static final int PW_MAX = 15;

    private final UserRepository userRepository;
    private final TokenStore tokenStore;

    public AdminController(UserRepository userRepository, TokenStore tokenStore) {
        this.userRepository = userRepository;
        this.tokenStore = tokenStore;
    }

    /** 分页列出用户（支持按用户名模糊搜索），仅管理员 */
    @GetMapping("/users")
    public ResponseEntity<?> listUsers(@RequestHeader(value = "Authorization", required = false) String auth,
                                       @RequestParam(defaultValue = "") String keyword,
                                       @RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "20") int size) {
        User admin = resolveAdmin(auth);
        if (admin == null) return forbidden();
        if (page < 1) page = 1;
        if (size < 1) size = 1;
        if (size > 100) size = 100;

        String kw = keyword == null ? "" : keyword.trim();
        Page<User> result;
        if (kw.isEmpty()) {
            result = userRepository.findAll(PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        } else {
            result = userRepository.findByUsernameContainingIgnoreCase(
                    kw, PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (User u : result.getContent()) {
            list.add(summary(u));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", list);
        out.put("total", result.getTotalElements());
        out.put("page", page);
        out.put("size", size);
        out.put("pages", result.getTotalPages());
        return ResponseEntity.ok(out);
    }

    /** 修改指定用户（属性/数值/权限/密码），仅管理员 */
    @PostMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@RequestHeader(value = "Authorization", required = false) String auth,
                                        @PathVariable Long id,
                                        @RequestBody Map<String, Object> body) {
        User admin = resolveAdmin(auth);
        if (admin == null) return forbidden();

        User u = userRepository.findById(id).orElse(null);
        if (u == null) return bad("用户不存在");

        // 禁止修改自己的权限（防自锁）
        if (u.getId().equals(admin.getId()) && body.containsKey("permission")) {
            return bad("不能修改自己的权限");
        }

        // 权限
        if (body.containsKey("permission")) {
            Integer perm = intOrNull(body.get("permission"));
            if (perm == null || (perm != 0 && perm != 1)) return bad("权限只能为 0(管理员) 或 1(普通用户)");
            u.setPermission(perm);
        }

        // 人物资料字段（可空字符串归一为 null 以允许清空）
        if (body.containsKey("gender")) u.setGender(str(body.get("gender")));
        if (body.containsKey("spiritRoot")) u.setSpiritRoot(str(body.get("spiritRoot")));
        if (body.containsKey("physique")) u.setPhysique(str(body.get("physique")));
        if (body.containsKey("realm")) u.setRealm(str(body.get("realm")));
        if (body.containsKey("sect")) u.setSect(str(body.get("sect")));
        if (body.containsKey("sectRank")) u.setSectRank(str(body.get("sectRank")));

        // 年龄（10–99 校验）
        if (body.containsKey("age")) {
            Integer age = intOrNull(body.get("age"));
            if (age == null || age < 10 || age > 99) return bad("年龄需在 10–99 之间");
            u.setAge(age);
        }

        // 修为总层数（1–81）
        if (body.containsKey("totalLevel")) {
            Integer lv = intOrNull(body.get("totalLevel"));
            if (lv == null || lv < 1 || lv > 81) return bad("修为总层数需在 1–81 之间");
            u.setTotalLevel(lv);
        }

        // 数值字段（灵石/灵气/寿元，均不得为负）
        if (body.containsKey("spiritStone")) {
            Integer v = intOrNull(body.get("spiritStone"));
            if (v == null || v < 0) return bad("灵石不能为负");
            u.setSpiritStone(v);
        }
        if (body.containsKey("spiritQi")) {
            Integer v = intOrNull(body.get("spiritQi"));
            if (v == null || v < 0) return bad("灵气不能为负");
            u.setSpiritQi(v);
        }
        if (body.containsKey("lifespan")) {
            Integer v = intOrNull(body.get("lifespan"));
            if (v == null || v < 0) return bad("寿元不能为负");
            u.setLifespan(v);
        }
        if (body.containsKey("ageUsed")) {
            Integer v = intOrNull(body.get("ageUsed"));
            if (v == null || v < 0) return bad("已耗天数不能为负");
            u.setAgeUsed(v);
        }

        // 重置密码（可选）
        String newPwd = str(body.get("password"));
        if (newPwd != null) {
            if (newPwd.length() < PW_MIN || newPwd.length() > PW_MAX) {
                return bad("新密码长度需为 " + PW_MIN + "–" + PW_MAX + " 位");
            }
            u.setPassword(sha256(newPwd));
        }

        userRepository.save(u);
        return ResponseEntity.ok(Map.of("message", "已更新用户 " + u.getUsername(), "user", summary(u)));
    }

    // ===== 内部辅助 =====

    private Map<String, Object> summary(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getId());
        m.put("username", u.getUsername());
        m.put("permission", u.getPermission() != null ? u.getPermission() : 1);
        m.put("gender", u.getGender());
        m.put("age", u.getAge());
        m.put("spiritRoot", u.getSpiritRoot());
        m.put("physique", u.getPhysique());
        m.put("realm", u.getRealm() != null ? u.getRealm() : "练气一层");
        m.put("totalLevel", u.getTotalLevel() != null ? u.getTotalLevel() : 1);
        m.put("spiritStone", u.getSpiritStone() != null ? u.getSpiritStone() : 0);
        m.put("spiritQi", u.getSpiritQi() != null ? u.getSpiritQi() : 0);
        m.put("ageUsed", u.getAgeUsed() != null ? u.getAgeUsed() : 0);
        m.put("lifespan", u.getLifespan() != null ? u.getLifespan() : 100);
        m.put("sect", u.getSect());
        m.put("sectRank", u.getSectRank());
        m.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().format(FMT) : "");
        return m;
    }

    /** 解析当前登录用户并校验管理员身份；非管理员返回 null */
    private User resolveAdmin(String auth) {
        String token = parseToken(auth);
        if (token == null || !tokenStore.contains(token)) return null;
        String username = tokenStore.getUsername(token);
        User u = userRepository.findByUsername(username).orElse(null);
        if (u == null) return null;
        if (u.getPermission() == null || u.getPermission() != 0) return null;
        return u;
    }

    private String parseToken(String auth) {
        if (auth == null || !auth.toLowerCase().startsWith("bearer ")) return null;
        String token = auth.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    private ResponseEntity<?> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "权限不足，仅管理员可访问"));
    }

    private ResponseEntity<?> bad(String msg) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", msg));
    }

    private static String str(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    private static Integer intOrNull(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return input;
        }
    }
}
