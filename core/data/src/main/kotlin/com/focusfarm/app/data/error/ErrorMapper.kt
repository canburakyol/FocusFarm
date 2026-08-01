package com.focusfarm.app.data.error

import android.content.Context
import com.focusfarm.app.data.repository.InsufficientSeedsException
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ErrorMapper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun map(throwable: Throwable): String {
        return when (throwable) {
            is InsufficientSeedsException -> {
                "Yetersiz tohum: ${throwable.required} gerekli, ${throwable.available} mevcut"
            }
            else -> throwable.localizedMessage ?: "Bilinmeyen bir hata oluştu."
        }
    }
}
