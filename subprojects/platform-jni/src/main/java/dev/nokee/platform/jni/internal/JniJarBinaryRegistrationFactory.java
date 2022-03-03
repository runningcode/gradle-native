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
package dev.nokee.platform.jni.internal;

import dev.nokee.model.internal.FullyQualifiedNameComponent;
import dev.nokee.model.internal.actions.ConfigurableTag;
import dev.nokee.model.internal.core.*;
import dev.nokee.platform.base.internal.BinaryIdentifier;
import dev.nokee.platform.base.internal.BinaryNamer;
import dev.nokee.platform.base.internal.IsBinary;
import dev.nokee.platform.base.internal.ModelBackedNamedMixIn;
import dev.nokee.platform.jni.JniJarBinary;
import dev.nokee.utils.TaskDependencyUtils;
import org.gradle.api.reflect.HasPublicType;
import org.gradle.api.reflect.TypeOf;
import org.gradle.api.tasks.TaskDependency;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.bundling.Jar;

import static dev.nokee.model.internal.DomainObjectIdentifierUtils.toPath;
import static dev.nokee.model.internal.core.ModelProjections.createdUsing;
import static dev.nokee.model.internal.type.ModelType.of;

public final class JniJarBinaryRegistrationFactory {
	private final JarTaskRegistrationActionFactory jarTaskFactory;

	public JniJarBinaryRegistrationFactory(JarTaskRegistrationActionFactory jarTaskFactory) {
		this.jarTaskFactory = jarTaskFactory;
	}

	public ModelRegistration create(BinaryIdentifier<?> identifier) {
		return ModelRegistration.builder()
			.withComponent(identifier)
			.withComponent(IsBinary.tag())
			.withComponent(ConfigurableTag.tag())
			.withComponent(new FullyQualifiedNameComponent(BinaryNamer.INSTANCE.determineName(identifier)))
			.withComponent(createdUsing(of(JniJarBinary.class), ModelBackedJniJarBinary::new))
			.action(jarTaskFactory.create(identifier))
			.action(ModelActionWithInputs.of(ModelComponentReference.of(BinaryIdentifier.class), ModelComponentReference.of(JarTask.class), (entity, id, jarTask) -> {
				if (id.equals(identifier)) {
					jarTask.configure(task -> task.getArchiveBaseName().convention(identifier.getName().get()));
					jarTask.configure(task -> {
						if (task.getDescription() == null) {
							task.setDescription(String.format("Assembles a JAR archive containing the shared library for %s.", identifier));
						}
					});
				}
			}))
			.build();
	}

	public static class ModelBackedJniJarBinary implements JniJarBinary, ModelNodeAware, HasPublicType, ModelBackedNamedMixIn {
		private final ModelNode node = ModelNodeContext.getCurrentModelNode();

		@Override
		public TaskProvider<Jar> getJarTask() {
			return (TaskProvider<Jar>) ModelElements.of(this).element("jar", Jar.class).asProvider();
		}

		@Override
		public TaskDependency getBuildDependencies() {
			return TaskDependencyUtils.of(getJarTask());
		}

		@Override
		public ModelNode getNode() {
			return node;
		}

		@Override
		public TypeOf<?> getPublicType() {
			return TypeOf.typeOf(JniJarBinary.class);
		}
	}
}
