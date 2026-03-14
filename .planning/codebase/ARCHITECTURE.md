# Architecture - FocusFarm

## Pattern
The project strictly follows the **MVVM (Model-View-ViewModel)** architectural pattern.

## Layers
- **UI Layer (Presentation)**: Built with Jetpack Compose. Screens and components are in `ui/`. Each screen has a corresponding ViewModel.
- **Domain Layer**: Contains business models (e.g., `Plant.kt`) and business rules in the `domain/` package.
- **Data Layer**: Handles data persistence (Room) and external interactions (Ads, Telemetry). Located in `data/`.
- **DI Layer**: Dagger Hilt manages dependency injection, with modules in `di/`.

## Data Flow
1. **Trigger**: UI event in a Compose screen.
2. **Action**: Screen calls a method in the associated ViewModel.
3. **Processing**: ViewModel interacts with Repositories (Data Layer). State is exposed via `StateFlow` or `MutableState`.
4. **Update**: Compose UI observes state changes and re-composes.

## Entry Point
- `MainActivity.kt`: The main entry point for the Android application.
- `FocusFarmApp.kt`: Custom Application class for DI (Hilt) and global initialization.
