package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.config.TokenStore;
import com.fenglin.springboottest.entity.Friend;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.repository.FriendRepository;
import com.fenglin.springboottest.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 好友系统接口（鉴权复用共享 TokenStore）：
 *   GET  /api/friend/search?kw=     -> 按用户名模糊搜索（排除自己），含 relation 标记
 *   POST /api/friend/add            {target} -> 发送好友请求（或对方已请求则直接接受）
 *   GET  /api/friend/list           -> 我的好友（含 online 标记）
 *   GET  /api/friend/requests       -> 收到的待处理好友请求
 *   POST /api/friend/respond        {from, accept} -> 同意/拒绝
 *   POST /api/friend/remove         {target} -> 删除好友（双向）
 *
 * relation 取值：none / pending_out（我发的待确认）/ pending_in（对方发的待我确认）/ friend
 */
@RestController
@RequestMapping("/api/friend")
public class FriendController {

    private static final String PENDING = "PENDING";
    private static final String ACCEPTED = "ACCEPTED";

    private final UserRepository userRepository;
    private final FriendRepository friendRepository;
    private final TokenStore tokenStore;

    public FriendController(UserRepository userRepository,
                          FriendRepository friendRepository,
                          TokenStore tokenStore) {
        this.userRepository = userRepository;
        this.friendRepository = friendRepository;
        this.tokenStore = tokenStore;
    }

