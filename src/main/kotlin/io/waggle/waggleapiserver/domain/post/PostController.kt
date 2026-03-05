package io.waggle.waggleapiserver.domain.post

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.common.dto.response.CursorResponse
import io.waggle.waggleapiserver.common.infrastructure.persistence.resolver.AllowIncompleteProfile
import io.waggle.waggleapiserver.common.infrastructure.persistence.resolver.CurrentUser
import io.waggle.waggleapiserver.domain.post.dto.request.PostGetQuery
import io.waggle.waggleapiserver.domain.post.dto.request.PostUpsertRequest
import io.waggle.waggleapiserver.domain.post.dto.response.PostDetailResponse
import io.waggle.waggleapiserver.domain.post.service.PostService
import io.waggle.waggleapiserver.domain.user.User
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springdoc.core.annotations.ParameterObject
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "모집글")
@RequestMapping("/posts")
@RestController
class PostController(
    private val postService: PostService,
) {
    @Operation(summary = "모집글 생성")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createPost(
        @Valid @RequestBody request: PostUpsertRequest,
        @CurrentUser user: User,
    ): PostDetailResponse = postService.createPost(request, user)

    @AllowIncompleteProfile
    @Operation(summary = "모집글 목록 커서 페이지네이션 조회")
    @GetMapping
    fun getPosts(
        @ParameterObject query: PostGetQuery,
        @ParameterObject cursorQuery: CursorGetQuery,
        @CurrentUser user: User?,
    ): CursorResponse<PostDetailResponse> = postService.getPosts(query, cursorQuery, user)

    @AllowIncompleteProfile
    @Operation(summary = "모집글 상세 조회")
    @GetMapping("/{postId}")
    fun getPost(
        @PathVariable postId: Long,
        @CurrentUser user: User?,
    ): PostDetailResponse = postService.getPost(postId, user)

    @Operation(summary = "모집글 수정")
    @PutMapping("/{postId}")
    fun updatePost(
        @PathVariable postId: Long,
        @Valid @RequestBody request: PostUpsertRequest,
        @CurrentUser user: User,
    ): PostDetailResponse = postService.updatePost(postId, request, user)

    @Operation(summary = "모집 마감")
    @PatchMapping("/{postId}/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun closePostRecruitments(
        @PathVariable postId: Long,
        @CurrentUser user: User,
    ) {
        postService.closePostRecruitments(postId, user)
    }

    @Operation(summary = "모집글 삭제")
    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deletePost(
        @PathVariable postId: Long,
        @CurrentUser user: User,
    ) {
        postService.deletePost(postId, user)
    }
}
