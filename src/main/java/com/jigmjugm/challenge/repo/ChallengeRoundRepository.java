package com.jigmjugm.challenge.repo;

import com.jigmjugm.challenge.domain.ChallengeRound;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChallengeRoundRepository extends JpaRepository<ChallengeRound, Long> {
    boolean existsByChallenge_ChallengeId(Long challengeId);

    Page<ChallengeRound> findByChallenge_ChallengeId(Long challengeId, Pageable pageable);
}
