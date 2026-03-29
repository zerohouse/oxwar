package com.zerohouse.oxwar.theme

import jakarta.persistence.*

@Entity
@Table(name = "leaderboard", uniqueConstraints = [UniqueConstraint(columnNames = ["theme_slug", "player_name"])])
class LeaderboardEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "theme_slug", nullable = false)
    val themeSlug: String = "",

    @Column(name = "player_name", nullable = false)
    val playerName: String = "",

    @Column(nullable = false)
    var score: Int = 0,
)
