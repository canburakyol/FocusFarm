package com.focusfarm.app.telemetry

interface AppTelemetry {
    fun logEvent(name: String, params: Map<String, String> = emptyMap())
    fun recordNonFatal(tag: String, message: String, throwable: Throwable? = null)
}
