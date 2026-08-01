package com.focusfarm.app.ui.navigation

object FocusFarmNavigationContracts {
    const val HOME = "home"
    const val ONBOARDING = "onboarding"
    const val GARDEN = "garden"
    const val STATS = "stats"
    const val SHOP = "shop"
    const val SETTINGS = "settings"
    const val COLLECTION = "collection"
    const val INVITE_FRIENDS = "invite_friends"
    const val FOCUS_PARTY = "focus_party"
    const val SESSION = "session/{sessionId}/{plantId}/{minutes}"

    fun sessionRoute(sessionId: String, plantId: String, minutes: Int) = "session/$sessionId/$plantId/$minutes"
    fun recoverSessionRoute(sessionId: String) = "session/$sessionId/_/0"
}
