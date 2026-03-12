package io.waggle.waggleapiserver.domain.team

import io.waggle.waggleapiserver.common.AuditingEntity
import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import io.waggle.waggleapiserver.domain.bookmark.BookmarkType
import io.waggle.waggleapiserver.domain.bookmark.Bookmarkable
import io.waggle.waggleapiserver.domain.team.enums.TeamStatus
import io.waggle.waggleapiserver.domain.team.enums.WorkMode
import jakarta.persistence.Access
import jakarta.persistence.AccessType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.util.UUID

@Access(AccessType.FIELD)
@Entity
@Table(
    name = "teams",
    indexes = [Index(name = "idx_teams_name", columnList = "name")],
)
class Team(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(unique = true, nullable = false)
    var name: String,
    @Column(nullable = false, columnDefinition = "TEXT")
    var description: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode", nullable = false, columnDefinition = "VARCHAR(20)")
    var workMode: WorkMode,
    @Column(name = "profile_image_url", columnDefinition = "VARCHAR(2083)")
    var profileImageUrl: String? = null,
    @Column(name = "leader_id", nullable = false)
    var leaderId: UUID,
    @Column(name = "creator_id", nullable = false, updatable = false)
    val creatorId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(20)")
    var status: TeamStatus = TeamStatus.ACTIVE,
) : AuditingEntity(),
    Bookmarkable {
    override val targetId: Long
        get() = id
    override val type: BookmarkType
        get() = BookmarkType.TEAM
    val isCompleted: Boolean
        get() = status == TeamStatus.COMPLETED

    fun update(
        name: String,
        description: String,
        workMode: WorkMode,
        profileImageUrl: String?,
    ) {
        this.name = name
        this.description = description
        this.workMode = workMode
        this.profileImageUrl = profileImageUrl
    }

    fun updateStatus(status: TeamStatus) {
        this.status = status
    }

    fun checkCompleted() {
        if (!isCompleted) {
            throw BusinessException(ErrorCode.INVALID_STATE, "Team is not completed yet")
        }
    }
}
