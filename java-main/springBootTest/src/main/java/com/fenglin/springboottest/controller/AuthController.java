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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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
 *   POST /api/register -> { username, password, captchaId, captcha } -> { token, username }
 *     失败：409 用户名已存在 / 400 验证码错误·失效 或 密码不合规(5–15 位)
 *   POST /api/login    -> { username, password } -> { token, username }；失败 401
 *   GET  /api/me       -> Header Authorization: Bearer <token> -> { username, realm, level }
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

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /** 启动后打印联调地址，方便前端对接 */
    @PostConstruct
    public void banner() {
        try {
            String host = InetAddress.getLocalHost().getHostAddress();
            System.out.println("[Auth] 鉴权接口已就绪：");
            System.out.println("       注册 POST http://" + host + ":<port>/api/register");
            System.out.println("       登录 POST http://" + host + ":<port>/api/login");
            System.out.println("       资料 GET  http://" + host + ":<port>/api/me   (Bearer token)");
            System.out.println("       登出 POST http://" + host + ":<port>/api/logout (Bearer token)");
        } catch (Exception ignored) {
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
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String captchaId = body.get("captchaId");
        String captcha = body.get("captcha");

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

        // 3) 用户名唯一性
        if (userRepository.existsByUsername(username.trim())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "用户名已存在"));
        }

        // 4) 落库（密码以 SHA-256 哈希存储，避免明文）
        User user = new User();
        user.setUsername(username.trim());
        user.setPassword(hash(password));
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
        if (user == null || !user.getPassword().equals(hash(password))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "用户名或密码错误"));
        }

        String token = issueToken(user.getUsername());
        return ResponseEntity.ok(Map.of("token", token, "username", user.getUsername()));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@RequestHeader(value = "Authorization", required = false) String auth) {
        String token = parseToken(auth);
        if (token == null || !tokenStore.containsKey(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "未登录或登录已失效"));
        }
        String username = tokenStore.get(token);
        Map<String, Object> profile = new HashMap<>(4);
        profile.put("username", username);
        profile.put("realm", "炼气期");   // 前端展示字段；实体暂无该列，给出默认境界
        profile.put("level", 1);
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

    /** JDK 原生 SHA-256，无需额外依赖 */
    private String hash(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("密码哈希失败", e);
        }
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
