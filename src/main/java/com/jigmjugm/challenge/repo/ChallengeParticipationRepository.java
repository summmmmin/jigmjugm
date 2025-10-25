package com.jigmjugm.challenge.repo;

import com.jigmjugm.challenge.domain.ChallengeParticipation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChallengeParticipationRepository extends JpaRepository<ChallengeParticipation, Long> {

    boolean existsByChallenge_ChallengeIdAndUserIdAndLeftAtIsNull(Long challengeId, Long userId);

    List<ChallengeParticipation> findByChallenge_ChallengeIdAndLeftAtIsNull(Long challengeId);

    Optional<ChallengeParticipation> findByChallenge_ChallengeIdAndUserIdAndLeftAtIsNull(Long challengeId, Long userId);
}
