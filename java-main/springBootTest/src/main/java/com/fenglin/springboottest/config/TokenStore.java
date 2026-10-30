package com.fenglin.springboottest.config;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 共享会话 token 存储（内存态，重启即失效）。
 * 由 AuthController 负责签发/校验登录，其他需要鉴权的接口（如数值系统 StatsController）
 * 通过注入本组件复用同一份会话表，避免各自维护导致状态不一致。
 */
@Component
public class TokenStore {

    /** token -> 用户名 */
    private final ConcurrentHashMap<String, String> tokenStore = new ConcurrentHashMap<>();

    public void put(String token, String username) {
        tokenStore.put(token, username);
    }

    public boolean contains(String token) {
        return token != null && tokenStore.containsKey(token);
    }

    public String getUsername(String token) {
        return token == null ? null : tokenStore.get(token);
    }

    public void remove(String token) {
        if (token != null) {
            tokenStore.remove(token);
        }
    }
}
