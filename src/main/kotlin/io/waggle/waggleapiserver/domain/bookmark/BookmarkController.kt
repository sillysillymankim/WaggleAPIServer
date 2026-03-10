package io.waggle.waggleapiserver.domain.bookmark

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.infrastructure.persistence.resolver.CurrentUser
import io.waggle.waggleapiserver.domain.bookmark.dto.request.BookmarkToggleRequest
import io.waggle.waggleapiserver.domain.bookmark.dto.response.BookmarkToggleResponse
import io.waggle.waggleapiserver.domain.bookmark.service.BookmarkService
import io.waggle.waggleapiserver.domain.user.User
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "북마크")
@RequestMapping("/bookmarks")
@RestController
class BookmarkController(
    private val bookmarkService: BookmarkService,
) {
    @Operation(summary = "북마크 토글(ON/OFF)")
    @PostMapping
    fun toggleBookmark(
        @Valid @RequestBody request: BookmarkToggleRequest,
        @CurrentUser user: User,
    ): BookmarkToggleResponse = bookmarkService.toggleBookmark(request, user)
}
