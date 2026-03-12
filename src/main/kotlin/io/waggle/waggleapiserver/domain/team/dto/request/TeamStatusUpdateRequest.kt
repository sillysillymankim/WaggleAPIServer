package io.waggle.waggleapiserver.domain.team.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import io.waggle.waggleapiserver.domain.team.enums.TeamStatus
import jakarta.validation.constraints.NotNull

@Schema(description = "팀 상태 변경 요청 DTO")
data class TeamStatusUpdateRequest(
    @Schema(description = "팀 상태", example = "COMPLETED")
    @field:NotNull
    val status: TeamStatus,
)
