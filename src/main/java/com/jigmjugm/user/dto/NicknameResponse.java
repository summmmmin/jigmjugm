package com.jigmjugm.user.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NicknameResponse {
    private String nickname;
    private LocalDateTime changedAt;
}
