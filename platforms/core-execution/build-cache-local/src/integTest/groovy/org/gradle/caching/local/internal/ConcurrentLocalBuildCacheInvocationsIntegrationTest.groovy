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

package org.gradle.caching.local.internal

import org.gradle.integtests.fixtures.AbstractIntegrationSpec

class ConcurrentLocalBuildCacheInvocationsIntegrationTest extends AbstractIntegrationSpec {
    def sharedLocalCacheDir = file("shared-local-cache")

    def "concurrent invocations can share local build cache directory"() {
        given:
        configureProject("project1")
        configureProject("project2")

        when:
        def build1 = executer
            .inDirectory(file("project1"))
            .withTasks("cacheable")
            .withArguments("--build-cache", "-Dorg.gradle.concurrent.invocations=true")
            .start()
        def build2 = executer
            .inDirectory(file("project2"))
            .withTasks("cacheable")
            .withArguments("--build-cache", "-Dorg.gradle.concurrent.invocations=true")
            .start()

        def result1 = build1.waitForFinish()
        def result2 = build2.waitForFinish()

        then:
        result1.assertTasksExecuted(":cacheable")
        result2.assertTasksExecuted(":cacheable")
        sharedLocalCacheDir.listFiles().find { it.name.length() > 20 }
    }

    private void configureProject(String projectName) {
        file("$projectName/settings.gradle") << """
            rootProject.name = '$projectName'
            buildCache {
                local {
                    enabled = true
                    push = true
                    directory = file('${sharedLocalCacheDir.absolutePath.replace('\\', '\\\\')}')
                }
            }
        """

        file("$projectName/build.gradle") << """
            import org.gradle.api.tasks.*

            @CacheableTask
            abstract class SharedCacheableTask extends DefaultTask {
                @Input
                String value = "same-input"

                @OutputFile
                File outputFile = project.layout.buildDirectory.file("generated/output.txt").get().asFile

                @TaskAction
                void run() {
                    Thread.sleep(1200)
                    outputFile.parentFile.mkdirs()
                    outputFile.text = value
                }
            }

            tasks.register("cacheable", SharedCacheableTask)
        """
    }
}
