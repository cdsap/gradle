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

package org.gradle.internal.concurrent;

import org.gradle.internal.operations.BuildOperationRunner;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Per-thread stack of concurrent-invocation scopes, created for each {@code BuildTreeState}.
 * <p>
 * File locking and other cross-invocation mechanisms consult this to emit extra diagnostics
 * without reading start parameters from low-level infrastructure.
 */
public final class ConcurrentBuildInvocationContext {

    private static final class Frame {
        private final boolean concurrentInvocationsEnabled;
        private final @Nullable BuildOperationRunner buildOperationRunner;

        private Frame(boolean concurrentInvocationsEnabled, @Nullable BuildOperationRunner buildOperationRunner) {
            this.concurrentInvocationsEnabled = concurrentInvocationsEnabled;
            this.buildOperationRunner = buildOperationRunner;
        }
    }

    private static final ThreadLocal<Deque<Frame>> STACK = ThreadLocal.withInitial(ArrayDeque::new);

    private ConcurrentBuildInvocationContext() {
    }

    /**
     * Pushes a build-tree scope. Call {@link #leave()} when the tree is closed.
     *
     * @param buildOperationRunner may be null when global services omit it (for example some CLI bootstrap paths)
     */
    public static void enter(boolean concurrentInvocationsEnabled, @Nullable BuildOperationRunner buildOperationRunner) {
        STACK.get().push(new Frame(concurrentInvocationsEnabled, buildOperationRunner));
    }

    /**
     * Pops the innermost scope. Must balance each {@link #enter(boolean, BuildOperationRunner)}.
     */
    public static void leave() {
        Deque<Frame> deque = STACK.get();
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
        Deque<Frame> deque = STACK.get();
        return !deque.isEmpty() && deque.peek().concurrentInvocationsEnabled;
    }

    /**
     * Build operation runner for the current build tree, when registered.
     */
    @Nullable
    public static BuildOperationRunner currentBuildOperationRunner() {
        Deque<Frame> deque = STACK.get();
        return deque.isEmpty() ? null : deque.peek().buildOperationRunner;
    }
}
