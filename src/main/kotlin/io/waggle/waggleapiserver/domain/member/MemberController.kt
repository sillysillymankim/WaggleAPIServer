package io.waggle.waggleapiserver.domain.member

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.infrastructure.persistence.resolver.CurrentUser
import io.waggle.waggleapiserver.domain.member.dto.request.MemberUpdateRoleRequest
import io.waggle.waggleapiserver.domain.member.dto.response.MemberResponse
import io.waggle.waggleapiserver.domain.member.service.MemberService
import io.waggle.waggleapiserver.domain.memberreview.dto.request.MemberReviewUpsertRequest
import io.waggle.waggleapiserver.domain.memberreview.dto.response.MemberReviewResponse
import io.waggle.waggleapiserver.domain.memberreview.service.MemberReviewService
import io.waggle.waggleapiserver.domain.user.User
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "팀 멤버")
@RequestMapping("/members")
@RestController
class MemberController(
    private val memberReviewService: MemberReviewService,
    private val memberService: MemberService,
) {
    @Operation(summary = "팀원 리뷰 작성/수정")
    @PutMapping("/{memberId}/reviews")
    fun upsertReview(
        @PathVariable memberId: Long,
        @Valid @RequestBody request: MemberReviewUpsertRequest,
        @CurrentUser user: User,
    ): MemberReviewResponse = memberReviewService.upsertReview(memberId, request, user)

    @Operation(
        summary = "멤버 역할 변경",
        description = "본인의 멤버 권한에 따라 타 멤버의 권한을 변경함",
    )
    @PatchMapping("/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun updateMemberRole(
        @PathVariable memberId: Long,
        @RequestBody request: MemberUpdateRoleRequest,
        @CurrentUser user: User,
    ) = memberService.updateMemberRole(memberId, request, user)

    @Operation(
        summary = "멤버 추방",
        description = "리더가 타 멤버를 추방함",
    )
    @DeleteMapping("/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun removeMember(
        @PathVariable memberId: Long,
        @CurrentUser user: User,
    ) {
        memberService.removeMember(memberId, user)
    }
}
