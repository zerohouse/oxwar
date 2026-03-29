package com.zerohouse.oxwar.game

data class Player(
    val id: String,
    val gender: String = "male",
    val posX: Double = 0.5,
    val posY: Double = 0.5,
    val score: Int = 0,
    val streak: Int = 0,
)
