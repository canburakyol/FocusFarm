package com.focusfarm.app.monetization

import android.app.Activity
import android.content.Context
import com.focusfarm.app.telemetry.AppTelemetry
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ConsentState(
    val isLoading: Boolean = false,
    val canRequestAds: Boolean = false,
    val privacyOptionsRequired: Boolean = false,
    val lastErrorMessage: String? = null,
)

interface ConsentManager {
    val state: StateFlow<ConsentState>

    fun observeConsentState(): Flow<ConsentState> = state

    fun refreshConsent(activity: Activity? = null)

    fun showPrivacyOptions(activity: Activity, onDismissed: (errorMessage: String?) -> Unit)

    fun canLoadAds(): Boolean = state.value.canRequestAds
}

@Singleton
class UmpConsentManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val telemetry: AppTelemetry,
) : ConsentManager {
    private val consentInformation = UserMessagingPlatform.getConsentInformation(context)
    private val _state = MutableStateFlow(
        ConsentState(
            canRequestAds = consentInformation.canRequestAds(),
            privacyOptionsRequired = consentInformation.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED,
        )
    )
    override val state: StateFlow<ConsentState> = _state.asStateFlow()

    @Volatile
    private var mobileAdsInitialized = false

    override fun refreshConsent(activity: Activity?) {
        if (activity == null) {
            val canRequestAds = consentInformation.canRequestAds()
            _state.update {
                it.copy(
                    isLoading = false,
                    canRequestAds = canRequestAds,
                    privacyOptionsRequired = consentInformation.privacyOptionsRequirementStatus ==
                        ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED,
                )
            }
            if (canRequestAds) {
                initializeMobileAdsIfNeeded()
            }
            return
        }

        _state.update { it.copy(isLoading = true, lastErrorMessage = null) }
        val params = ConsentRequestParameters.Builder().build()
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                syncStateAndMaybeShowForm(activity)
            },
            { error ->
                handleConsentError("consent_info_update_failed", error)
                syncStateAndMaybeShowForm(activity = null)
            },
        )
    }

    private fun syncStateAndMaybeShowForm(activity: Activity?) {
        val canRequestAds = consentInformation.canRequestAds()
        _state.update {
            it.copy(
                isLoading = activity != null && consentInformation.isConsentFormAvailable,
                canRequestAds = canRequestAds,
                privacyOptionsRequired = consentInformation.privacyOptionsRequirementStatus ==
                    ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED,
            )
        }

        if (canRequestAds) {
            initializeMobileAdsIfNeeded()
        }

        if (activity == null || !consentInformation.isConsentFormAvailable) {
            _state.update { it.copy(isLoading = false) }
            return
        }

        UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
            if (formError != null) {
                handleConsentError("consent_form_failed", formError)
            }
            val updatedCanRequestAds = consentInformation.canRequestAds()
            _state.update {
                it.copy(
                    isLoading = false,
                    canRequestAds = updatedCanRequestAds,
                    privacyOptionsRequired = consentInformation.privacyOptionsRequirementStatus ==
                        ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED,
                )
            }
            if (updatedCanRequestAds) {
                initializeMobileAdsIfNeeded()
            }
        }
    }

    private fun initializeMobileAdsIfNeeded() {
        if (mobileAdsInitialized) return
        synchronized(this) {
            if (mobileAdsInitialized) return
            MobileAds.initialize(context) {}
            mobileAdsInitialized = true
        }
    }

    override fun showPrivacyOptions(activity: Activity, onDismissed: (errorMessage: String?) -> Unit) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            onDismissed(formError?.message)
            val canRequestAds = consentInformation.canRequestAds()
            _state.update {
                it.copy(
                    canRequestAds = canRequestAds,
                    privacyOptionsRequired = consentInformation.privacyOptionsRequirementStatus ==
                        ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED,
                )
            }
        }
    }

    private fun handleConsentError(tag: String, error: FormError) {
        telemetry.recordNonFatal(
            tag = tag,
            message = "code=${error.errorCode} message=${error.message}",
        )
        _state.update { it.copy(isLoading = false, lastErrorMessage = error.message) }
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class MonetizationBindings {
    @Binds
    abstract fun bindConsentManager(impl: UmpConsentManager): ConsentManager
}
