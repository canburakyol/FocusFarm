# Coding Conventions - FocusFarm

## Language & Style
- **Language**: Kotlin.
- **UI Framework**: Jetpack Compose only (No XML for UI).
- **Patterns**: Strict MVVM architecture.

## Naming Standards
- **ViewModels**: Always end with `ViewModel` (e.g., `FocusViewModel`).
- **Compose Functions**: PascalCase (e.g., `FocusScreen`).
- **Models/Classes**: PascalCase.
- **Packages**: lowercase.

## Rules
- **Error Handling**: Network and async calls must be wrapped in a `Result` wrapper.
- **Dependency Injection**: Use Hilt for all dependencies.
- **Functionality**: Prefer functional approaches (map, filter, etc.) over loops.
- **Static Strings**: Use constants or enums for important values, avoid magic strings.
