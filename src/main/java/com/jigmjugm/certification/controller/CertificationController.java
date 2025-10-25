package com.jigmjugm.certification.controller;

import com.jigmjugm.certification.dto.CertificationCreateRequest;
import com.jigmjugm.certification.dto.CertificationResponse;
import com.jigmjugm.certification.dto.CertificationUpdateRequest;
import com.jigmjugm.certification.service.CertificationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Void> update(
            @PathVariable Long certificationId,
            @RequestBody CertificationUpdateRequest req) {
        certificationService.update(mockUserId(), certificationId, req);
        return ResponseEntity.ok().build();
    }

    // 인증 삭제
    @DeleteMapping("/certifications/{certificationId}")
    @Operation(summary = "인증 삭제 (본인만 가능)")
    public ResponseEntity<Void> delete(@PathVariable Long certificationId) {
        certificationService.delete(mockUserId(), certificationId);
        return ResponseEntity.noContent().build();
    }
}

