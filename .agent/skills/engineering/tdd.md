# Skill: Test-Driven Development (TDD)

Enforces the RED-GREEN-REFACTOR cycle to ensure every line of code is tested and necessary.

## The Cycle
1. **RED**: Write a failing test first. Verify that it fails for the expected reason.
2. **GREEN**: Write the *minimal* amount of code to make the test pass.
3. **REFACTOR**: Clean up the code while keeping tests passing.

## Rules
- No production code is written without a failing test.
- Every commit or task completion must have a passing test suite.
- If a bug is found, write a test that reproduces it before fixing it.

## Tools
- Application uses `vitest` for unit testing.
- Run tests using `npx vitest`.
