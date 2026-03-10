package io.waggle.waggleapiserver.domain.post.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.bookmark.dto.response.BookmarkResponse
import io.waggle.waggleapiserver.domain.post.Post
import io.waggle.waggleapiserver.domain.recruitment.RecruitmentStatus
import io.waggle.waggleapiserver.domain.recruitment.dto.response.RecruitmentResponse
import io.waggle.waggleapiserver.domain.team.dto.response.TeamResponse
import io.waggle.waggleapiserver.domain.user.dto.response.UserSimpleResponse
import java.time.Instant

@Schema(description = "모집글 상세 응답 DTO")
data class PostDetailResponse(
    @Schema(description = "모집글 ID", example = "1")
    val postId: Long,
    @Schema(description = "모집글 제목", example = "와글에서 기획자 구인합니다")
    val title: String,
    @Schema(description = "모집글 내용")
    val content: String,
    @Schema(description = "팀 정보")
    val team: TeamResponse,
    @Schema(description = "작성자 정보")
    val user: UserSimpleResponse,
    @Schema(description = "모집 중 여부")
    val isRecruiting: Boolean,
    @Schema(description = "모집 정보 목록")
    val recruitments: List<RecruitmentResponse>,
    @Schema(description = "모집글 생성일시", example = "2025-11-16T12:30:45.123456Z")
    val createdAt: Instant,
    @Schema(description = "모집글 수정일시", example = "2025-11-16T12:30:45.123456Z")
    val updatedAt: Instant,
) : BookmarkResponse {
    companion object {
        fun of(
            post: Post,
            user: UserSimpleResponse,
            team: TeamResponse,
            recruitments: List<RecruitmentResponse> = emptyList(),
        ): PostDetailResponse =
            PostDetailResponse(
                postId = post.id,
                title = post.title,
                content = post.content,
                user = user,
                team = team,
                isRecruiting = recruitments.any { it.status == RecruitmentStatus.RECRUITING },
                recruitments = recruitments,
                createdAt = post.createdAt,
                updatedAt = post.updatedAt,
            )
    }
}
