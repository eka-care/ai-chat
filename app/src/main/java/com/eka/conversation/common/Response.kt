package com.eka.conversation.common

sealed class Response<T>(
    val data: T? = null,
    val message: String? = null
) {
    class Success<T>(data: T) : Response<T>(data)

    class Error<T>(message: String? = null, data: T? = null) : Response<T>(data, message)

    class Loading<T> : Response<T>()
}