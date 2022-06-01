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
package dev.nokee.xcode;

import com.google.common.collect.ImmutableSet;
import lombok.EqualsAndHashCode;

import java.nio.file.Path;
import java.util.Set;

@EqualsAndHashCode
public final class XCProject {
	private final Path location;
	private final ImmutableSet<String> targetNames;
	private final ImmutableSet<String> schemeNames;

	public XCProject(Path location, ImmutableSet<String> targetNames, ImmutableSet<String> schemeNames) {
		this.location = location;
		this.targetNames = targetNames;
		this.schemeNames = schemeNames;
	}

	public Set<String> getTargetNames() {
		return targetNames;
	}

	public Set<String> getSchemeNames() {
		return schemeNames;
	}

	public XCProjectReference toReference() {
		return XCProjectReference.of(location);
	}
}
