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
package org.gradle.api.internal.artifacts.transform

import org.gradle.api.internal.StartParameterInternal
import org.gradle.api.internal.file.temp.GradleUserHomeTemporaryFileProvider
import org.gradle.internal.execution.workspace.ImmutableWorkspaceProvider
import org.gradle.internal.execution.workspace.impl.NonLockingImmutableWorkspaceProvider
import org.gradle.internal.file.FileAccessTimeJournal
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.junit.Rule
import spock.lang.Specification

class DefaultTransformInvocationFactoryTest extends Specification {
    @Rule
    TestNameTestDirectoryProvider temporaryFolder = new TestNameTestDirectoryProvider(getClass())

    def sharedWorkspaceProvider = Stub(ImmutableWorkspaceProvider)
    def immutableWorkspaceServices = Stub(ImmutableTransformWorkspaceServices) {
        getWorkspaceProvider() >> sharedWorkspaceProvider
    }
    def fileAccessTimeJournal = Stub(FileAccessTimeJournal)
    def tempProvider = Mock(GradleUserHomeTemporaryFileProvider)

    def "uses shared immutable workspace provider by default"() {
        def startParameter = new StartParameterInternal()

        when:
        def result = DefaultTransformInvocationFactory.createImmutableWorkspaceProvider(
            immutableWorkspaceServices,
            startParameter,
            fileAccessTimeJournal,
            tempProvider
        )

        then:
        result.is(sharedWorkspaceProvider)
        0 * tempProvider._
    }

    def "uses process-local immutable workspace provider in concurrent invocation mode"() {
        def startParameter = new StartParameterInternal()
        startParameter.setConcurrentInvocationModeEnabled(true)

        when:
        def result = DefaultTransformInvocationFactory.createImmutableWorkspaceProvider(
            immutableWorkspaceServices,
            startParameter,
            fileAccessTimeJournal,
            tempProvider
        )

        then:
        result instanceof NonLockingImmutableWorkspaceProvider
        1 * tempProvider.newTemporaryDirectory(_ as String[]) >> temporaryFolder.testDirectory.file("tmp/transforms")
    }
}
