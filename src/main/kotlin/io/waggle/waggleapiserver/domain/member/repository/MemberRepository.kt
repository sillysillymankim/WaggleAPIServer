package io.waggle.waggleapiserver.domain.member.repository

import io.waggle.waggleapiserver.domain.member.Member
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MemberRepository : JpaRepository<Member, Long> {
    fun findByUserIdAndTeamId(
        userId: UUID,
        teamId: Long,
    ): Member?

    fun findByIdNotAndTeamIdOrderByRoleAscCreatedAtAsc(
        id: Long,
        teamId: Long,
    ): List<Member>

    fun findByUserIdOrderByRoleAscCreatedAtAsc(userId: UUID): List<Member>

    fun findByUserIdAndIsVisibleTrueOrderByRoleAscCreatedAtAsc(userId: UUID): List<Member>

    fun findByTeamIdOrderByRoleAscCreatedAtAsc(teamId: Long): List<Member>

    fun countByTeamId(teamId: Long): Int

    fun existsByUserIdAndTeamId(
        userId: UUID,
        teamId: Long,
    ): Boolean
}
