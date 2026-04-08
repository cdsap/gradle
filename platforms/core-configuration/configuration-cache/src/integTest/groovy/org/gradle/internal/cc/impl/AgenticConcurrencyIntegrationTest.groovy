/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the \"License\");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an \"AS IS\" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.cc.impl

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.test.fixtures.server.http.BlockingHttpServer
import org.junit.Rule
import spock.lang.Timeout

import java.util.concurrent.TimeUnit

class AgenticConcurrencyIntegrationTest extends AbstractIntegrationSpec {
    @Rule
    public final BlockingHttpServer server = new BlockingHttpServer()

    def setup() {
        server.start()
    }

    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    def "multiple daemons can concurrently load the configuration cache in agentic mode"() {
        given:
        def agenticFlag = \"-Dorg.gradle.internal.concurrency.agentic=true\"
        
        // Use a shared user home to test cross-process locking
        def sharedUserHome = file(\"shared-user-home\")
        
        settingsFile << \"rootProject.name = 'concurrent-test'\"
        buildFile << \"\"\"
            abstract class SlowTask extends DefaultTask {
                @TaskAction
                void run() {
                    ${server.callFromBuild(\"task-running\")}
                }
            }
            tasks.register(\"slow\", SlowTask)
        \"\"\"

        // 1. Warm up the configuration cache
        executer.withArguments(\"--configuration-cache\", agenticFlag)
            .withGradleUserHomeDir(sharedUserHome)
            .withTasks(\"help\")
            .run()

        when:
        // 2. Start two builds concurrently that load from the configuration cache
        // They should both be able to load the cache and reach the task execution phase
        def build1 = executer.withArguments(\"--configuration-cache\", agenticFlag)
            .withGradleUserHomeDir(sharedUserHome)
            .withTasks(\"slow\")
            .start()

        def build2 = executer.withArguments(\"--configuration-cache\", agenticFlag)
            .withGradleUserHomeDir(sharedUserHome)
            .withTasks(\"slow\")
            .start()

        then:
        // Both builds should be able to reach the server call concurrently
        server.expectConcurrent(\"task-running\", \"task-running\")
        
        build1.waitForFinish()
        build2.waitForFinish()
        
        build1.output.contains(\"Reusing configuration cache.\")
        build2.output.contains(\"Reusing configuration cache.\")
    }

    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    def "shows lock contention details in agentic mode"() {
        given:
        def agenticFlag = \"-Dorg.gradle.internal.concurrency.agentic=true\"
        def sharedUserHome = file(\"shared-user-home\")
        
        settingsFile << \"rootProject.name = 'contention-test'\"
        buildFile << \"\"\"
            // A task that takes a long time while holding the config cache lock (during store)
            if (project.hasProperty(\"slowConfig\")) {
                println \"Configuring slowly...\"
                ${server.callFromBuild(\"configuring\")}
            }
        \"\"\"

        when:
        // Build 1: Stores the cache, holding an exclusive lock
        def build1 = executer.withArguments(\"--configuration-cache\", agenticFlag, \"-PslowConfig\")
            .withGradleUserHomeDir(sharedUserHome)
            .withTasks(\"help\")
            .start()

        server.expectAndBlock(\"configuring\")
        
        // Build 2: Tries to load or store, should wait and log contention
        def build2 = executer.withArguments(\"--configuration-cache\", agenticFlag)
            .withGradleUserHomeDir(sharedUserHome)
            .withTasks(\"help\")
            .start()

        then:
        // We can't easily poll build2's output while it's running, 
        // but we can release build1 and then check build2's logs.
        server.release(\"configuring\")
        
        build1.waitForFinish()
        build2.waitForFinish()
        
        // Build 2 should have logged that it was waiting for the lock
        build2.output.contains(\"Waiting to acquire shared lock on Configuration Cache\")
        build2.output.contains(\"held by PID\")
    }

    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    def "multiple daemons can concurrently access the local build cache in agentic mode"() {
        given:
        def agenticFlag = \"-Dorg.gradle.internal.concurrency.agentic=true\"
        def sharedUserHome = file(\"shared-user-home\")
        def sharedBuildCache = file(\"shared-build-cache\")
        
        settingsFile << \"rootProject.name = 'cache-test'\"
        buildFile << \"\"\"
            @CacheableTask
            abstract class SlowCacheableTask extends DefaultTask {
                @OutputFile
                abstract RegularFileProperty getOutputFile()

                @TaskAction
                void run() {
                    outputFile.get().asFile.text = \"done\"
                    ${server.callFromBuild(\"task-running\")}
                }
            }
            tasks.register(\"slow\", SlowCacheableTask) {
                outputFile = project.layout.buildDirectory.file(\"out.txt\")
            }
        \"\"\"

        // Use a common build cache for both builds
        def cacheArgs = [\"--build-cache\", \"-Dorg.gradle.caching.local.directory=${sharedBuildCache.absolutePath}\"]

        when:
        // Start two builds concurrently that execute the same cacheable task
        def build1 = executer.withArguments(agenticFlag, *cacheArgs)
            .withGradleUserHomeDir(sharedUserHome)
            .withTasks(\"slow\")
            .start()

        def build2 = executer.withArguments(agenticFlag, *cacheArgs)
            .withGradleUserHomeDir(sharedUserHome)
            .withTasks(\"slow\")
            .start()

        then:
        // One build will run the task and store to cache, the other will either wait or run too
        // In this test, we just want to ensure they don't deadlock on the build cache lock.
        server.expectConcurrent(\"task-running\", \"task-running\")
        
        build1.waitForFinish()
        build2.waitForFinish()
    }
}
