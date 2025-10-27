package com.jigmjugm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                    .allowedOrigins(
                        "http://localhost:5173",  // Vite React 개발 서버
                        "http://127.0.0.1:5173",  // Vite React 개발 서버 (대체)
                        "http://localhost:3000",  // CRA React 개발 서버
                        "http://localhost:3001"   // 대체 포트
                    )
                    .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD")
                    .allowedHeaders("*")
                    .exposedHeaders("Authorization")
                    .allowCredentials(false)
                    .maxAge(3600); // preflight 캐시 시간 (1시간)
            }
        };
    }
}