package io.waggle.waggleapiserver.domain.post.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.post.PostSort
import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.domain.user.enums.Skill

@Schema(description = "모집글 검색 쿼리 DTO")
data class PostGetQuery(
    @Schema(description = "검색 쿼리")
    val q: String? = null,
    @Schema(description = "포지션 필터")
    val positions: Set<Position>? = null,
    @Schema(description = "스킬 필터")
    val skills: Set<Skill>? = null,
    @Schema(description = "정렬 기준", defaultValue = "NEWEST")
    val sort: PostSort = PostSort.NEWEST,
)
