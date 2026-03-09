package io.waggle.waggleapiserver.domain.follow.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.follow.Follow
import io.waggle.waggleapiserver.domain.follow.dto.request.FollowToggleRequest
import io.waggle.waggleapiserver.domain.follow.dto.response.FollowCountResponse
import io.waggle.waggleapiserver.domain.follow.repository.FollowRepository
import io.waggle.waggleapiserver.domain.memberreview.enums.ReviewType
import io.waggle.waggleapiserver.domain.memberreview.repository.MemberReviewRepository
import io.waggle.waggleapiserver.domain.user.TemperatureCalculator
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.dto.response.UserSimpleResponse
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional(readOnly = true)
class FollowService(
    private val followRepository: FollowRepository,
    private val memberReviewRepository: MemberReviewRepository,
    private val userRepository: UserRepository,
    private val temperatureCalculator: TemperatureCalculator,
) {
    @Transactional
    fun toggleFollow(
        request: FollowToggleRequest,
        user: User,
    ): Boolean {
        val followeeId = request.userId
        val followerId = user.id

        if (followeeId == user.id) {
            throw BusinessException(ErrorCode.INVALID_INPUT_VALUE, "Cannot follow yourself")
        }

        if (!userRepository.existsById(followeeId)) {
            throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "User not found: $followeeId")
        }

        if (followRepository.existsByFollowerIdAndFolloweeId(followerId, followeeId)) {
            followRepository.deleteByFollowerIdAndFolloweeId(followerId, followeeId)
            return false
        }

        val follow =
            Follow(
                followerId = followerId,
                followeeId = followeeId,
            )
        followRepository.save(follow)

        return true
    }

    fun getUserFollowees(userId: UUID): List<UserSimpleResponse> {
        val follows = followRepository.findByFollowerId(userId)

        val followeeIds = follows.map { it.followeeId }
        val userById = userRepository.findAllById(followeeIds).associateBy { it.id }

        // Temperature 일괄 조회
        val reviewCounts = memberReviewRepository.countByRevieweeIdInGroupByType(followeeIds)
        val temperatureByUserId =
            followeeIds.associateWith { uid ->
                val likeCount =
                    reviewCounts.find { it.revieweeId == uid && it.type == ReviewType.LIKE }?.count
                        ?: 0
                val dislikeCount =
                    reviewCounts.find { it.revieweeId == uid && it.type == ReviewType.DISLIKE }?.count
                        ?: 0
                temperatureCalculator.calculate(likeCount, dislikeCount)
            }

        return follows.map { follow ->
            val user =
                userById[follow.followeeId]
                    ?: throw BusinessException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "User not found: ${follow.followerId}",
                    )
            UserSimpleResponse.of(user, temperatureByUserId[user.id]!!)
        }
    }

    fun getUserFollowers(userId: UUID): List<UserSimpleResponse> {
        val follows = followRepository.findByFolloweeId(userId)

        val followerIds = follows.map { it.followerId }
        val userById = userRepository.findAllById(followerIds).associateBy { it.id }

        // Temperature 일괄 조회
        val reviewCounts = memberReviewRepository.countByRevieweeIdInGroupByType(followerIds)
        val temperatureByUserId =
            followerIds.associateWith { uid ->
                val likeCount =
                    reviewCounts.find { it.revieweeId == uid && it.type == ReviewType.LIKE }?.count
                        ?: 0
                val dislikeCount =
                    reviewCounts.find { it.revieweeId == uid && it.type == ReviewType.DISLIKE }?.count
                        ?: 0
                temperatureCalculator.calculate(likeCount, dislikeCount)
            }

        return follows.map { follow ->
            val user =
                userById[follow.followerId]
                    ?: throw BusinessException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "User not found: ${follow.followeeId}",
                    )
            UserSimpleResponse.of(user, temperatureByUserId[user.id]!!)
        }
    }

    fun getUserFollowCount(userId: UUID): FollowCountResponse =
        FollowCountResponse(
            followedCount = followRepository.countByFolloweeId(userId).toInt(),
            followingCount = followRepository.countByFollowerId(userId).toInt(),
        )
}
