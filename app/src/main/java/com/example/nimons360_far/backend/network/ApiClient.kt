package com.example.nimons360_far.backend.network

import android.content.Context
import com.example.nimons360_far.backend.api.AuthApi
import com.example.nimons360_far.backend.api.FamilyApi
import com.example.nimons360_far.backend.api.UserApi
import com.example.nimons360_far.backend.utils.SessionManager
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "https://mad.labpro.hmif.dev"

    @Volatile private var retrofit: Retrofit? = null

    // Retrofit Client singleton initialization
    private fun getClient(context: Context): Retrofit {
        return retrofit
                ?: synchronized(this) {
                    val sessionManager = SessionManager(context)
                    val authInterceptor = AuthInterceptor(context, sessionManager)

                    val okHttpClient = OkHttpClient.Builder().addInterceptor(authInterceptor).build()

                    // build Retrofit instance
                    Retrofit.Builder().baseUrl(BASE_URL).client(okHttpClient)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build().also { retrofit = it }
                }
    }

    fun getAuthApi(context: Context): AuthApi {
        return getClient(context).create(AuthApi::class.java)
    }

    fun getUserApi(context: Context): UserApi {
        return getClient(context).create(UserApi::class.java)
    }

    fun getFamilyApi(context: Context): FamilyApi {
        return getClient(context).create(FamilyApi::class.java)
    }
}
