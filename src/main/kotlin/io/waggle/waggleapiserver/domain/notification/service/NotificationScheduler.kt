package io.waggle.waggleapiserver.domain.notification.service

import io.waggle.waggleapiserver.domain.application.ApplicationStatus
import io.waggle.waggleapiserver.domain.application.repository.ApplicationReadRepository
import io.waggle.waggleapiserver.domain.application.repository.ApplicationRepository
import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.notification.Notification
import io.waggle.waggleapiserver.domain.notification.NotificationType
import io.waggle.waggleapiserver.domain.notification.repository.NotificationRepository
import io.waggle.waggleapiserver.domain.recruitment.RecruitmentStatus
import io.waggle.waggleapiserver.domain.recruitment.repository.RecruitmentRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
class NotificationScheduler(
    private val applicationRepository: ApplicationRepository,
    private val applicationReadRepository: ApplicationReadRepository,
    private val memberRepository: MemberRepository,
    private val notificationRepository: NotificationRepository,
    private val recruitmentRepository: RecruitmentRepository,
) {
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    fun remindUnreadApplications() {
        val now = Instant.now()
        val threeDaysAgo = now.minus(3, ChronoUnit.DAYS)
        val twoDaysAgo = now.minus(2, ChronoUnit.DAYS)

        val pendingApplications =
            applicationRepository.findByStatusAndCreatedAtBetween(
                ApplicationStatus.PENDING,
                threeDaysAgo,
                twoDaysAgo,
            )

        if (pendingApplications.isEmpty()) return

        val postIds = pendingApplications.map { it.postId }.distinct()
        val recruitingPostIds =
            recruitmentRepository
                .findByPostIdIn(postIds)
                .filter { it.status == RecruitmentStatus.RECRUITING }
                .map { it.postId }
                .toSet()

        val targetApplications = pendingApplications.filter { it.postId in recruitingPostIds }

        if (targetApplications.isEmpty()) return

        val applicationIds = targetApplications.map { it.id }
        val teamIds = targetApplications.map { it.teamId }.distinct()

        val membersByTeamId =
            teamIds.associateWith { teamId ->
                memberRepository.findByTeamIdAndRoleIn(
                    teamId,
                    listOf(MemberRole.MANAGER, MemberRole.LEADER),
                )
            }

        val memberUserIds =
            membersByTeamId.values
                .flatten()
                .map { it.userId }
                .distinct()

        val userIdToReadApplicationIdSet =
            applicationReadRepository
                .findByApplicationIdInAndUserIdIn(applicationIds, memberUserIds)
                .map { it.userId to it.applicationId }
                .toSet()

        val userIdToRemindedApplicationIdSet =
            notificationRepository
                .findByApplicationIdInAndType(applicationIds, NotificationType.APPLICATION_REMIND)
                .map { it.userId to it.applicationId }
                .toSet()

        val notifications =
            targetApplications.flatMap { application ->
                val members = membersByTeamId[application.teamId] ?: emptyList()
                members
                    .filter { member ->
                        (member.userId to application.id) !in userIdToReadApplicationIdSet &&
                            (member.userId to application.id) !in userIdToRemindedApplicationIdSet
                    }.map { member ->
                        Notification(
                            type = NotificationType.APPLICATION_REMIND,
                            userId = member.userId,
                            teamId = application.teamId,
                            applicationId = application.id,
                            triggeredBy = application.userId,
                        )
                    }
            }

        notificationRepository.saveAll(notifications)
    }
}
