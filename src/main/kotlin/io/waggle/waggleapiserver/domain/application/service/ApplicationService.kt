package io.waggle.waggleapiserver.domain.application.service

import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.application.Application
import io.waggle.waggleapiserver.domain.application.ApplicationStatus
import io.waggle.waggleapiserver.domain.application.dto.request.ApplicationCreateRequest
import io.waggle.waggleapiserver.domain.application.dto.response.ApplicationResponse
import io.waggle.waggleapiserver.domain.application.repository.ApplicationRepository
import io.waggle.waggleapiserver.domain.member.Member
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.memberreview.enums.ReviewType
import io.waggle.waggleapiserver.domain.memberreview.repository.MemberReviewRepository
import io.waggle.waggleapiserver.domain.post.repository.PostRepository
import io.waggle.waggleapiserver.domain.recruitment.repository.RecruitmentRepository
import io.waggle.waggleapiserver.domain.user.TemperatureCalculator
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class ApplicationService(
    private val applicationRepository: ApplicationRepository,
    private val memberRepository: MemberRepository,
    private val memberReviewRepository: MemberReviewRepository,
    private val postRepository: PostRepository,
    private val recruitmentRepository: RecruitmentRepository,
    private val userRepository: UserRepository,
    private val temperatureCalculator: TemperatureCalculator,
) {
    @Transactional
    fun applyToTeam(
        teamId: Long,
        request: ApplicationCreateRequest,
        user: User,
    ): ApplicationResponse {
        val (postId, position, detail, portfolioUrls) = request

        val post =
            postRepository.findByIdOrNull(postId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Post not found: $postId")

        if (post.teamId != teamId) {
            throw BusinessException(
                ErrorCode.INVALID_STATE,
                "Post $postId does not belong to team $teamId",
            )
        }

        val recruitment =
            recruitmentRepository.findByPostIdAndPosition(postId, position)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Recruitment not found: $postId, $position",
                )

        if (!recruitment.isRecruiting()) {
            throw BusinessException(ErrorCode.INVALID_STATE, "$position is no longer recruiting")
        }

        if (applicationRepository.existsByTeamIdAndUserIdAndPosition(
                teamId,
                user.id,
                position,
            )
        ) {
            throw BusinessException(
                ErrorCode.DUPLICATE_RESOURCE,
                "Already applied to team: $teamId",
            )
        }

        val application =
            Application(
                position = position,
                teamId = teamId,
                postId = postId,
                userId = user.id,
                detail = detail,
            )
        application.portfolioUrls.addAll(portfolioUrls)
        val savedApplication = applicationRepository.save(application)

        val likeCount = memberReviewRepository.countByRevieweeIdAndType(user.id, ReviewType.LIKE)
        val dislikeCount =
            memberReviewRepository.countByRevieweeIdAndType(user.id, ReviewType.DISLIKE)
        val temperature = temperatureCalculator.calculate(likeCount, dislikeCount)

        return ApplicationResponse.of(savedApplication, user, temperature)
    }

    fun getUserApplications(user: User): List<ApplicationResponse> {
        val applications = applicationRepository.findByUserId(user.id)

        val likeCount = memberReviewRepository.countByRevieweeIdAndType(user.id, ReviewType.LIKE)
        val dislikeCount =
            memberReviewRepository.countByRevieweeIdAndType(user.id, ReviewType.DISLIKE)
        val temperature = temperatureCalculator.calculate(likeCount, dislikeCount)

        return applications.map { ApplicationResponse.of(it, user, temperature) }
    }

    fun getTeamApplications(
        teamId: Long,
        user: User,
    ): List<ApplicationResponse> {
        val member =
            memberRepository.findByUserIdAndTeamId(user.id, teamId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Member not found")
        member.checkMemberRole(MemberRole.MEMBER)

        val applications = applicationRepository.findByTeamId(teamId)
        val applicantIds = applications.map { it.userId }.distinct()
        val applicantById = userRepository.findAllById(applicantIds).associateBy { it.id }

        val reviewCounts = memberReviewRepository.countByRevieweeIdInGroupByType(applicantIds)
        val temperatureByUserId =
            applicantIds.associateWith { userId ->
                val likeCount =
                    reviewCounts.find { it.revieweeId == userId && it.type == ReviewType.LIKE }?.count
                        ?: 0
                val dislikeCount =
                    reviewCounts.find { it.revieweeId == userId && it.type == ReviewType.DISLIKE }?.count
                        ?: 0
                temperatureCalculator.calculate(likeCount, dislikeCount)
            }

        return applications.map { application ->
            val applicant =
                applicantById[application.userId]
                    ?: throw BusinessException(
                        ErrorCode.ENTITY_NOT_FOUND,
                        "User not found: ${application.userId}",
                    )
            ApplicationResponse.of(application, applicant, temperatureByUserId[applicant.id]!!)
        }
    }

    @Transactional
    fun approveApplication(
        applicationId: Long,
        user: User,
    ): ApplicationResponse {
        val application =
            applicationRepository.findByIdOrNull(applicationId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Application not found: $applicationId",
                )

        val approver =
            memberRepository.findByUserIdAndTeamId(user.id, application.teamId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Member not found")
        approver.checkMemberRole(MemberRole.MANAGER)

        application.updateStatus(ApplicationStatus.APPROVED)

        if (!memberRepository.existsByUserIdAndTeamId(application.userId, application.teamId)) {
            val member =
                Member(
                    userId = application.userId,
                    teamId = application.teamId,
                    position = application.position,
                    role = MemberRole.MEMBER,
                )
            memberRepository.save(member)
        }

        val applicant =
            userRepository.findByIdOrNull(application.userId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "User not found: ${application.userId}",
                )

        val likeCount =
            memberReviewRepository.countByRevieweeIdAndType(applicant.id, ReviewType.LIKE)
        val dislikeCount =
            memberReviewRepository.countByRevieweeIdAndType(applicant.id, ReviewType.DISLIKE)
        val temperature = temperatureCalculator.calculate(likeCount, dislikeCount)

        return ApplicationResponse.of(application, applicant, temperature)
    }

    @Transactional
    fun rejectApplication(
        applicationId: Long,
        user: User,
    ): ApplicationResponse {
        val application =
            applicationRepository.findByIdOrNull(applicationId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Application not found: $applicationId",
                )

        val member =
            memberRepository.findByUserIdAndTeamId(user.id, application.teamId)
                ?: throw BusinessException(ErrorCode.ENTITY_NOT_FOUND, "Member not found")
        member.checkMemberRole(MemberRole.MANAGER)

        application.updateStatus(ApplicationStatus.REJECTED)

        val applicant =
            userRepository.findByIdOrNull(application.userId)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "User not found: ${application.userId}",
                )

        val likeCount =
            memberReviewRepository.countByRevieweeIdAndType(applicant.id, ReviewType.LIKE)
        val dislikeCount =
            memberReviewRepository.countByRevieweeIdAndType(applicant.id, ReviewType.DISLIKE)
        val temperature = temperatureCalculator.calculate(likeCount, dislikeCount)

        return ApplicationResponse.of(application, applicant, temperature)
    }

    @Transactional
    fun deleteApplication(
        applicationId: Long,
        user: User,
    ) {
        val application =
            applicationRepository.findByIdAndUserId(applicationId, user.id)
                ?: throw BusinessException(
                    ErrorCode.ENTITY_NOT_FOUND,
                    "Application not found: $applicationId",
                )
        application.delete()
    }
}
