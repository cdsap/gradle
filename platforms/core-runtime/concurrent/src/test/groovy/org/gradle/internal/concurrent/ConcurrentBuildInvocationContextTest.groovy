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

package org.gradle.internal.concurrent

import spock.lang.Specification

class ConcurrentBuildInvocationContextTest extends Specification {

    def "default thread is not enabled"() {
        expect:
        !ConcurrentBuildInvocationContext.isEnabled()
    }

    def "enter and leave enable and disable"() {
        when:
        ConcurrentBuildInvocationContext.enter(false, null)
        then:
        !ConcurrentBuildInvocationContext.isEnabled()

        when:
        ConcurrentBuildInvocationContext.leave()
        then:
        !ConcurrentBuildInvocationContext.isEnabled()
    }

    def "enter true enables until leave"() {
        when:
        ConcurrentBuildInvocationContext.enter(true, null)
        then:
        ConcurrentBuildInvocationContext.isEnabled()

        when:
        ConcurrentBuildInvocationContext.leave()
        then:
        !ConcurrentBuildInvocationContext.isEnabled()
    }

    def "nested scopes use innermost flag"() {
        given:
        ConcurrentBuildInvocationContext.enter(true, null)
        ConcurrentBuildInvocationContext.enter(false, null)

        expect:
        !ConcurrentBuildInvocationContext.isEnabled()

        when:
        ConcurrentBuildInvocationContext.leave()
        then:
        ConcurrentBuildInvocationContext.isEnabled()

        when:
        ConcurrentBuildInvocationContext.leave()
        then:
        !ConcurrentBuildInvocationContext.isEnabled()
    }

    def "leave without enter throws"() {
        when:
        ConcurrentBuildInvocationContext.leave()
        then:
        thrown(IllegalStateException)
    }
}
