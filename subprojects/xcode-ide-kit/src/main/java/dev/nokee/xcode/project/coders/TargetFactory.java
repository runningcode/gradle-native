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
package dev.nokee.xcode.project.coders;

import dev.nokee.xcode.objects.targets.PBXTarget;
import dev.nokee.xcode.project.Codeable;
import dev.nokee.xcode.project.CodeableObjectFactory;
import dev.nokee.xcode.project.CodeablePBXAggregateTarget;
import dev.nokee.xcode.project.CodeablePBXLegacyTarget;
import dev.nokee.xcode.project.CodeablePBXNativeTarget;
import dev.nokee.xcode.project.KeyedObject;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public final class TargetFactory<T extends PBXTarget & Codeable> implements CodeableObjectFactory<T> {
	@Override
	@SuppressWarnings("unchecked")
	public T create(KeyedObject map) {
		switch (map.isa()) {
			case "PBXAggregateTarget": return (T) CodeablePBXAggregateTarget.newInstance(map);
			case "PBXLegacyTarget": return (T) CodeablePBXLegacyTarget.newInstance(map);
			case "PBXNativeTarget": return (T) CodeablePBXNativeTarget.newInstance(map);
			default: throw new UnsupportedOperationException();
		}
	}
}