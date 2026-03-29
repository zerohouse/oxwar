package com.zerohouse.oxwar.theme

import jakarta.persistence.*

@Entity
@Table(name = "themes")
class ThemeEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val slug: String = "",

    @Column(nullable = false)
    var title: String = "",

    @Column(nullable = false)
    var ownerUuid: String = "",

    @Column(nullable = false, columnDefinition = "TEXT")
    var categories: String = "[]",

    @Column(nullable = false, columnDefinition = "TEXT")
    var defaultPrompt: String = "",

    @OneToMany(mappedBy = "theme", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("sortOrder")
    val quizzes: MutableList<QuizEntity> = mutableListOf(),
)
