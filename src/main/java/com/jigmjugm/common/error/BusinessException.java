package com.jigmjugm.common.error;

import lombok.Getter;

import java.util.Map;

@Getter
public class BusinessException extends RuntimeException {
    private final ApiErrorCode errorCode;
    private final Map<String, Object> details; // 선택: 추가정보

    public BusinessException(ApiErrorCode errorCode) {
        super(errorCode.getCode());
        this.errorCode = errorCode;
        this.details = null;
    }
    public BusinessException(ApiErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }
    public BusinessException(ApiErrorCode errorCode, String message, Map<String, Object> details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

}
