## Phase 12: move concurrent transform workspace selection into the invocation factory

### Evidence from Phase 11

Phase 11 replaced the direct `StartParameterInternal` dependency with `CrossBuildSessionParameters`, but the real sample validation still failed at startup in all 5 worktrees:

```
required service of type CrossBuildSessionParameters for parameter #6 is not available
```

This showed that neither `StartParameterInternal` nor `CrossBuildSessionParameters` can be injected into `DependencyManagementGradleUserHomeScopeServices` in the real user-home service graph used by artifact transforms.

### Why the previous approach was still wrong

The problem was not the transform-isolation idea. The problem was trying to make the user-home-scoped transform workspace service depend on build-scoped invocation state.

The mode switch needs access to:

- `StartParameterInternal.isConcurrentInvocationModeEnabled()`

But that information is only available in the build-scoped dependency-resolution service graph, not in the user-home-scoped shared transform workspace service.

### New approach

Keep the user-home-scoped service simple and shared:

- it always provides the normal shared cache-backed immutable transform workspace provider
- it continues to provide the shared identity cache

Move the concurrent-mode decision into `DefaultTransformInvocationFactory`, which is created in the dependency-resolution scope and can safely see:

- `StartParameterInternal`
- `FileAccessTimeJournal`
- `GradleUserHomeTemporaryFileProvider`

When concurrent invocation mode is enabled, the invocation factory now uses a process-local `NonLockingImmutableWorkspaceProvider` for immutable transforms. Otherwise it uses the shared user-home provider from `ImmutableTransformWorkspaceServices`.

### Changes

1. Reverted `DependencyManagementGradleUserHomeScopeServices` to always create the shared cache-backed transform workspace provider.
2. Updated `DefaultDependencyManagementServices.createTransformInvocationFactory(...)`
   - now passes `StartParameterInternal`
   - passes `FileAccessTimeJournal`
   - passes `GradleUserHomeTemporaryFileProvider`
3. Updated `DefaultTransformInvocationFactory`
   - selects the immutable workspace provider in build scope
   - uses a process-local non-locking provider only in concurrent invocation mode
4. Added `DefaultTransformInvocationFactoryTest`
   - verifies default mode uses the shared provider
   - verifies concurrent invocation mode uses `NonLockingImmutableWorkspaceProvider`
5. Kept focused coverage for the provider itself and for the user-home service’s shared-provider behavior

### Validation before rerunning the sample

Focused tests passed:

- `:dependency-management:test --tests org.gradle.api.internal.artifacts.DependencyManagementGradleUserHomeScopeServicesTest --tests org.gradle.api.internal.artifacts.transform.DefaultTransformInvocationFactoryTest`
- `:execution:test --tests org.gradle.internal.execution.workspace.impl.NonLockingImmutableWorkspaceProviderTest`

This iteration keeps the scope boundaries correct while preserving the intended artifact-transform isolation in concurrent invocation mode.
