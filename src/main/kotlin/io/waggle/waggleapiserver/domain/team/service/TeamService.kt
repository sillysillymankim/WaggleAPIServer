package io.waggle.waggleapiserver.domain.team.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.common.storage.event.ImageDeleteEvent
import io.waggle.waggleapiserver.domain.notification.event.TeamCompletedEvent
import io.waggle.waggleapiserver.domain.team.enums.TeamStatus
import io.waggle.waggleapiserver.common.storage.StorageClient
import io.waggle.waggleapiserver.common.storage.dto.request.PresignedUrlRequest
import io.waggle.waggleapiserver.common.storage.dto.response.PresignedUrlResponse
import io.waggle.waggleapiserver.domain.member.Member
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.member.dto.response.MemberResponse
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.team.Team
import io.waggle.waggleapiserver.domain.team.dto.request.TeamStatusUpdateRequest
import io.waggle.waggleapiserver.domain.team.dto.request.TeamUpsertRequest
import io.waggle.waggleapiserver.domain.team.dto.response.TeamResponse
import io.waggle.waggleapiserver.domain.team.repository.TeamRepository
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class TeamService(
    private val eventPublisher: ApplicationEventPublisher,
    private val storageClient: StorageClient,
    private val memberRepository: MemberRepository,
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun createTeam(
        request: TeamUpsertRequest,
        user: User,
    ): TeamResponse {
        val (name, description, workMode, profileImageUrl) = request

        if (teamRepository.existsByName(name)) {
            throw BusinessException(
                ErrorCode.DUPLICATE_RESOURCE,
                "Already exists team name: $name",
            )
        }

        val team =
            Team(
                name = name,
                description = description,
                workMode = workMode,
                profileImageUrl = profileImageUrl,
                leaderId = user.id,
                creatorId = user.id,
            )

        val savedTeam = teamRepository.save(team)

        val member =
            Member(
                userId = user.id,
                teamId = savedTeam.id,
                position = user.position!!,
                role = MemberRole.LEADER,
            )

        memberRepository.save(member)

        val memberCount = memberRepository.countByTeamId(savedTeam.id)

        return TeamResponse.of(savedTeam, memberCount)
    }

    fun generateProfileImagePresignedUrl(request: PresignedUrlRequest): PresignedUrlResponse {
        val presignedUploadUrl =
            storageClient.generateUploadUrl(
                "teams",
                request.contentType,
            )
        return PresignedUrlResponse.from(presignedUploadUrl)
    }

    fun getTeam(teamId: Long): TeamResponse {
        val team =
            teamRepository.findByIdOrNull(teamId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Team not found: $teamId",
                )
        val memberCount = memberRepository.countByTeamId(teamId)

        return TeamResponse.of(team, memberCount)
    }

    fun getTeamMembers(
        teamId: Long,
        user: User?,
    ): List<MemberResponse> {
        val isTeamMember = user?.let { memberRepository.existsByUserIdAndTeamId(it.id, teamId) } ?: false

        val activeMembers = memberRepository.findByTeamIdOrderByRoleAscCreatedAtAsc(teamId)
        val deletedMembers =
            if (isTeamMember) {
                memberRepository.findByTeamIdAndDeletedAtIsNotNullOrderByRoleAscCreatedAtAsc(teamId)
            } else {
                emptyList()
            }
        val members = activeMembers + deletedMembers

        val userById = userRepository.findAllById(members.map { it.userId }).associateBy { it.id }

        return members.map {
            MemberResponse.of(
                it,
                userById[it.userId]!!,
            )
        }
    }

    @Transactional
    fun updateTeam(
        teamId: Long,
        request: TeamUpsertRequest,
        user: User,
    ): TeamResponse {
        val member =
            memberRepository.findByUserIdAndTeamId(user.id, teamId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Member not found: ${user.id}, $teamId",
                )
        member.checkMemberRole(MemberRole.MANAGER)

        val team =
            teamRepository.findByIdOrNull(teamId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Team not found: $teamId",
                )

        val (name, description, workMode, profileImageUrl) = request

        if (name != team.name) {
            member.checkMemberRole(MemberRole.LEADER)

            if (teamRepository.existsByName(name)) {
                throw BusinessException(
                    ErrorCode.DUPLICATE_RESOURCE,
                    "Already exists team name: $name",
                )
            }
        }

        team.profileImageUrl?.takeIf { it != profileImageUrl }?.let {
            eventPublisher.publishEvent(ImageDeleteEvent(it))
        }

        team.update(
            name = name,
            description = description,
            workMode = workMode,
            profileImageUrl = profileImageUrl,
        )

        val memberCount = memberRepository.countByTeamId(teamId)

        return TeamResponse.of(team, memberCount)
    }

    @Transactional
    fun updateTeamStatus(
        teamId: Long,
        request: TeamStatusUpdateRequest,
        user: User,
    ) {
        val member =
            memberRepository.findByUserIdAndTeamId(user.id, teamId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Member not found: ${user.id}, $teamId",
                )
        member.checkMemberRole(MemberRole.LEADER)

        val team =
            teamRepository.findByIdOrNull(teamId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Team not found: $teamId",
                )

        team.updateStatus(request.status)

        if (request.status == TeamStatus.COMPLETED) {
            eventPublisher.publishEvent(TeamCompletedEvent(teamId = teamId))
        }

    }

    @Transactional
    fun deleteTeam(
        teamId: Long,
        user: User,
    ) {
        val member =
            memberRepository.findByUserIdAndTeamId(user.id, teamId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Member not found: ${user.id}, $teamId",
                )
        member.checkMemberRole(MemberRole.LEADER)

        val team =
            teamRepository.findByIdOrNull(teamId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Team not found: $teamId",
                )

        team.profileImageUrl?.let {
            eventPublisher.publishEvent(ImageDeleteEvent(it))
        }

        team.delete()
    }
}
