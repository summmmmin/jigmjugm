package com.jigmjugm.challenge.service;

import com.jigmjugm.challenge.domain.Challenge;
import com.jigmjugm.challenge.domain.ChallengeParticipation;
import com.jigmjugm.challenge.dto.ParticipationResponse;
import com.jigmjugm.challenge.repo.ChallengeParticipationRepository;
import com.jigmjugm.challenge.repo.ChallengeRepository;
import com.jigmjugm.common.error.ApiErrorCode;
import com.jigmjugm.common.error.BusinessException;
import com.jigmjugm.common.util.ChallengePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChallengeParticipationService {

    private final ChallengeRepository challengeRepository;
    private final ChallengeParticipationRepository participationRepository;
    private final ChallengePolicy policy;

    @Transactional
    public ParticipationResponse joinChallenge(Long userId, Long challengeId) {
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new NoSuchElementException("챌린지를 찾을 수 없습니다."));

        String status = policy.computeStatus(challenge.getStartDate(), challenge.getEndDate(), LocalDate.now(ZoneId.of("Asia/Seoul")));
        if (!"PENDING".equals(status)) {
            throw new BusinessException(ApiErrorCode.INVALID_STATE, "진행 중 또는 완료된 챌린지는 참여할 수 없습니다.");
        }

        if (participationRepository.existsByChallenge_ChallengeIdAndUserIdAndLeftAtIsNull(challengeId, userId)) {
            throw new BusinessException(ApiErrorCode.ALREADY_PARTICIPATING, "이미 참여 중입니다.");
        }

        ChallengeParticipation participation = ChallengeParticipation.builder()
                .challenge(challenge)
                .userId(userId)
                .roleType(challenge.getCreatorUserId().equals(userId) ? "OWNER" : "MEMBER")
                .build();

        ChallengeParticipation saved = participationRepository.save(participation);
        return new ParticipationResponse(saved.getParticipationId(), challengeId, userId, saved.getJoinedAt());
    }

    @Transactional
    public void leaveChallenge(Long userId, Long participationId) {
        ChallengeParticipation part = participationRepository.findById(participationId)
                .orElseThrow(() -> new NoSuchElementException("참여 이력이 없습니다."));

        if (!Objects.equals(part.getUserId(), userId)) {
            throw new SecurityException("본인만 탈퇴할 수 있습니다.");
        }

        if (part.getLeftAt() != null) {
            throw new IllegalStateException("이미 탈퇴 처리된 참여자입니다.");
        }

        part.setLeftAt(OffsetDateTime.now(ZoneOffset.UTC));
        part.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        participationRepository.save(part);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getParticipants(Long challengeId) {
        List<ChallengeParticipation> list = participationRepository.findByChallenge_ChallengeIdAndLeftAtIsNull(challengeId);
        return list.stream()
                .map(p -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("participationId", p.getParticipationId());
                    map.put("userId", p.getUserId());
                    map.put("roleType", p.getRoleType());
                    map.put("joinedAt", p.getJoinedAt());
                    return map;
                })
                .toList();
    }
}

