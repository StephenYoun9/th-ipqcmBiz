package com.th.ipqcmbiz.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @ClassName CorsConfig
 * @Description WebMvc配置
 * @Author 杨兴明
 * @Date 2025/4/27 15:04
 * @Version 1.0
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // 匹配所有路径
                .allowedOrigins("http://localhost:63342") // 允许的前端域名
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 允许的请求方法:ml-citation{ref="2,6" data="citationList"}
                .allowedHeaders("*") // 允许所有请求头
                .allowCredentials(true) // 允许携带 Cookie
                .maxAge(3600); // 预检请求缓存时间（秒）
    }
}
