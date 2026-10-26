package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 接收 fanren-login-vue 鉴权请求（契约见前端 src/api/auth.js）：
 *   GET  /api/captcha  -> { captchaId, image(data:image/png;base64,...) }
 *   POST /api/register -> { username, password, captchaId, captcha,
 *                            gender, age, spiritRoot, physique, avatar,
 *                            initialTreasure, initialGongfa } -> { token, username }
 *     人物初始化资料：性别/年龄(10–99)/灵根必填；修为固定「练气一层」
 *     失败：409 用户名已存在 / 400 验证码错误·失效 或 密码不合规(5–15 位) 或 资料不合规
 *   POST /api/login    -> { username, password } -> { token, username, permission }；失败 401
 *   POST /api/admin/login -> { username, password } -> 仅 permission=0 可成功，否则 403
 *   GET  /api/me       -> Header Authorization: Bearer <token> -> { username, realm, level, permission }
 *   POST /api/logout   -> Header Authorization: Bearer <token> -> { ok:true }
 * 会话 token 以内存态 Map 维护（重启即失效），适合本地演示与联调。
 */
@RestController
@RequestMapping("/api")
public class AuthController {

    private final UserRepository userRepository;

    /** captchaId -> 验证码条目（含过期时间） */
    private final ConcurrentHashMap<String, CaptchaEntry> captchaStore = new ConcurrentHashMap<>();
    /** token -> 用户名，内存态会话表（重启即失效，适合本地演示） */
    private final ConcurrentHashMap<String, String> tokenStore = new ConcurrentHashMap<>();
    private final SecureRandom random = new SecureRandom();

    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // 去除易混字符 0/O/1/I/L
    private static final long CAPTCHA_TTL_MS = 5 * 60 * 1000L;             // 5 分钟有效
    private static final int PW_MIN = 5;
    private static final int PW_MAX = 15;
    /** 头像字段最大长度（默认键名或 128×128 JPEG dataURL，约 50KB 内；超长视为超大图拒绝） */
    private static final int AVATAR_MAX_LEN = 300 * 1024;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** 启动后打印联调地址，并播种管理员账号，方便前端对接 */
    @PostConstruct
    public void banner() {
        try {
            String host = InetAddress.getLocalHost().getHostAddress();
            System.out.println("[Auth] 鉴权接口已就绪：");
            System.out.println("       注册 POST http://" + host + ":<port>/api/register");
            System.out.println("       登录 POST http://" + host + ":<port>/api/login");
            System.out.println("       后台登录 POST http://" + host + ":<port>/api/admin/login (仅管理员)");
            System.out.println("       资料 GET  http://" + host + ":<port>/api/me   (Bearer token)");
            System.out.println("       登出 POST http://" + host + ":<port>/api/logout (Bearer token)");
        } catch (Exception ignored) {
        }
        seedAdmin();
    }

