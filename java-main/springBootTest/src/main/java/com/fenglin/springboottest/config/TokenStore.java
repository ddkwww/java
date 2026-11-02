package com.fenglin.springboottest.config;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    /** 用户名 -> 最后活跃时间戳(ms)，用于判定「在线」 */
    private final ConcurrentHashMap<String, Long> online = new ConcurrentHashMap<>();

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

    /**
     * 刷新某 token 对应用户的在线时间戳（由 OnlineFilter 在每次有效请求时调用）。
     */
    public void touch(String token) {
        if (token == null) return;
        String username = tokenStore.get(token);
        if (username != null) online.put(username, System.currentTimeMillis());
    }

    /**
     * 返回在 windowMs 毫秒内活跃、且不等于 exclude 的用户名列表。
     * 用于「对战」页列出当前登录在线的人。
     */
    public List<String> getOnline(String exclude, long windowMs) {
        long now = System.currentTimeMillis();
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Long> e : online.entrySet()) {
            if (now - e.getValue() <= windowMs && !e.getKey().equals(exclude)) {
                result.add(e.getKey());
            }
        }
        return result;
    }
}
