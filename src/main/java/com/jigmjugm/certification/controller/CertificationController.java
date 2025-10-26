package com.jigmjugm.certification.controller;

import com.jigmjugm.certification.dto.*;
import com.jigmjugm.certification.service.CertificationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class CertificationController {

    private final CertificationService certificationService;

    private Long mockUserId() { return 1L; } // TODO: 실제 userid

    // 인증 등록
    @PostMapping("/rounds/{roundId}/certifications")
    @Operation(summary = "인증 등록 (회차당 1건)")
    public ResponseEntity<CertificationResponse> create(
            @PathVariable Long roundId,
            @Valid @RequestBody CertificationCreateRequest req) {
        CertificationResponse res = certificationService.create(mockUserId(), roundId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(res);
    }

    // 인증 수정
    @PatchMapping("/certifications/{certificationId}")
    @Operation(summary = "인증 수정 (본인만 가능)")
    public ResponseEntity<Map<String, Object>> update(
            @PathVariable Long certificationId,
            @RequestBody CertificationUpdateRequest req) {
        boolean updated = certificationService.updateReturnFlag(mockUserId(), certificationId, req);
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    // 인증 삭제
    @DeleteMapping("/certifications/{certificationId}")
    @Operation(summary = "인증 삭제 (본인만 가능)")
    public ResponseEntity<Void> delete(@PathVariable Long certificationId) {
        certificationService.delete(mockUserId(), certificationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/certifications/{certificationId}")
    @Operation(summary = "인증 상세 조회")
    public ResponseEntity<CertificationDetailResponse> get(@PathVariable Long certificationId) {
        return ResponseEntity.ok(certificationService.getDetail(certificationId));
    }

    @GetMapping("/me/challenges/{challengeId}/certifications")
    @Operation(summary = "내 인증 목록 (회차 기준)")
    public ResponseEntity<MyCertificationListResponse> myList(
            @PathVariable Long challengeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "true") boolean includeNotCertified) {

        var myCertificationListResponse = certificationService.myCertificationList(mockUserId(), challengeId, page, size, sort, includeNotCertified);
        return ResponseEntity.ok(myCertificationListResponse);
    }
}

