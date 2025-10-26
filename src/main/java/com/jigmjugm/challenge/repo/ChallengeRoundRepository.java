package com.jigmjugm.challenge.repo;

import com.jigmjugm.challenge.domain.ChallengeRound;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChallengeRoundRepository extends JpaRepository<ChallengeRound, Long> {
    boolean existsByChallenge_ChallengeId(Long challengeId);
}
