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
package dev.nokee.gradle;

import dev.nokee.publishing.internal.metadata.GradleModuleMetadata;
import org.gradle.api.Action;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;

import java.io.OutputStream;

public interface AdhocComponentSupplierDetails {
	ModuleComponentIdentifier getId();

	void metadata(Action<? super GradleModuleMetadata.Builder> action);

	void file(String filename, Action<? super OutputStream> action);
}
