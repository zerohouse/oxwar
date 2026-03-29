package com.zerohouse.oxwar.theme

import jakarta.persistence.*

@Entity
@Table(name = "quizzes")
class QuizEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id", nullable = false)
    val theme: ThemeEntity = ThemeEntity(),

    @Column(nullable = false)
    var question: String = "",

    @Column(nullable = false, columnDefinition = "TEXT")
    var options: String = "[]",

    @Column(nullable = false)
    var answer: Int = 0,

    @Column(nullable = false)
    var category: String = "",

    @Column(nullable = false)
    var sortOrder: Int = 0,

    @Column(nullable = false)
    var playCount: Int = 0,
)
