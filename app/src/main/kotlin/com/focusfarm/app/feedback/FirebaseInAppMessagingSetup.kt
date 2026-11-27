package com.focusfarm.app.feedback

/**
 * TODO — Manual setup required for Firebase In-App Messaging survey.
 *
 * STATUS (2026-09-27): DEFERRED by owner. Firebase In-App Messaging requires
 * the paid Blaze plan, so this stays parked — do not add the dependency or
 * enable the campaign until the owner explicitly approves the plan change.
 *
 * The project does not currently include Firebase In-App Messaging, so this
 * file only records the exact dependency and trigger code. Do not merge this
 * TODO into production without adding the dependency and configuring the
 * Firebase campaign.
 *
 * 1. Dependency to add in app/build.gradle.kts:
 *
 *    implementation(platform(libs.firebase.bom))
 *    implementation("com.google.firebase:firebase-inappmessaging-display-ktx")
 *
 *    (This is the same BOM pattern already used for firebase-analytics-ktx,
 *    firebase-auth-ktx, etc. in the version catalog.)
 *
 * 2. Remote Config flag:
 *
 *    Name:  show_feedback_survey
 *    Type:  boolean
 *    Default: false
 *
 * 3. Trigger code (to run after at least one focus session has been completed
 *    and the user is on the Home screen):
 *
 *    val remoteConfig = Firebase.remoteConfig
 *    remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
 *        if (!task.isSuccessful) return@addOnCompleteListener
 *        val prefs = context.getSharedPreferences("feedback_survey", Context.MODE_PRIVATE)
 *        val alreadyShown = prefs.getBoolean("feedback_survey_shown", false)
 *        val enabled = remoteConfig.getBoolean("show_feedback_survey")
 *        if (enabled && !alreadyShown) {
 *            // Show the Firebase In-App Messaging campaign; the survey itself
 *            // is configured in the Firebase console with at most 2 questions:
 *            //   - Which feature do you use the most?
 *            //   - What feature is missing?
 *            prefs.edit { putBoolean("feedback_survey_shown", true) }
 *        }
 *    }
 *
 * 4. Frequency cap:
 *    The campaign must be set to "once per user" in the Firebase console and
 *    the local `feedback_survey_shown` flag above guarantees at most one
 *    display even if the Remote Config flag stays true.
 */
object FirebaseInAppMessagingSetup
