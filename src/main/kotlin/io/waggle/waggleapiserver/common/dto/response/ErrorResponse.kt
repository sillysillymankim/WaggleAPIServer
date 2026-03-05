package io.waggle.waggleapiserver.common.dto.response

data class ErrorResponse(
    val status: Int,
    val code: String,
    val message: String,
    val detail: String? = null,
)
