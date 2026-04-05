package io.waggle.waggleapiserver.domain.auth.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "WebSocket 연결 토큰 응답 DTO")
data class WsTokenResponse(
    @Schema(
        description = "일회용 WebSocket 연결 토큰 (1분 유효)",
        example = "550e8400-e29b-41d4-a716-446655440000",
    )
    val token: String,
)
