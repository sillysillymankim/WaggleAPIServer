package io.waggle.waggleapiserver.domain.team

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.common.dto.response.CursorResponse
import io.waggle.waggleapiserver.common.infrastructure.persistence.resolver.AllowIncompleteProfile
import io.waggle.waggleapiserver.common.infrastructure.persistence.resolver.CurrentUser
import io.waggle.waggleapiserver.common.storage.dto.request.PresignedUrlRequest
import io.waggle.waggleapiserver.common.storage.dto.response.PresignedUrlResponse
import io.waggle.waggleapiserver.domain.application.dto.request.ApplicationCreateRequest
import io.waggle.waggleapiserver.domain.application.dto.response.ApplicationResponse
import io.waggle.waggleapiserver.domain.application.service.ApplicationService
import io.waggle.waggleapiserver.domain.member.dto.response.MemberResponse
import io.waggle.waggleapiserver.domain.member.service.MemberService
import io.waggle.waggleapiserver.domain.post.dto.response.PostSimpleResponse
import io.waggle.waggleapiserver.domain.post.service.PostService
import io.waggle.waggleapiserver.domain.team.dto.request.TeamStatusUpdateRequest
import io.waggle.waggleapiserver.domain.team.dto.request.TeamUpsertRequest
import io.waggle.waggleapiserver.domain.team.dto.response.TeamResponse
import io.waggle.waggleapiserver.domain.team.service.TeamService
import io.waggle.waggleapiserver.domain.user.User
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "팀")
@RequestMapping("/teams")
@RestController
class TeamController(
    private val applicationService: ApplicationService,
    private val memberService: MemberService,
    private val postService: PostService,
    private val teamService: TeamService,
) {
    @Operation(summary = "팀 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createTeam(
        @Valid @RequestBody request: TeamUpsertRequest,
        @CurrentUser user: User,
    ): TeamResponse = teamService.createTeam(request, user)

    @Operation(
        summary = "팀 지원",
        description = "사용자가 해당 팀 합류를 지원함",
    )
    @PostMapping("/{teamId}/applications")
    @ResponseStatus(HttpStatus.CREATED)
    fun applyToTeam(
        @PathVariable teamId: Long,
        @Valid @RequestBody request: ApplicationCreateRequest,
        @CurrentUser user: User,
    ): ApplicationResponse = applicationService.applyToTeam(teamId, request, user)

    @Operation(summary = "팀 프로필 이미지 업로드용 Presigned URL 생성")
    @PostMapping("/profile-image/presigned-url")
    fun generateProfileImagePresignedUrl(
        @Valid @RequestBody request: PresignedUrlRequest,
        @CurrentUser user: User,
    ): PresignedUrlResponse = teamService.generateProfileImagePresignedUrl(request)

    @Operation(summary = "팀 상세 조회")
    @GetMapping("/{teamId}")
    fun getTeam(
        @PathVariable teamId: Long,
    ): TeamResponse = teamService.getTeam(teamId)

    @AllowIncompleteProfile
    @Operation(summary = "팀 멤버 목록 조회")
    @GetMapping("/{teamId}/members")
    fun getTeamMembers(
        @PathVariable teamId: Long,
        @CurrentUser user: User?,
    ): List<MemberResponse> = teamService.getTeamMembers(teamId, user)

    @Operation(
        summary = "팀 지원 목록 조회",
        description = "팀 멤버 권한 사용자가 팀 지원 목록을 조회함",
    )
    @GetMapping("/{teamId}/applications")
    fun getTeamApplications(
        @PathVariable teamId: Long,
        @RequestParam(required = false) postId: Long?,
        @ParameterObject cursorQuery: CursorGetQuery,
        @CurrentUser user: User,
    ): CursorResponse<ApplicationResponse> = applicationService.getTeamApplications(teamId, postId, cursorQuery, user)

    @AllowIncompleteProfile
    @Operation(summary = "팀 모집글 목록 조회")
    @GetMapping("/{teamId}/posts")
    fun getTeamPosts(
        @PathVariable teamId: Long,
        @CurrentUser user: User?,
    ): List<PostSimpleResponse> = postService.getTeamPosts(teamId, user)

    @Operation(summary = "팀 수정")
    @PutMapping("/{teamId}")
    fun updateTeam(
        @PathVariable teamId: Long,
        @Valid @RequestBody request: TeamUpsertRequest,
        @CurrentUser user: User,
    ): TeamResponse = teamService.updateTeam(teamId, request, user)

    @Operation(summary = "팀 상태 변경")
    @PatchMapping("/{teamId}/status")
    fun updateTeamStatus(
        @PathVariable teamId: Long,
        @Valid @RequestBody request: TeamStatusUpdateRequest,
        @CurrentUser user: User,
    ): TeamResponse = teamService.updateTeamStatus(teamId, request, user)

    @Operation(summary = "팀 삭제")
    @DeleteMapping("/{teamId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteTeam(
        @PathVariable teamId: Long,
        @CurrentUser user: User,
    ) {
        teamService.deleteTeam(teamId, user)
    }

    @Operation(
        summary = "팀 이탈",
        description = "멤버가 본인 혼자일 때는 이탈 불가, 본인이 리더일 때는 리더 위임 후 이탈",
    )
    @DeleteMapping("/{teamId}/members")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun leaveTeam(
        @PathVariable teamId: Long,
        @CurrentUser user: User,
    ) {
        memberService.leaveTeam(teamId, user)
    }
}
