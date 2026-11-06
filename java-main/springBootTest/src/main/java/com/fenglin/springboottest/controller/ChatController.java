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
 *   GET  /api/chat/history?peer=   -> 与某好友的完整历史（时间正序，含 type/stone）
 *   GET  /api/chat/unread          -> 每个好友发来的未读消息数（供红点/角标）
 *   POST /api/chat/read     {peer} -> 标记与某好友的会话已读
 *   POST /api/chat/gift     {to, amount} -> 向好友赠送灵石（扣己加彼，留 GIFT 消息）
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

    /** 向好友赠送灵石（扣发送方、加接收方，并以 GIFT 消息留存于双方聊天记录） */
    @PostMapping("/gift")
    public ResponseEntity<?> gift(@RequestHeader(value = "Authorization", required = false) String auth,
                                  @RequestBody Map<String, Object> body) {
        User me = resolveUser(auth);
        if (me == null) return unauthorized();

        Object toObj = body.get("to");
        Object amountObj = body.get("amount");
        if (toObj == null || String.valueOf(toObj).isBlank()) return bad("请指定赠送对象");
        String to = String.valueOf(toObj).trim();
        if (to.equals(me.getUsername())) return bad("不能赠送给自己");
        if (userRepository.findByUsername(to).isEmpty()) return bad("对方不存在");
        if (!isFriend(me.getUsername(), to)) return bad("你们还不是好友，无法赠送");

        int amount;
        try {
            amount = Integer.parseInt(String.valueOf(amountObj));
        } catch (Exception e) {
            return bad("赠送数量必须是正整数");
        }
        if (amount <= 0) return bad("赠送数量必须大于 0");

        int myStones = me.getSpiritStone() != null ? me.getSpiritStone() : 0;
        if (myStones < amount) return bad("灵石不足，当前仅 " + myStones + " 灵石");

        // 转账：扣发送方、加接收方（持久化）
        me.setSpiritStone(myStones - amount);
        User peer = userRepository.findByUsername(to).orElseThrow();
        int peerStones = peer.getSpiritStone() != null ? peer.getSpiritStone() : 0;
        peer.setSpiritStone(peerStones + amount);
        userRepository.save(me);
        userRepository.save(peer);

        // 留存一条赠送消息（双方历史可见，接收方会收到未读红点）
        Message m = new Message();
        m.setFromUser(me.getUsername());
        m.setToUser(to);
        m.setType("GIFT");
        m.setStone(amount);
        m.setContent("🎁 赠送 " + amount + " 灵石");
        messageRepository.save(m);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("message", "已赠送 " + amount + " 灵石给 " + to);
        out.put("spiritStone", me.getSpiritStone());
        out.put("to", to);
        out.put("amount", amount);
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
                .findConversation(me.getUsername(), peer, peer, me.getUsername());

        List<Map<String, Object>> list = new ArrayList<>();
        for (Message m : msgs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("from", m.getFromUser());
            item.put("to", m.getToUser());
            item.put("content", m.getContent());
            item.put("type", m.getType() != null ? m.getType() : "TEXT");
            item.put("stone", m.getStone());
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
