/*
 * Copyright 2020 the original author or authors.
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

import dev.nokee.language.base.testers.LanguageSourceSetTester;

import java.io.File;

import static dev.nokee.language.base.internal.plugins.LanguageBasePlugin.sourceSet;
import static dev.nokee.model.fixtures.ModelRegistryTestUtils.create;
import static dev.nokee.model.fixtures.ModelRegistryTestUtils.registry;

class CHeaderSetTest extends LanguageSourceSetTester<CHeaderSet> {
	@Override
	public CHeaderSet createSubject() {
		return create(sourceSet("test", CHeaderSet.class)).as(CHeaderSet.class).get();
	}

	@Override
	public CHeaderSet createSubject(File temporaryDirectory) {
		return create(registry(temporaryDirectory), sourceSet("test", CHeaderSet.class)).as(CHeaderSet.class).get();
	}
}
