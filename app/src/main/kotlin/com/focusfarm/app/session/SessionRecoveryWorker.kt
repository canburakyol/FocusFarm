package com.focusfarm.app.session

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.focusfarm.app.data.repository.RecoveryDecision
import com.focusfarm.app.data.repository.SessionRecoveryManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

class SessionRecoveryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = runCatching {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            RecoveryWorkerEntryPoint::class.java,
        )
        when (val decision = entryPoint.recoveryManager().reconcile()) {
            is RecoveryDecision.Resume -> entryPoint.notifier().showResume(decision.sessionId)
            is RecoveryDecision.OfferRecovery -> entryPoint.notifier().showRecovery(decision.sessionId, decision.suggestedMinutes)
            is RecoveryDecision.FinalizationRequired -> entryPoint.notifier().showFinalization(decision.sessionId)
            is RecoveryDecision.ConfirmCompletion -> entryPoint.notifier().showFinalization(decision.sessionId)
            RecoveryDecision.None -> Unit
        }
        Result.success()
    }.getOrElse { Result.retry() }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RecoveryWorkerEntryPoint {
    fun recoveryManager(): SessionRecoveryManager
    fun notifier(): SessionRecoveryNotifier
}
