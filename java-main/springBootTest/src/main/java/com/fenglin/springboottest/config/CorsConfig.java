package com.fenglin.springboottest.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 全局 CORS：放行前端 Vite 开发服务器（LAN 访问示例 http://192.168.68.100:5173）的跨域请求。
 * 前端注册请求带 credentials: 'include'，因此必须 allowCredentials(true)，
 * 且不能用 "*" 作为 origin，这里用 allowedOriginPatterns("*") 兼容本地多端口。
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}
