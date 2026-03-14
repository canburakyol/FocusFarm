# Directory Structure - FocusFarm

## Root Organization
- `app/` - Main Android application module
  - `src/main/java/com/focusfarm/app/` - Source code
  - `src/main/res/` - Android resources (xml, values, layout)

## Source Code Organization (`src/main/java/com/focusfarm/app/`)
- `ui/` - Compose screens, components, ViewModels, and UI state
- `domain/` - Business models and core logic
- `data/` - Repositories, Room Database, and local storage logic
- `di/` - Hilt modules and dependency injection configuration
- `notifications/` - System notification handling
- `telemetry/` - Usage tracking and analytics logic
- `ads/` - AdMob integration logic
- `FocusFarmApp.kt` - Global application class
- `MainActivity.kt` - Main activity entry point

## Naming Conventions
- **ViewModels**: Always end with `ViewModel` (e.g., `StatsViewModel`).
- **Screens**: Typically end with `Screen` (e.g., `StatsScreen`).
- **Models**: Simple descriptive names (e.g., `Plant`).
- **Directories**: lowercase package naming.
