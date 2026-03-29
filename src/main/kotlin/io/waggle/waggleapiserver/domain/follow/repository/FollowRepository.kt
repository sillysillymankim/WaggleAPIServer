package io.waggle.waggleapiserver.domain.follow.repository

import io.waggle.waggleapiserver.domain.follow.Follow
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface FollowRepository : JpaRepository<Follow, Long> {
    fun existsByFollowerIdAndFolloweeId(
        followerId: UUID,
        followeeId: UUID,
    ): Boolean

    fun deleteByFollowerIdAndFolloweeId(
        followerId: UUID,
        followeeId: UUID,
    )

    fun countByFollowerId(followerId: UUID): Long

    fun countByFolloweeId(followeeId: UUID): Long

    fun findByFollowerId(followerId: UUID): List<Follow>

    fun findByFolloweeId(followeeId: UUID): List<Follow>

    @Modifying
    @Query(
        """
        UPDATE follows SET deleted_at = CURRENT_TIMESTAMP
        WHERE (follower_id = :userId OR followee_id = :userId) AND deleted_at IS NULL
        """,
        nativeQuery = true,
    )
    fun updateDeletedAtByFollowerIdOrFolloweeIdAndDeletedAtIsNull(
        @Param("userId") userId: UUID,
    )

    @Modifying
    @Query(
        """
        UPDATE Follow f
        SET f.deletedAt = NULL
        WHERE (f.followerId = :userId OR f.followeeId = :userId) AND f.deletedAt IS NOT NULL
        """,
    )
    fun updateDeletedAtNullByFollowerIdOrFolloweeIdAndDeletedAtIsNotNull(
        @Param("userId") userId: UUID,
    )
}
