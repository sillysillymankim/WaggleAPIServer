package io.waggle.waggleapiserver.common.dto.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "커서 방향")
enum class CursorDirection {
    @Schema(description = "커서 기준 이전 (과거) 데이터 조회")
    BEFORE,

    @Schema(description = "커서 기준 이후 (최신) 데이터 조회")
    AFTER,
}

@Schema(description = "커서 기반 페이지네이션 쿼리 DTO")
data class CursorGetQuery(
    @Schema(description = "커서 (다음 페이지 조회 시 이전 응답의 nextCursor 값 사용)", example = "100")
    val cursor: Long?,
    @Schema(description = "페이지 크기", example = "20", defaultValue = "20")
    val size: Int = 20,
    @Schema(description = "커서 기준 조회 방향", example = "BEFORE")
    val direction: CursorDirection? = null,
)
