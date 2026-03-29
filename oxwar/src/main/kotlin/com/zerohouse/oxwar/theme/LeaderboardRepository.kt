package com.zerohouse.oxwar.theme

import org.springframework.data.jpa.repository.JpaRepository

interface LeaderboardRepository : JpaRepository<LeaderboardEntity, Long> {
    fun findByThemeSlugOrderByScoreDesc(themeSlug: String): List<LeaderboardEntity>
    fun findByThemeSlugAndPlayerName(themeSlug: String, playerName: String): LeaderboardEntity?
}
