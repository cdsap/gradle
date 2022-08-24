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
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.dsl.DependencyFactory;

import javax.inject.Inject;

/**
 * Universal APIs that are available for all {@code dependencies} blocks.
 *
 * @since 7.6
 */
@Incubating
public interface Dependencies {

    /**
     * A dependency factory is used to convert supported dependency notations into {@link org.gradle.api.artifacts.Dependency} instances.
     *
     * @return a dependency factory
     * @see DependencyFactory
     */
    @Inject
    DependencyFactory getDependencyFactory();

    /**
     * Converts an absolute or relative path to a project into a {@link ProjectDependency}. Project paths are separated by colons.
     *
     * This method fails if the project cannot be found.
     *
     * @param projectPath an absolute or relative path (from the current project) to a project
     * @return a {@link ProjectDependency} for the given path
     *
     * @see org.gradle.api.Project#project(String)
     */
    ProjectDependency project(String projectPath);

    /**
     * Returns the current project as a {@link ProjectDependency}.
     *
     * @return the current project as a dependency
     */
    ProjectDependency project();
}
