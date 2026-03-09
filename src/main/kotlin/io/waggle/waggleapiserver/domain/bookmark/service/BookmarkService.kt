package io.waggle.waggleapiserver.domain.bookmark.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.bookmark.Bookmark
import io.waggle.waggleapiserver.domain.bookmark.BookmarkId
import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import io.waggle.waggleapiserver.domain.bookmark.dto.request.BookmarkToggleRequest
import io.waggle.waggleapiserver.domain.bookmark.dto.response.BookmarkResponse
import io.waggle.waggleapiserver.domain.bookmark.dto.response.BookmarkToggleResponse
import io.waggle.waggleapiserver.domain.bookmark.repository.BookmarkRepository
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.memberreview.enums.ReviewType
import io.waggle.waggleapiserver.domain.memberreview.repository.MemberReviewRepository
import io.waggle.waggleapiserver.domain.post.dto.response.PostSimpleResponse
import io.waggle.waggleapiserver.domain.post.repository.PostRepository
import io.waggle.waggleapiserver.domain.recruitment.dto.response.RecruitmentResponse
import io.waggle.waggleapiserver.domain.recruitment.repository.RecruitmentRepository
import io.waggle.waggleapiserver.domain.team.dto.response.TeamResponse
import io.waggle.waggleapiserver.domain.team.repository.TeamRepository
import io.waggle.waggleapiserver.domain.user.TemperatureCalculator
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.dto.response.UserSimpleResponse
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class BookmarkService(
    private val bookmarkRepository: BookmarkRepository,
    private val memberRepository: MemberRepository,
    private val memberReviewRepository: MemberReviewRepository,
    private val postRepository: PostRepository,
    private val recruitmentRepository: RecruitmentRepository,
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository,
    private val temperatureCalculator: TemperatureCalculator,
) {
    fun toggleBookmark(
        request: BookmarkToggleRequest,
        user: User,
    ): BookmarkToggleResponse {
        val (targetId, type) = request

        val bookmarkId =
            BookmarkId(
                userId = user.id,
                targetId = targetId,
                type = type,
            )
        return if (bookmarkRepository.existsById(bookmarkId)) {
            bookmarkRepository.deleteById(bookmarkId)
            BookmarkToggleResponse(isBookmarked = false)
        } else {
            val bookmark = Bookmark(bookmarkId)
            bookmarkRepository.save(bookmark)
            BookmarkToggleResponse(isBookmarked = true)
        }
    }

    fun getUserBookmarkables(
        type: BookmarkType,
        user: User,
    ): List<BookmarkResponse> {
        val targetIds =
            bookmarkRepository
                .findByIdUserIdAndIdType(user.id, type)
                .map { it.targetId }

        return when (type) {
            BookmarkType.POST -> {
                val posts = postRepository.findByIdInOrderByCreatedAtDesc(targetIds)
                val authorIds = posts.map { it.userId }.distinct()
                val authorById = userRepository.findAllById(authorIds).associateBy { it.id }

                val reviewCounts = memberReviewRepository.countByRevieweeIdInGroupByType(authorIds)
                val temperatureByUserId =
                    authorIds.associateWith { userId ->
                        val likeCount =
                            reviewCounts.find { it.revieweeId == userId && it.type == ReviewType.LIKE }?.count
                                ?: 0
                        val dislikeCount =
                            reviewCounts.find { it.revieweeId == userId && it.type == ReviewType.DISLIKE }?.count
                                ?: 0
                        temperatureCalculator.calculate(likeCount, dislikeCount)
                    }

                val recruitmentsByPostId =
                    recruitmentRepository.findByPostIdIn(posts.map { it.id }).groupBy { it.postId }
                posts.map { post ->
                    val author =
                        authorById[post.userId]
                            ?: throw BusinessException(
                                ErrorCode.ENTITY_NOT_FOUND,
                                "User not found: ${post.userId}",
                            )
                    val recruitments =
                        (
                            recruitmentsByPostId[post.id]
                                ?: emptyList()
                        ).map { RecruitmentResponse.from(it) }
                    PostSimpleResponse.of(
                        post,
                        UserSimpleResponse.of(author, temperatureByUserId[author.id]!!),
                        recruitments,
                    )
                }
            }

            BookmarkType.TEAM -> {
                teamRepository
                    .findByIdInOrderByCreatedAtDesc(targetIds)
                    .map { team ->
                        val memberCount = memberRepository.countByTeamId(team.id)
                        TeamResponse.of(team, memberCount)
                    }
            }
        }
    }
}
