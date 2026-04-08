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

package org.gradle.internal.cc.impl

import org.gradle.cache.FileLockManager
import org.gradle.cache.LockTimeoutException
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import java.io.File

class ConfigurationCacheRepositoryLockTimeoutTaggingTest {
    @Test
    fun `tags shared lock timeout with configuration-cache reason`() {
        val lockFile = File("configuration-cache.lock")
        val original = LockTimeoutException("Timed out waiting for configuration cache lock", lockFile)

        val tagged = ConfigurationCacheRepository.taggedLockTimeout(FileLockManager.LockMode.Shared, original)

        assertThat(tagged.message, containsString("concurrency-limited:lock-contention:configuration-cache:shared:"))
        assertThat(tagged.lockFile.path, containsString(lockFile.path))
    }

    @Test
    fun `tags exclusive lock timeout with configuration-cache reason`() {
        val lockFile = File("configuration-cache.lock")
        val original = LockTimeoutException("Timed out waiting for configuration cache lock", lockFile)

        val tagged = ConfigurationCacheRepository.taggedLockTimeout(FileLockManager.LockMode.Exclusive, original)

        assertThat(tagged.message, containsString("concurrency-limited:lock-contention:configuration-cache:exclusive:"))
        assertThat(tagged.lockFile.path, containsString(lockFile.path))
    }
}
