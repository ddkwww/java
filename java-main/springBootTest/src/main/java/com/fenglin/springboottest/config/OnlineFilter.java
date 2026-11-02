package com.fenglin.springboottest.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 在线状态过滤器：对任意携带有效 Bearer token 的请求，刷新该用户在 TokenStore 的
 * 活跃时间戳。这样「当前登录且活跃」的用户都会被对战页识别为在线，
 * 与具体停留在哪个页面无关。过滤器只做记录、不拦截请求。
 */
@Component
public class OnlineFilter extends OncePerRequestFilter {

    private final TokenStore tokenStore;

    public OnlineFilter(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.toLowerCase().startsWith("bearer ")) {
                String token = auth.substring(7).trim();
                if (!token.isEmpty()) {
                    tokenStore.touch(token);
                }
            }
        } catch (Exception ignored) {
            // 不应因在线记录失败而影响正常业务
        }
        chain.doFilter(request, response);
    }
}
