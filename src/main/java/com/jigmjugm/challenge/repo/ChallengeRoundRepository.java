package com.jigmjugm.challenge.repo;

import com.jigmjugm.challenge.domain.ChallengeRound;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ChallengeRoundRepository extends JpaRepository<ChallengeRound, Long> {
    boolean existsByChallenge_ChallengeId(Long challengeId);

    Page<ChallengeRound> findByChallenge_ChallengeId(Long challengeId, Pageable pageable);

    List<ChallengeRound> findByChallenge_ChallengeIdAndScheduledDateGreaterThanEqualOrderByScheduledDateAsc(Long challengeId, LocalDate from);

    Optional<ChallengeRound> findFirstByChallenge_ChallengeIdAndScheduledDateGreaterThanEqualOrderByScheduledDateAsc(Long challengeId, LocalDate from);

    int countByChallenge_ChallengeIdAndScheduledDateBetween(Long challengeId, LocalDate start, LocalDate end);

}
