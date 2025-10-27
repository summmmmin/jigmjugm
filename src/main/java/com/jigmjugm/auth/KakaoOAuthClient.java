package com.jigmjugm.auth;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class KakaoOAuthClient {
    @Value("${app.kakao.userinfo-url}") private String userinfoUrl;
    private final org.springframework.web.reactive.function.client.WebClient webClient = WebClient.create();

    public KakaoProfile getProfile(String kakaoAccessToken) {
        return webClient.get()
                .uri(userinfoUrl)
                .header("Authorization", "Bearer " + kakaoAccessToken)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, r-> Mono.error(new RuntimeException("UNAUTHORIZED")))
                .onStatus(HttpStatusCode::is5xxServerError, r->Mono.error(new RuntimeException("UPSTREAM_ERROR")))
                .bodyToMono(KakaoProfile.class)
                .block();
    }

    @Getter
    @Setter
    public static class KakaoProfile {
        private Long id;
        private Map<String,Object> properties;
        public String nickname() {
            Object props = properties==null? null : properties.get("nickname");
            return props==null? ("user_"+id) : String.valueOf(props);
        }
    }
}

