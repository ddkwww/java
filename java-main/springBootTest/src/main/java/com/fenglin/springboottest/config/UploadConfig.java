package com.fenglin.springboottest.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 静态资源映射：将上传的功法图片目录对外暴露为 /images/manuals/**。
 * 优先从物理目录（UploadPaths.DIR，运行时可写）读取，回退到 classpath 种子图，
 * 保证开发模式与 jar 运行模式下新上传的图片都能正确访问。
 */
@Configuration
public class UploadConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(UploadPaths.URL_PREFIX + "/**")
                .addResourceLocations(
                        "file:" + UploadPaths.DIR.toString() + "/",
                        "classpath:/static/images/manuals/");
    }
}
