package io.waggle.waggleapiserver.domain.member.repository

import io.waggle.waggleapiserver.domain.member.Member
import io.waggle.waggleapiserver.domain.member.MemberRole
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

    @Query(
        """
        SELECT m.teamId AS teamId, COUNT(m) AS count
        FROM Member m
        WHERE m.teamId IN :teamIds
        GROUP BY m.teamId
        """,
    )
    fun countByTeamIds(
        @Param("teamIds") teamIds: List<Long>,
    ): List<TeamMemberCount>

    fun findByUserIdAndTeamId(
        userId: UUID,
        teamId: Long,
    ): Member?

    fun findByTeamId(teamId: Long): List<Member>

    fun findByTeamIdAndUserIdNot(
        teamId: Long,
        userId: UUID,
    ): List<Member>

    fun findByIdNotAndTeamIdOrderByRoleAscCreatedAtAsc(
        id: Long,
        teamId: Long,
    ): List<Member>

    fun findByUserIdOrderByRoleAscCreatedAtAsc(userId: UUID): List<Member>

    fun findByUserIdAndVisibleTrueOrderByRoleAscCreatedAtAsc(userId: UUID): List<Member>

    fun findByTeamIdOrderByRoleAscCreatedAtAsc(teamId: Long): List<Member>

    fun findByTeamIdAndRoleIn(
        teamId: Long,
        roles: List<MemberRole>,
    ): List<Member>

    @Query(
        """
        SELECT * FROM members
        WHERE team_id = :teamId AND deleted_at IS NOT NULL
        ORDER BY role ASC, created_at ASC
        """,
        nativeQuery = true,
    )
    fun findByTeamIdAndDeletedAtIsNotNullOrderByRoleAscCreatedAtAsc(
        @Param("teamId") teamId: Long,
    ): List<Member>
}

interface TeamMemberCount {
    val teamId: Long
    val count: Long
}
