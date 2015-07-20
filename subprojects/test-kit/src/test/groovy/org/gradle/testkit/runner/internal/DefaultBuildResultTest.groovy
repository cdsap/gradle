/*
 * Copyright 2015 the original author or authors.
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

package org.gradle.testkit.runner.internal

import org.gradle.testkit.runner.BuildTask
import org.gradle.testkit.runner.TaskResult
import spock.lang.Specification

class DefaultBuildResultTest extends Specification {
    BuildTask successBuildResult = new DefaultBuildTask(':a', TaskResult.SUCCESS)
    BuildTask failedBuildResult = new DefaultBuildTask(':b', TaskResult.FAILED)
    BuildTask skippedBuildResult = new DefaultBuildTask(':c', TaskResult.SKIPPED)
    def buildTasks = [successBuildResult, failedBuildResult]
    DefaultBuildResult defaultBuildResult = new DefaultBuildResult('output', 'error', buildTasks)

    def "provides expected field values"() {
        expect:
        defaultBuildResult.standardOutput == 'output'
        defaultBuildResult.standardError == 'error'
        defaultBuildResult.tasks == buildTasks
        defaultBuildResult.tasks(TaskResult.SUCCESS) == [successBuildResult]
        defaultBuildResult.tasks(TaskResult.FAILED) == [failedBuildResult]
        defaultBuildResult.taskPaths(TaskResult.SUCCESS) == [successBuildResult.path]
        defaultBuildResult.taskPaths(TaskResult.FAILED) == [failedBuildResult.path]
    }

    def "returned tasks are unmodifiable"() {
        when:
        defaultBuildResult.tasks << skippedBuildResult

        then:
        thrown(UnsupportedOperationException)

        when:
        defaultBuildResult.tasks(TaskResult.SUCCESS) << skippedBuildResult

        then:
        thrown(UnsupportedOperationException)

        when:
        defaultBuildResult.taskPaths(TaskResult.SUCCESS) << skippedBuildResult

        then:
        thrown(UnsupportedOperationException)
    }
}
