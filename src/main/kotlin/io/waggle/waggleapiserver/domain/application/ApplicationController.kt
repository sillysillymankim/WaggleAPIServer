package io.waggle.waggleapiserver.domain.application

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.infrastructure.persistence.resolver.CurrentUser
import io.waggle.waggleapiserver.domain.application.dto.response.ApplicationResponse
import io.waggle.waggleapiserver.domain.application.service.ApplicationService
import io.waggle.waggleapiserver.domain.user.User
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "팀 지원")
@RequestMapping("/applications")
@RestController
class ApplicationController(
    private val applicationService: ApplicationService,
) {
    @Operation(
        summary = "팀 지원 읽음 처리",
        description = "팀 관리자가 지원을 읽음 처리함",
    )
    @PostMapping("/{applicationId}/read")
    fun markApplicationAsRead(
        @PathVariable applicationId: Long,
        @CurrentUser user: User,
    ): ApplicationResponse = applicationService.markApplicationAsRead(applicationId, user)

    @Operation(
        summary = "팀 지원 승인",
        description = "팀 관리자가 지원자를 승인함",
    )
    @PatchMapping("/{applicationId}/approve")
    fun approveApplication(
        @PathVariable applicationId: Long,
        @CurrentUser user: User,
    ): ApplicationResponse = applicationService.approveApplication(applicationId, user)

    @Operation(
        summary = "팀 지원 거절",
        description = "팀 관리자가 지원자를 거절함",
    )
    @PatchMapping("/{applicationId}/reject")
    fun rejectApplication(
        @PathVariable applicationId: Long,
        @CurrentUser user: User,
    ): ApplicationResponse = applicationService.rejectApplication(applicationId, user)

    @Operation(
        summary = "팀 지원 삭제",
        description = "팀 지원자가 본인의 지원 내역을 삭제함",
    )
    @DeleteMapping("/{applicationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteApplication(
        @PathVariable applicationId: Long,
        @CurrentUser user: User,
    ) {
        applicationService.deleteApplication(applicationId, user)
    }
}
