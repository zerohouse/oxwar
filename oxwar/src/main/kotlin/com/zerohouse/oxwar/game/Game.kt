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

    fun renamePlayer(oldId: String, newId: String): Boolean {
        val player = players.remove(oldId) ?: return false
        players[newId] = player.copy(id = newId)
        return true
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

        // 1패스: 정답/오답 판별
        val correctIds = mutableSetOf<String>()
        players.forEach { (_, p) ->
            if (playerChoice(p, optionCount) == correctAnswer) correctIds.add(p.id)
        }
        val wrongCount = players.size - correctIds.size

        // 2패스: 점수 적용 (맞춘 사람에게 틀린 사람 수 보너스)
        players.replaceAll { _, p ->
            if (correctIds.contains(p.id)) {
                val newStreak = p.streak + 1
                val comboBonus = minOf(newStreak, 5)
                val milestoneBonus = if (newStreak >= 10 && newStreak % 10 == 0) 100 else 0
                val newScore = p.score + comboBonus + (comboBonus * wrongCount) + milestoneBonus
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
