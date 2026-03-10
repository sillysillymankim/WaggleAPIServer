package io.waggle.waggleapiserver.domain.application

import io.waggle.waggleapiserver.common.AuditingEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "application_reads",
    uniqueConstraints = [UniqueConstraint(columnNames = ["application_id", "user_id"])],
    indexes = [
        Index(name = "idx_application_reads_application", columnList = "application_id"),
        Index(name = "idx_application_reads_user", columnList = "user_id"),
    ],
)
class ApplicationRead(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "application_id", nullable = false)
    val applicationId: Long,
    @Column(name = "user_id", nullable = false)
    val userId: UUID,
) : AuditingEntity()
