package com.focusfarm.app.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.focusfarm.app.telemetry.AppTelemetry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

data class ReferralState(
    val referralCode: String = "",
    val referredCount: Int = 0,
    val rewardSeeds: Int = 0,
    val canRefer: Boolean = true,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

data class ReferralResult(
    val success: Boolean,
    val message: String,
    val rewardAmount: Int = 0,
)

@Singleton
class ReferralManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telemetry: AppTelemetry,
    private val functions: FirebaseFunctions,
    private val auth: FirebaseAuth,
) {

    private val _state = MutableStateFlow(ReferralState())
    val state: StateFlow<ReferralState> = _state.asStateFlow()

    init {
        observeAuthState()
    }

    private fun observeAuthState() {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                loadReferralState(user.uid)
            } else {
                _state.update { ReferralState() }
            }
        }
    }

    private fun loadReferralState(userId: String) {
        _state.update { it.copy(isLoading = true) }

        // Firestore'dan kullanıcı bilgilerini çek
        // Bu kısım Firebase Firestore entegrasyonu ile tamamlanacak
        // Şimdilik local state'i kullanıyoruz
        _state.update {
            it.copy(
                isLoading = false,
                referralCode = getLocalReferralCode(),
                canRefer = true,
            )
        }
    }

    fun getReferralLink(): String {
        val code = _state.value.referralCode
        if (code.isEmpty()) return ""
        return "https://focusfarm.app/referral?code=$code"
    }

    fun shareReferralLink() {
        val link = getReferralLink()
        if (link.isEmpty()) return

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "FocusFarm ile odaklanarak bahcemi buyutuyorum! Sen de katil: $link")
        }
        val chooser = Intent.createChooser(shareIntent, "Arkadasini Davet Et").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        telemetry.logEvent("referral_sent", emptyMap())
    }

    suspend fun verifyReferral(referralCode: String): ReferralResult {
        return try {
            val user = auth.currentUser
            if (user == null) {
                return ReferralResult(
                    success = false,
                    message = "Kullanıcı giriş yapmamış.",
                )
            }

            _state.update { it.copy(isLoading = true, errorMessage = null) }

            val result = functions
                .getHttpsCallable("verifyReferral")
                .call(mapOf("referralCode" to referralCode))
                .await()

            val data = result.data as? Map<*, *>
            val success = data?.get("success") as? Boolean ?: false
            val message = data?.get("message") as? String ?: "Bilinmeyen hata"
            val rewardAmount = (data?.get("rewardAmount") as? Number)?.toInt() ?: 0

            _state.update {
                it.copy(
                    isLoading = false,
                    errorMessage = if (success) null else message,
                )
            }

            telemetry.logEvent(
                "referral_verified",
                mapOf(
                    "success" to success.toString(),
                    "reward_amount" to rewardAmount.toString(),
                ),
            )

            ReferralResult(
                success = success,
                message = message,
                rewardAmount = rewardAmount,
            )

        } catch (e: FirebaseFunctionsException) {
            val errorMessage = when (e.code) {
                FirebaseFunctionsException.Code.NOT_FOUND -> "Geçersiz referral kodu."
                FirebaseFunctionsException.Code.ALREADY_EXISTS -> e.message ?: "Bu kod daha önce kullanılmış."
                FirebaseFunctionsException.Code.UNAUTHENTICATED -> "Kullanıcı giriş yapmamış."
                else -> "Referral doğrulanırken hata oluştu: ${e.message}"
            }

            _state.update {
                it.copy(
                    isLoading = false,
                    errorMessage = errorMessage,
                )
            }

            telemetry.logEvent(
                "referral_verification_failed",
                mapOf("error" to e.code.name),
            )

            ReferralResult(
                success = false,
                message = errorMessage,
            )
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Bağlantı hatası: ${e.message}",
                )
            }

            ReferralResult(
                success = false,
                message = "Bağlantı hatası: ${e.message}",
            )
        }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }

    private fun getLocalReferralCode(): String {
        val prefs = context.getSharedPreferences(PREFS_REFERRAL, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_REFERRAL_CODE, null)
        if (existing != null) return existing

        val newCode = generateReferralCode()
        prefs.edit().putString(KEY_REFERRAL_CODE, newCode).apply()
        return newCode
    }

    private fun generateReferralCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..8)
            .map { chars.random() }
            .joinToString("")
    }

    private companion object {
        const val PREFS_REFERRAL = "referral_state"
        const val KEY_REFERRAL_CODE = "referral_code"
    }
}
