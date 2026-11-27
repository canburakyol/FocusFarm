package com.focusfarm.app.data.auth

import android.app.AlarmManager
import android.content.Context
import com.focusfarm.app.data.local.AppDatabase
import com.focusfarm.app.notifications.FocusReminderScheduler
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.NonCancellable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: AppDatabase,
    @ApplicationContext private val context: Context,
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

    suspend fun deleteAccount(): AccountDeletionResult {
        val user = auth.currentUser
        return AccountDeletionCoordinator.deleteAccount(
            hasSignedInAccount = user != null,
            deleteRemote = { user!!.delete().await() },
            clearLocal = { withContext(NonCancellable + Dispatchers.IO) { clearDeviceData() } },
        )
    }

    suspend fun clearLocalData(): AccountDeletionResult =
        AccountDeletionCoordinator.clearLocalData {
            withContext(Dispatchers.IO) { clearDeviceData() }
        }

    private fun clearDeviceData() {
        db.clearAllTables()
        AppDatabase.seedBuiltInPlantSpecies(db.openHelper.writableDatabase)
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager)
            .cancel(FocusReminderScheduler.reminderPendingIntent(context))
        listOf(
            "ad_reward_state", "garden_preferences", "product_onboarding", "retention_cohort",
            "local_first_week_cohort", "referral_state", "focus_reminders", "verified_billing_purchases",
            "focusfarm_review_prompt", "secure_billing_cache", "focus_allowed_apps",
        ).forEach { name ->
            check(context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()) {
                "Failed to clear local preference: $name"
            }
        }
        val sharedGardenImages = context.cacheDir.resolve("shares")
        check(!sharedGardenImages.exists() || sharedGardenImages.deleteRecursively()) {
            "Failed to clear shared garden images"
        }
    }
}
