package com.th.iqpcmbiz.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @ClassName SwaggerConfig
 * @Description swagger配置类
 * @Author 杨兴明
 * @Date 2025/4/18 10:59
 * @Version 1.0
 */

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "API 文档",
        version = "1.0",
        description = "接口说明"
))
public class SwaggerConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .contact(new Contact().name("th-ipqcm"))
                        .license(new License().name("Apache 2.0")));
    }
}
