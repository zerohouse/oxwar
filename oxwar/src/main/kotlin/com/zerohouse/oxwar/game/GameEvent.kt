package com.zerohouse.oxwar.game

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class GameEvent(
    val type: String,
    val player: Player? = null,
    val playerId: String? = null,
    val posX: Double? = null,
    val posY: Double? = null,
    val question: String? = null,
    val category: String? = null,
    val options: List<String>? = null,
    val quizIndex: Int? = null,
    val players: List<Player>? = null,
    val scores: Map<String, Int>? = null,
    val nextQuizAt: Long? = null,
    val message: String? = null,
    val correctAnswer: Int? = null,
    val streaks: Map<String, Int>? = null,
    val top3: List<TopPlayer>? = null,
) {
    companion object {
        fun sync(myId: String, players: List<Player>, quiz: Quiz, quizIndex: Int, nextQuizAt: Long) =
            GameEvent("sync", playerId = myId, players = players, question = quiz.question, category = quiz.category, options = quiz.options, quizIndex = quizIndex, nextQuizAt = nextQuizAt)

        fun playerJoin(player: Player) =
            GameEvent("player_join", player = player)

        fun playerLeave(playerId: String) =
            GameEvent("player_leave", playerId = playerId)

        fun playerMove(playerId: String, posX: Double, posY: Double) =
            GameEvent("player_move", playerId = playerId, posX = posX, posY = posY)

        fun quizNext(quiz: Quiz, quizIndex: Int, scores: Map<String, Int>, nextQuizAt: Long) =
            GameEvent("quiz_next", question = quiz.question, category = quiz.category, options = quiz.options, quizIndex = quizIndex, scores = scores, nextQuizAt = nextQuizAt)

        fun quizReveal(correctAnswer: Int, explanation: String, scores: Map<String, Int>, streaks: Map<String, Int>, top3: List<TopPlayer>) =
            GameEvent("quiz_reveal", correctAnswer = correctAnswer, message = explanation.ifBlank { null }, scores = scores, streaks = streaks, top3 = top3)

        fun chat(playerId: String, message: String) =
            GameEvent("chat", playerId = playerId, message = message)
    }
}
