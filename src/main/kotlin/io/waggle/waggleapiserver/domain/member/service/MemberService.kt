package io.waggle.waggleapiserver.domain.member.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.member.Member
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.member.dto.request.MemberUpdateRoleRequest
import io.waggle.waggleapiserver.domain.member.dto.response.MemberResponse
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.team.repository.TeamRepository
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MemberService(
    private val memberRepository: MemberRepository,
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun updateMemberRole(
        memberId: Long,
        request: MemberUpdateRoleRequest,
        user: User,
    ): MemberResponse {
        val role = request.role

        val targetMember =
            memberRepository.findByIdOrNull(memberId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Member not found")
        if (user.id == targetMember.userId) {
            throw BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Cannot update your own role")
        }

        val team =
            teamRepository.findByIdOrNull(targetMember.teamId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Team Not Found: ${targetMember.teamId}",
                )

        val member =
            memberRepository.findByUserIdAndTeamId(user.id, team.id)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Member not found")

        when (role) {
            MemberRole.MEMBER, MemberRole.MANAGER -> {
                member.checkMemberRole(MemberRole.LEADER)
                targetMember.updateRole(role)
            }
            MemberRole.LEADER -> delegateLeader(member, targetMember)
        }

        val targetUser =
            userRepository.findByIdOrNull(targetMember.userId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "User not found: ${targetMember.userId}")

        return MemberResponse.of(targetMember, targetUser)
    }

    @Transactional
    fun leaveTeam(
        teamId: Long,
        user: User,
    ) {
        val member =
            memberRepository.findByUserIdAndTeamId(user.id, teamId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Member Not Found")
        val members =
            memberRepository.findByIdNotAndTeamIdOrderByRoleAscCreatedAtAsc(member.id, teamId)
        if (members.isEmpty()) {
            throw BusinessException(ErrorCode.INVALID_STATE, "Cannot leave as the only member")
        }

        if (member.isLeader) {
            delegateLeader(member, members[0])
        }

        member.deleteBy(user.id)
    }

    @Transactional
    fun removeMember(
        memberId: Long,
        user: User,
    ) {
        val member =
            memberRepository.findByIdOrNull(memberId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Member not found: $memberId",
                )

        val team =
            teamRepository.findByIdOrNull(member.teamId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Team Not Found: ${member.teamId}",
                )

        val leader =
            memberRepository.findByUserIdAndTeamId(user.id, team.id)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Member not found")
        leader.checkMemberRole(MemberRole.LEADER)

        member.deleteBy(user.id)
    }

    private fun delegateLeader(
        oldLeader: Member,
        newLeader: Member,
    ) {
        oldLeader.checkMemberRole(MemberRole.LEADER)
        if (newLeader.teamId != oldLeader.teamId) {
            throw BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Not in the same team")
        }

        val team =
            teamRepository.findByIdOrNull(oldLeader.teamId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Team Not Found: ${oldLeader.teamId}",
                )

        oldLeader.updateRole(MemberRole.MANAGER)
        newLeader.updateRole(MemberRole.LEADER)
        team.leaderId = newLeader.userId
    }
}
