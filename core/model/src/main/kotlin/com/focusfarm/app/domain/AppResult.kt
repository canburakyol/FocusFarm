package com.focusfarm.app.domain

sealed interface AppResult<out T> {
    data class Success<out T>(val data: T) : AppResult<T>
    data class Failure(val exception: Throwable) : AppResult<Nothing>
}
