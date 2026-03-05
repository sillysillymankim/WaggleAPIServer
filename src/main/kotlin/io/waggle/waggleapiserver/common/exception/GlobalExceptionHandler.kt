package io.waggle.waggleapiserver.common.exception

import io.waggle.waggleapiserver.common.dto.response.ErrorResponse
import io.waggle.waggleapiserver.common.infrastructure.discord.DiscordErrorContext
import io.waggle.waggleapiserver.common.infrastructure.discord.DiscordWebhookClient
import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.ObjectProvider
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

@RestControllerAdvice
class GlobalExceptionHandler(
    discordWebhookClientProvider: ObjectProvider<DiscordWebhookClient>,
) : ResponseEntityExceptionHandler() {
    private val discordWebhookClient = discordWebhookClientProvider.getIfAvailable()

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(e: BusinessException): ResponseEntity<ErrorResponse> {
        logger.warn("Business exception occurred: ${e.errorCode}", e)
        val errorResponse =
            ErrorResponse(
                status = e.errorCode.status.value(),
                code = e.errorCode.name,
                message = e.errorCode.message,
                detail = e.message,
            )
        return ResponseEntity
            .status(e.errorCode.status)
            .body(errorResponse)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(e: MethodArgumentTypeMismatchException): ResponseEntity<ErrorResponse> {
        logger.warn("Type mismatch exception occurred", e)
        val detail =
            "Invalid type for parameter '${e.name}': expected ${e.requiredType?.simpleName}"
        val errorResponse =
            ErrorResponse(
                status = ErrorCode.INVALID_TYPE_VALUE.status.value(),
                code = ErrorCode.INVALID_TYPE_VALUE.name,
                message = ErrorCode.INVALID_TYPE_VALUE.message,
                detail = detail,
            )
        return ResponseEntity
            .status(ErrorCode.INVALID_TYPE_VALUE.status)
            .body(errorResponse)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(
        e: Exception,
        request: HttpServletRequest,
    ): ResponseEntity<ErrorResponse> {
        logger.error("Unexpected exception occurred", e)
        discordWebhookClient?.send(DiscordErrorContext.from(request, e))
        val errorResponse =
            ErrorResponse(
                status = ErrorCode.INTERNAL_SERVER_ERROR.status.value(),
                code = ErrorCode.INTERNAL_SERVER_ERROR.name,
                message = ErrorCode.INTERNAL_SERVER_ERROR.message,
                detail = e.message,
            )
        return ResponseEntity
            .status(ErrorCode.INTERNAL_SERVER_ERROR.status)
            .body(errorResponse)
    }

    override fun handleMethodArgumentNotValid(
        e: MethodArgumentNotValidException,
        headers: HttpHeaders,
        status: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        logger.warn("Validation exception occurred", e)
        val errors =
            e.bindingResult.allErrors.joinToString(", ") { error ->
                val fieldName = (error as? FieldError)?.field ?: "unknown"
                val errorMessage = error.defaultMessage ?: "validation failed"
                "$fieldName: $errorMessage"
            }
        val errorResponse =
            ErrorResponse(
                status = ErrorCode.INVALID_INPUT_VALUE.status.value(),
                code = ErrorCode.INVALID_INPUT_VALUE.name,
                message = ErrorCode.INVALID_INPUT_VALUE.message,
                detail = errors,
            )
        return ResponseEntity
            .status(ErrorCode.INVALID_INPUT_VALUE.status)
            .body(errorResponse)
    }

    override fun handleExceptionInternal(
        e: Exception,
        body: Any?,
        headers: HttpHeaders,
        statusCode: HttpStatusCode,
        request: WebRequest,
    ): ResponseEntity<Any> {
        logger.warn("Spring MVC exception occurred: ${e.javaClass.simpleName}", e)
        val code = (statusCode as? HttpStatus)?.name ?: statusCode.value().toString()
        val errorResponse =
            ErrorResponse(
                status = statusCode.value(),
                code = code,
                message = e.message ?: "Request processing failed",
            )
        return ResponseEntity
            .status(statusCode)
            .headers(headers)
            .body(errorResponse)
    }
}
