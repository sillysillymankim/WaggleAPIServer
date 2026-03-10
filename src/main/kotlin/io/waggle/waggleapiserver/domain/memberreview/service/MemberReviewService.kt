package io.waggle.waggleapiserver.domain.memberreview.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.memberreview.MemberReview
import io.waggle.waggleapiserver.domain.memberreview.dto.request.MemberReviewUpsertRequest
import io.waggle.waggleapiserver.domain.memberreview.dto.response.MemberReviewResponse
import io.waggle.waggleapiserver.domain.memberreview.enums.ReviewTag
import io.waggle.waggleapiserver.domain.memberreview.enums.ReviewType
import io.waggle.waggleapiserver.domain.memberreview.repository.MemberReviewRepository
import io.waggle.waggleapiserver.domain.team.repository.TeamRepository
import io.waggle.waggleapiserver.domain.user.TemperatureCalculator
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class MemberReviewService(
    private val memberRepository: MemberRepository,
    private val memberReviewRepository: MemberReviewRepository,
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository,
    private val temperatureCalculator: TemperatureCalculator,
) {
    @Transactional
    fun upsertReview(
        memberId: Long,
        request: MemberReviewUpsertRequest,
        user: User,
    ): MemberReviewResponse {
        val revieweeMember =
            memberRepository.findByIdOrNull(memberId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Member not found: $memberId",
                )

        val teamId = revieweeMember.teamId

        val team =
            teamRepository.findByIdOrNull(teamId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Team not found: $teamId")

        team.checkCompleted()

        val reviewerMember =
            memberRepository.findByUserIdAndTeamId(user.id, teamId)
                ?: throw BusinessException(ErrorCode.ACCESS_DENIED, "Not a member of the team")

        if (reviewerMember.userId == revieweeMember.userId) {
            throw BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Cannot review yourself")
        }

        val existingReview =
            memberReviewRepository.findByReviewerIdAndRevieweeIdAndTeamId(
                user.id,
                revieweeMember.userId,
                teamId,
            )

        val (type, tags) = request

        ReviewTag.validateTags(type, tags)

        val review =
            if (existingReview != null) {
                existingReview.update(type, tags)
                existingReview
            } else {
                memberReviewRepository
                    .save(
                        MemberReview(
                            reviewerId = user.id,
                            revieweeId = revieweeMember.userId,
                            teamId = teamId,
                            type = type,
                        ),
                    ).also { it.tags.addAll(tags) }
            }

        val revieweeUser =
            userRepository.findByIdOrNull(revieweeMember.userId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "User not found: ${revieweeMember.userId}",
                )

        val likeCount =
            memberReviewRepository.countByRevieweeIdAndType(revieweeUser.id, ReviewType.LIKE)
        val dislikeCount =
            memberReviewRepository.countByRevieweeIdAndType(revieweeUser.id, ReviewType.DISLIKE)
        revieweeUser.temperature = temperatureCalculator.calculate(likeCount, dislikeCount)

        return MemberReviewResponse.of(review, revieweeUser.username!!)
    }

    fun getReceivedReviews(userId: UUID): List<MemberReviewResponse> {
        val user =
            userRepository.findByIdOrNull(userId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "User not found: $userId")

        val reviews = memberReviewRepository.findByRevieweeId(userId)
        return reviews.map { MemberReviewResponse.of(it, user.username!!) }
    }

    fun getWrittenReviews(userId: UUID): List<MemberReviewResponse> {
        val reviews = memberReviewRepository.findByReviewerId(userId)
        val revieweeIds = reviews.map { it.revieweeId }.distinct()
        val usernameById =
            userRepository.findAllById(revieweeIds).associate { it.id to it.username!! }

        return reviews.map { review ->
            MemberReviewResponse.of(review, usernameById[review.revieweeId]!!)
        }
    }
}
