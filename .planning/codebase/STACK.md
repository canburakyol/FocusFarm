# Technology Stack - FocusFarm

## Core
- **Language**: Kotlin 2.0.10
- **UI Framework**: Jetpack Compose (Material 3)
- **Minimum SDK**: 26 (Android 8.0)
- **Target/Compile SDK**: 36

## Key Dependencies
- **Dependency Injection**: Dagger Hilt 2.54
- **Database**: Room 2.8.4
- **Navigation**: Navigation Compose 2.9.7
- **Lifecycle**: Lifecycle Runtime Compose 2.10.0
- **Asynchronous**: Kotlin Coroutines
- **JSON/Network**: KSP (Google Devtools)

## Build & Tooling
- **Build System**: Gradle Kotlin DSL (AGP 8.9.1)
- **Compiler Plugins**: KSP, Compose Compiler, Dagger Hilt

## Configuration Files
- `build.gradle.kts` (root) - Global plugin versions
- `app/build.gradle.kts` - Module dependencies and Android settings
- `settings.gradle.kts` - Project organization
- `local.properties` - Local SDK paths
