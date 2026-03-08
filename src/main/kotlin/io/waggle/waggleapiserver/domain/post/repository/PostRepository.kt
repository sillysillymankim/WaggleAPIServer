package io.waggle.waggleapiserver.domain.post.repository

import io.waggle.waggleapiserver.domain.post.Post
import io.waggle.waggleapiserver.domain.post.PostSort
import io.waggle.waggleapiserver.domain.user.enums.Position
import io.waggle.waggleapiserver.domain.user.enums.Skill
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PostRepository : JpaRepository<Post, Long> {
    @Query(
        """
        SELECT p FROM Post p
        WHERE (
            (:sort = 'NEWEST' AND (:cursor IS NULL OR p.id < :cursor))
            OR (:sort = 'OLDEST' AND (:cursor IS NULL OR p.id > :cursor))
        )
        AND (:q IS NULL OR p.title LIKE CONCAT('%', :q, '%'))
        AND (:#{#positions.empty} = true OR p.id IN (
            SELECT r.postId FROM Recruitment r WHERE r.position IN :positions
        ))
        AND (:#{#skills.empty} = true OR p.id IN (
            SELECT r2.postId FROM Recruitment r2 JOIN r2.skills s WHERE s IN :skills
        ))
        ORDER BY
            CASE WHEN :sort = 'NEWEST' THEN p.id END DESC,
            CASE WHEN :sort = 'OLDEST' THEN p.id END ASC
        """,
    )
    fun findWithFilter(
        cursor: Long?,
        q: String?,
        positions: Set<Position>,
        skills: Set<Skill>,
        sort: PostSort,
        pageable: Pageable,
    ): List<Post>

    fun findByIdInOrderByCreatedAtDesc(ids: List<Long>): List<Post>

    fun findByTeamIdOrderByCreatedAtDesc(teamId: Long): List<Post>
}
