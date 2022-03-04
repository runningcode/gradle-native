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
package dev.nokee.platform.base.internal;

import com.google.common.collect.Iterables;
import com.google.common.collect.Streams;
import dev.nokee.model.internal.ModelPropertyIdentifier;
import dev.nokee.model.internal.actions.ConfigurableTag;
import dev.nokee.model.internal.core.*;
import dev.nokee.model.internal.registry.ModelConfigurer;
import dev.nokee.model.internal.registry.ModelLookup;
import dev.nokee.model.internal.registry.ModelRegistry;
import dev.nokee.model.internal.state.ModelState;
import dev.nokee.model.internal.state.ModelStates;
import dev.nokee.platform.base.TaskView;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Task;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;

import java.util.stream.Collectors;

import static dev.nokee.model.internal.DomainObjectIdentifierUtils.toPath;
import static dev.nokee.model.internal.core.ModelProjections.createdUsing;
import static dev.nokee.model.internal.type.ModelType.of;
import static dev.nokee.model.internal.type.ModelTypes.map;

public final class ComponentTasksPropertyRegistrationFactory {
	private final ModelRegistry registry;
	private final ModelPropertyRegistrationFactory propertyFactory;
	private final ModelConfigurer modelConfigurer;
	private final ProviderFactory providers;
	private final ModelLookup modelLookup;
	private final ObjectFactory objects;

	public ComponentTasksPropertyRegistrationFactory(ModelRegistry registry, ModelPropertyRegistrationFactory propertyFactory, ModelConfigurer modelConfigurer, ProviderFactory providers, ModelLookup modelLookup, ObjectFactory objects) {
		this.registry = registry;
		this.propertyFactory = propertyFactory;
		this.modelConfigurer = modelConfigurer;
		this.providers = providers;
		this.modelLookup = modelLookup;
		this.objects = objects;
	}

	public ModelRegistration create(ModelPropertyIdentifier identifier) {
		val path = toPath(identifier);
		assert path.getParent().isPresent();
		val ownerPath = path.getParent().get();
		return ModelRegistration.builder()
			.withComponent(identifier)
			.withComponent(ModelPropertyTag.instance())
			.withComponent(ConfigurableTag.tag())
			.withComponent(new ViewConfigurationBaseComponent(modelLookup.get(ownerPath)))
			.withComponent(new ModelPropertyTypeComponent(map(of(String.class), of(Task.class))))
			.withComponent(createdUsing(of(TaskView.class), () -> new TaskViewAdapter<>(new ViewAdapter<>(Task.class, new ModelNodeBackedViewStrategy(providers, objects, () -> {
				ModelStates.realize(modelLookup.get(ownerPath));
				ModelStates.finalize(modelLookup.get(ownerPath));
			})))))
			.action(ModelActionWithInputs.of(ModelComponentReference.of(ModelPropertyIdentifier.class), ModelComponentReference.of(ModelState.IsAtLeastRegistered.class), (ee, id, ignored) -> {
				if (id.equals(identifier)) {
					modelConfigurer.configure(ModelActionWithInputs.of(ModelComponentReference.of(ModelPath.class), ModelComponentReference.of(ModelState.IsAtLeastCreated.class), ModelComponentReference.of(IsTask.class), ModelComponentReference.ofProjection(Task.class), (e, p, ignored1, ignored2, projection) -> {
						if (ownerPath.isDescendant(p)) {
							val elementName = StringUtils.uncapitalize(Streams.stream(Iterables.skip(p, Iterables.size(ownerPath)))
								.filter(it -> !it.isEmpty())
								.map(StringUtils::capitalize)
								.collect(Collectors.joining()));
							registry.register(propertyFactory.create(ModelPropertyIdentifier.of(identifier, elementName), e));
						}
					}));
				}
			}))
			.build();
	}

	public ModelRegistration create(ModelPropertyIdentifier identifier, Class<? extends Task> elementType) {
		val path = toPath(identifier);
		assert path.getParent().isPresent();
		val ownerPath = path.getParent().get();
		return ModelRegistration.builder()
			.withComponent(identifier)
			.withComponent(ModelPropertyTag.instance())
			.withComponent(ConfigurableTag.tag())
			.withComponent(new ViewConfigurationBaseComponent(modelLookup.get(ownerPath)))
			.withComponent(new ModelPropertyTypeComponent(map(of(String.class), of(elementType))))
			.withComponent(createdUsing(of(TaskView.class), () -> new TaskViewAdapter<>(new ViewAdapter<>(elementType, new ModelNodeBackedViewStrategy(providers, objects, () -> {
				ModelStates.realize(modelLookup.get(ownerPath));
				ModelStates.finalize(modelLookup.get(ownerPath));
			})))))
			.build();
	}
}
