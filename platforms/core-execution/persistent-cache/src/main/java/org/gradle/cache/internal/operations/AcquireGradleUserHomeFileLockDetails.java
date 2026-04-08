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

import org.jspecify.annotations.Nullable;

/**
 * Build operation details for cross-process file lock acquisition on shared Gradle state (typically under Gradle user home).
 */
public final class AcquireGradleUserHomeFileLockDetails {

    private final String lockTargetDisplayName;
    private final String lockMode;
    private final String lockFileAbsolutePath;
    private final @Nullable String operationDisplayName;

    public AcquireGradleUserHomeFileLockDetails(
        String lockTargetDisplayName,
        String lockMode,
        String lockFileAbsolutePath,
        @Nullable String operationDisplayName
    ) {
        this.lockTargetDisplayName = lockTargetDisplayName;
        this.lockMode = lockMode;
        this.lockFileAbsolutePath = lockFileAbsolutePath;
        this.operationDisplayName = operationDisplayName;
    }

    public String getLockTargetDisplayName() {
        return lockTargetDisplayName;
    }

    public String getLockMode() {
        return lockMode;
    }

    public String getLockFileAbsolutePath() {
        return lockFileAbsolutePath;
    }

    @Nullable
    public String getOperationDisplayName() {
        return operationDisplayName;
    }
}
