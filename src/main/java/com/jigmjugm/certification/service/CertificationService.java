package com.jigmjugm.certification.service;

import com.jigmjugm.certification.domain.Certification;
import com.jigmjugm.certification.dto.CertificationCreateRequest;
import com.jigmjugm.certification.dto.CertificationResponse;
import com.jigmjugm.certification.dto.CertificationUpdateRequest;
import com.jigmjugm.certification.repo.CertificationRepository;
import com.jigmjugm.challenge.domain.ChallengeParticipation;
import com.jigmjugm.challenge.domain.ChallengeRound;
import com.jigmjugm.challenge.repo.ChallengeParticipationRepository;
import com.jigmjugm.challenge.repo.ChallengeRoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
public class CertificationService {

    private final ChallengeParticipationRepository participationRepository;
    private final ChallengeRoundRepository roundRepository;
    private final CertificationRepository certificationRepository;

    @Transactional
    public CertificationResponse create(Long userId, Long roundId, CertificationCreateRequest req) {
        ChallengeRound round = roundRepository.findById(roundId)
                .orElseThrow(() -> new NoSuchElementException("회차가 존재하지 않습니다."));

        // 사용자 참여 중인지 확인
        ChallengeParticipation participation = participationRepository
                .findByChallenge_ChallengeIdAndUserIdAndLeftAtIsNull(round.getChallenge().getChallengeId(), userId)
                .orElseThrow(() -> new IllegalStateException("참여 중인 챌린지가 아닙니다."));

        // 회차당 1건 제한
        if (certificationRepository.existsByParticipation_ParticipationIdAndRound_RoundId(
                participation.getParticipationId(), roundId)) {
            throw new IllegalStateException("이미 해당 회차에 인증이 존재합니다.");
        }

        Certification cert = Certification.builder()
                .participation(participation)
                .round(round)
                .amount(req.getAmount())
                .comment(req.getComment())
                .imageUrl(req.getImageUrl())
                .doubleYn(req.getDoubleYn())
                .certificationStatus("APPROVED") // 바로 승인
                .build();

        Certification saved = certificationRepository.save(cert);

        return new CertificationResponse(
                saved.getCertificationId(),
                participation.getParticipationId(),
                roundId,
                saved.getAmount(),
                saved.getDoubleYn(),
                saved.getCertificationStatus(),
                saved.getCertifiedAt()
        );
    }

    @Transactional
    public void update(Long userId, Long certId, CertificationUpdateRequest req) {
        Certification cert = certificationRepository
                .findByCertificationIdAndParticipation_UserId(certId, userId)
                .orElseThrow(() -> new SecurityException("수정 권한이 없습니다."));

        if (req.getAmount() != null) cert.setAmount(req.getAmount());
        if (req.getComment() != null) cert.setComment(req.getComment());
        if (req.getImageUrl() != null) cert.setImageUrl(req.getImageUrl());
        if (req.getDoubleYn() != null) cert.setDoubleYn(req.getDoubleYn());
        cert.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
    }

    @Transactional
    public void delete(Long userId, Long certId) {
        Certification cert = certificationRepository
                .findByCertificationIdAndParticipation_UserId(certId, userId)
                .orElseThrow(() -> new SecurityException("삭제 권한이 없습니다."));

        certificationRepository.delete(cert);
    }
}

