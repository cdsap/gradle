## Phase 11: source concurrent-invocation mode from cross-build session state

### Evidence from Phase 10

Phase 10 introduced a process-local immutable workspace provider for artifact transforms, but the first real validation against the Android sample failed immediately in all 5 worktrees.

The startup error was:

```
Cannot create service of type ImmutableTransformWorkspaceServices ...
required service of type StartParameterInternal for parameter #6 is not available
```

This happened in `DependencyManagementGradleUserHomeScopeServices.createTransformWorkspaceServices(...)`.

### Why Phase 10 failed

`DependencyManagementGradleUserHomeScopeServices` is a user-home-scoped service. `StartParameterInternal` is not directly available in that scope, so wiring it into the user-home service constructor path worked in the focused test but failed in a real build.

The transform-isolation idea was still correct. The issue was only how the concurrent-invocation flag was sourced.

### New approach

Use `CrossBuildSessionParameters` instead of `StartParameterInternal` directly.

`CrossBuildSessionParameters` is available above the user-home scope and carries the current `StartParameterInternal`, so the user-home-scoped transform service can safely read:

- `crossBuildSessionParameters.getStartParameter().isConcurrentInvocationModeEnabled()`

This preserves the Phase 10 behavior change while fixing the service-scope mismatch that broke real builds.

### Changes

1. Updated `DependencyManagementGradleUserHomeScopeServices`
   - replaced the direct `StartParameterInternal` dependency with `CrossBuildSessionParameters`
   - continued to switch artifact transforms to `NonLockingImmutableWorkspaceProvider` only when concurrent invocation mode is enabled

2. Updated `DependencyManagementGradleUserHomeScopeServicesTest`
   - stubs `CrossBuildSessionParameters`
   - verifies default mode still uses `CacheBasedImmutableWorkspaceProvider`
   - verifies concurrent invocation mode uses `NonLockingImmutableWorkspaceProvider`

### Validation before rerunning the sample

Focused tests passed:

- `:dependency-management:test --tests org.gradle.api.internal.artifacts.DependencyManagementGradleUserHomeScopeServicesTest`
- `:execution:test --tests org.gradle.internal.execution.workspace.impl.NonLockingImmutableWorkspaceProviderTest`

With the scope fix in place, the next step is to rebuild the distribution and rerun the 5-worktree validation against the sample project.
