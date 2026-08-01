package com.focusfarm.app.domain

/**
 * Lifecycle state of a plant in the garden.
 */
enum class GardenPlantState {
    /** Session still in progress (reserved for future real-time preview). */
    GROWING,

    /** Session completed successfully — plant fully bloomed. */
    BLOOMED,

    /** Session failed or abandoned — plant withered. */
    WITHERED,
}
