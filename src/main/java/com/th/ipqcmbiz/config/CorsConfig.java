package com.th.ipqcmbiz.config;

import com.th.ipqcmbiz.interceptor.AuthInterceptor;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
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

    @Resource
    private AuthInterceptor authInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:63342")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/",
                        "/index.html",
                        "/login/**",
                        "/logout",
                        "/auth/heartbeat",
                        "/auth/check",
                        "/auth/server-starttime",
                        "/js/**",
                        "/css/**",
                        "/image/**",
                        "/images/**",
                        "/static/**"
                );
    }
}
