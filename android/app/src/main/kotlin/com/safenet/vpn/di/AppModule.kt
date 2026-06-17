package com.safenet.vpn.di

import android.content.Context
import androidx.room.Room
import com.safenet.vpn.BuildConfig
import com.safenet.vpn.core.security.TokenManager
import com.safenet.vpn.data.local.SafeNetDatabase
import com.safenet.vpn.data.remote.SafeNetApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URI
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ── Room Database ─────────────────────────────────────────────────────────
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SafeNetDatabase {
        return Room.databaseBuilder(
            context,
            SafeNetDatabase::class.java,
            "safenet_vpn.db",
        )
            .addMigrations(SafeNetDatabase.MIGRATION_1_2)
            .build()
    }

    @Provides
    @Singleton
    fun provideSafeNetDao(database: SafeNetDatabase): com.safenet.vpn.data.local.dao.SafeNetDao {
        return database.safeNetDao()
    }

    // ── OkHttp with TLS Pinning ───────────────────────────────────────────────
    @Provides
    @Singleton
    fun provideOkHttpClient(tokenManager: TokenManager): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        }

        val certificatePin = BuildConfig.API_CERT_PIN_SHA256.trim()
        val apiHost = runCatching { URI(BuildConfig.API_BASE_URL).host }.getOrNull()
        val certificatePinner = if (!BuildConfig.DEBUG && certificatePin.isNotBlank() && !apiHost.isNullOrBlank()) {
            CertificatePinner.Builder()
                .add(apiHost, certificatePin)
                .build()
        } else {
            null
        }

        return OkHttpClient.Builder()
            .also { builder -> certificatePinner?.let { builder.certificatePinner(it) } }
            .addInterceptor(loggingInterceptor)
            .addInterceptor { chain ->
                val original = chain.request()
                val builder = original.newBuilder()
                    .addHeader("X-App-Version", BuildConfig.VERSION_NAME)
                    .addHeader("X-Platform", "android")
                    .addHeader("X-Device-Platform", "ANDROID")
                    .addHeader("X-Device-Fingerprint", tokenManager.getDeviceId().orEmpty())

                val authPath = original.url.encodedPath
                val isAuthEndpoint = authPath.endsWith("/auth/device/activate") ||
                    authPath.endsWith("/auth/device/bootstrap") ||
                    authPath.endsWith("/auth/refresh")

                if (!isAuthEndpoint && original.header("Authorization").isNullOrBlank()) {
                    tokenManager.getAccessToken()?.takeIf { it.isNotBlank() }?.let { token ->
                        builder.addHeader("Authorization", "Bearer $token")
                    }
                }

                val request = builder.build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // ── Retrofit ──────────────────────────────────────────────────────────────
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL + "/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // ── API Service ───────────────────────────────────────────────────────────
    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): SafeNetApiService {
        return retrofit.create(SafeNetApiService::class.java)
    }

    // ── Vercel API ────────────────────────────────────────────────────────────
    @Provides
    @Singleton
    @Named("vercel")
    fun provideVercelRetrofit(): Retrofit {
        val cleanClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
            
        return Retrofit.Builder()
            .baseUrl(BuildConfig.VERCEL_API_URL)
            .client(cleanClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideVercelApiService(@Named("vercel") retrofit: Retrofit): com.safenet.vpn.data.remote.VercelApiService {
        return retrofit.create(com.safenet.vpn.data.remote.VercelApiService::class.java)
    }
}
