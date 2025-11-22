package com.jigmjugm.challenge.repo;

import com.jigmjugm.challenge.domain.ChallengeParticipation;
import com.jigmjugm.challenge.dto.MyChallengeListItemView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
        (case when c.creatorUserId = :userId and (select count(p2) from ChallengeParticipation p2 where p2.challenge.challengeId = c.challengeId and p2.leftAt is null) = 1 then true else false end) as canEdit,
        true as canDelete,
        (select count(*) from ChallengeParticipation p3
                where p3.challenge.challengeId = c.challengeId and p.leftAt is null) as participantCount,
        c.perRoundAmount as perRoundAmount
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
        and (:myCreatedOnly = false or c.creatorUserId = :userId)
      """)
    Page<MyChallengeListItemView> findMyChallenges(
            Long userId, String category, String status, LocalDate today, boolean includeWithdrawn, boolean myCreatedOnly, Pageable pageable);

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


    @Query(value = """
        select count(distinct cp.challenge.challengeId)
        from ChallengeParticipation cp
        join Challenge c on c.challengeId = cp.challenge.challengeId
        where cp.userId = :userId
          and cp.leftAt is null
          and c.isDeleted = false
          and c.endDate >= :today
        """)
    long countMyActiveOrUpcoming(@Param("userId") Long userId, @Param("today") LocalDate today);

    @Query(value = """
        select count(distinct cp.challenge_id)
        from challenge_participation cp
        join challenge c on c.challenge_id = cp.challenge_id
        where cp.user_id = :userId
          and c.is_deleted = false
          and c.end_date < :today
        """, nativeQuery = true)
    long countMyCompleted(@Param("userId") Long userId, @Param("today") LocalDate today);


    @Query(value = """
        with period as (
          select (:today)::date as today,
                 ((:today)::date - (:periodDays - 1) * interval '1 day')::date as since
        ),
        my_approved as (
          select r.challenge_id,
                 count(*) as approved_cnt
          from certification cf
          join challenge_round r
            on r.round_id = cf.round_id
          join challenge_participation p
            on p.participation_id = cf.participation_id
          where p.user_id = :userId
            and p.left_at is null
            and cf.certification_status = 'APPROVED'
            and r.scheduled_date between (select since from period) and (select today from period)
          group by r.challenge_id
        ),
        scheduled as (
          select r.challenge_id,
                 count(*) as scheduled_cnt
          from challenge_round r
          where r.scheduled_date between (select since from period) and (select today from period)
          group by r.challenge_id
        ),
        rate as (
          select
            c.challenge_id,
            coalesce(
              case when s.scheduled_cnt > 0
                   then a.approved_cnt::float / s.scheduled_cnt
                   else 0 end
            ,0) as myCertRate
          from challenge c
          left join scheduled s on s.challenge_id = c.challenge_id
          left join my_approved a on a.challenge_id = c.challenge_id
          where c.is_deleted = false
        )
        select
          c.challenge_id   as challengeId,
          c.title          as title,
          c.category_type  as categoryType,
          c.frequency_type as frequencyType,
          c.start_date     as startDate,
          c.end_date       as endDate,
          c.created_at     as createdAt,
          c.thumbnail_url  as thumbnailUrl,
          case
            when :today < c.start_date then 'PENDING'
            when c.start_date <= :today and c.end_date >= :today then 'ACTIVE'
            else 'COMPLETED'
          end as status,
          case when c.creator_user_id = :userId and (select count(p2) from challenge_participation p2 where p2.challenge_id = c.challenge_id and p2.left_at is null) = 1 then true else false end as canEdit,
          true as canDelete,
          (select count(*) from challenge_participation p
                              where p.challenge_id = c.challenge_id and p.left_at is null) as participantCount,
              c.per_round_amount perRoundAmount
        from challenge c
        join challenge_participation p
          on p.challenge_id = c.challenge_id
         and p.user_id = :userId
        left join rate
          on rate.challenge_id = c.challenge_id
        where c.is_deleted = false
          and (:includeWithdrawn = true or p.left_at is null)
          and (:myCreatedOnly = false or c.creator_user_id = :userId)
          and (:category = 'ALL' or c.category_type = :category)
          and (
            :status is null
            or (:status = 'PENDING'  and :today < c.start_date)
            or (:status = 'ACTIVE'   and c.start_date <= :today and c.end_date >= :today)
            or (:status = 'COMPLETED' and c.end_date < :today)
          )
        order by rate.myCertRate desc nulls last, c.created_at desc
        """,
            countQuery = """
          select count(*)
          from challenge c
          join challenge_participation p
            on p.challenge_id = c.challenge_id
           and p.user_id = :userId
          where c.is_deleted = false
            and (:includeWithdrawn = true or p.left_at is null)
            and (:myCreatedOnly = false or c.creator_user_id = :userId)
            and (:category = 'ALL' or c.category_type = :category)
            and (
              :status is null
              or (:status = 'PENDING'  and :today < c.start_date)
              or (:status = 'ACTIVE'   and c.start_date <= :today and c.end_date >= :today)
              or (:status = 'COMPLETED' and c.end_date < :today)
            )
        """,
            nativeQuery = true)
    Page<MyChallengeListItemView> searchMyChallengesOrderByMyCertRate(
            @Param("userId") Long userId,
            @Param("category") String category,
            @Param("status") String status,
            @Param("today") LocalDate today,
            @Param("periodDays") int periodDays,
            @Param("includeWithdrawn") boolean includeWithdrawn,
            @Param("myCreatedOnly") boolean myCreatedOnly,
            Pageable pageable);

    @Query(value = """
            with sum_cf as (
              select p.challenge_id,
                     coalesce(sum(case
                                    when cf.certification_status = 'APPROVED'
                                    then cf.amount end), 0) as totalAmount
              from challenge_participation p
              left join certification cf
                on cf.participation_id = p.participation_id
              group by p.challenge_id
            )
            select
              c.challenge_id   as challengeId,
              c.title          as title,
              c.category_type  as categoryType,
              c.frequency_type as frequencyType,
              c.start_date     as startDate,
              c.end_date       as endDate,
              c.created_at     as createdAt,
              c.thumbnail_url  as thumbnailUrl,
              case
                when :today < c.start_date then 'PENDING'
                when c.start_date <= :today and c.end_date >= :today then 'ACTIVE'
                else 'COMPLETED'
              end as status,
              case when c.creator_user_id = :userId and (select count(p2) from challenge_participation p2 where p2.challenge_id = c.challenge_id and p2.left_at is null) = 1 then true else false end as canEdit,
              true as canDelete,
              (select count(*) from challenge_participation p
                              where p.challenge_id = c.challenge_id and p.left_at is null) as participantCount,
              c.per_round_amount perRoundAmount
            from challenge c
            join challenge_participation p
              on p.challenge_id = c.challenge_id
             and p.user_id = :userId
            left join sum_cf
              on sum_cf.challenge_id = c.challenge_id
            where c.is_deleted = false
              and (:includeWithdrawn = true or p.left_at is null)
              and (:myCreatedOnly = false or c.creator_user_id = :userId)
              and (:category = 'ALL' or c.category_type = :category)
              and (
                :status is null
                or (:status = 'PENDING'  and :today < c.start_date)
                or (:status = 'ACTIVE'   and c.start_date <= :today and c.end_date >= :today)
                or (:status = 'COMPLETED' and c.end_date < :today)
              )
            order by sum_cf.totalAmount desc nulls last, c.created_at desc
            """,
            countQuery = """
              select count(*)
              from challenge c
              join challenge_participation p
                on p.challenge_id = c.challenge_id
               and p.user_id = :userId
              where c.is_deleted = false
                and (:includeWithdrawn = true or p.left_at is null)
                and (:myCreatedOnly = false or c.creator_user_id = :userId)
                and (:category = 'ALL' or c.category_type = :category)
                and (
                  :status is null
                  or (:status = 'PENDING'  and :today < c.start_date)
                  or (:status = 'ACTIVE'   and c.start_date <= :today and c.end_date >= :today)
                  or (:status = 'COMPLETED' and c.end_date < :today)
                )
            """,
            nativeQuery = true)
    Page<MyChallengeListItemView> searchMyChallengesOrderByTotalAmount(
            @Param("userId") Long userId,
            @Param("category") String category,
            @Param("status") String status,
            @Param("today") LocalDate today,
            @Param("includeWithdrawn") boolean includeWithdrawn,
            @Param("myCreatedOnly") boolean myCreatedOnly,
            Pageable pageable);

}
