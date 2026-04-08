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

import spock.lang.Specification

class DefaultFileLockManagerConcurrentDiagnosticsPropertyTest extends Specification {

    void cleanup() {
        System.clearProperty(DefaultFileLockManager.CONCURRENT_LOCK_DIAGNOSTICS_PROPERTY)
    }

    def "concurrent lock diagnostics default to enabled when property is unset"() {
        given:
        System.clearProperty(DefaultFileLockManager.CONCURRENT_LOCK_DIAGNOSTICS_PROPERTY)

        expect:
        DefaultFileLockManager.concurrentLockDiagnosticsEnabled()
    }

    def "concurrent lock diagnostics can be disabled via system property"() {
        given:
        System.setProperty(DefaultFileLockManager.CONCURRENT_LOCK_DIAGNOSTICS_PROPERTY, "false")

        expect:
        !DefaultFileLockManager.concurrentLockDiagnosticsEnabled()
    }
}
