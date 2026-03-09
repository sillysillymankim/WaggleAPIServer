package io.waggle.waggleapiserver.domain.memberreview.repository

import io.waggle.waggleapiserver.domain.memberreview.MemberReview
import io.waggle.waggleapiserver.domain.memberreview.enums.ReviewTag
import io.waggle.waggleapiserver.domain.memberreview.enums.ReviewType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface MemberReviewRepository : JpaRepository<MemberReview, Long> {
    fun findByReviewerIdAndRevieweeIdAndTeamId(
        reviewerId: UUID,
        revieweeId: UUID,
        teamId: Long,
    ): MemberReview?

    fun findByReviewerId(reviewerId: UUID): List<MemberReview>

    fun findByRevieweeId(revieweeId: UUID): List<MemberReview>

    fun countByRevieweeIdAndType(
        revieweeId: UUID,
        type: ReviewType,
    ): Long

    @Query(
        """
        SELECT mr.revieweeId AS revieweeId, mr.type AS type, COUNT(mr) AS count
        FROM MemberReview mr
        WHERE mr.revieweeId IN :revieweeIds
        GROUP BY mr.revieweeId, mr.type
        """,
    )
    fun countByRevieweeIdInGroupByType(
        @Param("revieweeIds") revieweeIds: List<UUID>,
    ): List<MemberReviewCount>

    @Query(
        """
        SELECT t AS tag, COUNT(t) AS count
        FROM MemberReview mr JOIN mr.tags t
        WHERE mr.revieweeId = :revieweeId AND mr.type = :type
        GROUP BY t
        ORDER BY COUNT(t) DESC
        """,
    )
    fun countTagsByRevieweeIdAndType(
        @Param("revieweeId") revieweeId: UUID,
        @Param("type") type: ReviewType,
        pageable: Pageable,
    ): List<MemberReviewTagCount>
}

interface MemberReviewTagCount {
    val tag: ReviewTag
    val count: Long
}

interface MemberReviewCount {
    val revieweeId: UUID
    val type: ReviewType
    val count: Long
}
