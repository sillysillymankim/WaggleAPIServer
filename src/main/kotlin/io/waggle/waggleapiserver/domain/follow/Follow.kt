package io.waggle.waggleapiserver.domain.follow

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "follows",
    uniqueConstraints = [UniqueConstraint(columnNames = ["follower_id", "followee_id"])],
    indexes = [Index(name = "idx_follows_followee", columnList = "followee_id")],
)
class Follow(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "follower_id", nullable = false)
    val followerId: UUID,
    @Column(name = "followee_id", nullable = false)
    val followeeId: UUID,
) {
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: Instant = Instant.now()
}
