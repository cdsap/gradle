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

package org.gradle.cache.internal.operations;

/**
 * Outcome attached as the build operation {@linkplain org.gradle.internal.operations.BuildOperationContext#setResult result}
 * after acquiring a cross-process file lock (Phase 5).
 */
public final class AcquireGradleUserHomeFileLockResult {

    private final long totalDurationMillis;
    private final boolean observedContention;

    public AcquireGradleUserHomeFileLockResult(long totalDurationMillis, boolean observedContention) {
        this.totalDurationMillis = totalDurationMillis;
        this.observedContention = observedContention;
    }

    /**
     * Wall-clock time from starting the acquisition loop until the state-region lock was obtained.
     */
    public long getTotalDurationMillis() {
        return totalDurationMillis;
    }

    /**
     * True if at least one attempt failed before the lock was acquired (another holder or transient failure).
     */
    public boolean isObservedContention() {
        return observedContention;
    }
}
