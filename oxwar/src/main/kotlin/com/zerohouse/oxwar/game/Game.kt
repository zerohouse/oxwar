package com.zerohouse.oxwar.game

import java.util.concurrent.ConcurrentHashMap

class Game(val themeSlug: String, quizList: List<Quiz>) {

    var quizzes: List<Quiz> = quizList.ifEmpty {
        listOf(Quiz("퀴즈가 없습니다", listOf("대기", "대기"), 0))
    }
        private set

    fun reloadQuizzes(newQuizzes: List<Quiz>) {
        quizzes = newQuizzes
        currentQuizIndex = 0
    }

    val players = ConcurrentHashMap<String, Player>()
    val dirtyPlayers = ConcurrentHashMap.newKeySet<String>()
    private data class SavedState(val score: Int, val streak: Int)
    private val uuidState = ConcurrentHashMap<String, SavedState>()
    var currentQuizIndex = 0
    var nextQuizAt: Long = 0

    val currentQuiz get() = quizzes[currentQuizIndex]

    fun addPlayer(id: String, uuid: String? = null): Player {
        val saved = if (uuid != null) uuidState[uuid] else null
        val player = Player(
            id,
            posX = Math.random() * 0.8 + 0.1,
            posY = Math.random() * 0.6 + 0.2,
            score = saved?.score ?: 0,
            streak = saved?.streak ?: 0,
        )
        players[id] = player
        return player
    }

    fun removePlayer(id: String, uuid: String? = null) {
        val p = players.remove(id)
        if (uuid != null && p != null) {
            uuidState[uuid] = SavedState(p.score, p.streak)
        }
    }

    fun movePlayer(id: String, posX: Double, posY: Double) {
        players.computeIfPresent(id) { _, p ->
            dirtyPlayers.add(id)
            p.copy(posX = posX.coerceIn(0.0, 1.0), posY = posY.coerceIn(0.0, 1.0))
        }
    }

    fun drainDirtyPositions(): List<Triple<String, Double, Double>> {
        val result = mutableListOf<Triple<String, Double, Double>>()
        val iter = dirtyPlayers.iterator()
        while (iter.hasNext()) {
            val id = iter.next()
            iter.remove()
            players[id]?.let { result.add(Triple(id, it.posX, it.posY)) }
        }
        return result
    }

    data class RoundResult(val scores: Map<String, Int>, val streaks: Map<String, Int>)

    fun nextRound(): RoundResult {
        val quiz = quizzes[currentQuizIndex]
        val correctAnswer = quiz.answer
        val optionCount = quiz.options.size
        val scores = mutableMapOf<String, Int>()
        val streaks = mutableMapOf<String, Int>()

        players.replaceAll { _, p ->
            val chosen = playerChoice(p, optionCount)
            val correct = chosen == correctAnswer
            if (correct) {
                val newStreak = p.streak + 1
                val newScore = p.score + fib(newStreak)
                scores[p.id] = newScore
                streaks[p.id] = newStreak
                p.copy(score = newScore, streak = newStreak)
            } else {
                val newScore = maxOf(0, p.score - 1)
                scores[p.id] = newScore
                streaks[p.id] = 0
                p.copy(score = newScore, streak = 0)
            }
        }

        currentQuizIndex = (currentQuizIndex + 1) % quizzes.size
        return RoundResult(scores, streaks)
    }

    private fun fib(n: Int): Int {
        var a = 1; var b = 1
        repeat(n - 1) { val t = a + b; a = b; b = t }
        return a
    }

    private fun playerChoice(p: Player, optionCount: Int): Int {
        return if (optionCount <= 2) {
            if (p.posX < 0.5) 0 else 1
        } else {
            val col = if (p.posX < 0.5) 0 else 1
            val row = if (p.posY < 0.5) 0 else 1
            row * 2 + col
        }
    }
}
