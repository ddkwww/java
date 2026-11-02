package com.fenglin.springboottest.controller;

import com.fenglin.springboottest.config.TokenStore;
import com.fenglin.springboottest.entity.User;
import com.fenglin.springboottest.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 排行榜接口（鉴权复用共享 TokenStore）：
 *   GET  /api/ranking/tianjiao
 *       Header Authorization: Bearer <token>
 *       -> 按修为总层数(totalLevel)降序取前 100 名（境界相同按注册时间早者优先）：
 *          [ { rank, username, realm, totalLevel } ]
 */
@RestController
@RequestMapping("/api/ranking")
public class RankingController {

    private final UserRepository userRepository;
    private final TokenStore tokenStore;

    public RankingController(UserRepository userRepository, TokenStore tokenStore) {
        this.userRepository = userRepository;
        this.tokenStore = tokenStore;
    }

    /** 天骄榜：取修为前 100 名 */
    @GetMapping("/tianjiao")
    public ResponseEntity<?> tianjiao(@RequestHeader(value = "Authorization", required = false) String auth) {
        User u = resolveUser(auth);
        if (u == null) return unauthorized();

        List<User> top = userRepository.findTop100ByOrderByTotalLevelDescCreatedAtAsc();
        List<Map<String, Object>> list = new ArrayList<>();
        int rank = 1;
        for (User x : top) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rank", rank++);
            m.put("username", x.getUsername());
            m.put("realm", x.getRealm());
            m.put("totalLevel", x.getTotalLevel() != null ? x.getTotalLevel() : 1);
            list.add(m);
        }
        return ResponseEntity.ok(list);
    }

    /** 从 Authorization 头解析并加载用户，失败返回 null */
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
