package com.jigmjugm.challenge.repo;

import com.jigmjugm.challenge.domain.ChallengeParticipation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChallengeParticipationRepository extends JpaRepository<ChallengeParticipation, Long> {

    boolean existsByChallenge_ChallengeIdAndUserIdAndLeftAtIsNull(Long challengeId, Long userId);

    List<ChallengeParticipation> findByChallenge_ChallengeIdAndLeftAtIsNull(Long challengeId);
}
