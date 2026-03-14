# Skill: Systematic Debugging

A 4-phase process to resolve bugs efficiently and prevent regressions.

## Phase 1: Reproduce
- Create a minimal reproduction case (ideally a failing test).
- Document the exact steps/conditions to trigger the bug.

## Phase 2: Trace
- Use logs and debugger to trace the data flow.
- Identify the exact point where the state deviates from expectations.

## Phase 3: Fix (TDD)
- Write a test that covers the edge case.
- Implement the fix.
- Verify the test passes and no regressions are introduced.

## Phase 4: Defense in Depth
- Add validations or guards to prevent similar bugs in the future.
- Check if similar patterns exist elsewhere in the codebase.
