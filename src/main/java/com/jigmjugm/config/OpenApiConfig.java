package com.jigmjugm.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "jigmjugm",
                version = "1.0.0",
                description = "Savings Challenge API (Auth, User, Challenge, Round, Participation, Certification, Home, Stats)"
        ),
        security = { @SecurityRequirement(name = "bearerAuth") },
        servers = {
                // ✅ HTTPS 서버 명시 (Swagger가 이 URL을 기본으로 사용)
                @Server(url = "https://api.savenow.kr/api/v1", description = "Production API Server"),
                @Server(url = "http://localhost:8080/api/v1", description = "Local Development Server")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
