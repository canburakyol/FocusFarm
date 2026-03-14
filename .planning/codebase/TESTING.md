# Testing Strategy - FocusFarm

## Frameworks
- **Unit Testing**: JUnit 4.
- **Assertion**: Google Truth library.
- **Asynchronous**: `kotlinx-coroutines-test`.
- **Instrumentation**: AndroidJUnitRunner with Espresso (Standard).

## Test Focus
- **ViewModels**: Verification of UI state updates based on data inputs.
- **Domain Logic**: Unit tests for models and business rules.
- **Room DAOs**: Testing database operations and migrations.

## Locations
- `app/src/test`: Unit tests.
- `app/src/androidTest`: Instrumentation/UI tests.
