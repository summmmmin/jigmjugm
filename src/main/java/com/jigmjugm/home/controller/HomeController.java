package com.jigmjugm.home.controller;

import com.jigmjugm.common.error.ApiErrorCode;
import com.jigmjugm.common.error.BusinessException;
import com.jigmjugm.home.dto.HomeResponse;
import com.jigmjugm.home.service.HomeService;
import com.jigmjugm.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1")
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/home")
    public ResponseEntity<HomeResponse> home(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "ALL") String categoryType,
            @RequestParam(defaultValue = "3") int limit,
            @RequestParam(defaultValue = "28") int periodDays,
            @RequestParam(defaultValue = "5") int minParticipants,
            @RequestParam(defaultValue = "3") int myUpcomingLimit
    ) {
        // 기본 파라미터 검증
        if (limit < 1 || limit > 10)
            throw new BusinessException(ApiErrorCode.INVALID_INPUT_VALUE, "limit는 1~10 사이여야 합니다.");
        if (myUpcomingLimit < 1 || myUpcomingLimit > 10)
            throw new BusinessException(ApiErrorCode.INVALID_INPUT_VALUE, "myUpcomingLimit는 1~10 사이여야 합니다.");
        if (periodDays < 7 || periodDays > 90)
            throw new BusinessException(ApiErrorCode.INVALID_INPUT_VALUE, "periodDays는 7~90 사이여야 합니다.");
        if (minParticipants < 1)
            throw new BusinessException(ApiErrorCode.INVALID_INPUT_VALUE, "minParticipants는 1 이상이어야 합니다.");

        // 로그인하지 않았을 경우 userId는 null
        Long userId = (principal != null) ? principal.getUserId() : null;

        var res = homeService.buildHome(
                userId, categoryType, limit, periodDays, minParticipants, myUpcomingLimit);

        return ResponseEntity.ok(res);
    }
}
