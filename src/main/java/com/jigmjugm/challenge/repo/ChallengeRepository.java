package com.jigmjugm.challenge.repo;

import com.jigmjugm.challenge.domain.Challenge;
import com.jigmjugm.challenge.dto.ChallengeListItemView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface ChallengeRepository  extends JpaRepository<Challenge, Long> {
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
          end) as status
        from Challenge c
        where c.isDeleted = false
          and (:category = 'ALL' or c.categoryType = :category)
          and (
            :status is null
            or (:status = 'PENDING' and :today < c.startDate)
            or (:status = 'ACTIVE' and c.startDate <= :today and c.endDate >= :today)
            or (:status = 'COMPLETED' and c.endDate < :today)
          )
        """)
    Page<ChallengeListItemView> searchDiscover(
            @Param("category") String category,
            @Param("status") String status,
            @Param("today") LocalDate today,
            Pageable pageable);

    @Query(value = """
      select exists(
        select 1
        from challenge c
        where c.is_deleted = false
          and regexp_replace(trim(c.title), '\\s+', ' ', 'g') = :normalizedTitle
      )
      """, nativeQuery = true)
    boolean existsnormalizedTitle(@Param("normalizedTitle") String normalizedTitle);

    @Query(value = """
      select exists(
        select 1
        from challenge c
        where c.is_deleted = false
          and c.challenge_id <> :excludeId
          and regexp_replace(trim(c.title), '\\s+', ' ', 'g') = :normalizedTitle
      )
      """, nativeQuery = true)
    boolean existsNormalizedTitleExceptId(@Param("normalizedTitle") String normalizedTitle, @Param("excludeId") Long excludeId);
}
