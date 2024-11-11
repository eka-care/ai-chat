package com.eka.conversation.data.remote.models

import androidx.annotation.Keep

@Keep
data class QueryPostRequest(
    val queryParams : Map<String,String> = emptyMap(),
    val body : QueryPostBody = QueryPostBody(listOf())
)
