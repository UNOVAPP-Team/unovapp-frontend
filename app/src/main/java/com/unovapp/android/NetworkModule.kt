package com.unovapp.android

import com.unovapp.android.data.auth.AuthApi
import com.unovapp.android.data.auth.AuthRepository
import com.unovapp.android.data.auth.AuthRepositoryImpl
import com.unovapp.android.data.auth.AuthRepositoryStub
import com.unovapp.android.data.network.AuthInterceptor
import com.unovapp.android.data.network.RetryInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(tokenStore: TokenDataStore): OkHttpClient {
        val loggingLevel = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            // Ordre important : retry doit envelopper l'appel HTTP brut,
            // l'auth doit modifier la requête avant le logging.
            .addInterceptor(AuthInterceptor(tokenStore))
            .addInterceptor(RetryInterceptor(maxAttempts = 3, baseDelayMs = 400L))
            .addInterceptor(HttpLoggingInterceptor().apply { level = loggingLevel })
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.AUTH_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideAuthRepository(
        api: AuthApi,
        tokenStore: TokenDataStore
    ): AuthRepository =
        if (BuildConfig.USE_STUB_AUTH) AuthRepositoryStub(tokenStore)
        else AuthRepositoryImpl(api, tokenStore)
}
