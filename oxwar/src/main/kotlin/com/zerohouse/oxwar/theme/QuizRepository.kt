package com.zerohouse.oxwar.theme

import org.springframework.data.jpa.repository.JpaRepository

interface QuizRepository : JpaRepository<QuizEntity, Long> {
    fun findByThemeIdOrderBySortOrder(themeId: Long): List<QuizEntity>
}
