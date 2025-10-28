package com.jigmjugm.certification.repo;

import com.jigmjugm.certification.domain.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CertificationRepository extends JpaRepository<Certification, Long> {

    boolean existsByParticipation_ParticipationIdAndRound_RoundId(Long participationId, Long roundId);

    List<Certification> findByParticipation_UserId(Long userId);

    Optional<Certification> findByCertificationIdAndParticipation_UserId(Long certificationId, Long userId);

    @Query("""
      select c from Certification c
      where c.participation.userId = :userId
        and c.participation.leftAt is null
        and c.participation.challenge.challengeId = :challengeId
    """)
    List<Certification> findActiveByUserAndChallenge(Long userId, Long challengeId);

    long countByParticipation_ParticipationIdAndCertificationStatus(Long participationId, String certificationStatus);

}

