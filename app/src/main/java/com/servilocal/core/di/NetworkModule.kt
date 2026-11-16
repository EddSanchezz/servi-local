package com.servilocal.core.di

import com.servilocal.data.remote.openrouter.OpenRouterApi
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun provideGson(): Gson = Gson()

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS).readTimeout(45, TimeUnit.SECONDS).writeTimeout(20, TimeUnit.SECONDS).build()

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl("https://openrouter.ai/").client(client)
        .addConverterFactory(GsonConverterFactory.create()).build()

    @Provides @Singleton
    fun provideOpenRouterApi(retrofit: Retrofit): OpenRouterApi = retrofit.create(OpenRouterApi::class.java)
}
