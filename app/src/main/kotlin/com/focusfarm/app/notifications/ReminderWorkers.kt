package com.focusfarm.app.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationSchedulerEntryPoint {
    fun notificationScheduler(): NotificationScheduler
}

class SessionReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            NotificationSchedulerEntryPoint::class.java,
        )

        entryPoint.notificationScheduler().showNotification(
            title = "Odak Zamani!",
            message = "Kisa bir oturumla ritmini koru. Bahcen seni bekliyor!",
        )

        return Result.success()
    }
}

class StreakReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            NotificationSchedulerEntryPoint::class.java,
        )

        entryPoint.notificationScheduler().showNotification(
            title = "Seri Riski!",
            message = "Serini korumak icin bugun bir oturum tamamla. Bitkin susuz kaldi!",
        )

        return Result.success()
    }
}

class ComebackReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            NotificationSchedulerEntryPoint::class.java,
        )

        entryPoint.notificationScheduler().showNotification(
            title = "Seni Ozledik!",
            message = "Bahcin seni bekliyor. Hemen bir oturum baslat ve buyumeye devam et!",
        )

        return Result.success()
    }
}
