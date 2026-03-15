package io.waggle.waggleapiserver.domain.notification.service

import io.waggle.waggleapiserver.common.dto.request.CursorGetQuery
import io.waggle.waggleapiserver.common.dto.response.CursorResponse
import io.waggle.waggleapiserver.domain.member.repository.MemberRepository
import io.waggle.waggleapiserver.domain.notification.dto.response.NotificationCountResponse
import io.waggle.waggleapiserver.domain.notification.dto.response.NotificationResponse
import io.waggle.waggleapiserver.domain.notification.repository.NotificationRepository
import io.waggle.waggleapiserver.domain.team.dto.response.TeamResponse
import io.waggle.waggleapiserver.domain.team.repository.TeamRepository
import io.waggle.waggleapiserver.domain.user.User
import io.waggle.waggleapiserver.domain.user.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
@Transactional(readOnly = true)
class NotificationService(
    private val memberRepository: MemberRepository,
    private val notificationRepository: NotificationRepository,
    private val teamRepository: TeamRepository,
    private val userRepository: UserRepository,
) {
    fun getUserNotifications(
        cursorQuery: CursorGetQuery,
        user: User,
    ): CursorResponse<NotificationResponse> {
        val pageable = PageRequest.of(0, cursorQuery.size + 1)
        val notifications =
            notificationRepository.findByUserIdWithCursor(user.id, cursorQuery.cursor, pageable)

        val hasNext = notifications.size > cursorQuery.size
        val slicedNotifications =
            if (hasNext) notifications.take(cursorQuery.size) else notifications

        val teamIds = slicedNotifications.mapNotNull { it.teamId }.distinct()
        val teamById = teamRepository.findAllById(teamIds).associateBy { it.id }
        val memberCountByTeamId =
            memberRepository
                .countByTeamIds(teamIds)
                .associate { it.teamId to it.count.toInt() }

        val triggeredByUserIds = slicedNotifications.mapNotNull { it.triggeredBy }
        val triggeredByUserById =
            userRepository.findAllById(triggeredByUserIds).associateBy { it.id }

        val data =
            slicedNotifications.map { notification ->
                val team =
                    notification.teamId?.let { teamById[it] }?.let {
                        TeamResponse.of(it, memberCountByTeamId[it.id] ?: 0)
                    }

                val triggeredBy =
                    notification.triggeredBy?.let {
                        triggeredByUserById[it]?.let { user ->
                            NotificationResponse.TriggeredByResponse.of(
                                user,
                            )
                        }
                    }

                NotificationResponse.of(
                    notification = notification,
                    team = team,
                    triggeredBy = triggeredBy,
                )
            }

        return CursorResponse(
            data = data,
            nextCursor = if (hasNext) slicedNotifications.lastOrNull()?.id else null,
            hasNext = hasNext,
        )
    }

    fun getNotificationCount(user: User): NotificationCountResponse =
        NotificationCountResponse(
            totalCount = notificationRepository.countByUserId(user.id),
            unreadCount = notificationRepository.countByUserIdAndReadAtIsNull(user.id),
        )

    @Transactional
    fun readNotifications(user: User, notificationIds: List<Long>) {
        notificationRepository.markAsReadByIds(user.id, notificationIds, Instant.now())
    }

    @Transactional
    fun readAllNotifications(user: User) {
        notificationRepository.markAllAsRead(user.id, Instant.now())
    }
}
