/*
 * Copyright 2021 the original author or authors.
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
package dev.nokee.language.c;

import dev.nokee.internal.testing.testers.ConfigureMethodTester;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public interface HasCSourcesTester {
	HasCSources subject();

	@Test
	default void hasCSources() {
		assertNotNull(subject().getCSources());
	}

	@Test
	default void canConfigureCSources() {
		ConfigureMethodTester.of(subject(), HasCSources::getCSources)
			.testAction(HasCSources::cSources)
			.testClosure(HasCSources::cSources);
	}

	@Test
	default void canAccessCSourcesAsGroovyProperty() {
		assertEquals(subject().getCSources(), HasCSourcesGroovyDslHelper.getCSourcesAsGroovyProperty(subject()));
	}
}
