package com.zerohouse.oxwar.error

import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.stereotype.Controller

@Controller
class RSocketExceptionHandler {

    @MessageExceptionHandler(AppException::class)
    fun handle(ex: AppException): ErrorResponse {
        return ErrorResponse(ex.code, ex.message)
    }
}
