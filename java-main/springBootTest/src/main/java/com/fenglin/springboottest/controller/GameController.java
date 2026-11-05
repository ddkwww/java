package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.config.TokenStore;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.game.MonopolyRoom;
import com.fenglin.springboottest.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 小游戏接口（鉴权复用共享 TokenStore，与拍卖行一致）：
 *   GET  /api/game/monopoly/state                       -> 牌桌快照（报名/棋盘/回合/日志/结算）
 *   POST /api/game/monopoly/join                        -> 报名（扣 ENTRY_FEE 灵石进奖池）
 *   POST /api/game/monopoly/leave                       -> 报名后退出（退回灵石，仅等待阶段）
 *   POST /api/game/monopoly/start                       -> 满 4 人开局
 *   POST /api/game/monopoly/roll                        -> 当前玩家掷骰（服务器权威移动+事件）
 *   POST /api/game/monopoly/reset                       -> 结算后重置牌桌，开启下一局
 */
@RestController
@RequestMapping("/api/game/monopoly")
public class GameController {

    private final MonopolyRoom room;
    private final TokenStore tokenStore;
    private final UserRepository userRepository;

    public GameController(MonopolyRoom room, TokenStore tokenStore, UserRepository userRepository) {
        this.room = room;
        this.tokenStore = tokenStore;
        this.userRepository = userRepository;
    }

    @GetMapping("/state")
    public ResponseEntity<?> state(@RequestHeader(value = "Authorization", required = false) String auth) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();
        return ResponseEntity.ok(room.snapshot(System.currentTimeMillis(), u.getUsername()));
    }

    @PostMapping("/join")
    public ResponseEntity<?> join(@RequestHeader(value = "Authorization", required = false) String auth) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();
        String err = room.join(u.getUsername(), u);
        if (err != null) return bad(err);
        return ResponseEntity.ok(room.snapshot(System.currentTimeMillis(), u.getUsername()));
    }

    @PostMapping("/leave")
    public ResponseEntity<?> leave(@RequestHeader(value = "Authorization", required = false) String auth) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();
        String err = room.leave(u.getUsername(), u);
        if (err != null) return bad(err);
        return ResponseEntity.ok(room.snapshot(System.currentTimeMillis(), u.getUsername()));
    }

    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestHeader(value = "Authorization", required = false) String auth) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();
        String err = room.start(u);
        if (err != null) return bad(err);
        return ResponseEntity.ok(room.snapshot(System.currentTimeMillis(), u.getUsername()));
    }

    @PostMapping("/roll")
    public ResponseEntity<?> roll(@RequestHeader(value = "Authorization", required = false) String auth) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();
        String err = room.roll(u.getUsername());
        if (err != null) return bad(err);
        return ResponseEntity.ok(room.snapshot(System.currentTimeMillis(), u.getUsername()));
    }

    @PostMapping("/reset")
    public ResponseEntity<?> reset(@RequestHeader(value = "Authorization", required = false) String auth) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();
        String err = room.reset();
        if (err != null) return bad(err);
        return ResponseEntity.ok(room.snapshot(System.currentTimeMillis(), u.getUsername()));
    }

    /** 占领/升级选择：body { action: "occupy"|"skip"|"upgrade" } */
    @PostMapping("/decide")
    public ResponseEntity<?> decide(@RequestHeader(value = "Authorization", required = false) String auth,
                                    @RequestBody Map<String, Object> body) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();
        String action = body.get("action") != null ? body.get("action").toString() : null;
        if (action == null || action.isBlank()) return bad("缺少操作类型");
        String err = room.decide(u.getUsername(), action);
        if (err != null) return bad(err);
        return ResponseEntity.ok(room.snapshot(System.currentTimeMillis(), u.getUsername()));
    }

    /** 提前结算：任意在场玩家可发起 */
    @PostMapping("/end")
    public ResponseEntity<?> end(@RequestHeader(value = "Authorization", required = false) String auth) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();
        String err = room.end(u.getUsername());
        if (err != null) return bad(err);
        return ResponseEntity.ok(room.snapshot(System.currentTimeMillis(), u.getUsername()));
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
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new LinkedHashMap<>(Map.of("message", msg)));
    }
}
