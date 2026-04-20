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

package org.gradle.internal.execution.workspace.impl

import org.gradle.internal.file.FileAccessTracker
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.gradle.test.fixtures.work.TestWorkerLeaseService
import org.junit.Rule
import spock.lang.Specification

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

class NonLockingImmutableWorkspaceProviderTest extends Specification {
    @Rule
    TestNameTestDirectoryProvider temporaryFolder = new TestNameTestDirectoryProvider(getClass())

    def fileAccessTracker = Mock(FileAccessTracker)
    def provider = new NonLockingImmutableWorkspaceProvider(fileAccessTracker, temporaryFolder.testDirectory.file("workspaces"))
    def workerLeaseService = new TestWorkerLeaseService()

    def "workspace does not acquire a cross-process lock"() {
        def workspace
        def expectedWorkspaceLocation = temporaryFolder.testDirectory.file("workspaces/identity")

        when:
        workspace = provider.getWorkspace("identity")
        def result = workspace.withFileLock { "ok" }

        then:
        result == "ok"
        workspace.immutableLocation == expectedWorkspaceLocation
        !workspace.softDeleted
        1 * fileAccessTracker.markAccessed(expectedWorkspaceLocation)
    }

    def "only one thread computes a workspace result at a time"() {
        def workspace
        def expectedWorkspaceLocation = temporaryFolder.testDirectory.file("workspaces/identity")
        def delegateCalls = new AtomicInteger()
        def work1Started = new CountDownLatch(1)
        def work2Started = new CountDownLatch(1)

        def thread1Result
        def thread2Result

        def thread1 = new Thread({
            thread1Result = workspace.getOrCompute(workerLeaseService, {
                delegateCalls.incrementAndGet()
                work1Started.countDown()
                work2Started.await()
                Thread.sleep(250)
                "value"
            })
        })
        def thread2 = new Thread({
            work1Started.await()
            work2Started.countDown()
            thread2Result = workspace.getOrCompute(workerLeaseService, { "other" })
        })

        when:
        workspace = provider.getWorkspace("identity")
        thread1.start()
        thread2.start()
        thread1.join(10000)
        thread2.join(10000)

        then:
        !thread1.alive
        !thread2.alive
        delegateCalls.get() == 1
        thread1Result.producedByCurrentThread
        !thread2Result.producedByCurrentThread
        thread1Result.get() == "value"
        thread2Result.get() == "value"

        and:
        1 * fileAccessTracker.markAccessed(expectedWorkspaceLocation)
    }
}
