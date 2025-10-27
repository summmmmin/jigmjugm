package com.jigmjugm.certification.service;

import com.jigmjugm.certification.domain.Certification;
import com.jigmjugm.certification.dto.*;
import com.jigmjugm.certification.repo.CertificationRepository;
import com.jigmjugm.challenge.domain.ChallengeParticipation;
import com.jigmjugm.challenge.domain.ChallengeRound;
import com.jigmjugm.challenge.repo.ChallengeParticipationRepository;
import com.jigmjugm.challenge.repo.ChallengeRoundRepository;
import com.jigmjugm.common.error.ApiErrorCode;
import com.jigmjugm.common.error.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

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
                .orElseThrow(() -> new BusinessException(ApiErrorCode.FORBIDDEN, "참여 중인 챌린지가 아닙니다."));

        // 회차당 1건 제한
        if (certificationRepository.existsByParticipation_ParticipationIdAndRound_RoundId(
                participation.getParticipationId(), roundId)) {
            throw new BusinessException(ApiErrorCode.ALREADY_CERTIFIED, "이미 해당 회차에 인증했습니다.");
        }

        Certification certification = Certification.builder()
                .participation(participation)
                .round(round)
                .amount(req.getAmount())
                .comment(req.getComment())
                .imageUrl(req.getImageUrl())
                .doubleYn(req.getDoubleYn())
                .certificationStatus("APPROVED") // 바로 승인
                .build();

        Certification saved = certificationRepository.save(certification);

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
                .orElseThrow(() -> new BusinessException(ApiErrorCode.FORBIDDEN, "수정 권한이 없습니다."));

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
                .orElseThrow(() -> new BusinessException(ApiErrorCode.FORBIDDEN, "삭제 권한이 없습니다."));

        certificationRepository.delete(cert);
    }

    @Transactional
    public boolean updateReturnFlag(Long userId, Long certId, CertificationUpdateRequest req) {
        var certification = certificationRepository
                .findByCertificationIdAndParticipation_UserId(certId, userId)
                .orElseThrow(() -> new BusinessException(ApiErrorCode.FORBIDDEN, "수정 권한이 없습니다."));
        boolean changed = false;
        if (req.getAmount() != null) {
            certification.setAmount(req.getAmount());
            changed = true;
        }
        if (req.getComment() != null) {
            certification.setComment(req.getComment());
            changed = true;
        }
        if (req.getImageUrl() != null) {
            certification.setImageUrl(req.getImageUrl());
            changed = true;
        }
        if (req.getDoubleYn() != null) {
            certification.setDoubleYn(req.getDoubleYn());
            changed = true;
        }
        if (changed) certification.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        return changed;
    }

    @Transactional(readOnly = true)
    public CertificationDetailResponse getDetail(Long certificationId) {
        var certification = certificationRepository.findById(certificationId)
                .orElseThrow(() -> new NoSuchElementException("인증이 존재하지 않습니다."));
        var participation = certification.getParticipation();
        var challenge = participation.getChallenge();

        // 누적 승인금액(해당 참여자의 cert.roundNo <= 현재 cert.roundNo)
        var allMine = certificationRepository.findByParticipation_UserId(participation.getUserId()).stream()
                .filter(c -> c.getParticipation().getChallenge().getChallengeId().equals(challenge.getChallengeId()))
                .toList();

        int currentNo = certification.getRound().getRoundNo();
        long accumulated = allMine.stream()
                .filter(c -> "APPROVED".equals(c.getCertificationStatus()))
                .filter(c -> c.getRound().getRoundNo() <= currentNo)
                .mapToLong(c -> c.getAmount() == null ? 0 : c.getAmount())
                .sum();

        // 회차 상태표
        var today = java.time.LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        var myCertByRoundId = allMine.stream()
                .collect(Collectors.toMap(c -> c.getRound().getRoundId(), c -> c));

        var rows = challenge.getRounds().stream()
                .sorted(Comparator.comparingInt(r -> r.getRoundNo()))
                .map(r -> {
                    String status;
                    if (r.getScheduledDate().isAfter(today)) status = "UPCOMING";
                    else if (myCertByRoundId.containsKey(r.getRoundId())) status = "CERTIFIED";
                    else status = "NOT_CERTIFIED";
                    return CertificationDetailResponse.RoundStatusRow.builder()
                            .roundNo(r.getRoundNo())
                            .scheduledDate(r.getScheduledDate())
                            .status(status)
                            .build();
                }).toList();

        var certificationDto = CertificationDetailResponse.Certification.builder()
                .certificationId(certification.getCertificationId())
                .challengeId(challenge.getChallengeId())
                .challengeTitle(challenge.getTitle())
                .roundNo(currentNo)
                .certifiedAt(certification.getCertifiedAt())
                .amount(certification.getAmount())
                .doubleYn(certification.getDoubleYn())
                .accumulatedAmount(accumulated)
                .build();

        return CertificationDetailResponse.builder()
                .certification(certificationDto)
                .rounds(rows)
                .build();
    }

    @Transactional(readOnly = true)
    public MyCertificationListResponse myCertificationList(Long userId, Long challengeId, int page, int size,
                                                           String sort, boolean includeNotCertified) {

        var challengeParticipation = participationRepository
                .findByChallenge_ChallengeIdAndUserIdAndLeftAtIsNull(challengeId, userId)
                .orElseThrow(() -> new BusinessException(ApiErrorCode.FORBIDDEN, "참여 중인 챌린지가 아닙니다."));

        var participationChallenge = challengeParticipation.getChallenge();

        var pageable = PageRequest.of(
                Math.max(0,page), Math.max(1, Math.min(size,100)),
                Sort.by(
                        (sort==null||sort.isBlank()||sort.toLowerCase().startsWith("scheduleddate")) ?
                                Sort.Direction.ASC :
                                Sort.Direction.ASC,
                        "scheduledDate"));

        var roundsPage = roundRepository.findByChallenge_ChallengeId(challengeId, pageable);
        var myAllCerts = certificationRepository.findByParticipation_UserId(userId).stream()
                .filter(c -> c.getParticipation().getChallenge().getChallengeId().equals(challengeId))
                .toList();
        var byRoundId = myAllCerts.stream().collect(java.util.stream.Collectors.toMap(
                c -> c.getRound().getRoundId(), c -> c));

        var content = new java.util.ArrayList<MyCertificationRow>();
        for (var r : roundsPage.getContent()) {
            var c = byRoundId.get(r.getRoundId());
            if (!includeNotCertified && c == null) continue;

            content.add(MyCertificationRow.builder()
                    .roundId(r.getRoundId())
                    .roundNo(r.getRoundNo())
                    .scheduledDate(r.getScheduledDate())
                    .certified(c != null)
                    .certificationId(c==null?null:c.getCertificationId())
                    .amount(c==null?0L:c.getAmount())
                    .doubleYn(c==null?null:c.getDoubleYn())
                    .comment(c==null?null:c.getComment())
                    .imageUrl(c==null?null:c.getImageUrl())
                    .certifiedAt(c==null?null:c.getCertifiedAt())
                    .build());
        }

        long totalElements = includeNotCertified ? roundsPage.getTotalElements()
                : content.size(); // 필요 시 count 쿼리로 치환 가능

        long accumulated = myAllCerts.stream()
                .filter(c -> "APPROVED".equals(c.getCertificationStatus()))
                .mapToLong(c -> c.getAmount()==null?0:c.getAmount())
                .sum();

        return MyCertificationListResponse.builder()
                .content(content)
                .page(roundsPage.getNumber())
                .size(roundsPage.getSize())
                .totalElements(totalElements)
                .summary(MyCertificationListResponse.Summary.builder().accumulatedAmount(accumulated).build())
                .build();
    }

}

