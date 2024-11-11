package com.eka.conversation.data.remote.api

import com.eka.conversation.data.remote.models.QueryPostBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.HeaderMap
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.QueryMap
import retrofit2.http.Streaming
import retrofit2.http.Url

interface ChatService {
    @Streaming
    @POST
    suspend fun postQuery(
        @Url url : String,
        @QueryMap params : Map<String,String>,
        @Body body : QueryPostBody,
        @HeaderMap headers : Map<String,String>
    ) : Response<ResponseBody>
}