package com.servilocal.core.util

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject

fun interface ResourceProvider {
    fun getString(id: Int, vararg formatArgs: Any): String
}

class AndroidResourceProvider @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ResourceProvider {
    override fun getString(id: Int, vararg formatArgs: Any): String =
        if (formatArgs.isEmpty()) context.getString(id) else context.getString(id, *formatArgs)
}
