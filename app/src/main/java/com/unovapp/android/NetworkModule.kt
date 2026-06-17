package com.unovapp.android

import com.unovapp.android.data.auth.AuthApi
import com.unovapp.android.data.auth.AuthRepository
import com.unovapp.android.data.auth.AuthRepositoryImpl
import com.unovapp.android.data.auth.AuthRepositoryStub
import com.unovapp.android.data.auth.GoogleSignInHelper
import com.unovapp.android.data.network.AuthInterceptor
import com.unovapp.android.data.network.RetryInterceptor
import com.unovapp.android.data.network.TokenAuthenticator
import com.unovapp.android.data.social.SocialApi
import com.unovapp.android.data.social.SocialRepository
import com.unovapp.android.data.social.SocialRepositoryImpl
import com.unovapp.android.data.user.UserApi
import com.unovapp.android.data.user.UserRepository
import com.unovapp.android.data.user.UserRepositoryImpl
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
            // Rafraîchit automatiquement le token sur 401 puis rejoue la requête.
            .authenticator(TokenAuthenticator(tokenStore))
            // Render free tier : cold start ~30–90 s. On laisse le temps au backend
            // de se réveiller avant de jeter l'éponge.
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
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

    // --- Service User (2ᵉ Retrofit, même OkHttpClient → Bearer partagé) ---

    @Provides
    @Singleton
    fun provideUserApi(okHttpClient: OkHttpClient): UserApi =
        Retrofit.Builder()
            .baseUrl(BuildConfig.USER_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(UserApi::class.java)

    @Provides
    @Singleton
    fun provideUserRepository(api: UserApi): UserRepository = UserRepositoryImpl(api)

    // --- Service Social (3e Retrofit, même OkHttpClient → Bearer + refresh partagés) ---

    @Provides
    @Singleton
    fun provideSocialApi(okHttpClient: OkHttpClient): SocialApi =
        Retrofit.Builder()
            .baseUrl(BuildConfig.SOCIAL_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SocialApi::class.java)

    @Provides
    @Singleton
    fun provideSocialRepository(api: SocialApi): SocialRepository = SocialRepositoryImpl(api)

    @Provides
    @Singleton
    fun provideAuthRepository(
        api: AuthApi,
        tokenStore: TokenDataStore,
        googleSignInHelper: GoogleSignInHelper
    ): AuthRepository =
        if (BuildConfig.USE_STUB_AUTH) AuthRepositoryStub(tokenStore)
        else AuthRepositoryImpl(api, tokenStore, googleSignInHelper)
}
