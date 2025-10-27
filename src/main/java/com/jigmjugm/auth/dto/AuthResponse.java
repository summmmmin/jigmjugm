package com.jigmjugm.auth.dto;

import com.jigmjugm.user.dto.UserProfileResponse;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private Integer refreshTokenExpiresIn; // seconds
    private String tokenType;              // "Bearer"
    private Integer expiresIn;             // access exp (seconds)
    private Boolean isNewUser;
    private UserProfileResponse user;
}
