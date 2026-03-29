package com.zerohouse.oxwar.theme

import com.querydsl.jpa.impl.JPAQueryFactory
import jakarta.persistence.EntityManager
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

interface ThemeRepository : JpaRepository<ThemeEntity, Long> {
    fun findBySlug(slug: String): ThemeEntity?
}

@Repository
class ThemeQueryRepository(private val em: EntityManager) {
    private val query = JPAQueryFactory(em)
    private val theme = QThemeEntity.themeEntity
    private val quiz = QQuizEntity.quizEntity

    fun findBySlugWithQuizzes(slug: String): ThemeEntity? {
        return query
            .selectFrom(theme)
            .leftJoin(theme.quizzes, quiz).fetchJoin()
            .where(theme.slug.eq(slug))
            .orderBy(quiz.sortOrder.asc())
            .fetchOne()
    }

    @Transactional
    fun fetchQuizBatch(slug: String, limit: Int): List<QuizEntity> {
        val themeEntity = query.selectFrom(theme).where(theme.slug.eq(slug)).fetchOne()
            ?: return emptyList()

        val quizzes = query
            .selectFrom(quiz)
            .where(quiz.theme.eq(themeEntity))
            .orderBy(quiz.playCount.asc(), quiz.id.asc())
            .limit(limit.toLong())
            .fetch()

        if (quizzes.isNotEmpty()) {
            query.update(quiz)
                .set(quiz.playCount, quiz.playCount.add(1))
                .where(quiz.id.`in`(quizzes.map { it.id }))
                .execute()
            em.flush()
        }

        return quizzes
    }
}
