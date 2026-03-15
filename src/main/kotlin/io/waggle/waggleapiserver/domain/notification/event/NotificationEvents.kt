package io.waggle.waggleapiserver.domain.notification.event

import java.util.UUID

data class ApplicationReceivedEvent(
    val teamId: Long,
    val triggeredBy: UUID,
)

data class ApplicationApprovedEvent(
    val teamId: Long,
    val applicantUserId: UUID,
    val triggeredBy: UUID,
)

data class ApplicationRejectedEvent(
    val teamId: Long,
    val applicantUserId: UUID,
    val triggeredBy: UUID,
)

data class MemberJoinedEvent(
    val teamId: Long,
    val triggeredBy: UUID,
)

data class MemberLeftEvent(
    val teamId: Long,
    val triggeredBy: UUID,
)

data class MemberRemovedEvent(
    val teamId: Long,
    val removedUserId: UUID,
    val triggeredBy: UUID,
)

data class TeamCompletedEvent(
    val teamId: Long,
)
