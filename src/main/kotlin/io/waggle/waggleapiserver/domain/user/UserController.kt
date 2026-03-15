package io.waggle.waggleapiserver.domain.user

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.common.dto.response.CursorResponse
import io.waggle.waggleapiserver.common.infrastructure.persistence.resolver.AllowIncompleteProfile
import io.waggle.waggleapiserver.common.infrastructure.persistence.resolver.CurrentUser
import io.waggle.waggleapiserver.common.storage.dto.request.PresignedUrlRequest
import io.waggle.waggleapiserver.common.storage.dto.response.PresignedUrlResponse
import io.waggle.waggleapiserver.domain.application.dto.response.ApplicationResponse
import io.waggle.waggleapiserver.domain.application.service.ApplicationService
import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import io.waggle.waggleapiserver.domain.bookmark.dto.response.BookmarkResponse
import io.waggle.waggleapiserver.domain.bookmark.service.BookmarkService
import io.waggle.waggleapiserver.domain.follow.dto.response.FollowCountResponse
import io.waggle.waggleapiserver.domain.follow.service.FollowService
import io.waggle.waggleapiserver.domain.memberreview.dto.response.MemberReviewResponse
import io.waggle.waggleapiserver.domain.memberreview.enums.ReviewQueryType
import io.waggle.waggleapiserver.domain.memberreview.service.MemberReviewService
import io.waggle.waggleapiserver.domain.notification.dto.request.ReadNotificationsRequest
import io.waggle.waggleapiserver.domain.notification.dto.response.NotificationCountResponse
import io.waggle.waggleapiserver.domain.notification.dto.response.NotificationResponse
import io.waggle.waggleapiserver.domain.notification.service.NotificationService
import io.waggle.waggleapiserver.domain.team.dto.response.TeamResponse
import io.waggle.waggleapiserver.domain.user.dto.request.MemberUpdateVisibilityRequest
import io.waggle.waggleapiserver.domain.user.dto.request.UserSetupProfileRequest
import io.waggle.waggleapiserver.domain.user.dto.request.UserUpdateRequest
import io.waggle.waggleapiserver.domain.user.dto.response.UserCheckUsernameResponse
import io.waggle.waggleapiserver.domain.user.dto.response.UserDetailResponse
import io.waggle.waggleapiserver.domain.user.dto.response.UserProfileCompletionResponse
import io.waggle.waggleapiserver.domain.user.dto.response.UserProfileResponse
import io.waggle.waggleapiserver.domain.user.dto.response.UserSimpleResponse
import io.waggle.waggleapiserver.domain.user.service.UserService
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "사용자")
@RequestMapping("/users")
@RestController
class UserController(
    private val applicationService: ApplicationService,
    private val bookmarkService: BookmarkService,
    private val followService: FollowService,
    private val memberReviewService: MemberReviewService,
    private val notificationService: NotificationService,
    private val userService: UserService,
) {
    @AllowIncompleteProfile
    @Operation(summary = "사용자 프로필 초기 설정")
    @PostMapping("/me/profile")
    fun setupProfile(
        @Valid @RequestBody request: UserSetupProfileRequest,
        @CurrentUser user: User,
    ): UserDetailResponse = userService.setupProfile(request, user)

    @AllowIncompleteProfile
    @Operation(summary = "사용자 프로필 이미지 업로드용 Presigned URL 생성")
    @PostMapping("/me/profile-image/presigned-url")
    fun generateProfileImagePresignedUrl(
        @Valid @RequestBody request: PresignedUrlRequest,
        @CurrentUser user: User,
    ): PresignedUrlResponse = userService.generateProfileImagePresignedUrl(request, user)

    @Operation(summary = "사용자명 사용 가능 여부 조회")
    @GetMapping("/check")
    fun checkUsername(
        @RequestParam username: String,
    ): UserCheckUsernameResponse = userService.checkUsername(username)

    @Operation(summary = "사용자 조회")
    @GetMapping("/{userId}")
    fun getUser(
        @PathVariable userId: UUID,
    ): UserProfileResponse = userService.getUserProfile(userId)

    @Operation(summary = "사용자 팔로우 개수 정보 조회")
    @GetMapping("/{userId}/follow-count")
    fun getUserFollowCount(
        @PathVariable userId: UUID,
    ): FollowCountResponse = followService.getUserFollowCount(userId)

    @Deprecated(
        message = "사용자 프로필 조회 API(GET /users/{userId})에서 온도와 상위 태그를 제공합니다.",
        replaceWith = ReplaceWith("getUser(userId)"),
        level = DeprecationLevel.WARNING,
    )
    @Operation(
        summary = "사용자가 받은 리뷰 목록 조회",
        deprecated = true,
        description = "⚠️ Deprecated: 사용자 프로필 조회 API를 사용하세요",
    )
    @GetMapping("/{userId}/reviews")
    fun getUserReviews(
        @PathVariable userId: UUID,
    ): List<MemberReviewResponse> = memberReviewService.getReceivedReviews(userId)

    @Operation(summary = "사용자 참여 팀 목록 조회")
    @GetMapping("/{userId}/teams")
    fun getUserTeams(
        @PathVariable userId: UUID,
    ): List<TeamResponse> = userService.getUserTeams(userId, includeHidden = false)

    @Operation(summary = "본인 프로필 조회")
    @GetMapping("/me")
    fun getMyProfile(
        @CurrentUser user: User,
    ): UserProfileResponse = userService.getUserProfile(user)

    @Operation(summary = "본인 지원 목록 조회")
    @GetMapping("/me/applications")
    fun getMyApplications(
        @CurrentUser user: User,
    ): List<ApplicationResponse> = applicationService.getUserApplications(user)

    @Deprecated(
        message = "본인 프로필 조회 API(GET /users/me)에서 온도와 상위 태그를 제공합니다.",
        replaceWith = ReplaceWith("getMyProfile(user)"),
        level = DeprecationLevel.WARNING,
    )
    @Operation(
        summary = "본인 리뷰 목록 조회",
        deprecated = true,
        description = "⚠️ Deprecated: 본인 프로필 조회 API를 사용하세요",
    )
    @GetMapping("/me/reviews")
    fun getMyReviews(
        @RequestParam type: ReviewQueryType,
        @CurrentUser user: User,
    ): List<MemberReviewResponse> =
        when (type) {
            ReviewQueryType.RECEIVED -> memberReviewService.getReceivedReviews(user.id)
            ReviewQueryType.WRITTEN -> memberReviewService.getWrittenReviews(user.id)
        }

    @Operation(summary = "본인 북마크 목록 조회")
    @GetMapping("/me/bookmarks")
    fun getMyBookmarks(
        @RequestParam type: BookmarkType,
        @CurrentUser user: User,
    ): List<BookmarkResponse> = bookmarkService.getUserBookmarkables(type, user)

    @Operation(summary = "본인이 팔로우 하는 계정 목록 조회")
    @GetMapping("/me/followees")
    fun getMyFollowees(
        @CurrentUser user: User,
    ): List<UserSimpleResponse> = followService.getUserFollowees(user.id)

    @Operation(summary = "본인을 팔로우 하는 계정 목록 조회")
    @GetMapping("/me/followers")
    fun getMyFollowers(
        @CurrentUser user: User,
    ): List<UserSimpleResponse> = followService.getUserFollowers(user.id)

    @Operation(summary = "본인 알림 목록 조회")
    @GetMapping("/me/notifications")
    fun getMyNotifications(
        @ParameterObject cursorQuery: CursorGetQuery,
        @CurrentUser user: User,
    ): CursorResponse<NotificationResponse> = notificationService.getUserNotifications(cursorQuery, user)

    @Operation(summary = "본인 알림 개수 조회")
    @GetMapping("/me/notifications/count")
    fun getMyNotificationCount(
        @CurrentUser user: User,
    ): NotificationCountResponse = notificationService.getNotificationCount(user)

    @AllowIncompleteProfile
    @Operation(summary = "프로필 완성 여부 조회")
    @GetMapping("/me/profile-completion")
    fun getMyProfileCompletion(
        @CurrentUser user: User,
    ): UserProfileCompletionResponse = userService.getUserProfileCompletion(user)

    @Operation(summary = "본인 참여 팀 목록 조회")
    @GetMapping("/me/teams")
    fun getMyTeams(
        @CurrentUser user: User,
    ): List<TeamResponse> = userService.getUserTeams(user.id, includeHidden = true)

    @Operation(summary = "본인 팀 공개/비공개 설정")
    @PatchMapping("/me/teams/{teamId}/visibility")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateMyTeamVisibility(
        @PathVariable teamId: Long,
        @Valid @RequestBody request: MemberUpdateVisibilityRequest,
        @CurrentUser user: User,
    ) = userService.updateTeamVisibility(user.id, teamId, request)

    @Operation(summary = "본인 알림 읽음 처리")
    @PatchMapping("/me/notifications/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun readMyNotifications(
        @Valid @RequestBody request: ReadNotificationsRequest,
        @CurrentUser user: User,
    ) = notificationService.readNotifications(user, request.notificationIds)

    @Operation(summary = "본인 알림 전체 읽음 처리")
    @PatchMapping("/me/notifications/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun readAllMyNotifications(
        @CurrentUser user: User,
    ) = notificationService.readAllNotifications(user)

    @Operation(summary = "본인 프로필 수정")
    @PutMapping("/me")
    fun updateMe(
        @Valid @RequestBody request: UserUpdateRequest,
        @CurrentUser user: User,
    ): UserDetailResponse = userService.updateUser(request, user)
}
