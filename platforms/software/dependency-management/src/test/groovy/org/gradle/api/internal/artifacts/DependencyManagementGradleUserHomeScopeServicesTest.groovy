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
package org.gradle.api.internal.artifacts

import org.gradle.api.internal.StartParameterInternal
import org.gradle.api.internal.artifacts.transform.ImmutableTransformWorkspaceServices
import org.gradle.api.internal.cache.CacheConfigurationsInternal
import org.gradle.api.internal.cache.CacheResourceConfigurationInternal
import org.gradle.api.internal.file.temp.GradleUserHomeTemporaryFileProvider
import org.gradle.cache.FineGrainedCacheBuilder
import org.gradle.cache.FineGrainedCacheCleanupStrategyFactory
import org.gradle.cache.FineGrainedMarkAndSweepCacheCleanupStrategy
import org.gradle.cache.FineGrainedPersistentCache
import org.gradle.cache.CleanupFrequency
import org.gradle.cache.scopes.GlobalScopedCacheBuilderFactory
import org.gradle.cache.internal.CrossBuildInMemoryCacheFactory
import org.gradle.internal.execution.workspace.impl.CacheBasedImmutableWorkspaceProvider
import org.gradle.internal.execution.workspace.impl.NonLockingImmutableWorkspaceProvider
import org.gradle.internal.file.FileAccessTimeJournal
import org.gradle.internal.service.scopes.CrossBuildSessionParameters
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.junit.Rule
import spock.lang.Specification

class DependencyManagementGradleUserHomeScopeServicesTest extends Specification {
    @Rule
    TestNameTestDirectoryProvider temporaryFolder = new TestNameTestDirectoryProvider(getClass())

    def cacheBuilder = Stub(FineGrainedCacheBuilder)
    def cacheBuilderFactory = Stub(GlobalScopedCacheBuilderFactory) {
        createFineGrainedCacheBuilder("transforms") >> cacheBuilder
    }
    def crossBuildInMemoryCacheFactory = Stub(CrossBuildInMemoryCacheFactory) {
        newCacheRetainingDataFromPreviousBuild(_) >> Stub(org.gradle.cache.internal.CrossBuildInMemoryCache)
    }
    def fileAccessTimeJournal = Stub(FileAccessTimeJournal)
    def cleanupFrequency = Stub(org.gradle.api.provider.Provider) {
        get() >> CleanupFrequency.ALWAYS
    }
    def cacheConfigurations = Stub(CacheConfigurationsInternal) {
        getCreatedResources() >> Stub(CacheResourceConfigurationInternal) {
            getEntryRetentionTimestampSupplier() >> ({ -> 0L } as java.util.function.Supplier<Long>)
        }
        getCleanupFrequency() >> cleanupFrequency
    }
    def cleanupStrategyFactory = Stub(FineGrainedCacheCleanupStrategyFactory) {
        markAndSweepCleanupStrategy(_, _) >> Stub(FineGrainedMarkAndSweepCacheCleanupStrategy)
    }
    def tempProvider = Mock(GradleUserHomeTemporaryFileProvider)
    def services = new DependencyManagementGradleUserHomeScopeServices()

    def setup() {
        cacheBuilder.withDisplayName("Artifact transforms cache") >> cacheBuilder
        cacheBuilder.withCleanupStrategy(_ as FineGrainedMarkAndSweepCacheCleanupStrategy) >> cacheBuilder
        cacheBuilder.open() >> Stub(FineGrainedPersistentCache) {
            getBaseDir() >> temporaryFolder.testDirectory.file("cache/transforms")
        }
    }

    def "uses shared cache-backed transform workspaces by default"() {
        def startParameter = new StartParameterInternal()
        def crossBuildSessionParameters = Stub(CrossBuildSessionParameters) {
            getStartParameter() >> startParameter
        }

        when:
        ImmutableTransformWorkspaceServices result = services.createTransformWorkspaceServices(
            cacheBuilderFactory,
            crossBuildInMemoryCacheFactory,
            fileAccessTimeJournal,
            cacheConfigurations,
            cleanupStrategyFactory,
            crossBuildSessionParameters,
            tempProvider
        )

        then:
        result.workspaceProvider instanceof CacheBasedImmutableWorkspaceProvider
        0 * tempProvider.newTemporaryDirectory(_ as String[])
    }

    def "uses process-local transform workspaces in concurrent invocation mode"() {
        def startParameter = new StartParameterInternal()
        startParameter.setConcurrentInvocationModeEnabled(true)
        def crossBuildSessionParameters = Stub(CrossBuildSessionParameters) {
            getStartParameter() >> startParameter
        }

        when:
        ImmutableTransformWorkspaceServices result = services.createTransformWorkspaceServices(
            cacheBuilderFactory,
            crossBuildInMemoryCacheFactory,
            fileAccessTimeJournal,
            cacheConfigurations,
            cleanupStrategyFactory,
            crossBuildSessionParameters,
            tempProvider
        )

        then:
        result.workspaceProvider instanceof NonLockingImmutableWorkspaceProvider
        1 * tempProvider.newTemporaryDirectory(_ as String[]) >> temporaryFolder.testDirectory.file("tmp/transforms")
    }
}
