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

package org.gradle.internal.execution.history.impl

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSortedMap
import com.google.common.collect.Interners
import org.gradle.cache.CacheDecorator
import org.gradle.cache.IndexedCache
import org.gradle.cache.LockTimeoutException
import org.gradle.cache.PersistentCache
import org.gradle.cache.internal.InMemoryCacheDecoratorFactory
import org.gradle.caching.internal.origin.OriginMetadata
import org.gradle.internal.execution.history.AfterExecutionState
import org.gradle.internal.hash.ClassLoaderHierarchyHasher
import org.gradle.internal.snapshot.FileSystemSnapshot
import org.gradle.internal.snapshot.ValueSnapshot
import org.gradle.internal.snapshot.impl.ImplementationSnapshot
import spock.lang.Specification

import java.util.function.Supplier

class DefaultExecutionHistoryStoreTest extends Specification {
    def indexedCache = Mock(IndexedCache)
    def persistentCache = Stub(PersistentCache) {
        createIndexedCache(_) >> indexedCache
    }
    def inMemoryCacheDecoratorFactory = Stub(InMemoryCacheDecoratorFactory) {
        decorator(10000, false) >> Mock(CacheDecorator)
    }
    def stringInterner = Interners.<String>newWeakInterner()
    def classLoaderHasher = Mock(ClassLoaderHierarchyHasher)
    def store = new DefaultExecutionHistoryStore(
        ({ persistentCache } as Supplier<PersistentCache>),
        inMemoryCacheDecoratorFactory,
        stringInterner,
        classLoaderHasher
    )

    def "tags lock timeout during load"() {
        given:
        def lockFile = new File("execution-history.lock")
        indexedCache.getIfPresent(_) >> { throw new LockTimeoutException("Timed out reading execution history", lockFile) }

        when:
        store.load("key")

        then:
        def ex = thrown(LockTimeoutException)
        ex.message.contains("concurrency-limited:lock-contention:execution-history-store:load:")
        ex.lockFile == lockFile
    }

    def "tags lock timeout during store"() {
        given:
        def lockFile = new File("execution-history.lock")
        indexedCache.put(_, _) >> { throw new LockTimeoutException("Timed out writing execution history", lockFile) }
        def executionState = Stub(AfterExecutionState) {
            getOriginMetadata() >> Stub(OriginMetadata)
            getCacheKey() >> Stub(org.gradle.internal.hash.HashCode)
            getImplementation() >> Stub(ImplementationSnapshot)
            getAdditionalImplementations() >> ImmutableList.<ImplementationSnapshot>of()
            getInputProperties() >> ImmutableSortedMap.<String, ValueSnapshot>of()
            getInputFileProperties() >> ImmutableSortedMap.of()
            getOutputFilesProducedByWork() >> ImmutableSortedMap.<String, FileSystemSnapshot>of()
            isSuccessful() >> true
        }

        when:
        store.store("key", executionState)

        then:
        def ex = thrown(LockTimeoutException)
        ex.message.contains("concurrency-limited:lock-contention:execution-history-store:store:")
        ex.lockFile == lockFile
    }

    def "tags lock timeout during remove"() {
        given:
        def lockFile = new File("execution-history.lock")
        indexedCache.remove(_) >> { throw new LockTimeoutException("Timed out removing execution history entry", lockFile) }

        when:
        store.remove("key")

        then:
        def ex = thrown(LockTimeoutException)
        ex.message.contains("concurrency-limited:lock-contention:execution-history-store:remove:")
        ex.lockFile == lockFile
    }
}
