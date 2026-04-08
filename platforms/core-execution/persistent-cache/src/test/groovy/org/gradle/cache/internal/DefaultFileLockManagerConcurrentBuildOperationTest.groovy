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

package org.gradle.cache.internal

import org.gradle.cache.FileLockManager
import org.gradle.cache.internal.filelock.DefaultLockOptions
import org.gradle.cache.internal.locklistener.DefaultFileLockContentionHandler
import org.gradle.cache.internal.locklistener.InetAddressProvider
import org.gradle.cache.internal.operations.AcquireGradleUserHomeFileLockDetails
import org.gradle.cache.internal.operations.AcquireGradleUserHomeFileLockResult
import org.gradle.internal.concurrent.CompositeStoppable
import org.gradle.internal.concurrent.ConcurrentBuildInvocationContext
import org.gradle.internal.operations.TestBuildOperationRunner
import org.gradle.internal.remote.internal.inet.InetAddressFactory
import org.gradle.test.fixtures.concurrent.ConcurrentSpec
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.junit.Rule

import java.net.InetAddress

import static org.gradle.cache.FileLockManager.LockMode.Exclusive

/**
 * Phase 6: ensures structured build operations for concurrent file-lock acquisition (Phase 4–5)
 * record details and results when {@link ConcurrentBuildInvocationContext} is active.
 */
class DefaultFileLockManagerConcurrentBuildOperationTest extends ConcurrentSpec {

    @Rule
    final TestNameTestDirectoryProvider tmpDir = new TestNameTestDirectoryProvider(getClass())

    def addressFactory = new InetAddressFactory()
    def addressProvider = new InetAddressProvider() {
        @Override
        InetAddress getWildcardBindingAddress() {
            return addressFactory.wildcardBindingAddress
        }

        @Override
        InetAddress getCommunicationAddress() {
            return addressFactory.localBindingAddress
        }
    }
    def contentionHandler = new DefaultFileLockContentionHandler(executorFactory, addressProvider)
    FileLockManager manager = new DefaultFileLockManager(Stub(ProcessMetaDataProvider), 5000, contentionHandler)

    def cleanup() {
        CompositeStoppable.stoppable(contentionHandler).stop()
    }

    def "records build operation with details and result for uncontended lock when concurrent context and runner present"() {
        given:
        def runner = new TestBuildOperationRunner()
        ConcurrentBuildInvocationContext.enter(true, runner)

        when:
        def file = tmpDir.file("exclusive.bin")
        def lock = manager.lock(file, DefaultLockOptions.mode(Exclusive), "test cache", "")
        lock.close()

        then:
        def record = runner.log.records.find {
            it.descriptor.displayName.startsWith("Acquire file lock on")
        }
        record != null
        record.descriptor.details instanceof AcquireGradleUserHomeFileLockDetails
        def details = (AcquireGradleUserHomeFileLockDetails) record.descriptor.details
        details.lockTargetDisplayName == "test cache"
        details.lockMode == Exclusive.name()
        details.lockFileAbsolutePath.contains('exclusive.bin')

        record.result instanceof AcquireGradleUserHomeFileLockResult
        def result = (AcquireGradleUserHomeFileLockResult) record.result
        result.totalDurationMillis >= 0
        !result.observedContention

        cleanup:
        ConcurrentBuildInvocationContext.leave()
    }
}
