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

    public interface StatsRow {
        Long   getAvgTotalAmount();
        Double getAvgCertRate();
        Integer getTotalParticipants();
    }

    public interface RankedRow {
        Long getUserId();
        String getNickname();
        Double getCertRate();
        Integer getRank();
    }

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

    @Query("""
        select p
        from ChallengeParticipation p
        join fetch p.challenge c
        where p.userId = :userId
          and p.leftAt is null
          and c.isDeleted = false
    """)
    List<ChallengeParticipation> findActiveByUserExcludingDeleted(Long userId);


    @Query(value = """
            with vars as (select (:today)::date as today),
            participants as (
              select p.participation_id
              from challenge_participation p
              where p.challenge_id = :challengeId and p.left_at is null
            ),
            rtotal as (
              select count(*) as total_rounds
              from challenge_round r
              join challenge c on c.challenge_id = r.challenge_id
              join vars v on true
              where r.challenge_id = :challengeId
                and r.scheduled_date between c.start_date and least(c.end_date, v.today)
            ),
            per as (
              select
                p.participation_id,
                coalesce(sum(case when cf.certification_status = 'APPROVED' then cf.amount end), 0) as total_amount,
                coalesce(sum(case when cf.certification_status = 'APPROVED' then 1 else 0 end), 0) as approved_cnt
              from participants p
              left join certification cf on cf.participation_id = p.participation_id
              group by p.participation_id
            )
            select
              coalesce(avg(per.total_amount),0)::bigint                             as avgTotalAmount,
              coalesce( avg(per.approved_cnt::float) / nullif(max(rtotal.total_rounds),0), 0 ) as avgCertRate,
              (select count(*)::int from participants)                               as totalParticipants
            from per
            cross join rtotal
            """, nativeQuery = true)
    StatsRow statsByChallenge(Long challengeId, LocalDate today);

    // ==== 기간 "일자 범위" 랭킹 (상세 recentRankings에서 사용) ====
    @Query(value = """
            with r as (
              select round_id
              from challenge_round
              where challenge_id = :challengeId
                and scheduled_date between :startInclusive and :endInclusive
            ),
            sc as (
              select count(*)::int as scheduled_count from r
            ),
            x as (
              select
                p.user_id                                    as userId,
                u.nickname                                   as nickname,
                sc.scheduled_count                           as scheduled_count,
                coalesce(sum(case
                  when rr.round_id is not null and cf.certification_status = 'APPROVED' then 1 else 0 end), 0)::int
                  as approved_count
              from challenge_participation p
              join user_account u on u.user_id = p.user_id
              cross join sc
              left join certification cf on cf.participation_id = p.participation_id
              left join r rr on rr.round_id = cf.round_id
              where p.challenge_id = :challengeId
                and p.left_at is null
              group by p.user_id, u.nickname, sc.scheduled_count
            ),
            ranked as (
              select
                userId,
                nickname,
                case when scheduled_count > 0
                     then approved_count::float / scheduled_count
                     else 0 end                as certRate,
                dense_rank() over (
                  order by
                    case when scheduled_count > 0
                         then approved_count::float / scheduled_count
                         else 0 end desc,
                    approved_count desc,
                    userId asc
                )                              as rank_no
              from x
            )
            select userId, nickname, certRate, rank_no as rank
            from ranked
            order by rank_no asc
            limit :limit
            """, nativeQuery = true)
    List<RankedRow> rankingTopRange(Long challengeId, LocalDate startInclusive, LocalDate endInclusive, int limit);

    @Query(value = """
            with r as (
              select round_id
              from challenge_round
              where challenge_id = :challengeId
                and scheduled_date between :startInclusive and :endInclusive
            ),
            sc as (
              select count(*)::int as scheduled_count from r
            ),
            x as (
              select
                p.user_id                                    as userId,
                sc.scheduled_count                           as scheduled_count,
                coalesce(sum(case
                  when rr.round_id is not null and cf.certification_status = 'APPROVED' then 1 else 0 end), 0)::int
                  as approved_count
              from challenge_participation p
              cross join sc
              left join certification cf on cf.participation_id = p.participation_id
              left join r rr on rr.round_id = cf.round_id
              where p.challenge_id = :challengeId
                and p.left_at is null
              group by p.user_id, sc.scheduled_count
            ),
            ranked as (
              select
                userId,
                case when scheduled_count > 0
                     then approved_count::float / scheduled_count
                     else 0 end                as certRate,
                dense_rank() over (
                  order by
                    case when scheduled_count > 0
                         then approved_count::float / scheduled_count
                         else 0 end desc,
                    approved_count desc,
                    userId asc
                )                              as rank_no
              from x
            )
            select userId, certRate, rank_no as rank
            from ranked
            where userId = :userId
            """, nativeQuery = true)
    List<RankedRow> rankingMeRange(Long challengeId, Long userId, LocalDate startInclusive, LocalDate endInclusive);

    Optional<ChallengeParticipation>findFirstByChallenge_ChallengeIdAndUserIdAndLeftAtIsNull(Long challengeId, Long userId);

}
