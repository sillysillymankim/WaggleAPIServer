package io.waggle.waggleapiserver.domain.message

import io.waggle.waggleapiserver.common.AuditingEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "messages",
    indexes = [
        Index(
            name = "idx_messages_sender_receiver_created",
            columnList = "sender_id, receiver_id, created_at",
        ),
        Index(
            name = "idx_messages_receiver_sender_created",
            columnList = "receiver_id, sender_id, created_at",
        ),
        Index(name = "idx_messages_receiver_read", columnList = "receiver_id, read_at"),
    ],
)
class Message(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "sender_id", nullable = false)
    val senderId: UUID,
    @Column(name = "receiver_id", nullable = false)
    val receiverId: UUID,
    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,
) : AuditingEntity() {
    @Column(name = "read_at")
    var readAt: Instant? = null
}
