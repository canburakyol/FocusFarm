# ===== FocusFarm Release ProGuard Rules =====

# --- General ---
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod

# --- Hilt / Dagger ---
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-dontwarn dagger.hilt.**

# --- Room ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class * { *; }

# --- Jetpack Compose ---
-dontwarn androidx.compose.**

# --- Kotlin Coroutines ---
-dontwarn kotlinx.coroutines.**

# --- Google Play Billing ---
-keep class com.android.vending.billing.** { *; }
-keep class com.android.billingclient.** { *; }
-dontwarn com.android.billingclient.**

# --- Google Mobile Ads (AdMob) ---
-keep class com.google.android.gms.ads.** { *; }
-dontwarn com.google.android.gms.ads.**

# --- Firebase ---
-dontwarn com.google.firebase.**
-keepnames class com.google.firebase.crashlytics.** { *; }

# --- Navigation Compose ---
-keep class * extends androidx.navigation.Navigator { *; }

# --- Data classes used in Room entities (prevent field name obfuscation) ---
-keepclassmembers class com.focusfarm.app.data.local.** {
    <fields>;
}

