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

package org.gradle.cache.internal;

/**
 * Controls whether advanced concurrency features for agent-driven workflows are enabled.
 */
public class ConcurrencyMode {
    public static final String AGENTIC_SYSTEM_PROPERTY = \"org.gradle.internal.concurrency.agentic\";

    /**
     * Returns true if agent-driven concurrency improvements are enabled.
     */
    public static boolean isAgentic() {
        return Boolean.getBoolean(AGENTIC_SYSTEM_PROPERTY);
    }
}
