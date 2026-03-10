package io.waggle.waggleapiserver.domain.follow.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "팔로우 토글 응답 DTO")
data class FollowToggleResponse(
    @Schema(description = "팔로우 여부", example = "true")
    val isFollowing: Boolean,
)