    /** 按用户名模糊搜索，返回 relation 标记便于前端禁用重复按钮 */
    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestHeader(value = "Authorization", required = false) String auth,
                                   @RequestParam String kw) {
        User me = resolveUser(auth);
        if (me == null) return unauthorized();
        String key = (kw == null ? "" : kw.trim()).toLowerCase();
        if (key.isEmpty()) return ResponseEntity.ok(Map.of("users", List.of()));

        List<User> matched = userRepository.findByUsernameContainingIgnoreCase(key);
        Set<String> online = new HashSet<>(tokenStore.getOnline(me.getUsername(), 30_000L));

        List<Map<String, Object>> list = matched.stream()
                .filter(u -> !u.getUsername().equals(me.getUsername()))
                .limit(20)
                .map(u -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("username", u.getUsername());
                    m.put("realm", u.getRealm());
                    m.put("totalLevel", u.getTotalLevel());
                    m.put("spiritRoot", u.getSpiritRoot());
                    m.put("physique", u.getPhysique());
                    m.put("online", online.contains(u.getUsername()));
                    m.put("relation", relationOf(me.getUsername(), u.getUsername()));
                    return m;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("users", list));
    }

    /** 发送好友请求 */
    @PostMapping("/add")
    public ResponseEntity<?> add(@RequestHeader(value = "Authorization", required = false) String auth,
                                @RequestBody Map<String, String> body) {
        User me = resolveUser(auth);
        if (me == null) return unauthorized();
        String target = body.get("target");
        if (target == null || target.isBlank()) return bad("请指定好友");
        if (target.equals(me.getUsername())) return bad("不能添加自己为好友");
        if (userRepository.findByUsername(target).isEmpty()) return bad("用户不存在");

        String rel = relationOf(me.getUsername(), target);
        if ("friend".equals(rel)) return bad("已经是好友了");
        if ("pending_out".equals(rel)) return bad("请求已发送，等待对方确认");

        // 对方已向你发过请求 -> 直接接受
        if ("pending_in".equals(rel)) {
            Friend incoming = friendRepository.findByOwnerAndFriend(target, me.getUsername()).orElse(null);
            if (incoming != null) {
                incoming.setStatus(ACCEPTED);
                friendRepository.save(incoming);
                ensureReciprocal(me.getUsername(), target);
            }
            return ResponseEntity.ok(Map.of("message", "已添加为好友"));
        }

        // 新建待确认请求
        Friend f = new Friend();
        f.setOwner(me.getUsername());
        f.setFriend(target);
        f.setStatus(PENDING);
        friendRepository.save(f);
        return ResponseEntity.ok(Map.of("message", "已发送好友请求"));
    }

    /** 我的好友列表（含在线标记） */
    @GetMapping("/list")
    public ResponseEntity<?> list(@RequestHeader(value = "Authorization", required = false) String auth) {
        User me = resolveUser(auth);
        if (me == null) return unauthorized();
        Set<String> online = new HashSet<>(tokenStore.getOnline(me.getUsername(), 30_000L));

        List<Map<String, Object>> list = friendRepository.findByOwnerAndStatus(me.getUsername(), ACCEPTED).stream()
                .map(f -> {
                    User u = userRepository.findByUsername(f.getFriend()).orElse(null);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("username", f.getFriend());
                    if (u != null) {
                        m.put("realm", u.getRealm());
                        m.put("totalLevel", u.getTotalLevel());
                        m.put("spiritRoot", u.getSpiritRoot());
                        m.put("physique", u.getPhysique());
                    }
                    m.put("online", online.contains(f.getFriend()));
                    return m;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("friends", list));
    }

    /** 收到的好友请求（被请求方轮询） */
    @GetMapping("/requests")
    public ResponseEntity<?> requests(@RequestHeader(value = "Authorization", required = false) String auth) {
        User me = resolveUser(auth);
        if (me == null) return unauthorized();
        List<Map<String, Object>> list = friendRepository.findByFriendAndStatus(me.getUsername(), PENDING).stream()
                .map(f -> {
                    User u = userRepository.findByUsername(f.getOwner()).orElse(null);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("from", f.getOwner());
                    if (u != null) {
                        m.put("realm", u.getRealm());
                        m.put("totalLevel", u.getTotalLevel());
                        m.put("spiritRoot", u.getSpiritRoot());
                        m.put("physique", u.getPhysique());
                    }
                    return m;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("requests", list));
    }

    /** 同意 / 拒绝 收到的好友请求 */
    @PostMapping("/respond")
    public ResponseEntity<?> respond(@RequestHeader(value = "Authorization", required = false) String auth,
                                    @RequestBody Map<String, Object> body) {
        User me = resolveUser(auth);
        if (me == null) return unauthorized();
        String from = body.get("from") != null ? body.get("from").toString() : null;
        boolean accept = Boolean.TRUE.equals(body.get("accept"));
        if (from == null || from.isBlank()) return bad("缺少请求来源");

        Friend incoming = friendRepository.findByOwnerAndFriend(from, me.getUsername()).orElse(null);
        if (incoming == null || !PENDING.equals(incoming.getStatus())) return bad("无待处理的好友请求");

        if (accept) {
            incoming.setStatus(ACCEPTED);
            friendRepository.save(incoming);
            ensureReciprocal(me.getUsername(), from);
            return ResponseEntity.ok(Map.of("message", "已添加为好友", "status", ACCEPTED));
        } else {
            friendRepository.delete(incoming);
            return ResponseEntity.ok(Map.of("message", "已拒绝", "status", "REJECTED"));
        }
    }

    /** 删除好友（双向清除） */
    @PostMapping("/remove")
    public ResponseEntity<?> remove(@RequestHeader(value = "Authorization", required = false) String auth,
                                   @RequestBody Map<String, String> body) {
        User me = resolveUser(auth);
        if (me == null) return unauthorized();
        String target = body.get("target");
        if (target == null || target.isBlank()) return bad("请指定好友");
        friendRepository.deleteByOwnerAndFriend(me.getUsername(), target);
        friendRepository.deleteByOwnerAndFriend(target, me.getUsername());
        return ResponseEntity.ok(Map.of("message", "已删除好友"));
    }

    // ===== 内部辅助 =====

    /** 判定 me 与 other 的关系标记 */
    private String relationOf(String me, String other) {
        Friend asOwner = friendRepository.findByOwnerAndFriend(me, other).orElse(null);
        if (asOwner != null) {
            if (ACCEPTED.equals(asOwner.getStatus())) return "friend";
            if (PENDING.equals(asOwner.getStatus())) return "pending_out";
        }
        Friend asFriend = friendRepository.findByOwnerAndFriend(other, me).orElse(null);
        if (asFriend != null) {
            if (ACCEPTED.equals(asFriend.getStatus())) return "friend";
            if (PENDING.equals(asFriend.getStatus())) return "pending_in";
        }
        return "none";
    }

    /** 补一条反向好友记录（owner=me, friend=other, ACCEPTED），存在则跳过 */
    private void ensureReciprocal(String me, String other) {
        if (friendRepository.findByOwnerAndFriend(me, other).isEmpty()) {
            Friend r = new Friend();
            r.setOwner(me);
            r.setFriend(other);
            r.setStatus(ACCEPTED);
            friendRepository.save(r);
        }
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

    private ResponseEntity<?> bad(String msg) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", msg));
    }
}
