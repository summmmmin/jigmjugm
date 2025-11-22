package com.jigmjugm.config;

import com.jigmjugm.security.JwtAuthenticationFilter;
import com.jigmjugm.security.RestAccessDeniedHandler;
import com.jigmjugm.security.RestAuthEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final RestAuthEntryPoint restAuthEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(fl -> fl.disable())
                .httpBasic(hb -> hb.disable())
                // ✅ OAuth2 로그인 켬 (기본 엔드포인트 사용)
                .oauth2Login(oauth -> oauth
                        .authorizationEndpoint(a -> a.baseUri("/oauth2/authorization"))
                        .redirectionEndpoint(r -> r.baseUri("/login/oauth2/code/*"))
                )
                .authorizeHttpRequests(auth -> auth
                        // ✅ 테스트 단계: 전부 허용(끝나면 보호로 되돌리기)
                        .anyRequest().permitAll()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                );

        // ✅ 테스트 동안엔 JWT 필터를 임시로 빼거나,
        //    "Authorization 헤더가 없으면 그냥 통과" 하도록 구현해야 합니다.
        // http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        //cfg.setAllowedOrigins(List.of("http://localhost:8080", "https://"));
        //cfg.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        //cfg.setAllowedHeaders(List.of("*"));
        cfg.addAllowedOriginPattern("*"); // ✅ 모든 Origin 허용
        cfg.addAllowedMethod("*");        // ✅ 모든 HTTP 메서드 허용
        cfg.addAllowedHeader("*");        // ✅ 모든 헤더 허용
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}