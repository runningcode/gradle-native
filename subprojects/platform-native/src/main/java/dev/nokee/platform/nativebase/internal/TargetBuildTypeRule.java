/*
 * Copyright 2020-2021 the original author or authors.
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
package dev.nokee.platform.nativebase.internal;

import com.google.common.collect.ImmutableList;
import dev.nokee.runtime.nativebase.TargetBuildType;
import dev.nokee.runtime.nativebase.internal.TargetBuildTypes;
import lombok.AccessLevel;
import lombok.Getter;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyHandler;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.SetProperty;

import javax.inject.Inject;

public class TargetBuildTypeRule implements Action<Project> {
	private final SetProperty<TargetBuildType> targetBuildTypes;
	@Getter(AccessLevel.PROTECTED) private final ObjectFactory objects;
	@Getter(AccessLevel.PROTECTED) private final DependencyHandler dependencies;

	@Inject
	public TargetBuildTypeRule(SetProperty<TargetBuildType> targetBuildTypes, String componentName, ObjectFactory objects, DependencyHandler dependencies) {
		this.targetBuildTypes = targetBuildTypes;
		this.objects = objects;
		this.dependencies = dependencies;
		targetBuildTypes.convention(ImmutableList.of(TargetBuildTypes.DEFAULT));
	}

	@Override
	public void execute(Project project) {
		this.targetBuildTypes.disallowChanges();
		this.targetBuildTypes.finalizeValue();
	}
}