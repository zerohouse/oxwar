package com.zerohouse.oxwar.error

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class HttpExceptionHandler {

    @ExceptionHandler(AppException::class)
    fun handle(ex: AppException): ResponseEntity<ErrorResponse> {
        return ResponseEntity
            .status(ex.status)
            .body(ErrorResponse(ex.code, ex.message))
    }
}
