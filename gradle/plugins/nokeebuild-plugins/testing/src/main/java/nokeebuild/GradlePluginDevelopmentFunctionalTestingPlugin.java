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
package nokeebuild;

import dev.gradleplugins.GradlePluginDevelopmentTestSuite;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import javax.inject.Inject;

import static java.util.Arrays.asList;
import static nokeebuild.UseJUnitJupiter.junitVersion;

abstract /*final*/ class GradlePluginDevelopmentFunctionalTestingPlugin implements Plugin<Project> {
	@Inject
	public GradlePluginDevelopmentFunctionalTestingPlugin() {}

	@Override
	public void apply(Project project) {
		project.getPluginManager().apply("dev.gradleplugins.gradle-plugin-functional-test");
		functionalTest(project, testSuite -> {
			testSuite.getTestingStrategies().convention(asList(
				testSuite.getStrategies().getCoverageForMinimumVersion(),
				testSuite.getStrategies().getCoverageForLatestGlobalAvailableVersion(),
				testSuite.getStrategies().getCoverageForLatestNightlyVersion()
			));
			testSuite.dependencies(it -> {
				it.implementation(project.project(":internalTesting"));
				it.implementation(it.spockFramework());
				it.implementation(it.gradleFixtures());
			});
			testSuite.getTestTasks().configureEach(task -> {
				task.systemProperty("org.gradle.testkit.dir", project.getLayout().getBuildDirectory().dir("gradle-runner-kit").get().getAsFile().getAbsolutePath());
			});
		});
		functionalTest(project, new UseJUnitJupiter(junitVersion(project)));
	}

	private static void functionalTest(Project project, Action<? super GradlePluginDevelopmentTestSuite> action) {
		final GradlePluginDevelopmentTestSuite extension = (GradlePluginDevelopmentTestSuite) project.getExtensions().getByName("functionalTest");
		action.execute(extension);
	}
}
