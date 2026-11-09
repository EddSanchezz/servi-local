package com.servilocal.core.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/** Creates a temporary cache URI that can be safely shared with the camera app. */
fun createTempImageUri(context: Context): Uri {
    val imageFile = File.createTempFile("servilocal_photo_", ".jpg", context.cacheDir)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile
    )
}
