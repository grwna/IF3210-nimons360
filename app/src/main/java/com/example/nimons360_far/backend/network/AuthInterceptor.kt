package com.example.nimons360_far.backend.network

import android.content.Context
import android.content.Intent
import com.example.nimons360_far.backend.utils.SessionManager
import okhttp3.Interceptor
import okhttp3.Response

// Intercepts requests and responses for cusotm behaviours
class AuthInterceptor(private val context: Context, private val sessionManager: SessionManager) :
        Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val requestBuilder = chain.request().newBuilder()

        // inject JWT to request header
        sessionManager.fetchAuthToken()?.let {
            requestBuilder.addHeader("Authorization", "Bearer $it")
        }

        val response = chain.proceed(requestBuilder.build())

        // if res 401, reset session, will cause user to get redirect to login
        if (response.code() == 401) {
            sessionManager.clearSession()
            val intent = Intent("com.example.nimons360_far.ACTION_LOGOUT")
            intent.setPackage(context.packageName)
            context.sendBroadcast(intent)
        }

        return response
    }
}
