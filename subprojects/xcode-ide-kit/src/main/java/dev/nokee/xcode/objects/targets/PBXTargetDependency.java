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
package dev.nokee.xcode.objects.targets;

import dev.nokee.xcode.objects.PBXProjectItem;

import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;

/**
 * Element of the {@link PBXTarget#getDependencies()}. Represents a dependency of one target upon another target.
 */
public final class PBXTargetDependency extends PBXProjectItem {
	@Nullable private final String name;
	private final PBXTarget target;

	private PBXTargetDependency(@Nullable String name, PBXTarget target) {
		this.name = name;
		this.target = target;
	}

	public Optional<String> getName() {
		return Optional.ofNullable(name);
	}

	public PBXTarget getTarget() {
		return target;
	}

	// The field targetProxy is a bit complicated to support hence we delay its implementation until required.
	//   It references a PBXContainerItemProxy which seems to allow cross-project reference, e.g. reference
	//   an PBXObject from another xcodeproj. So far, we have only seen reference in the local xcodeproj.
	//   Given we split out the encoding/decoding and the PBXObject model, we don't have readily available
	//   global ID used to reference PBXObject. On top of this limitation, we decided the PBXObject models
	//   should be immutable once built. Given the PBXContainerItemProxy needs to reference the current PBXProject,
	//   we cannot fully create a working PBXContainerItemProxy. There is most likely a way around this limitation.
	//   However, since we don't need the targetProxy in all of our current use cases, we will simply ignore the field.

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private String name;
		private PBXTarget target;

		public Builder name(String name) {
			this.name = Objects.requireNonNull(name);
			return this;
		}

		public Builder target(PBXTarget target) {
			this.target = Objects.requireNonNull(target);
			return this;
		}

		public PBXTargetDependency build() {
			return new PBXTargetDependency(name,Objects.requireNonNull(target, "'target' must not be null"));
		}
	}
}