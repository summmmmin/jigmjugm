package com.jigmjugm.auth;

import com.jigmjugm.common.error.ApiErrorCode;
import com.jigmjugm.common.error.BusinessException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Exceptions;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class KakaoOAuthClient {
    @Value("${app.kakao.userinfo-url}") private String userinfoUrl;
    private final org.springframework.web.reactive.function.client.WebClient webClient = WebClient.create();

    public KakaoProfile getProfile(String kakaoAccessToken) {
        try {
            return webClient.get()
                    .uri(userinfoUrl)
                    .header("Authorization", "Bearer " + kakaoAccessToken)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError,
                            r -> Mono.error(new BusinessException(ApiErrorCode.UNAUTHORIZED, "유효하지 않은 카카오 토큰")))
                    .onStatus(HttpStatusCode::is5xxServerError,
                            r -> Mono.error(new BusinessException(ApiErrorCode.EXTERNAL_API_ERROR, "카카오 오류")))
                    .bodyToMono(KakaoProfile.class)
                    .timeout(Duration.ofSeconds(3))
                    .block();
        } catch (Exception e) {
            if (Exceptions.isRetryExhausted(e) || e.getCause() instanceof java.util.concurrent.TimeoutException) {
                throw new BusinessException(ApiErrorCode.GATEWAY_TIMEOUT, "카카오 응답 지연");
            }
            throw e;
        }
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

