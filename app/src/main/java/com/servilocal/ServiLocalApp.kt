package com.servilocal

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.cloudinary.android.MediaManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ServiLocalApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (AppCompatDelegate.getApplicationLocales().isEmpty) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("es"))
        }
        if (BuildConfig.CLOUDINARY_CLOUD_NAME.isNotBlank()) {
            try {
                MediaManager.init(this, mapOf("cloud_name" to BuildConfig.CLOUDINARY_CLOUD_NAME))
            } catch (alreadyInitialized: IllegalStateException) {
                if (alreadyInitialized.message != "MediaManager is already initialized") {
                    throw alreadyInitialized
                }
            }
        }
    }
}