    /**
     * 数据初始化：
     * 1) 历史存在 permission 为 NULL 的用户回补为 1（默认普通用户）；
     * 2) 若不存在任何管理员(permission=0)，自动播种 admin / admin123（明文）。
     */
    private void seedAdmin() {
        try {
            for (User u : userRepository.findByPermissionIsNull()) {
                u.setPermission(1);
                userRepository.save(u);
            }
            if (!userRepository.existsByPermission(0)) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setPassword("admin123"); // 明文存储
                admin.setPermission(0);
                userRepository.save(admin);
                System.out.println("[Auth] 已自动播种管理员账号：admin / admin123");
            }
        } catch (Exception e) {
            System.out.println("[Auth] 管理员播种失败（请检查数据库）：" + e.getMessage());
        }
    }

    @GetMapping("/captcha")
    public Map<String, String> captcha() throws Exception {
        String code = randomCode(4);
        String captchaId = UUID.randomUUID().toString();
        captchaStore.put(captchaId, new CaptchaEntry(code, Instant.now().toEpochMilli() + CAPTCHA_TTL_MS));

        Map<String, String> resp = new HashMap<>(2);
        resp.put("captchaId", captchaId);
        resp.put("image", "data:image/png;base64," + drawCaptcha(code));
        return resp;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> body) {
        String username = str(body.get("username"));
        String password = str(body.get("password"));
        String captchaId = str(body.get("captchaId"));
        String captcha = str(body.get("captcha"));
        String gender = str(body.get("gender"));
        Integer age = intOrNull(body.get("age"));
        String spiritRoot = str(body.get("spiritRoot"));
        String physique = str(body.get("physique"));
        String avatar = str(body.get("avatar"));
        String initialTreasure = str(body.get("initialTreasure"));
        String initialGongfa = str(body.get("initialGongfa"));

        // 1) 验证码校验（一次性 + 有效期）
        CaptchaEntry entry = (captchaId == null) ? null : captchaStore.get(captchaId);
        if (entry == null) {
            return bad("验证码已失效，请刷新");
        }
        captchaStore.remove(captchaId); // 一次性使用，无论成败都作废
        if (Instant.now().toEpochMilli() > entry.expireAt) {
            return bad("验证码已过期，请刷新");
        }
        if (captcha == null || !entry.code.equalsIgnoreCase(captcha.trim())) {
            return bad("验证码错误");
        }

        // 2) 参数校验
        if (username == null || username.trim().isEmpty()) {
            return bad("用户名不能为空");
        }
        if (password == null || password.length() < PW_MIN || password.length() > PW_MAX) {
            return bad("密码长度需为 " + PW_MIN + "–" + PW_MAX + " 位");
        }

        // 3) 人物初始化资料校验（性别/年龄/灵根必填；修为由服务端固定为「练气一层」）
        if (gender == null || !("男".equals(gender) || "女".equals(gender))) {
            return bad("请择定男修或女修");
        }
        if (age == null || age < 10 || age > 99) {
            return bad("年龄需在 10–99 之间");
        }
        if (spiritRoot == null || spiritRoot.trim().isEmpty()) {
            return bad("请择定灵根");
        }
        // 头像：默认键名(avatar:xxx)或上传的 dataURL；限制长度防止超大图入库
        if (avatar != null && avatar.length() > AVATAR_MAX_LEN) {
            return bad("头像图片过大，请更换后重试");
        }

        // 4) 用户名唯一性
        if (userRepository.existsByUsername(username.trim())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "用户名已存在"));
        }

        // 5) 落库（按需求密码以明文存储，注册用户默认权限=1 普通用户）
        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(password);
        user.setPermission(1);
        user.setGender(gender);
        user.setAge(age);
        user.setSpiritRoot(spiritRoot.trim());
        user.setPhysique(trimToNull(physique));
        user.setAvatar(trimToNull(avatar));
        user.setInitialTreasure(trimToNull(initialTreasure));
        user.setInitialGongfa(trimToNull(initialGongfa));
        user.setRealm("练气一层"); // 修为系统初始，只读不可修改
        userRepository.save(user);

        String token = issueToken(user.getUsername());
        return ResponseEntity.ok(Map.of("token", token, "username", user.getUsername()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || username.trim().isEmpty() || password == null) {
            return bad("用户名或密码不能为空");
        }

        User user = userRepository.findByUsername(username.trim()).orElse(null);
        if (user == null || !password.equals(user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "用户名或密码错误"));
        }

        String token = issueToken(user.getUsername());
        return ResponseEntity.ok(Map.of("token", token, "username", user.getUsername(), "permission", user.getPermission()));
    }

    @PostMapping("/admin/login")
    public ResponseEntity<?> adminLogin(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || username.trim().isEmpty() || password == null) {
            return bad("用户名或密码不能为空");
        }

        User user = userRepository.findByUsername(username.trim()).orElse(null);
        if (user == null || !password.equals(user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "用户名或密码错误"));
        }
        // 仅管理员(permission=0)可进入后台
        if (user.getPermission() == null || user.getPermission() != 0) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "权限不足，仅管理员可登录后台管理系统"));
        }

        String token = issueToken(user.getUsername());
        return ResponseEntity.ok(Map.of("token", token, "username", user.getUsername(), "permission", user.getPermission()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String auth) {
        String token = parseToken(auth);
        if (token == null || !tokenStore.containsKey(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "未登录或登录已失效"));
        }
        String username = tokenStore.get(token);
        User u = userRepository.findByUsername(username).orElse(null);
        Map<String, Object> profile = new HashMap<>(12);
        profile.put("username", username);
        profile.put("permission", u != null && u.getPermission() != null ? u.getPermission() : 1);
        if (u != null) {
            // 人物初始化资料（老账号可能为空，前端需兜底）
            profile.put("gender", u.getGender());
            profile.put("age", u.getAge());
            profile.put("spiritRoot", u.getSpiritRoot());
            profile.put("physique", u.getPhysique());
            profile.put("avatar", u.getAvatar());
            profile.put("initialTreasure", u.getInitialTreasure());
            profile.put("initialGongfa", u.getInitialGongfa());
            profile.put("realm", u.getRealm() != null ? u.getRealm() : "练气一层");
            profile.put("level", 1);
        } else {
            profile.put("realm", "练气期");
            profile.put("level", 1);
        }
        return ResponseEntity.ok(profile);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String auth) {
        String token = parseToken(auth);
        if (token != null) {
            tokenStore.remove(token);
        }
        return ResponseEntity.ok(Map.of("ok", true));
    }

    /** 从 Authorization: Bearer <token> 中解析 token，失败返回 null */
    private String parseToken(String auth) {
        if (auth == null || !auth.toLowerCase().startsWith("bearer ")) {
            return null;
        }
        String token = auth.substring(7).trim();
        return token.isEmpty() ? null : token;
    }

    /** 生成会话 token 并登记到内存表，返回 token */
    private String issueToken(String username) {
        String token = "fx_" + UUID.randomUUID().toString().replace("-", "");
        tokenStore.put(token, username);
        return token;
    }

    private ResponseEntity<Map<String, String>> bad(String msg) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", msg));
    }

    // ===== Map<String,Object> 取值辅助 =====

    /** 取字符串；空串归一为 null */
    private static String str(Object v) {
        if (v == null) return null;
        String s = String.valueOf(v).trim();
        return s.isEmpty() ? null : s;
    }

    /** 取整数（兼容 JSON 数字与字符串数字）；非法返回 null */
    private static Integer intOrNull(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(v).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 去除首尾空白；空白归一为 null */
    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private String randomCode(int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(CHARS.charAt(random.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    /** 纯 JDK 绘制 4 位验证码 PNG，返回 base64 */
    private String drawCaptcha(String code) throws Exception {
        int w = 110, h = 40;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(245, 247, 250));
        g.fillRect(0, 0, w, h);

        // 干扰线
        for (int i = 0; i < 5; i++) {
            g.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
            g.drawLine(random.nextInt(w), random.nextInt(h), random.nextInt(w), random.nextInt(h));
        }

        // 字符（带旋转 + 随机色）
        for (int i = 0; i < code.length(); i++) {
            g.setFont(new Font("Serif", Font.BOLD, 24));
            g.setColor(new Color(30 + random.nextInt(120), 30 + random.nextInt(120), 60 + random.nextInt(140)));
            int x = 16 + i * 22;
            int y = 28 + random.nextInt(4) - 2;
            double angle = (random.nextDouble() - 0.5) * 0.5;
            g.translate(x, y);
            g.rotate(angle);
            g.drawString(String.valueOf(code.charAt(i)), 0, 0);
            g.rotate(-angle);
            g.translate(-x, -y);
        }

        // 干扰点
        for (int i = 0; i < 30; i++) {
            g.setColor(new Color(random.nextInt(200), random.nextInt(200), random.nextInt(200)));
            g.fillRect(random.nextInt(w), random.nextInt(h), 2, 2);
        }
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }

    /** 验证码条目：正确码 + 过期时间戳 */
    private static class CaptchaEntry {
        final String code;
        final long expireAt;

        CaptchaEntry(String code, long expireAt) {
            this.code = code;
            this.expireAt = expireAt;
        }
    }
}
