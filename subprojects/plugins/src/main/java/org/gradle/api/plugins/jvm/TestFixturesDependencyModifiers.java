/*
 * Copyright 2022 the original author or authors.
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

package org.gradle.api.plugins.jvm;

import org.gradle.api.Incubating;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.MinimalExternalModuleDependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.dsl.DependencyFactory;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderConvertible;
import org.gradle.internal.component.external.model.ImmutableCapability;
import org.gradle.internal.component.external.model.ProjectTestFixtures;

import javax.annotation.Nullable;

import static org.gradle.internal.component.external.model.TestFixturesSupport.TEST_FIXTURES_CAPABILITY_APPENDIX;

/**
 * Dependency APIs for using <a href="https://docs.gradle.org/current/userguide/java_testing.html#sec:java_test_fixtures">Test Fixtures</a> in {@code dependencies} blocks.
 *
 * @since 7.6
 */
@Incubating
public interface TestFixturesDependencyModifiers extends Dependencies {

    /**
     * Creates an {@link ExternalModuleDependency} for the given dependencyNotation and modifies it to select the Test Fixtures variant of the given module.
     *
     * @param dependencyNotation dependency notation
     * @return the modified dependency
     * @see DependencyFactory#create(CharSequence) Valid dependency notation for this method
     */
    default ExternalModuleDependency testFixtures(CharSequence dependencyNotation) {
        return testFixtures(getDependencyFactory().create(dependencyNotation));
    }

    /**
     * Creates an {@link ExternalModuleDependency} for the given group, name and version and modifies it to select the Test Fixtures variant of the given module.
     *
     * @param group the group
     * @param name the name
     * @param version the version
     * @return the modified dependency
     * @see DependencyFactory#create(String, String, String)
     */
    default ExternalModuleDependency testFixtures(@Nullable String group, String name, @Nullable String version) {
        return testFixtures(getDependencyFactory().create(group, name, version));
    }

    /**
     * Takes a given {@link ExternalModuleDependency} and modifies it to select the Test Fixtures variant of the given module.
     *
     * @param dependency the dependency
     * @return the modified dependency
     */
    default ExternalModuleDependency testFixtures(ExternalModuleDependency dependency) {
        dependency.capabilities(capabilities -> {
            capabilities.requireCapability(new ImmutableCapability(dependency.getGroup(), dependency.getName() + TEST_FIXTURES_CAPABILITY_APPENDIX, null));
        });
        return dependency;
    }

    /**
     * Takes a given {@link MinimalExternalModuleDependency} and modifies it to select the Test Fixtures variant of the given module.
     *
     * @param dependency the dependency
     * @return the modified dependency
     */
    default MinimalExternalModuleDependency testFixtures(MinimalExternalModuleDependency dependency) {
        dependency.capabilities(capabilities -> {
            capabilities.requireCapability(new ImmutableCapability(dependency.getGroup(), dependency.getName() + TEST_FIXTURES_CAPABILITY_APPENDIX, null));
        });
        return dependency;
    }

    /**
     * Takes a given {@link ProjectDependency} and modifies it to select the Test Fixtures variant of the given module.
     *
     * @param dependency the dependency
     * @return the modified dependency
     */
    default ProjectDependency testFixtures(ProjectDependency dependency) {
        dependency.capabilities(new ProjectTestFixtures(dependency.getDependencyProject()));
        return dependency;
    }

    /**
     * Takes a given {@code Provider} to a {@link MinimalExternalModuleDependency} and modifies the dependency to select the Test Fixtures variant of the given module.
     *
     * @param dependency the provider
     * @return a provider to the modified dependency
     */
    default Provider<? extends MinimalExternalModuleDependency> testFixtures(ProviderConvertible<? extends MinimalExternalModuleDependency> dependency) {
        return dependency.asProvider().map(this::testFixtures);
    }

    /**
     * Takes a given {@code Provider} to a {@link ExternalModuleDependency} and modifies the dependency to select the Test Fixtures variant of the given module.
     *
     * @param dependency the provider
     * @return a provider to the modified dependency
     */
    default Provider<? extends ExternalModuleDependency> testFixtures(Provider<? extends ExternalModuleDependency> dependency) {
        return dependency.map(this::testFixtures);
    }
}
