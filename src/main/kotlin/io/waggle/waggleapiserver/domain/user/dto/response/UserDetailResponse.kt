package io.waggle.waggleapiserver.domain.user.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.domain.user.enums.Skill
import java.time.Instant
import java.util.UUID

@Schema(description = "사용자 상세 응답 DTO")
data class UserDetailResponse(
    @Schema(description = "사용자 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val userId: UUID,
    @Schema(description = "사용자명", example = "testUser")
    val username: String,
    @Schema(description = "이메일", example = "testUser@gmail.com")
    val email: String,
    @Schema(description = "협업 온도", example = "36.5")
    val temperature: Double,
    @Schema(
        description = "프로필 이미지 URL",
        example = "https://avatars.githubusercontent.com/u/112466204?s=80&v=4",
    )
    val profileImageUrl: String?,
    @Schema(description = "직무", example = "BACKEND")
    val position: Position,
    @Schema(description = "보유 스킬", example = "[\"JAVA\", \"SPRING\"]")
    val skills: Set<Skill>,
    @Schema(description = "자기소개")
    val bio: String?,
    @Schema(description = "포트폴리오 URL 목록", example = "[\"https://github.com/user\"]")
    val portfolioUrls: List<String>,
    @Schema(description = "사용자 생성일시", example = "2025-11-16T12:30:45.123456Z")
    val createdAt: Instant,
    @Schema(description = "사용자 수정일시", example = "2025-11-16T12:30:45.123456Z")
    val updatedAt: Instant,
) {
    companion object {
        fun from(user: User): UserDetailResponse =
            UserDetailResponse(
                userId = user.id,
                username = user.username!!,
                temperature = user.temperature,
                email = user.email,
                profileImageUrl = user.profileImageUrl,
                position = user.position!!,
                skills = user.skills,
                bio = user.bio,
                portfolioUrls = user.portfolioUrls,
                createdAt = user.createdAt,
                updatedAt = user.updatedAt,
            )
    }
}
