package io.waggle.waggleapiserver.domain.post

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "모집글 정렬 기준")
enum class PostSort {
    @Schema(description = "최신순")
    NEWEST,

    @Schema(description = "오래된순")
    OLDEST,
}
