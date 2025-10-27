package com.jigmjugm.challenge.repo;

import com.jigmjugm.challenge.domain.ChallengeRound;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChallengeRoundRepository extends JpaRepository<ChallengeRound, Long> {
    boolean existsByChallenge_ChallengeId(Long challengeId);

    Page<ChallengeRound> findByChallenge_ChallengeId(Long challengeId, Pageable pageable);

    List<ChallengeRound> findByChallenge_ChallengeIdAndScheduledDateGreaterThanEqualOrderByScheduledDateAsc(Long challengeId, java.time.LocalDate from);

}
