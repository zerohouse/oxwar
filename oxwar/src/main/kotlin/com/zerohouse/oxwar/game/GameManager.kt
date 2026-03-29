package com.zerohouse.oxwar.game

import com.zerohouse.oxwar.theme.LeaderboardRepository
import com.zerohouse.oxwar.theme.LeaderboardEntity
import com.zerohouse.oxwar.theme.ThemeQueryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

data class GameRoom(
    val game: Game,
    val eventFlow: MutableSharedFlow<GameEvent> = MutableSharedFlow(extraBufferCapacity = 64),
)

data class TopPlayer(val name: String, val score: Int, val online: Boolean)

@Component
class GameManager(
    private val themeQueryRepository: ThemeQueryRepository,
    private val leaderboardRepository: LeaderboardRepository,
) {
    private val rooms = ConcurrentHashMap<String, GameRoom>()
    private val scope = CoroutineScope(Dispatchers.Default)

    suspend fun getOrCreate(slug: String): GameRoom {
        rooms[slug]?.let { return it }
        return withContext(Dispatchers.IO) {
            rooms.computeIfAbsent(slug) { createRoom(it) }
        }
    }

    fun getTop3(slug: String): List<TopPlayer> {
        val entries = leaderboardRepository.findByThemeSlugOrderByScoreDesc(slug).take(3)
        val onlineIds = rooms[slug]?.game?.players?.keys ?: emptySet()
        return entries.map { TopPlayer(it.playerName, it.score, onlineIds.contains(it.playerName)) }
    }

    fun updateLeaderboard(slug: String, scores: Map<String, Int>) {
        for ((name, score) in scores) {
            val entry = leaderboardRepository.findByThemeSlugAndPlayerName(slug, name)
            if (entry != null) {
                if (score > entry.score) {
                    entry.score = score
                    leaderboardRepository.save(entry)
                }
            } else {
                leaderboardRepository.save(LeaderboardEntity(themeSlug = slug, playerName = name, score = score))
            }
        }
    }

    private fun createRoom(slug: String): GameRoom {
        val quizzes = loadQuizBatch(slug)
        val game = Game(slug, quizzes)
        val room = GameRoom(game)
        startRoundLoop(room)
        startPositionBroadcast(room)
        return room
    }

    fun loadQuizBatch(slug: String): List<Quiz> {
        val mapper = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper()
        val entities = themeQueryRepository.fetchQuizBatch(slug, 100)
        return entities.map { entity ->
            val opts: List<String> = try {
                mapper.readValue(entity.options, com.fasterxml.jackson.module.kotlin.jacksonTypeRef<List<String>>())
            } catch (_: Exception) { listOf("O", "X") }
            Quiz(entity.question, opts, entity.answer, entity.category, entity.explanation)
        }.shuffled()
    }

    private fun startPositionBroadcast(room: GameRoom) {
        scope.launch {
            while (rooms.containsKey(room.game.themeSlug)) {
                delay(100)
                val dirty = room.game.drainDirtyPositions()
                for ((id, x, y) in dirty) {
                    room.eventFlow.emit(GameEvent.playerMove(id, x, y))
                }
            }
        }
    }

    private val ANSWER_TIME = 12_000L
    private val REVEAL_TIME = 5_000L

    private fun startRoundLoop(room: GameRoom) {
        val game = room.game
        game.nextQuizAt = System.currentTimeMillis() + ANSWER_TIME
        var emptyRounds = 0
        scope.launch {
            while (true) {
                delay(ANSWER_TIME)
                if (game.players.isEmpty()) {
                    emptyRounds++
                    if (emptyRounds >= 2) {
                        rooms.remove(game.themeSlug)
                        break
                    }
                    continue
                }
                emptyRounds = 0
                val quiz = game.currentQuiz
                val result = game.nextRound()

                // DB 리더보드 업데이트
                withContext(Dispatchers.IO) { updateLeaderboard(game.themeSlug, result.scores) }
                val top3 = withContext(Dispatchers.IO) { getTop3(game.themeSlug) }

                game.nextQuizAt = 0
                room.eventFlow.emit(GameEvent.quizReveal(quiz.answer, quiz.explanation, result.scores, result.streaks, top3))

                delay(REVEAL_TIME)

                if (game.currentQuizIndex == 0) {
                    val fresh = withContext(Dispatchers.IO) { loadQuizBatch(game.themeSlug) }
                    if (fresh.isNotEmpty()) game.reloadQuizzes(fresh)
                }
                game.nextQuizAt = System.currentTimeMillis() + ANSWER_TIME
                room.eventFlow.emit(
                    GameEvent.quizNext(game.currentQuiz, game.currentQuizIndex, result.scores, game.nextQuizAt)
                )
            }
        }
    }
}
