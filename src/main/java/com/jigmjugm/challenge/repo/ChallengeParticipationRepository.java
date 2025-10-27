package com.jigmjugm.challenge.repo;

import com.jigmjugm.challenge.domain.ChallengeParticipation;
import com.jigmjugm.challenge.dto.MyChallengeListItemView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ChallengeParticipationRepository extends JpaRepository<ChallengeParticipation, Long> {

    boolean existsByChallenge_ChallengeIdAndUserIdAndLeftAtIsNull(Long challengeId, Long userId);

    List<ChallengeParticipation> findByChallenge_ChallengeIdAndLeftAtIsNull(Long challengeId);

    Optional<ChallengeParticipation> findByChallenge_ChallengeIdAndUserIdAndLeftAtIsNull(Long challengeId, Long userId);
    @Query("""
      select
        c.challengeId as challengeId,
        c.title as title,
        c.categoryType as categoryType,
        c.frequencyType as frequencyType,
        c.startDate as startDate,
        c.endDate as endDate,
        c.createdAt as createdAt,
        c.thumbnailUrl as thumbnailUrl,
        (case
          when :today < c.startDate then 'PENDING'
          when c.startDate <= :today and c.endDate >= :today then 'ACTIVE'
          else 'COMPLETED'
        end) as status,
        (case when c.creatorUserId = :userId and :today < c.startDate then true else false end) as canEdit,
        (case when c.creatorUserId = :userId and :today < c.startDate then true else false end) as canDelete
      from ChallengeParticipation p
      join p.challenge c
      where c.isDeleted = false
        and p.userId = :userId
        and (:includeWithdrawn = true or p.leftAt is null)
        and (:category = 'ALL' or c.categoryType = :category)
        and (
          :status is null
          or (:status = 'PENDING' and :today < c.startDate)
          or (:status = 'ACTIVE' and c.startDate <= :today and c.endDate >= :today)
          or (:status = 'COMPLETED' and c.endDate < :today)
        )
      """)
    Page<MyChallengeListItemView> findMyChallenges(
            Long userId, String category, String status, LocalDate today, boolean includeWithdrawn, Pageable pageable);

    List<ChallengeParticipation> findByUserIdAndLeftAtIsNull(Long userId);
}
