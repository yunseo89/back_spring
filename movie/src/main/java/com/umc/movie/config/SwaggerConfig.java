package com.umc.movie.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "MovieLog API", version = "1.0",
        description = "공개 API로 가입·로그인한 뒤 Authorize에 accessToken을 입력하세요."))
@SecurityScheme(name = SwaggerConfig.BEARER_AUTH, type = SecuritySchemeType.HTTP,
        scheme = "bearer", bearerFormat = "JWT")
public class SwaggerConfig {
    public static final String BEARER_AUTH = "bearerAuth";
}
