package com.jigmjugm.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KakaoTokenExchangeRequest {
    @NotBlank
    private String kakaoAccessToken;
}
