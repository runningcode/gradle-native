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
package dev.nokee.buildadapter.xcode;

import dev.nokee.buildadapter.xcode.internal.plugins.AllXCProjectLocationsValueSource;
import dev.nokee.buildadapter.xcode.internal.plugins.XCProjectLocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;

import static dev.nokee.buildadapter.xcode.TestProjectReference.project;
import static dev.nokee.internal.testing.FileSystemMatchers.aFile;
import static dev.nokee.internal.testing.FileSystemMatchers.hasAbsolutePath;
import static dev.nokee.internal.testing.util.ProjectTestUtils.objectFactory;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

@ExtendWith(MockitoExtension.class)
class AllXCProjectLocationsValueSourceTests {
	@TempDir Path testDirectory;
	@Mock XCProjectLocator locator;
	AllXCProjectLocationsValueSource.Parameters parameters;
	AllXCProjectLocationsValueSource subject;

	@BeforeEach
	void createSubject() {
		parameters = objectFactory().newInstance(AllXCProjectLocationsValueSource.Parameters.class);
		parameters.getSearchDirectory().set(testDirectory.toFile());
		subject = new AllXCProjectLocationsValueSource(locator) {
			@Override
			public Parameters getParameters() {
				return parameters;
			}
		};
	}

	@Test
	void returnsProjectReferenceAsFoundByLocator() {
		Mockito.when(locator.findProjects(usingSearchDirectory())).thenReturn(asList(
			project("A"), project("C"), project("B")));

		assertThat(subject.obtain(), contains(project("A"), project("C"), project("B")));
	}

	@Test
	void returnsEmptyIterableWhenLocatorDoesNotFoundProjects() {
		Mockito.when(locator.findProjects(usingSearchDirectory())).thenReturn(emptyList());

		assertThat(subject.obtain(), emptyIterable());
	}

	private Path usingSearchDirectory() {
		return argThat(aFile(hasAbsolutePath(testDirectory.toAbsolutePath().toString())));
	}
}