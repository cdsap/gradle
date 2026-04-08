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
 * See the License for the specific language and limitations under the License.
 */

package org.gradle.internal.concurrent;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Per-thread stack of "concurrent invocations" opt-in flags, scoped to build tree entry/exit.
 * <p>
 * File locking and other cross-invocation mechanisms consult this to emit extra diagnostics
 * without reading start parameters from low-level infrastructure.
 */
public final class ConcurrentBuildInvocationContext {

    private static final ThreadLocal<Deque<Boolean>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private ConcurrentBuildInvocationContext() {
    }

    /**
     * Pushes the effective flag for a nested build tree scope. Call {@link #leave()} when the tree is closed.
     */
    public static void enter(boolean concurrentInvocationsEnabled) {
        STACK.get().push(concurrentInvocationsEnabled);
    }

    /**
     * Pops the innermost scope. Must balance each {@link #enter(boolean)}.
     */
    public static void leave() {
        Deque<Boolean> deque = STACK.get();
        if (deque.isEmpty()) {
            throw new IllegalStateException("ConcurrentBuildInvocationContext underflow");
        }
        deque.pop();
        if (deque.isEmpty()) {
            STACK.remove();
        }
    }

    /**
     * True when the current thread is inside a build tree that opted into concurrent invocations.
     */
    public static boolean isEnabled() {
        Deque<Boolean> deque = STACK.get();
        return !deque.isEmpty() && Boolean.TRUE.equals(deque.peek());
    }
}
