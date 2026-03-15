package io.waggle.waggleapiserver.domain.notification.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotEmpty

data class ReadNotificationsRequest(
    @Schema(description = "읽음 처리할 알림 ID 목록")
    @field:NotEmpty
    val notificationIds: List<Long>,
)
