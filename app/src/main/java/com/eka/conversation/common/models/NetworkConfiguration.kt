package com.eka.conversation.common.models

import okhttp3.Authenticator
import okhttp3.Interceptor

data class NetworkConfiguration(
    val headers : HashMap<String,String>,
    val params : HashMap<String,String>,
    val baseUrl : String,
    val aiBotEndpoint : String,
    val interceptor: Interceptor? = null,
    val authenticator: Authenticator? = null,
)
