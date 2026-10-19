package com.example.core.di

import com.example.core.data.repository.MockDataRepository
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.ChatRepository
import com.example.domain.repository.ModerationRepository
import com.example.domain.repository.NotificationRepository
import com.example.domain.repository.PostRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindAuthRepository(repository: MockDataRepository): AuthRepository

    @Binds
    @Singleton
    abstract fun bindPostRepository(repository: MockDataRepository): PostRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(repository: MockDataRepository): ChatRepository

    @Binds
    @Singleton
    abstract fun bindNotificationRepository(repository: MockDataRepository): NotificationRepository

    @Binds
    @Singleton
    abstract fun bindModerationRepository(repository: MockDataRepository): ModerationRepository
}
