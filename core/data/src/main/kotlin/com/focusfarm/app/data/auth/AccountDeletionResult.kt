package com.focusfarm.app.data.auth

import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import kotlinx.coroutines.CancellationException

sealed interface AccountDeletionResult {
    data object Success : AccountDeletionResult
    data object NoSignedInAccount : AccountDeletionResult
    data object RequiresRecentLogin : AccountDeletionResult
    data object RemoteDeletionFailed : AccountDeletionResult
    data object LocalCleanupFailed : AccountDeletionResult
    data object AccountDeletedLocalCleanupFailed : AccountDeletionResult
}

/** Keeps account deletion and device-only cleanup as separate operations. */
internal object AccountDeletionCoordinator {
    suspend fun deleteAccount(
        hasSignedInAccount: Boolean,
        deleteRemote: suspend () -> Unit,
        clearLocal: suspend () -> Unit,
        requiresRecentLogin: (Exception) -> Boolean = { it is FirebaseAuthRecentLoginRequiredException },
    ): AccountDeletionResult {
        if (!hasSignedInAccount) return AccountDeletionResult.NoSignedInAccount
        try {
            deleteRemote()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            return if (requiresRecentLogin(error)) AccountDeletionResult.RequiresRecentLogin
            else AccountDeletionResult.RemoteDeletionFailed
        }
        return when (clearLocalData(clearLocal)) {
            AccountDeletionResult.LocalCleanupFailed -> AccountDeletionResult.AccountDeletedLocalCleanupFailed
            else -> AccountDeletionResult.Success
        }
    }

    suspend fun clearLocalData(clearLocal: suspend () -> Unit): AccountDeletionResult = try {
        clearLocal()
        AccountDeletionResult.Success
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        AccountDeletionResult.LocalCleanupFailed
    }
}
