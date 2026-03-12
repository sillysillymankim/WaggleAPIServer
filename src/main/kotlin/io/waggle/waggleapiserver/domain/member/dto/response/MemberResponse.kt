package io.waggle.waggleapiserver.domain.member.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.member.Member
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.enums.Skill
import java.time.Instant
import java.util.UUID

@Schema(description = "멤버 응답 DTO")
data class MemberResponse(
    @Schema(description = "멤버 ID", example = "1")
    val memberId: Long,
    @Schema(description = "팀 ID", example = "1")
    val teamId: Long,
    @Schema(description = "멤버 사용자 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    val userId: UUID,
    @Schema(description = "사용자명", example = "testUser")
    val username: String,
    @Schema(description = "직무", example = "BACKEND")
    val position: Position,
    @Schema(description = "멤버 역할", example = "LEADER")
    val role: MemberRole,
    @Schema(
        description = "프로필 이미지 URL",
        example = "https://avatars.githubusercontent.com/u/112466204?s=80&v=4",
    )
    val profileImageUrl: String?,
    @Schema(description = "사용자 스킬 목록")
    val skills: Set<Skill>,
    @Schema(description = "멤버 합류일시", example = "2025-11-16T12:30:45.123456Z")
    val createdAt: Instant,
    @Schema(description = "멤버 수정일시", example = "2025-11-16T12:30:45.123456Z")
    val updatedAt: Instant,
) {
    companion object {
        fun of(
            member: Member,
            user: User,
        ): MemberResponse =
            MemberResponse(
                memberId = member.id,
                teamId = member.teamId,
                userId = member.userId,
                username = user.username!!,
                position = member.position,
                role = member.role,
                profileImageUrl = user.profileImageUrl,
                skills = user.skills,
                createdAt = member.createdAt,
                updatedAt = member.updatedAt,
            )
    }
}
