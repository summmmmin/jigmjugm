package com.jigmjugm.certification.repo;

import com.jigmjugm.certification.domain.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
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

    @Query("""
        select coalesce(sum(c.amount), 0)
        from Certification c
          join c.participation p
        where p.userId = :userId
          and c.certifiedAt >= :start
          and c.certifiedAt < :end
          and p.challenge.isDeleted = false
        """)
    Long sumAmountByUserAndCertifiedAtBetween(
            @Param("userId") Long userId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end
    );

    @Query("""
        select coalesce(sum(c.amount), 0)
        from Certification c
        where c.participation.participationId = :participationId
          and c.certificationStatus = :status
    """)
    Long sumApprovedAmountByParticipationId(
            @Param("participationId") Long participationId,
            @Param("status") String status
    );
}

