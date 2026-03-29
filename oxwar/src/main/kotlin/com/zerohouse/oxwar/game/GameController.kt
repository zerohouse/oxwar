package com.zerohouse.oxwar.game

import com.zerohouse.oxwar.theme.ChatMessageEntity
import com.zerohouse.oxwar.theme.ChatMessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.annotation.ConnectMapping
import org.springframework.stereotype.Controller
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope

data class JoinRequest(val theme: String = "default", val uuid: String? = null)
data class MoveRequest(val posX: Double = 0.5, val posY: Double = 0.5)
data class ChatRequest(val message: String = "")

@Controller
class GameController(
    private val gameManager: GameManager,
    private val chatMessageRepository: ChatMessageRepository,
) {
    private data class Session(val playerId: String, val uuid: String?, val room: GameRoom)

    private val sessions = ConcurrentHashMap<RSocketRequester, Session>()
    private val uuidToRequester = ConcurrentHashMap<String, RSocketRequester>()
    private val ioScope = CoroutineScope(Dispatchers.IO)

    @ConnectMapping
    fun onConnect(requester: RSocketRequester) {}

    @MessageMapping("events")
    suspend fun events(data: JoinRequest, requester: RSocketRequester): Flow<GameEvent> {
        val slug = data.theme.ifBlank { "default" }
        val uuid = data.uuid
        val room = gameManager.getOrCreate(slug)

        // 같은 UUID 기존 커넥션 종료
        if (uuid != null) {
            val prev = uuidToRequester.remove(uuid)
            if (prev != null) {
                val prevSession = sessions.remove(prev)
                if (prevSession != null) {
                    prevSession.room.game.removePlayer(prevSession.playerId, uuid)
                    room.eventFlow.tryEmit(GameEvent.playerLeave(prevSession.playerId))
                }
                try { prev.rsocket()?.dispose() } catch (_: Exception) {}
            }
            uuidToRequester[uuid] = requester
        }

        val id = NicknameGenerator.generate()
        val player = room.game.addPlayer(id, uuid)
        sessions[requester] = Session(id, uuid, room)

        room.eventFlow.emit(GameEvent.playerJoin(player))

        requester.rsocket()!!.onClose().doFinally {
            val session = sessions.remove(requester) ?: return@doFinally
            if (session.uuid != null) uuidToRequester.remove(session.uuid, requester)
            session.room.game.removePlayer(session.playerId, session.uuid)
            runBlocking {
                session.room.eventFlow.emit(GameEvent.playerLeave(session.playerId))
            }
        }.subscribe()

        val game = room.game
        return room.eventFlow.onStart {
            emit(GameEvent.sync(id, game.players.values.toList(), game.currentQuiz, game.currentQuizIndex, game.nextQuizAt))
        }
    }

    @MessageMapping("move")
    fun move(data: MoveRequest, requester: RSocketRequester) {
        val session = sessions[requester] ?: return
        session.room.game.movePlayer(session.playerId, data.posX, data.posY)
    }

    @MessageMapping("chat")
    suspend fun chat(data: ChatRequest, requester: RSocketRequester) {
        val session = sessions[requester] ?: return
        val msg = data.message.take(100)
        if (msg.isNotBlank()) {
            session.room.eventFlow.emit(GameEvent.chat(session.playerId, msg))
            ioScope.launch {
                chatMessageRepository.save(
                    ChatMessageEntity(
                        themeSlug = session.room.game.themeSlug,
                        playerId = session.playerId,
                        message = msg,
                    )
                )
            }
        }
    }
}
