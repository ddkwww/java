package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.config.TokenStore;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.game.MonopolyRoomManager;
import com.fenglin.springboottest.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 小游戏接口（鉴权复用共享 TokenStore，与拍卖行一致）：
 *   GET  /api/game/monopoly/state                       -> 牌桌快照（按登录用户自动定位其所在/可加入牌桌）
 *   POST /api/game/monopoly/join                        -> 报名（扣 ENTRY_FEE 灵石进奖池；满 4 人开一局，其余人另开新局）
 *   POST /api/game/monopoly/leave                       -> 报名后退出（退回灵石，仅等待阶段）
 *   POST /api/game/monopoly/start                       -> 满 4 人开局
 *   POST /api/game/monopoly/roll                        -> 当前玩家掷骰（服务器权威移动+事件）
 *   POST /api/game/monopoly/reset                       -> 结算后重置牌桌，开启下一局
 *
 * 多房间说明：后端按登录用户自动路由到其所在的牌桌（MonopolyRoomManager），
 * 前端无需传递 roomId；未报名者会看到一张可加入的牌桌快照。
 */
@RestController
@RequestMapping("/api/game/monopoly")
public class GameController {

    private final MonopolyRoomManager manager;
    private final TokenStore tokenStore;
    private final UserRepository userRepository;

    public GameController(MonopolyRoomManager manager, TokenStore tokenStore, UserRepository userRepository) {
        this.manager = manager;
        this.tokenStore = tokenStore;
        this.userRepository = userRepository;
    }

    /** 统一处理：快照原样返回；若含 error 字段则返回 400 */
    private ResponseEntity<?> okOrBad(Map<String, Object> res) {
        if (res != null && res.containsKey("error")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new LinkedHashMap<>(Map.of("message", res.get("error"))));
        }
        return ResponseEntity.ok(res);
    }

    @GetMapping("/state")
    public ResponseEntity<?> state(@RequestHeader(value = "Authorization", required = false) String auth) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();
        return okOrBad(manager.state(u.getUsername()));
    }

    @PostMapping("/join")
    public ResponseEntity<?> join(@RequestHeader(value = "Authorization", required = false) String auth) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();
        return okOrBad(manager.join(u.getUsername(), u));
    }

    @PostMapping("/leave")
    public ResponseEntity<?> leave(@RequestHeader(value = "Authorization", required = false) String auth) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();
        return okOrBad(manager.leave(u.getUsername(), u));
    }

    @PostMapping("/start")
    public ResponseEntity<?> start(@RequestHeader(value = "Authorization", required = false) String auth) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();
        return okOrBad(manager.start(u.getUsername(), u));
    }

    @PostMapping("/roll")
    public ResponseEntity<?> roll(@RequestHeader(value = "Authorization", required = false) String auth) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();
        return okOrBad(manager.roll(u.getUsername()));
    }

    @PostMapping("/reset")
    public ResponseEntity<?> reset(@RequestHeader(value = "Authorization", required = false) String auth) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();
        return okOrBad(manager.reset(u.getUsername()));
    }

    /** 占领/升级选择：body { action: "occupy"|"skip"|"upgrade" } */
    @PostMapping("/decide")
    public ResponseEntity<?> decide(@RequestHeader(value = "Authorization", required = false) String auth,
                                    @RequestBody Map<String, Object> body) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();
        String action = body.get("action") != null ? body.get("action").toString() : null;
        if (action == null || action.isBlank()) return bad("缺少操作类型");
        return okOrBad(manager.decide(u.getUsername(), action));
    }

    /** 发起结束投票：任意在场玩家可提议 */
    @PostMapping("/propose-end")
    public ResponseEntity<?> proposeEnd(@RequestHeader(value = "Authorization", required = false) String auth) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();
        return okOrBad(manager.proposeEnd(u.getUsername()));
    }

    /** 结束投票表态：body { agree: true|false } */
    @PostMapping("/vote-end")
    public ResponseEntity<?> voteEnd(@RequestHeader(value = "Authorization", required = false) String auth,
                                    @RequestBody Map<String, Object> body) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();
        Object ag = body.get("agree");
        boolean agree = ag != null && Boolean.parseBoolean(ag.toString());
        return okOrBad(manager.voteEnd(u.getUsername(), agree));
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
