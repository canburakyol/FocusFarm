package com.focusfarm.app.review

import android.app.Activity
import android.content.Context
import com.focusfarm.app.review.InAppReviewLauncher
import com.google.android.play.core.review.ReviewManagerFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class PlayInAppReviewLauncher @Inject constructor(
    @ApplicationContext private val context: Context,
) : InAppReviewLauncher {

    private val manager by lazy { ReviewManagerFactory.create(context) }

    override suspend fun launchReview(activity: Activity): Boolean {
        return runCatching {
            val reviewInfo = manager.requestReviewFlow().await()
            manager.launchReviewFlow(activity, reviewInfo).await()
        }.isSuccess
    }
}
