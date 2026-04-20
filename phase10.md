## Phase 10: isolate transform immutable workspaces in concurrent invocation mode

### Evidence from the previous iteration

Phase 9 fixed configuration cache startup in concurrent invocation mode, but a later validation on another machine still failed when multiple worktrees shared the same Gradle user home. The failure was a `LockTimeoutException` for the artifact transforms cache:

```
Timeout waiting to lock Artifact transforms cache.
Lock file: ~/.gradle/caches/9.6.0-.../transforms/.internal/locks/...
```

The stacktrace showed contention in the immutable workspace assignment path:

- `CacheBasedImmutableWorkspaceProvider`
- `AssignImmutableWorkspaceStep`
- `CacheBasedImmutableWorkspaceProvider$1.withFileLock(...)`

That means concurrent invocation mode could start correctly, but artifact transforms still serialized or failed on the shared `~/.gradle/.../transforms` cache.

### Why the previous approach was insufficient

The earlier fixes focused on configuration cache startup and on failing fast for some shared-cache locks. They did not change how artifact transforms obtain immutable workspaces. Artifact transforms still used the shared fine-grained cache-backed workspace provider under the Gradle user home, so multiple processes could still block on the same per-transform lock file.

Disabling transform caching would not have been enough, because immutable workspace acquisition happens before cacheability decisions are applied.

### New approach

When `-Dorg.gradle.concurrent.invocations=true` is enabled:

- keep the transform identity cache logic unchanged
- stop using the shared cache-backed immutable workspace provider for artifact transforms
- instead, create a process-local immutable workspace provider rooted in a temporary directory under the Gradle user home temp area
- avoid cross-process file locking there while still deduplicating work inside a single process

This keeps default behavior unchanged for normal builds and narrows the isolation to the concurrent-invocation mode that is explicitly opting into cross-process concurrency.

### Changes

1. Added `NonLockingImmutableWorkspaceProvider`
   - process-local immutable workspaces
   - no cross-process file locking
   - retains in-process `getOrCompute(...)` coalescing for the same workspace identity

2. Updated `DependencyManagementGradleUserHomeScopeServices`
   - in normal mode, artifact transforms still use `CacheBasedImmutableWorkspaceProvider`
   - in concurrent invocation mode, artifact transforms now use `NonLockingImmutableWorkspaceProvider`
   - the process-local workspace root comes from `GradleUserHomeTemporaryFileProvider`

3. Added focused tests
   - `NonLockingImmutableWorkspaceProviderTest`
   - `DependencyManagementGradleUserHomeScopeServicesTest`

### Expected outcome

Concurrent builds in different worktrees should no longer contend on the shared artifact transform immutable workspace locks in `~/.gradle/caches/.../transforms`, while standard Gradle behavior remains unchanged outside concurrent invocation mode.
