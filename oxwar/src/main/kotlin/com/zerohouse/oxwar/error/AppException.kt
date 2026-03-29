package com.zerohouse.oxwar.error

import org.springframework.http.HttpStatus

open class AppException(
    val code: String,
    val status: HttpStatus = HttpStatus.BAD_REQUEST,
    override val message: String = code,
) : RuntimeException(message)

class NotFoundException(message: String = "찾을 수 없습니다") :
    AppException("NOT_FOUND", HttpStatus.NOT_FOUND, message)

class DuplicateException(message: String = "이미 존재합니다") :
    AppException("DUPLICATE", HttpStatus.CONFLICT, message)

class InvalidInputException(message: String = "잘못된 입력입니다") :
    AppException("INVALID_INPUT", HttpStatus.BAD_REQUEST, message)

class ForbiddenException(message: String = "권한이 없습니다") :
    AppException("FORBIDDEN", HttpStatus.FORBIDDEN, message)
