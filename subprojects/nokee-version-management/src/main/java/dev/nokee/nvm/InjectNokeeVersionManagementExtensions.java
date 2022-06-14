/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.nokee.nvm;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtensionAware;
import org.gradle.api.provider.Provider;

final class InjectNokeeVersionManagementExtensions implements Action<Project> {
	private final Provider<NokeeVersion> versionProvider;

	public InjectNokeeVersionManagementExtensions(Provider<NokeeVersion> versionProvider) {
		this.versionProvider = versionProvider;
	}

	@Override
	public void execute(Project project) {
		project.getDependencies().getExtensions().add("$nokeeExtension", new DefaultNokeeVersionManagementDependencyExtension(project.getDependencies(), versionProvider));
		((ExtensionAware) project.getRepositories()).getExtensions().add("$nokeeExtension", new DefaultNokeeVersionManagementRepositoryExtension(project.getRepositories(), versionProvider.map(new InferNokeeRepositoryUrl(project.getProviders()))));
	}
}
