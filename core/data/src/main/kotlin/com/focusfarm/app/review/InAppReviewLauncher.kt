package com.focusfarm.app.review

import android.app.Activity

/**
 * Abstraction for launching the platform in-app review flow.
 *
 * The implementation lives in the `:app` module because it depends on the
 * Google Play In-App Review library, which must not leak into core library
 * modules.
 */
interface InAppReviewLauncher {
    /**
     * Attempts to show the in-app review sheet on the given [activity].
     *
     * @return `true` if the Play Review API returned successfully. This does
     *         not guarantee that the UI was actually shown.
     */
    suspend fun launchReview(activity: Activity): Boolean
}
