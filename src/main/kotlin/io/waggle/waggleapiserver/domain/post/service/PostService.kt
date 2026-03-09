package io.waggle.waggleapiserver.domain.post.service

import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.common.dto.response.CursorResponse
import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.application.repository.ApplicationRepository
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.memberreview.enums.ReviewType
import io.waggle.waggleapiserver.domain.memberreview.repository.MemberReviewRepository
import io.waggle.waggleapiserver.domain.post.Post
import io.waggle.waggleapiserver.domain.post.PostSort
import io.waggle.waggleapiserver.domain.post.dto.request.PostGetQuery
import io.waggle.waggleapiserver.domain.post.dto.request.PostUpsertRequest
import io.waggle.waggleapiserver.domain.post.dto.response.PostDetailResponse
import io.waggle.waggleapiserver.domain.post.dto.response.PostSimpleResponse
import io.waggle.waggleapiserver.domain.post.repository.PostRepository
import io.waggle.waggleapiserver.domain.recruitment.Recruitment
import io.waggle.waggleapiserver.domain.recruitment.dto.response.RecruitmentResponse
import io.waggle.waggleapiserver.domain.recruitment.repository.RecruitmentRepository
import io.waggle.waggleapiserver.domain.team.dto.response.TeamResponse
import io.waggle.waggleapiserver.domain.team.repository.TeamRepository
import io.waggle.waggleapiserver.domain.user.TemperatureCalculator
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.dto.response.UserSimpleResponse
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostService(
    private val applicationRepository: ApplicationRepository,
    private val memberRepository: MemberRepository,
    private val memberReviewRepository: MemberReviewRepository,
    private val postRepository: PostRepository,
    private val recruitmentRepository: RecruitmentRepository,
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository,
    private val temperatureCalculator: TemperatureCalculator,
) {
    @Transactional
    fun createPost(
        request: PostUpsertRequest,
        user: User,
    ): PostDetailResponse {
        val (teamId, title, content, recruitments) = request

        val member =
            memberRepository.findByUserIdAndTeamId(user.id, teamId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Member not found: ${user.id}, $teamId",
                )
        member.checkMemberRole(MemberRole.MEMBER)

        val post =
            Post(
                title = title,
                content = content,
                userId = user.id,
                teamId = teamId,
            )
        val savedPost = postRepository.save(post)

        val savedRecruitments =
            recruitmentRepository.saveAll(
                recruitments.map {
                    Recruitment(
                        position = it.position,
                        count = it.count,
                        postId = savedPost.id,
                        skills = it.skills.toMutableSet(),
                    )
                },
            )

        val team =
            teamRepository.findByIdOrNull(teamId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Team not found: $teamId")
        val memberCount = memberRepository.countByTeamId(teamId)

        val likeCount = memberReviewRepository.countByRevieweeIdAndType(user.id, ReviewType.LIKE)
        val dislikeCount =
            memberReviewRepository.countByRevieweeIdAndType(user.id, ReviewType.DISLIKE)
        val temperature = temperatureCalculator.calculate(likeCount, dislikeCount)

        return PostDetailResponse.of(
            savedPost,
            UserSimpleResponse.of(user, temperature),
            TeamResponse.of(team, memberCount),
            savedRecruitments.map { RecruitmentResponse.from(it) },
        )
    }

    fun getPosts(
        query: PostGetQuery,
        cursorQuery: CursorGetQuery,
        user: User?,
    ): CursorResponse<PostSimpleResponse> {
        val direction =
            when (query.sort) {
                PostSort.NEWEST -> Sort.Direction.DESC
                PostSort.OLDEST -> Sort.Direction.ASC
            }
        val posts =
            postRepository.findWithFilter(
                cursor = cursorQuery.cursor,
                q = query.q,
                positions = query.positions ?: emptySet(),
                skills = query.skills ?: emptySet(),
                sort = query.sort,
                pageable = PageRequest.of(0, cursorQuery.size + 1, Sort.by(direction, "id")),
            )

        val hasNext = posts.size > cursorQuery.size
        val content = if (hasNext) posts.take(cursorQuery.size) else posts
        val nextCursor = if (hasNext) content.last().id else null

        val authorIds = content.map { it.userId }.distinct()
        val authorById = userRepository.findAllById(authorIds).associateBy { it.id }

        // Temperature 일괄 조회
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

        val postIds = content.map { it.id }
        val recruitmentsByPostId =
            recruitmentRepository.findByPostIdIn(postIds).groupBy { it.postId }

        val memberTeamIdSet =
            user?.let {
                memberRepository
                    .findByUserIdOrderByRoleAscCreatedAtAsc(it.id)
                    .map { m -> m.teamId }
                    .toSet()
            } ?: emptySet()

        val memberPostIds = content.filter { it.teamId in memberTeamIdSet }.map { it.id }
        val applicantCountByPostId =
            if (memberPostIds.isNotEmpty()) {
                applicationRepository
                    .countApplicantsGroupByPostId(memberPostIds)
                    .associate { it.postId to it.applicantCount.toInt() }
            } else {
                emptyMap()
            }

        val data =
            content.map { post ->
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
                val applicantCount =
                    if (post.teamId in memberTeamIdSet) {
                        applicantCountByPostId[post.id]
                            ?: 0
                    } else {
                        null
                    }
                PostSimpleResponse.of(
                    post,
                    UserSimpleResponse.of(author, temperatureByUserId[author.id]!!),
                    recruitments,
                    applicantCount,
                )
            }

        return CursorResponse(
            data = data,
            nextCursor = nextCursor,
            hasNext = hasNext,
        )
    }

    fun getPost(
        postId: Long,
        user: User?,
    ): PostDetailResponse {
        val post =
            postRepository.findByIdOrNull(postId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Post not found: $postId")
        val author =
            userRepository.findByIdOrNull(post.userId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "User not found: ${post.userId}",
                )
        val team =
            teamRepository.findByIdOrNull(post.teamId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Team not found: ${post.teamId}",
                )
        val recruitments =
            recruitmentRepository.findByPostId(postId).map { RecruitmentResponse.from(it) }

        val applicantCount =
            user?.let {
                if (memberRepository.existsByUserIdAndTeamId(it.id, post.teamId)) {
                    applicationRepository.countByPostId(postId)
                } else {
                    null
                }
            }

        val memberCount = memberRepository.countByTeamId(team.id)

        val likeCount = memberReviewRepository.countByRevieweeIdAndType(author.id, ReviewType.LIKE)
        val dislikeCount =
            memberReviewRepository.countByRevieweeIdAndType(author.id, ReviewType.DISLIKE)
        val temperature = temperatureCalculator.calculate(likeCount, dislikeCount)

        return PostDetailResponse.of(
            post,
            UserSimpleResponse.of(author, temperature),
            TeamResponse.of(team, memberCount),
            recruitments,
            applicantCount,
        )
    }

    fun getTeamPosts(
        teamId: Long,
        user: User?,
    ): List<PostSimpleResponse> {
        val posts = postRepository.findByTeamIdOrderByCreatedAtDesc(teamId)

        val authorIds = posts.map { it.userId }.distinct()
        val authorById = userRepository.findAllById(authorIds).associateBy { it.id }

        // Temperature 일괄 조회
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

        val postIds = posts.map { it.id }
        val recruitmentsByPostId =
            recruitmentRepository.findByPostIdIn(postIds).groupBy { it.postId }

        val isMember =
            user?.let { memberRepository.existsByUserIdAndTeamId(it.id, teamId) } ?: false

        val applicantCountByPostId =
            if (isMember && postIds.isNotEmpty()) {
                applicationRepository
                    .countApplicantsGroupByPostId(postIds)
                    .associate { it.postId to it.applicantCount.toInt() }
            } else {
                emptyMap()
            }

        return posts.map { post ->
            val author =
                authorById[post.userId]
                    ?: throw BusinessException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "User not found: ${post.userId}",
                    )
            val recruitments =
                (recruitmentsByPostId[post.id] ?: emptyList()).map { RecruitmentResponse.from(it) }
            val applicantCount = if (isMember) applicantCountByPostId[post.id] ?: 0 else null
            PostSimpleResponse.of(
                post,
                UserSimpleResponse.of(author, temperatureByUserId[author.id]!!),
                recruitments,
                applicantCount,
            )
        }
    }

    @Transactional
    fun updatePost(
        postId: Long,
        request: PostUpsertRequest,
        user: User,
    ): PostDetailResponse {
        val (teamId, title, content, recruitments) = request

        val post =
            postRepository.findByIdOrNull(postId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Post not found: $postId")

        post.checkOwnership(user.id)
        post.update(title, content, teamId)

        val existingRecruitments = recruitmentRepository.findByPostId(postId)
        recruitmentRepository.deleteAll(existingRecruitments)

        val savedRecruitments =
            recruitmentRepository.saveAll(
                recruitments.map {
                    Recruitment(
                        position = it.position,
                        count = it.count,
                        postId = postId,
                        skills = it.skills.toMutableSet(),
                    )
                },
            )

        val team =
            teamRepository.findByIdOrNull(post.teamId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Team not found: ${post.teamId}",
                )
        val memberCount = memberRepository.countByTeamId(post.teamId)

        val likeCount = memberReviewRepository.countByRevieweeIdAndType(user.id, ReviewType.LIKE)
        val dislikeCount =
            memberReviewRepository.countByRevieweeIdAndType(user.id, ReviewType.DISLIKE)
        val temperature = temperatureCalculator.calculate(likeCount, dislikeCount)

        return PostDetailResponse.of(
            post,
            UserSimpleResponse.of(user, temperature),
            TeamResponse.of(team, memberCount),
            savedRecruitments.map { RecruitmentResponse.from(it) },
        )
    }

    @Transactional
    fun closePostRecruitments(
        postId: Long,
        user: User,
    ) {
        val post =
            postRepository.findByIdOrNull(postId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Post not found: $postId")

        val member =
            memberRepository.findByUserIdAndTeamId(user.id, post.teamId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Member not found: ${user.id}, ${post.teamId}",
                )
        member.checkMemberRole(MemberRole.MANAGER)

        val recruitments = recruitmentRepository.findByPostId(postId)
        recruitments.forEach { it.close() }
    }

    @Transactional
    fun deletePost(
        postId: Long,
        user: User,
    ) {
        val post =
            postRepository.findByIdOrNull(postId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Post not found: $postId")
        post.checkOwnership(user.id)

        post.delete()
    }
}
