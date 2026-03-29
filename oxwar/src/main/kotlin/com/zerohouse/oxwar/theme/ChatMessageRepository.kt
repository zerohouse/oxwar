package com.zerohouse.oxwar.theme

import org.springframework.data.jpa.repository.JpaRepository

interface ChatMessageRepository : JpaRepository<ChatMessageEntity, Long>
