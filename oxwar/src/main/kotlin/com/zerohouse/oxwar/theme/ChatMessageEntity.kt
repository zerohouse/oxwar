package com.zerohouse.oxwar.theme

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "chat_messages")
class ChatMessageEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val themeSlug: String = "",

    @Column(nullable = false)
    val playerId: String = "",

    @Column(nullable = false)
    val message: String = "",

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),
)
