package com.focusfarm.app.telemetry

import android.content.Context
import android.os.Bundle
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface AppTelemetry {
    fun logEvent(name: String, params: Map<String, String> = emptyMap())
    fun recordNonFatal(tag: String, message: String, throwable: Throwable? = null)
}

@Singleton
class FirebaseAppTelemetry @Inject constructor(
    @ApplicationContext context: Context,
) : AppTelemetry {

    private val analytics: FirebaseAnalytics?
    private val crashlytics: FirebaseCrashlytics?

    init {
        val firebaseAvailable = runCatching {
            FirebaseApp.initializeApp(context) ?: FirebaseApp.getApps(context).firstOrNull()
        }.getOrNull() != null

        analytics = if (firebaseAvailable) {
            runCatching { FirebaseAnalytics.getInstance(context) }.getOrNull()
        } else {
            null
        }

        crashlytics = if (firebaseAvailable) {
            runCatching { FirebaseCrashlytics.getInstance() }.getOrNull()
        } else {
            null
        }

        runCatching { crashlytics?.setCrashlyticsCollectionEnabled(true) }
    }

    override fun logEvent(name: String, params: Map<String, String>) {
        val safeName = name.take(MAX_EVENT_NAME_LENGTH)
        val bundle = Bundle().apply {
            params.forEach { (key, value) ->
                putString(
                    key.take(MAX_PARAM_KEY_LENGTH),
                    value.take(MAX_PARAM_VALUE_LENGTH),
                )
            }
        }

        if (analytics != null) {
            analytics.logEvent(safeName, bundle)
        } else {
            Log.d(TAG, "event=$safeName params=$params")
        }
    }

    override fun recordNonFatal(tag: String, message: String, throwable: Throwable?) {
        if (crashlytics != null) {
            crashlytics.setCustomKey("error_tag", tag.take(MAX_PARAM_VALUE_LENGTH))
            crashlytics.log("$tag: $message")
            if (throwable != null) {
                crashlytics.recordException(throwable)
            } else {
                crashlytics.recordException(IllegalStateException("$tag: $message"))
            }
        } else {
            Log.w(TAG, "nonfatal tag=$tag message=$message throwable=$throwable")
        }
    }

    private companion object {
        const val TAG = "FocusFarmTelemetry"
        const val MAX_EVENT_NAME_LENGTH = 40
        const val MAX_PARAM_KEY_LENGTH = 40
        const val MAX_PARAM_VALUE_LENGTH = 100
    }
}
