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

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.executer.GradleContextualExecuter
import org.gradle.integtests.fixtures.executer.GradleHandle

/**
 * Phase 8: two real Gradle processes (--no-daemon) share one {@code GRADLE_USER_HOME}
 * with {@code --concurrent}, exercising user-home cache locking without corrupt markers.
 */
class ConcurrentInvocationsSharedUserHomeIntegrationTest extends AbstractIntegrationSpec {

    def "two parallel Gradle clients resolve against the same Gradle user home with --concurrent"() {
        given:
        def sharedGradleUserHome = file("shared-gradle-user-home").createDir()
        def clientA = file("client-a").createDir()
        def clientB = file("client-b").createDir()

        mavenRepo.module("org.agentic", "concurrent-marker", "1.0").publish()

        [clientA, clientB].each { dir ->
            dir.file("settings.gradle") << "rootProject.name = '${dir.name}'\n"
            dir.file("build.gradle") << """
                plugins { id 'java' }
                repositories { maven { url = uri('${mavenRepo.uri}') } }
                dependencies { implementation 'org.agentic:concurrent-marker:1.0' }
            """.stripIndent()
            dir.file("src/main/java/MarkerUse.java") << """
                public class MarkerUse {
                    public static void touch() {}
                }
            """.stripIndent()
        }

        def buildContext = getBuildContext()
        def newClient = { File projectDir ->
            new GradleContextualExecuter(distribution, temporaryFolder, buildContext)
                .withGradleUserHomeDir(sharedGradleUserHome)
                .withArguments("--concurrent", "--no-daemon")
                .usingProjectDirectory(projectDir)
                .withTasks("compileJava")
        }

        GradleHandle h1 = newClient(clientA).start()
        GradleHandle h2 = newClient(clientB).start()

        when:
        def result1 = h1.waitForFinish()
        def result2 = h2.waitForFinish()

        then:
        [result1, result2].each {
            it.assertOutputContains("Concurrent invocations is an incubating feature")
            it.assertTasksExecuted(":compileJava")
        }
        sharedGradleUserHome.file("caches/modules-2").assertExists()
    }
}
