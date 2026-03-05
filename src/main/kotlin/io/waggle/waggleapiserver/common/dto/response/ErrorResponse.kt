package io.waggle.waggleapiserver.common.dto.response

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "에러 응답 DTO")
data class ErrorResponse(
    @Schema(description = "HTTP 상태 코드", example = "400")
    val status: Int,
    @Schema(description = "에러 코드", example = "INVALID_INPUT")
    val code: String,
    @Schema(description = "에러 메시지", example = "잘못된 요청입니다")
    val message: String,
    @Schema(description = "에러 상세 정보 (옵셔널)", example = "필드 'email'은 필수입니다")
    val detail: String? = null,
)
