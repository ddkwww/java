package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.config.TokenStore;
import com.fenglin.springboottest.entity.Friend;
import com.fenglin.springboottest.entity.Message;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.repository.FriendRepository;
import com.fenglin.springboottest.repository.MessageRepository;
import com.fenglin.springboottest.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 好友聊天接口（鉴权复用共享 TokenStore）：
 *   POST /api/chat/send     {to, content} -> 发送消息（仅限好友）
 *   GET  /api/chat/history?peer=   -> 与某好友的完整历史（时间正序）
 *   GET  /api/chat/unread          -> 每个好友发来的未读消息数（供红点/角标）
 *   POST /api/chat/read     {peer} -> 标记与某好友的会话已读
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final String ACCEPTED = "ACCEPTED";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final UserRepository userRepository;
    private final FriendRepository friendRepository;
    private final MessageRepository messageRepository;
    private final TokenStore tokenStore;

    public ChatController(UserRepository userRepository,
                          FriendRepository friendRepository,
                          MessageRepository messageRepository,
                          TokenStore tokenStore) {
        this.userRepository = userRepository;
        this.friendRepository = friendRepository;
        this.messageRepository = messageRepository;
        this.tokenStore = tokenStore;
    }

    /** 发送消息（仅限互为好友） */
    @PostMapping("/send")
    public ResponseEntity<?> send(@RequestHeader(value = "Authorization", required = false) String auth,
                                  @RequestBody Map<String, String> body) {
        User me = resolveUser(auth);
        if (me == null) return unauthorized();
        String to = body.get("to");
        String content = body.get("content");
        if (to == null || to.isBlank()) return bad("请指定发送对象");
        if (content == null || content.trim().isEmpty()) return bad("消息内容不能为空");
        content = content.trim();
        if (content.length() > 1000) return bad("消息过长");

        if (to.equals(me.getUsername())) return bad("不能给自己发消息");
        if (userRepository.findByUsername(to).isEmpty()) return bad("对方不存在");
        if (!isFriend(me.getUsername(), to)) return bad("你们还不是好友，无法私聊");

        Message m = new Message();
        m.setFromUser(me.getUsername());
        m.setToUser(to);
        m.setContent(content);
        messageRepository.save(m);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("from", m.getFromUser());
        out.put("to", m.getToUser());
        out.put("content", m.getContent());
        out.put("time", m.getCreatedAt() != null ? m.getCreatedAt().format(FMT) : "");
        return ResponseEntity.ok(out);
    }

    /** 与某好友的历史消息（时间正序） */
    @GetMapping("/history")
    public ResponseEntity<?> history(@RequestHeader(value = "Authorization", required = false) String auth,
                                     @RequestParam String peer) {
        User me = resolveUser(auth);
        if (me == null) return unauthorized();
        if (peer == null || peer.isBlank()) return bad("缺少对方用户名");

        List<Message> msgs = messageRepository
                .findByFromUserAndToUserOrFromUserAndToUserOrderByCreatedAtAsc(
                        me.getUsername(), peer, peer, me.getUsername());

        List<Map<String, Object>> list = new ArrayList<>();
        for (Message m : msgs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("from", m.getFromUser());
            item.put("to", m.getToUser());
            item.put("content", m.getContent());
            item.put("time", m.getCreatedAt() != null ? m.getCreatedAt().format(FMT) : "");
            item.put("mine", m.getFromUser().equals(me.getUsername()));
            list.add(item);
        }
        return ResponseEntity.ok(Map.of("messages", list));
    }

    /** 每个好友发来的未读消息数（供导航红点/好友角标） */
    @GetMapping("/unread")
    public ResponseEntity<?> unread(@RequestHeader(value = "Authorization", required = false) String auth) {
        User me = resolveUser(auth);
        if (me == null) return unauthorized();

        List<Message> incoming = messageRepository.findByToUserAndReadFalseOrderByCreatedAtAsc(me.getUsername());
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (Message m : incoming) {
            counts.merge(m.getFromUser(), 1, Integer::sum);
        }
        int total = incoming.size();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("counts", counts);
        out.put("total", total);
        return ResponseEntity.ok(out);
    }

    /** 标记与某好友的会话已读（把对方发给我的未读消息全部置为已读） */
    @PostMapping("/read")
    public ResponseEntity<?> read(@RequestHeader(value = "Authorization", required = false) String auth,
                                  @RequestBody Map<String, String> body) {
        User me = resolveUser(auth);
        if (me == null) return unauthorized();
        String peer = body.get("peer");
        if (peer == null || peer.isBlank()) return bad("缺少对方用户名");

        messageRepository.markRead(me.getUsername(), peer);
        return ResponseEntity.ok(Map.of("message", "ok"));
    }

    // ===== 内部辅助 =====

    private boolean isFriend(String a, String b) {
        Optional<Friend> f1 = friendRepository.findByOwnerAndFriend(a, b);
        if (f1.isPresent() && ACCEPTED.equals(f1.get().getStatus())) return true;
        Optional<Friend> f2 = friendRepository.findByOwnerAndFriend(b, a);
        return f2.isPresent() && ACCEPTED.equals(f2.get().getStatus());
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
