package com.focusfarm.app.domain

enum class FocusSessionType {
    COUNTDOWN,
    STOPWATCH,
    POMODORO,
    SHORT_BREAK,
    LONG_BREAK
}

enum class SessionStatus {
    COMPLETED,
    FAILED,
    CANCELLED
}

enum class SessionState {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}
