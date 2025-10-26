package com.jigmjugm.common.web;

import lombok.Builder;

import java.util.Map;

@Builder
public record ErrorResponse(
        String code,
        String message,
        Map<String, Object> details // 문서: additionalProperties true
) {
    public static ErrorResponse of(String code, String message, Map<String, Object> details) {
        return ErrorResponse.builder().code(code).message(message).details(details).build();
    }
}
