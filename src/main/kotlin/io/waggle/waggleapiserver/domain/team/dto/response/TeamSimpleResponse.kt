package io.waggle.waggleapiserver.domain.team.dto.response

import com.fasterxml.jackson.annotation.JsonInclude
import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.bookmark.dto.response.BookmarkResponse
import io.waggle.waggleapiserver.domain.team.Team
import io.waggle.waggleapiserver.domain.team.enums.TeamStatus
import java.time.Instant

@Schema(description = "팀 응답 DTO")
data class TeamSimpleResponse(
    @Schema(description = "팀 ID", example = "1")
    val teamId: Long,
    @Schema(description = "팀명", example = "Waggle")
    val name: String,
    @Schema(description = "팀 설명")
    val description: String,
    @Schema(description = "팀 상태", example = "ACTIVE")
    val status: TeamStatus,
    @Schema(
        description = "프로필 이미지 URL",
        example = "https://waggle-server.s3.ap-northeast-2.amazonaws.com/prod/teams/6df573f0-9e2e-46b5-ba7f-7d2d2873684b.png",
    )
    val profileImageUrl: String?,
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "프로필 공개 여부", example = "true")
    val isVisible: Boolean? = null,
    @Schema(description = "팀 생성일시", example = "2025-11-16T12:30:45.123456Z")
    val createdAt: Instant,
    @Schema(description = "팀 수정일시", example = "2025-11-16T12:30:45.123456Z")
    val updatedAt: Instant,
) : BookmarkResponse {
    companion object {
        fun from(
            team: Team,
            isVisible: Boolean? = null,
        ) = TeamSimpleResponse(
            teamId = team.id,
            name = team.name,
            description = team.description,
            status = team.status,
            profileImageUrl = team.profileImageUrl,
            isVisible = isVisible,
            createdAt = team.createdAt,
            updatedAt = team.updatedAt,
        )
    }
}
