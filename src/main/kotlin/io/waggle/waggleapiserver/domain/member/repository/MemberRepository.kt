package io.waggle.waggleapiserver.domain.member.repository

import io.waggle.waggleapiserver.domain.member.Member
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface MemberRepository : JpaRepository<Member, Long> {
    fun existsByUserIdAndTeamId(
        userId: UUID,
        teamId: Long,
    ): Boolean

    fun countByTeamId(teamId: Long): Int

    fun findByUserIdAndTeamId(
        userId: UUID,
        teamId: Long,
    ): Member?

    fun findByIdNotAndTeamIdOrderByRoleAscCreatedAtAsc(
        id: Long,
        teamId: Long,
    ): List<Member>

    fun findByUserIdOrderByRoleAscCreatedAtAsc(userId: UUID): List<Member>

    fun findByUserIdAndVisibleTrueOrderByRoleAscCreatedAtAsc(userId: UUID): List<Member>

    fun findByTeamIdOrderByRoleAscCreatedAtAsc(teamId: Long): List<Member>

    @Query(
        """
        SELECT * FROM members
        WHERE team_id = :teamId AND deleted_at IS NOT NULL
        ORDER BY role ASC, created_at ASC
        """,
        nativeQuery = true,
    )
    fun findByTeamIdAndDeletedAtIsNotNullOrderByRoleAscCreatedAtAsc(@Param("teamId") teamId: Long): List<Member>
}
