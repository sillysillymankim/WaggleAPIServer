package io.waggle.waggleapiserver.domain.conversation

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.common.dto.response.CursorResponse
import io.waggle.waggleapiserver.common.infrastructure.persistence.CurrentUser
import io.waggle.waggleapiserver.domain.conversation.dto.response.ConversationResponse
import io.waggle.waggleapiserver.domain.conversation.service.ConversationService
import io.waggle.waggleapiserver.domain.user.User
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@Tag(name = "대화")
@RestController
@RequestMapping("/conversations")
class ConversationController(
    private val conversationService: ConversationService,
) {
    @Operation(summary = "대화방 목록 조회")
    @GetMapping
    fun getConversations(
        @ParameterObject cursorQuery: CursorGetQuery,
        @CurrentUser user: User,
    ): CursorResponse<ConversationResponse> = conversationService.getConversations(cursorQuery, user)

    @Operation(summary = "대화방 읽음 처리")
    @PatchMapping("/{partnerId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun readMessages(
        @PathVariable partnerId: UUID,
        @CurrentUser user: User,
    ) = conversationService.readMessages(partnerId, user)
}
