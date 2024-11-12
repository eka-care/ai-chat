package com.eka.conversation.data.remote.api

import android.util.Log
import com.eka.conversation.BuildConfig
import com.eka.conversation.ChatInit
import com.moczul.ok2curl.CurlInterceptor
import com.moczul.ok2curl.logger.Logger
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class RetrofitClient {
    companion object {
        private lateinit var retrofit : Retrofit

        private val instance by lazy {
            RetrofitClient()
        }

        fun init(baseUrl: String) {
            instance.init(baseUrl = baseUrl)
        }

        val chatApiService : ChatService by lazy {
            retrofit.create(ChatService::class.java)
        }
    }

    fun init(baseUrl : String) {
        val builder = OkHttpClient.Builder()

        ChatInit.getChatInitConfiguration().networkConfiguration.interceptor?.let {
            builder.addInterceptor(it)
        }

        ChatInit.getChatInitConfiguration().networkConfiguration.authenticator?.let {
            builder.authenticator(it)
        }

        builder.connectTimeout(60, TimeUnit.SECONDS)
        builder.readTimeout(60, TimeUnit.SECONDS)
        builder.writeTimeout(60, TimeUnit.SECONDS)
        builder.retryOnConnectionFailure(true)
        if(BuildConfig.IS_DEBUG) {
            builder.addInterceptor(CurlInterceptor(object : Logger {
                override fun log(message: String) {
                    Log.v("ChatSDK", message)
                }
            }))
        }

        val okHttpClient = builder.build()

        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}