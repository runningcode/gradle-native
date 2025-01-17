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
package dev.nokee.model.internal.actions;

import lombok.EqualsAndHashCode;
import org.gradle.api.specs.Spec;

@EqualsAndHashCode
final class EqualSpec implements ModelSpec, Spec<DomainObjectIdentity> {
	private final Object value;

	public EqualSpec(Object value) {
		this.value = value;
	}

	@Override
	public boolean isSatisfiedBy(DomainObjectIdentity element) {
		return element.get(value.getClass()).map(value::equals).orElse(false);
	}
}
