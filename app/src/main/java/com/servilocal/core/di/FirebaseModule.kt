package com.servilocal.core.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.content.Context
import androidx.credentials.CredentialManager
import com.servilocal.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {
    @Volatile
    private var authEmulatorConfigured = false
    @Volatile
    private var firestoreEmulatorConfigured = false

    @Provides
    @Singleton
    fun provideCredentialManager(@ApplicationContext context: Context): CredentialManager =
        CredentialManager.create(context)

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance().also { auth ->
        configureAuthEmulator(auth)
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance().also { firestore ->
        configureFirestoreEmulator(firestore)
    }

    @Synchronized
    private fun configureAuthEmulator(auth: FirebaseAuth) {
        if (BuildConfig.DEBUG && BuildConfig.FIREBASE_EMULATOR_ENABLED && !authEmulatorConfigured) {
            auth.useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, AUTH_EMULATOR_PORT)
            authEmulatorConfigured = true
        }
    }

    @Synchronized
    private fun configureFirestoreEmulator(firestore: FirebaseFirestore) {
        if (BuildConfig.DEBUG && BuildConfig.FIREBASE_EMULATOR_ENABLED && !firestoreEmulatorConfigured) {
            firestore.useEmulator(BuildConfig.FIREBASE_EMULATOR_HOST, FIRESTORE_EMULATOR_PORT)
            firestoreEmulatorConfigured = true
        }
    }

    private const val AUTH_EMULATOR_PORT = 9099
    private const val FIRESTORE_EMULATOR_PORT = 8080
}
