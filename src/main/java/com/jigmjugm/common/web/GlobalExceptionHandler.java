package com.jigmjugm.common.web;

import com.jigmjugm.common.error.ApiErrorCode;
import com.jigmjugm.common.error.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    /* ---------- helpers ---------- */

    private Map<String, Object> baseDetails(HttpStatus status, HttpServletRequest req) {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("status", status.value());
        d.put("error", status.getReasonPhrase());
        d.put("path", req.getRequestURI());
        d.put("timestamp", Instant.now().toString());
        String traceId = Optional.ofNullable(req.getHeader("X-Request-Id"))
                .orElse(UUID.randomUUID().toString());
        d.put("traceId", traceId);
        return d;
    }

    private ResponseEntity<ErrorResponse> build(ApiErrorCode code, String message,
                                                HttpStatus status, HttpServletRequest req,
                                                Map<String, Object> extraDetails) {
        Map<String, Object> details = baseDetails(status, req);
        if (extraDetails != null && !extraDetails.isEmpty()) {
            details.putAll(extraDetails);
        }
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(code.getCode(),
                        (message == null || message.isBlank()) ? code.getCode() : message,
                        details));
    }

    /* ---------- 1) 비즈니스 예외 ---------- */

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> onBusiness(BusinessException ex, HttpServletRequest req) {
        var code = ex.getErrorCode();
        var status = code.getHttpStatus();
        Map<String, Object> extra = ex.getDetails();
        log.debug("[Business] {} {}", code.getCode(), ex.getMessage());
        return build(code, ex.getMessage(), status, req, extra);
    }

    /* ---------- 2) 검증/바인딩 ---------- */

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<ErrorResponse> onValidation(Exception ex, HttpServletRequest req) {
        ApiErrorCode code = ApiErrorCode.INVALID_INPUT_VALUE;
        HttpStatus status = code.getHttpStatus();

        List<Map<String, Object>> errors = new ArrayList<>();

        if (ex instanceof MethodArgumentNotValidException manv) {
            manv.getBindingResult().getFieldErrors().forEach(fe -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("field", fe.getField());
                m.put("rejected", Objects.toString(fe.getRejectedValue(), "null"));
                m.put("message", fe.getDefaultMessage());
                errors.add(m);
            });
        } else if (ex instanceof BindException be) {
            be.getBindingResult().getFieldErrors().forEach(fe -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("field", fe.getField());
                m.put("message", fe.getDefaultMessage());
                errors.add(m);
            });
        }

        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("errors", errors);

        return build(code, "입력값 검증에 실패했습니다.", status, req, extra);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> onConstraintViolation(ConstraintViolationException ex, HttpServletRequest req) {
        ApiErrorCode code = ApiErrorCode.INVALID_INPUT_VALUE;
        HttpStatus status = code.getHttpStatus();

        List<Map<String, Object>> errors = ex.getConstraintViolations().stream()
                .map(v -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("parameter", v.getPropertyPath().toString());
                    m.put("message", v.getMessage());
                    return m;
                })
                .collect(Collectors.toList());

        Map<String, Object> extra = new LinkedHashMap<>();
        extra.put("errors", errors);

        return build(code, "입력값 검증에 실패했습니다.", status, req, extra);
    }

    /* ---------- 3) 필수/타입/본문 ---------- */

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> onMissingParam(MissingServletRequestParameterException ex, HttpServletRequest req) {
        return build(ApiErrorCode.MISSING_PARAMETER, ex.getMessage(),
                ApiErrorCode.MISSING_PARAMETER.getHttpStatus(), req, null);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> onMissingHeader(MissingRequestHeaderException ex, HttpServletRequest req) {
        return build(ApiErrorCode.MISSING_HEADER, ex.getMessage(),
                ApiErrorCode.MISSING_HEADER.getHttpStatus(), req, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> onTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        Map<String, Object> extra = Map.of(
                "parameter", ex.getName(),
                "requiredType", ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        return build(ApiErrorCode.TYPE_MISMATCH, ex.getMessage(),
                ApiErrorCode.TYPE_MISMATCH.getHttpStatus(), req, extra);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> onNotReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        String msg = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : "요청 본문을 읽을 수 없습니다.";
        return build(ApiErrorCode.MESSAGE_NOT_READABLE, msg,
                ApiErrorCode.MESSAGE_NOT_READABLE.getHttpStatus(), req, null);
    }

    /* ---------- 4) 권한/리소스 ---------- */

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> onAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
        return build(ApiErrorCode.FORBIDDEN, "접근 권한이 없습니다.",
                ApiErrorCode.FORBIDDEN.getHttpStatus(), req, null);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ErrorResponse> onNotFound(NoSuchElementException ex, HttpServletRequest req) {
        return build(ApiErrorCode.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다.",
                ApiErrorCode.NOT_FOUND.getHttpStatus(), req, null);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> onResponseStatus(ResponseStatusException ex, HttpServletRequest req) {
        HttpStatus s = HttpStatus.valueOf(ex.getStatusCode().value());
        ApiErrorCode code = switch (s) {
            case BAD_REQUEST -> ApiErrorCode.INVALID_INPUT_VALUE;
            case NOT_FOUND -> ApiErrorCode.NOT_FOUND;
            case METHOD_NOT_ALLOWED -> ApiErrorCode.METHOD_NOT_ALLOWED;
            case CONFLICT -> ApiErrorCode.DATA_INTEGRITY;
            case FORBIDDEN -> ApiErrorCode.FORBIDDEN;
            default -> ApiErrorCode.INTERNAL_ERROR;
        };
        String reason = (ex.getReason() == null || ex.getReason().isBlank()) ? code.getCode() : ex.getReason();
        return build(code, reason, s, req, null);
    }

    /* ---------- 5) 무결성/중복 ---------- */

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> onDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest req) {
        // PostgreSQL 중복키: SQLState 23505
        Throwable root = getRootCause(ex);
        if (root instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
            // 메시지로 타겟 제약 구분 가능하면 상세 코드로 변환
            String msg = root.getMessage();
            if (msg != null && msg.toLowerCase().contains("duplicate") || msg.contains("uq") || msg.contains("unique")) {
                return build(ApiErrorCode.DATA_INTEGRITY, "무결성 제약 위반(중복키)", ApiErrorCode.DATA_INTEGRITY.getHttpStatus(), req, null);
            }
        }
        log.warn("[DataIntegrity] {}", root != null ? root.getMessage() : ex.getMessage());
        return build(ApiErrorCode.DATA_INTEGRITY, "무결성 제약 위반", ApiErrorCode.DATA_INTEGRITY.getHttpStatus(), req, null);
    }

    private Throwable getRootCause(Throwable t) {
        Throwable r = t;
        while (r.getCause() != null && r.getCause() != r) r = r.getCause();
        return r;
    }

    /* ---------- 6) 나머지 ---------- */

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> onAny(Exception ex, HttpServletRequest req) {
        log.error("[Unhandled] {}", ex.getMessage(), ex);
        return build(ApiErrorCode.INTERNAL_ERROR, "서버 내부 오류가 발생했습니다.",
                ApiErrorCode.INTERNAL_ERROR.getHttpStatus(), req, null);
    }
}
