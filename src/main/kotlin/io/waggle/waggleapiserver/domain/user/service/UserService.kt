package io.waggle.waggleapiserver.domain.user.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.common.storage.StorageClient
import io.waggle.waggleapiserver.common.storage.dto.request.PresignedUrlRequest
import io.waggle.waggleapiserver.common.storage.dto.response.PresignedUrlResponse
import io.waggle.waggleapiserver.common.storage.event.ImageDeleteEvent
import io.waggle.waggleapiserver.domain.application.repository.ApplicationRepository
import io.waggle.waggleapiserver.domain.auth.service.AuthService
import io.waggle.waggleapiserver.domain.bookmark.repository.BookmarkRepository
import io.waggle.waggleapiserver.domain.follow.repository.FollowRepository
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.memberreview.enums.ReviewType
import io.waggle.waggleapiserver.domain.memberreview.repository.MemberReviewRepository
import io.waggle.waggleapiserver.domain.notification.repository.NotificationRepository
import io.waggle.waggleapiserver.domain.post.repository.PostRepository
import io.waggle.waggleapiserver.domain.team.dto.response.TeamResponse
import io.waggle.waggleapiserver.domain.team.repository.TeamRepository
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.dto.request.MemberUpdateVisibilityRequest
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
    private val authService: AuthService,
    private val applicationRepository: ApplicationRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val followRepository: FollowRepository,
    private val memberRepository: MemberRepository,
    private val memberReviewRepository: MemberReviewRepository,
    private val notificationRepository: NotificationRepository,
    private val postRepository: PostRepository,
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository,
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

        user.checkProfileComplete()

        return getUserProfile(user)
    }

    fun getUserProfile(user: User): UserProfileResponse {
        val top3LikeTags =
            memberReviewRepository.countTagsByRevieweeIdAndType(
                user.id,
                ReviewType.LIKE,
                PageRequest.of(0, 3),
            )

        return UserProfileResponse.of(user, top3LikeTags)
    }

    fun getUserTeams(
        userId: UUID,
        includeHidden: Boolean = false,
    ): List<TeamResponse> {
        val members =
            if (includeHidden) {
                memberRepository.findByUserIdOrderByRoleAscCreatedAtAsc(userId)
            } else {
                memberRepository.findByUserIdAndVisibleTrueOrderByRoleAscCreatedAtAsc(userId)
            }

        val teamIds = members.map { it.teamId }
        val teamById = teamRepository.findAllById(teamIds).associateBy { it.id }
        val memberCountByTeamId =
            memberRepository.countByTeamIds(teamIds).associate { it.teamId to it.count.toInt() }

        return members.mapNotNull { member ->
            teamById[member.teamId]?.let { team ->
                TeamResponse.of(
                    team = team,
                    memberCount = memberCountByTeamId[team.id] ?: 0,
                    position = member.position,
                    role = member.role,
                    visible = if (includeHidden) member.visible else null,
                )
            }
        }
    }

    fun getUserProfileCompletion(user: User): UserProfileCompletionResponse =
        UserProfileCompletionResponse(isComplete = user.isProfileComplete())

    @Transactional
    fun updateTeamVisibility(
        userId: UUID,
        teamId: Long,
        request: MemberUpdateVisibilityRequest,
    ) {
        val member =
            memberRepository.findByUserIdAndTeamId(userId, teamId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Member not found")

        val (visible) = request
        member.updateVisibility(visible)
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

    @Transactional
    fun deactivateUser(user: User) {
        memberRepository.updateDeletedAtByUserIdAndDeletedAtIsNull(user.id)
        applicationRepository.updateDeletedAtByUserIdAndDeletedAtIsNull(user.id)
        postRepository.updateDeletedAtByUserIdAndDeletedAtIsNull(user.id)
        followRepository.updateDeletedAtByFollowerIdOrFolloweeIdAndDeletedAtIsNull(user.id)
        bookmarkRepository.deleteByIdUserId(user.id)
        notificationRepository.deleteByUserId(user.id)

        authService.deleteRefreshToken(user.id)

        user.deactivate()

        userRepository.save(user)
    }
}
