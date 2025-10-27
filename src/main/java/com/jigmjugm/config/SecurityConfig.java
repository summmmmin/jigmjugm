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
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final RestAuthEntryPoint restAuthEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf->csrf.disable())
                .sessionManagement(sm->sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(fl->fl.disable())
                .httpBasic(hb->hb.disable())
                .oauth2Login(oauth->oauth.disable())
                .authorizeHttpRequests(auth->auth
                        .requestMatchers(
                                "/auth/kakao", "/api/v1/*",
                                "/swagger-ui/**", "/v3/api-docs/**", "/actuator/**", "/health/**"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex->ex
                        .authenticationEntryPoint(restAuthEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler)
                )
                .addFilterBefore(jwtFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}