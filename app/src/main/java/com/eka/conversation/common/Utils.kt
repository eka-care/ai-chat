package com.eka.conversation.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.eka.conversation.data.local.db.entities.models.MessageFileType
import java.util.Calendar
import java.util.TimeZone
import java.util.UUID


object Utils {
    fun getCurrentUTCEpochMillis(): Long {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).timeInMillis
    }

    fun getNewSessionId() : String {
        return UUID.randomUUID().toString() + "_" + getCurrentUTCEpochMillis()
    }

    fun getNewFileName(fileType: MessageFileType): String {
        return getCurrentUTCEpochMillis().toString() + "_" + fileType.name
    }

    fun formatMillisToMinutesSeconds(milliseconds: Long): String {
        val minutes = milliseconds / 1000 / 60
        val seconds = (milliseconds / 1000) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        // For 29 api or above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork) ?: return false
            return when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ->    true
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->   true
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->   true
                else ->     false
            }
        }
        // For below 29 api
        else {
            if (connectivityManager.activeNetworkInfo != null && connectivityManager.activeNetworkInfo!!.isConnectedOrConnecting) {
                return true
            }
        }
        return false
    }
}