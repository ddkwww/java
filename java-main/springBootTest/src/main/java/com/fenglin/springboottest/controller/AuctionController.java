package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.auction.AuctionSession;
import com.fenglin.springboottest.config.TokenStore;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 拍卖行接口（鉴权复用共享 TokenStore）：
 *   GET  /api/auction/status
 *       Header Authorization: Bearer <token>
 *       -> { state, nextOpenAt, openCount, yourStone,
 *            currentLot: { type,id,name,category,grade,realm,image,price,desc,origin,
 *                          bonusHp..bonusLifespan, currentPrice, highestBidder, lotEndsAt, bidLog[] },
 *            queue: [ 后续拍品... ], remaining,
 *            lastResult: { lotName, winner, price, success, passed, seq } | null }
 *       state=RUNNING 时 currentLot 有值；WAITING/ENDED 时当前无拍品，前端按 nextOpenAt 显示倒计时。
 *
 *   POST /api/auction/bid
 *       Header Authorization: Bearer <token>
 *       body { type:"manual"|"treasure", id:Long, amount:Int }
 *       -> 成功：返回最新 status 快照；失败 400（非当前拍品 / 加价档位非法 / 已是最高出价者）。
 *       未登录返回 401。
 */
@RestController
@RequestMapping("/api/auction")
public class AuctionController {

    private final AuctionSession session;
    private final TokenStore tokenStore;
    private final UserRepository userRepository;

    public AuctionController(AuctionSession session, TokenStore tokenStore, UserRepository userRepository) {
        this.session = session;
        this.tokenStore = tokenStore;
        this.userRepository = userRepository;
    }

    @GetMapping("/status")
    public ResponseEntity<?> status(@RequestHeader(value = "Authorization", required = false) String auth) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();
        long now = System.currentTimeMillis();
        int stone = u.getSpiritStone() != null ? u.getSpiritStone() : 0;
        return ResponseEntity.ok(session.snapshot(now, stone, u.getUsername()));
    }

    @PostMapping("/bid")
    public ResponseEntity<?> bid(@RequestHeader(value = "Authorization", required = false) String auth,
                                @RequestBody Map<String, Object> body) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();

        Object typeObj = body.get("type");
        Object idObj = body.get("id");
        Object amtObj = body.get("amount");
        if (typeObj == null || idObj == null || amtObj == null) {
            return bad("缺少参数 type / id / amount");
        }
        String type = String.valueOf(typeObj);
        long id;
        int amount;
        try {
            id = Long.parseLong(String.valueOf(idObj));
            amount = Integer.parseInt(String.valueOf(amtObj));
        } catch (NumberFormatException e) {
            return bad("参数格式错误（id/amount 需为整数）");
        }

        String err = session.placeBid(u.getUsername(), type, id, amount);
        if (err != null) return bad(err);

        long now = System.currentTimeMillis();
        int stone = u.getSpiritStone() != null ? u.getSpiritStone() : 0;
        return ResponseEntity.ok(session.snapshot(now, stone, u.getUsername()));
    }

    /**
     * 报名 / 取消参与下一场拍卖。
     * body { join: true|false }
     * 返回 { participated, participantCount } 供前端即时刷新。
     */
    @PostMapping("/participate")
    public ResponseEntity<?> participate(@RequestHeader(value = "Authorization", required = false) String auth,
                                        @RequestBody Map<String, Object> body) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();
        Object joinObj = body.get("join");
        boolean join = joinObj instanceof Boolean ? (Boolean) joinObj : "true".equals(String.valueOf(joinObj));
        session.setParticipate(u.getUsername(), join);
        return ResponseEntity.ok(Map.of(
                "participated", session.hasParticipated(u.getUsername()),
                "participantCount", session.participantCount()
        ));
    }

    // ===== 鉴权辅助（与 EconomyController 一致） =====

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
