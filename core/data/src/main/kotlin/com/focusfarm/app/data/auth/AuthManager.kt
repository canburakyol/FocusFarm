package com.focusfarm.app.data.auth

import com.focusfarm.app.data.local.AppDatabase
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: AppDatabase,
) {
    private val _currentUserId = MutableStateFlow<String?>(auth.currentUser?.uid)
    val currentUserId = _currentUserId.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUserId.value = firebaseAuth.currentUser?.uid
        }
    }

    suspend fun signInAnonymouslyIfNeeded() {
        if (auth.currentUser == null) {
            try {
                auth.signInAnonymously().await()
            } catch (e: Exception) {
                // Silent failure, might be offline
            }
        }
    }

    suspend fun deleteAccount(): Boolean {
        val user = auth.currentUser ?: return true
        return try {
            user.delete().await()
            // Local Room DB temizliği
            db.clearAllTables()
            true
        } catch (e: Exception) {
            false
        }
    }
}
