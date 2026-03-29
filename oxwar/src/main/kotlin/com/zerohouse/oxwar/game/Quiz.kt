package com.zerohouse.oxwar.game

data class Quiz(
    val question: String,
    val options: List<String>,
    val answer: Int,
    val category: String = "",
)
