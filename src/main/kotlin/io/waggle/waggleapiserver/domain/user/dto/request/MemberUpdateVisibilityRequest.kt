package io.waggle.waggleapiserver.domain.user.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "프로필 팀 공개 여부 설정 요청")
data class MemberUpdateVisibilityRequest(
    @Schema(description = "프로필에 공개 여부", example = "true", required = true)
    val isVisible: Boolean,
)
