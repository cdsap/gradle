/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.execution.workspace.impl;

import org.gradle.internal.Cast;
import org.gradle.internal.execution.workspace.ImmutableWorkspaceProvider;
import org.gradle.internal.file.FileAccessTracker;
import org.gradle.internal.work.WorkerLeaseService;

import java.io.Closeable;
import java.io.File;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * An immutable workspace provider which isolates workspaces to the current process and does not use
 * cross-process file locking. This is intended for concurrent-invocation mode where avoiding shared
 * user-home contention is more important than cross-process workspace reuse.
 */
public class NonLockingImmutableWorkspaceProvider implements ImmutableWorkspaceProvider, Closeable {

    private final FileAccessTracker fileAccessTracker;
    private final File baseDirectory;
    private final Map<String, CompletableFuture<?>> workspaceResults = new ConcurrentHashMap<>();

    public NonLockingImmutableWorkspaceProvider(FileAccessTracker fileAccessTracker, File baseDirectory) {
        this.fileAccessTracker = fileAccessTracker;
        this.baseDirectory = baseDirectory;
    }

    @Override
    public ImmutableWorkspace getWorkspace(String path) {
        File workspace = new File(baseDirectory, path);
        fileAccessTracker.markAccessed(workspace);
        return new ImmutableWorkspace() {
            @Override
            public File getImmutableLocation() {
                return workspace;
            }

            @Override
            public <T> T withFileLock(Supplier<T> action) {
                return action.get();
            }

            @Override
            public <T> ConcurrentResult<T> getOrCompute(WorkerLeaseService workerLeaseService, Supplier<T> action) {
                CompletableFuture<T> thisOperationFuture = new CompletableFuture<>();
                CompletableFuture<T> runningOperationFuture = Cast.uncheckedCast(workspaceResults.putIfAbsent(path, thisOperationFuture));

                if (runningOperationFuture != null) {
                    T result = workerLeaseService.whileDisallowingProjectLockChanges(() ->
                        workerLeaseService.blocking(runningOperationFuture::join)
                    );
                    return ConcurrentResult.producedByOtherThread(result);
                }

                try {
                    T result = action.get();
                    thisOperationFuture.complete(result);
                    return ConcurrentResult.producedByCurrentThread(result);
                } catch (Exception e) {
                    thisOperationFuture.completeExceptionally(e);
                    throw e;
                } finally {
                    workspaceResults.remove(path);
                }
            }

            @Override
            public boolean isSoftDeleted() {
                return false;
            }

            @Override
            public void ensureUnSoftDeleted() {
                // No-op. Process-local workspaces are not soft deleted.
            }
        };
    }

    @Override
    public void close() {
        // Nothing to close.
    }
}
