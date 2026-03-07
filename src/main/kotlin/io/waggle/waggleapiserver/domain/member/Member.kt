package io.waggle.waggleapiserver.domain.member

import io.waggle.waggleapiserver.common.AuditingEntity
import io.waggle.waggleapiserver.common.exception.BusinessException
import io.waggle.waggleapiserver.common.exception.ErrorCode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.util.UUID

@Entity
@Table(
    name = "members",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "team_id"])],
    indexes = [Index(name = "idx_members_team", columnList = "team_id")],
)
class Member(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "user_id", nullable = false, updatable = false)
    val userId: UUID,
    @Column(name = "team_id", nullable = false, updatable = false)
    val teamId: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(10)")
    var role: MemberRole = MemberRole.MEMBER,
    @Column(name = "is_visible", nullable = false)
    var isVisible: Boolean = true,
) : AuditingEntity() {
    val isLeader: Boolean
        get() = this.role == MemberRole.LEADER

    fun updateRole(role: MemberRole) {
        this.role = role
    }

    fun updateVisibility(isVisible: Boolean) {
        this.isVisible = isVisible
    }

    fun checkMemberRole(requiredRole: MemberRole) {
        if (role.level < requiredRole.level) {
            throw BusinessException(ErrorCode.ACCESS_DENIED, "Do not have the authority")
        }
    }
}
