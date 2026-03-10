package io.waggle.waggleapiserver.domain.follow

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.infrastructure.persistence.resolver.CurrentUser
import io.waggle.waggleapiserver.domain.follow.dto.request.FollowToggleRequest
import io.waggle.waggleapiserver.domain.follow.dto.response.FollowToggleResponse
import io.waggle.waggleapiserver.domain.follow.service.FollowService
import io.waggle.waggleapiserver.domain.user.User
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "팔로우")
@RequestMapping("/follows")
@RestController
class FollowController(
    private val followService: FollowService,
) {
    @Operation(summary = "팔로우 토글(ON/OFF)")
    @PostMapping
    fun toggleFollow(
        @Valid @RequestBody request: FollowToggleRequest,
        @CurrentUser user: User,
    ): FollowToggleResponse = followService.toggleFollow(request, user)
}
