package io.waggle.waggleapiserver.domain.user.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.common.storage.ImageDeleteEvent
import io.waggle.waggleapiserver.common.storage.StorageClient
import io.waggle.waggleapiserver.common.storage.dto.request.PresignedUrlRequest
import io.waggle.waggleapiserver.common.storage.dto.response.PresignedUrlResponse
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.memberreview.enums.ReviewType
import io.waggle.waggleapiserver.domain.memberreview.repository.MemberReviewRepository
import io.waggle.waggleapiserver.domain.team.dto.response.TeamResponse
import io.waggle.waggleapiserver.domain.team.repository.TeamRepository
import io.waggle.waggleapiserver.domain.user.TemperatureCalculator
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.dto.request.UserSetupProfileRequest
import io.waggle.waggleapiserver.domain.user.dto.request.UserUpdateRequest
import io.waggle.waggleapiserver.domain.user.dto.response.UserCheckUsernameResponse
import io.waggle.waggleapiserver.domain.user.dto.response.UserDetailResponse
import io.waggle.waggleapiserver.domain.user.dto.response.UserProfileCompletionResponse
import io.waggle.waggleapiserver.domain.user.dto.response.UserProfileResponse
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageRequest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class UserService(
    private val eventPublisher: ApplicationEventPublisher,
    private val storageClient: StorageClient,
    private val memberRepository: MemberRepository,
    private val memberReviewRepository: MemberReviewRepository,
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository,
    private val temperatureCalculator: TemperatureCalculator,
) {
    @Transactional
    fun setupProfile(
        request: UserSetupProfileRequest,
        user: User,
    ): UserDetailResponse {
        if (user.username != null) {
            throw BusinessException(ErrorCode.INVALID_STATE, "Profile already set up")
        }

        val (username, position, bio, profileImageUrl, skills, portfolioUrls) = request

        if (userRepository.existsByUsername(username)) {
            throw BusinessException(ErrorCode.DUPLICATE_RESOURCE, "$username exists already")
        }

        user.setupProfile(
            username = username,
            position = position,
            bio = bio,
            profileImageUrl = profileImageUrl,
            skills = skills,
            portfolioUrls = portfolioUrls,
        )
        val savedUser = userRepository.save(user)

        return UserDetailResponse.from(savedUser)
    }

    fun generateProfileImagePresignedUrl(
        request: PresignedUrlRequest,
        user: User,
    ): PresignedUrlResponse {
        val presignedUploadUrl =
            storageClient.generateUploadUrl("users", request.contentType)
        return PresignedUrlResponse.from(presignedUploadUrl)
    }

    fun checkUsername(username: String): UserCheckUsernameResponse {
        val isAvailable = !userRepository.existsByUsername(username)
        return UserCheckUsernameResponse(isAvailable)
    }

    fun getUserProfile(userId: UUID): UserProfileResponse {
        val user =
            userRepository.findByIdOrNull(userId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "User not found: $userId")

        return getUserProfile(user)
    }

    fun getUserProfile(user: User): UserProfileResponse {
        user.checkProfileComplete()

        val likeCount = memberReviewRepository.countByRevieweeIdAndType(user.id, ReviewType.LIKE)
        val dislikeCount =
            memberReviewRepository.countByRevieweeIdAndType(user.id, ReviewType.DISLIKE)
        val temperature = temperatureCalculator.calculate(likeCount, dislikeCount)
        val top3LikeTags =
            memberReviewRepository.countTagsByRevieweeIdAndType(
                user.id,
                ReviewType.LIKE,
                PageRequest.of(0, 3),
            )

        return UserProfileResponse.of(user, temperature, top3LikeTags)
    }

    fun getUserTeams(
        userId: UUID,
        includeHidden: Boolean = false,
    ): List<TeamResponse> {
        val members =
            if (includeHidden) {
                memberRepository.findByUserIdOrderByRoleAscCreatedAtAsc(userId)
            } else {
                memberRepository.findByUserIdAndIsVisibleTrueOrderByRoleAscCreatedAtAsc(userId)
            }

        val teamIds = members.map { it.teamId }
        val teamById = teamRepository.findAllById(teamIds).associateBy { it.id }

        return members.mapNotNull { member ->
            teamById[member.teamId]?.let { team ->
                val memberCount = memberRepository.countByTeamId(team.id)
                TeamResponse.of(
                    team = team,
                    memberCount = memberCount,
                    position = if (includeHidden) member.position else null,
                    role = if (includeHidden) member.role else null,
                    isVisible = if (includeHidden) member.isVisible else null,
                )
            }
        }
    }

    fun getUserProfileCompletion(user: User): UserProfileCompletionResponse = UserProfileCompletionResponse(user.isProfileComplete())

    @Transactional
    fun updateTeamVisibility(
        userId: UUID,
        teamId: Long,
        isVisible: Boolean,
    ): TeamResponse {
        val member =
            memberRepository.findByUserIdAndTeamId(userId, teamId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Member not found")

        member.updateVisibility(isVisible)

        val team =
            teamRepository.findByIdOrNull(teamId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Team not found")

        val memberCount = memberRepository.countByTeamId(teamId)

        return TeamResponse.of(
            team = team,
            memberCount = memberCount,
            position = member.position,
            role = member.role,
            isVisible = isVisible,
        )
    }

    @Transactional
    fun updateUser(
        request: UserUpdateRequest,
        user: User,
    ): UserDetailResponse {
        val (position, bio, profileImageUrl, skills, portfolioUrls) = request

        user.profileImageUrl?.takeIf { it != profileImageUrl }?.let {
            eventPublisher.publishEvent(ImageDeleteEvent(it))
        }

        user.update(
            position = position,
            bio = bio,
            profileImageUrl = profileImageUrl,
            skills = skills,
            portfolioUrls = portfolioUrls,
        )
        val savedUser = userRepository.save(user)

        return UserDetailResponse.from(savedUser)
    }
}
