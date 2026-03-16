package io.waggle.waggleapiserver.domain.notification.event

import io.waggle.waggleapiserver.domain.member.MemberRole
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.notification.Notification
import io.waggle.waggleapiserver.domain.notification.NotificationType
import io.waggle.waggleapiserver.domain.notification.repository.NotificationRepository
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
class NotificationEventListener(
    private val memberRepository: MemberRepository,
    private val notificationRepository: NotificationRepository,
) {
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleApplicationReceived(event: ApplicationReceivedEvent) {
        val members =
            memberRepository.findByTeamIdAndRoleIn(
                event.teamId,
                listOf(MemberRole.MANAGER, MemberRole.LEADER),
            )
        val notifications =
            members.map {
                Notification(
                    type = NotificationType.APPLICATION_RECEIVED,
                    teamId = event.teamId,
                    userId = it.userId,
                    triggeredBy = event.triggeredBy,
                )
            }
        notificationRepository.saveAll(notifications)
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleApplicationApproved(event: ApplicationApprovedEvent) {
        notificationRepository.save(
            Notification(
                type = NotificationType.APPLICATION_ACCEPTED,
                teamId = event.teamId,
                userId = event.applicantUserId,
                triggeredBy = event.triggeredBy,
            ),
        )
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleApplicationRejected(event: ApplicationRejectedEvent) {
        notificationRepository.save(
            Notification(
                type = NotificationType.APPLICATION_REJECTED,
                teamId = event.teamId,
                userId = event.applicantUserId,
                triggeredBy = event.triggeredBy,
            ),
        )
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleMemberJoined(event: MemberJoinedEvent) {
        val members = memberRepository.findByTeamIdAndUserIdNot(event.teamId, event.triggeredBy)
        val notifications =
            members.map {
                Notification(
                    type = NotificationType.MEMBER_JOINED,
                    teamId = event.teamId,
                    userId = it.userId,
                    triggeredBy = event.triggeredBy,
                )
            }
        notificationRepository.saveAll(notifications)
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleMemberLeft(event: MemberLeftEvent) {
        val members = memberRepository.findByTeamId(event.teamId)
        val notifications =
            members.map {
                Notification(
                    type = NotificationType.MEMBER_LEFT,
                    teamId = event.teamId,
                    userId = it.userId,
                    triggeredBy = event.triggeredBy,
                )
            }
        notificationRepository.saveAll(notifications)
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleMemberRemoved(event: MemberRemovedEvent) {
        notificationRepository.save(
            Notification(
                type = NotificationType.MEMBER_REMOVED,
                teamId = event.teamId,
                userId = event.removedUserId,
                triggeredBy = event.triggeredBy,
            ),
        )
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleTeamCompleted(event: TeamCompletedEvent) {
        val members = memberRepository.findByTeamId(event.teamId)
        val notifications =
            members.map {
                Notification(
                    type = NotificationType.REVIEW_REQUESTED,
                    teamId = event.teamId,
                    userId = it.userId,
                )
            }
        notificationRepository.saveAll(notifications)
    }
}
