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
package dev.nokee.platform.nativebase.internal;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import dev.nokee.language.base.LanguageSourceSet;
import dev.nokee.language.nativebase.NativeHeaderSet;
import dev.nokee.language.swift.SwiftSourceSet;
import dev.nokee.language.swift.tasks.internal.SwiftCompileTask;
import dev.nokee.model.DependencyFactory;
import dev.nokee.model.DomainObjectProvider;
import dev.nokee.model.NamedDomainObjectRegistry;
import dev.nokee.model.PolymorphicDomainObjectRegistry;
import dev.nokee.model.internal.ModelPropertyIdentifier;
import dev.nokee.model.internal.actions.ConfigurableTag;
import dev.nokee.model.internal.core.IdentifierComponent;
import dev.nokee.model.internal.core.ModelActionWithInputs;
import dev.nokee.model.internal.core.ModelComponentReference;
import dev.nokee.model.internal.core.ModelComponentType;
import dev.nokee.model.internal.core.ModelNode;
import dev.nokee.model.internal.core.ModelNodeUtils;
import dev.nokee.model.internal.core.ModelNodes;
import dev.nokee.model.internal.core.ModelPath;
import dev.nokee.model.internal.core.ModelPathComponent;
import dev.nokee.model.internal.core.ModelPropertyRegistrationFactory;
import dev.nokee.model.internal.core.ModelRegistration;
import dev.nokee.model.internal.core.ModelSpecs;
import dev.nokee.model.internal.names.ExcludeFromQualifyingNameTag;
import dev.nokee.model.internal.registry.ModelLookup;
import dev.nokee.model.internal.registry.ModelRegistry;
import dev.nokee.model.internal.state.ModelState;
import dev.nokee.model.internal.type.ModelType;
import dev.nokee.platform.base.BuildVariant;
import dev.nokee.platform.base.Component;
import dev.nokee.platform.base.internal.BaseComponent;
import dev.nokee.platform.base.internal.BuildVariantInternal;
import dev.nokee.platform.base.internal.ComponentIdentifier;
import dev.nokee.platform.base.internal.ComponentName;
import dev.nokee.platform.base.internal.DimensionPropertyRegistrationFactory;
import dev.nokee.platform.base.internal.IsComponent;
import dev.nokee.platform.base.internal.VariantIdentifier;
import dev.nokee.platform.base.internal.Variants;
import dev.nokee.platform.base.internal.dependencies.DeclarableDependencyBucketRegistrationFactory;
import dev.nokee.platform.base.internal.dependencies.DefaultDependencyBucketFactory;
import dev.nokee.platform.base.internal.dependencies.DependencyBucketIdentifier;
import dev.nokee.platform.nativebase.NativeBinary;
import dev.nokee.platform.nativebase.NativeLibrary;
import dev.nokee.platform.nativebase.internal.dependencies.FrameworkAwareDependencyBucketFactory;
import dev.nokee.platform.nativebase.internal.dependencies.VariantComponentDependencies;
import dev.nokee.platform.nativebase.internal.rules.BuildableDevelopmentVariantConvention;
import dev.nokee.platform.nativebase.internal.rules.RegisterAssembleLifecycleTaskRule;
import dev.nokee.runtime.nativebase.BinaryLinkage;
import dev.nokee.runtime.nativebase.BuildType;
import dev.nokee.runtime.nativebase.TargetBuildType;
import dev.nokee.runtime.nativebase.TargetLinkage;
import dev.nokee.runtime.nativebase.TargetMachine;
import dev.nokee.runtime.nativebase.internal.TargetBuildTypes;
import dev.nokee.runtime.nativebase.internal.TargetLinkages;
import dev.nokee.runtime.nativebase.internal.TargetMachines;
import lombok.val;
import org.gradle.api.Project;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;

import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static dev.nokee.language.base.internal.LanguageSourceSetConventionSupplier.maven;
import static dev.nokee.language.base.internal.LanguageSourceSetConventionSupplier.withConventionOf;
import static dev.nokee.language.base.internal.SourceAwareComponentUtils.sourceViewOf;
import static dev.nokee.model.internal.core.ModelNodes.withType;
import static dev.nokee.model.internal.core.ModelProjections.createdUsing;
import static dev.nokee.model.internal.type.ModelType.of;
import static dev.nokee.platform.base.internal.dependencies.DependencyBucketIdentity.declarable;
import static dev.nokee.platform.nativebase.internal.plugins.NativeComponentBasePlugin.nativeLibraryProjection;
import static dev.nokee.platform.nativebase.internal.plugins.NativeLibraryPlugin.nativeLibraryVariant;
import static dev.nokee.utils.TransformerUtils.transformEach;

public final class NativeLibraryComponentModelRegistrationFactory {
	private final Class<Component> componentType;
	private final Class<Component> implementationComponentType;
	private final BiConsumer<? super ModelNode, ? super ModelPath> sourceRegistration;
	private final Project project;

	@SuppressWarnings("unchecked")
	public <T extends Component> NativeLibraryComponentModelRegistrationFactory(Class<? super T> componentType, Class<T> implementationComponentType, Project project, BiConsumer<? super ModelNode, ? super ModelPath> sourceRegistration) {
		this.componentType = (Class<Component>) componentType;
		this.implementationComponentType = (Class<Component>) implementationComponentType;
		this.sourceRegistration = sourceRegistration;
		this.project = project;
	}

	public ModelRegistration create(ComponentIdentifier identifier) {
		val entityPath = ModelPath.path(identifier.getName().get());
		val name = entityPath.getName();
		val builder = ModelRegistration.builder()
			.withComponent(new ModelPathComponent(entityPath))
			.withComponent(createdUsing(of(implementationComponentType), () -> project.getObjects().newInstance(implementationComponentType)))
			.withComponent(new IdentifierComponent(identifier))
			.withComponent(IsComponent.tag())
			.withComponent(ConfigurableTag.tag())
			// TODO: Should configure FileCollection on CApplication
			//   and link FileCollection to source sets
			.action(ModelActionWithInputs.of(ModelComponentReference.of(ModelPathComponent.class), ModelComponentReference.ofProjection(LanguageSourceSet.class), ModelComponentReference.of(ModelState.IsAtLeastRealized.class), (entity, path, sourceSet, ignored) -> {
				if (entityPath.isDescendant(path.get())) {
					withConventionOf(maven(ComponentName.of(name))).accept(ModelNodeUtils.get(entity, LanguageSourceSet.class));
				}
			}))
			.withComponent(createdUsing(of(DefaultNativeLibraryComponent.class), nativeLibraryProjection(name, project)))
			.action(ModelActionWithInputs.of(ModelComponentReference.of(ModelPathComponent.class), ModelComponentReference.of(ModelState.class), new ModelActionWithInputs.A2<ModelPathComponent, ModelState>() {
				private boolean alreadyExecuted = false;
				@Override
				public void execute(ModelNode entity, ModelPathComponent path, ModelState state) {
					if (entityPath.equals(path.get()) && state.equals(ModelState.Registered) && !alreadyExecuted) {
						alreadyExecuted = true;
						val registry = project.getExtensions().getByType(ModelRegistry.class);

						sourceRegistration.accept(entity, path.get());

						val bucketFactory = new DeclarableDependencyBucketRegistrationFactory(NamedDomainObjectRegistry.of(project.getConfigurations()), new FrameworkAwareDependencyBucketFactory(project.getObjects(), new DefaultDependencyBucketFactory(NamedDomainObjectRegistry.of(project.getConfigurations()), DependencyFactory.forProject(project))));

						registry.register(bucketFactory.create(DependencyBucketIdentifier.of(declarable("api"), identifier)));
						registry.register(bucketFactory.create(DependencyBucketIdentifier.of(declarable("implementation"), identifier)));
						registry.register(bucketFactory.create(DependencyBucketIdentifier.of(declarable("compileOnly"), identifier)));
						registry.register(bucketFactory.create(DependencyBucketIdentifier.of(declarable("linkOnly"), identifier)));
						registry.register(bucketFactory.create(DependencyBucketIdentifier.of(declarable("runtimeOnly"), identifier)));

						registry.register(project.getExtensions().getByType(ModelPropertyRegistrationFactory.class).createProperty(ModelPropertyIdentifier.of(identifier, "developmentVariant"), DefaultNativeLibraryVariant.class));

						val dimensions = project.getExtensions().getByType(DimensionPropertyRegistrationFactory.class);
						registry.register(dimensions.newAxisProperty(ModelPropertyIdentifier.of(identifier, "targetLinkages"))
							.elementType(TargetLinkage.class)
							.axis(BinaryLinkage.BINARY_LINKAGE_COORDINATE_AXIS)
							.defaultValue(TargetLinkages.SHARED)
							.build());
						registry.register(dimensions.newAxisProperty(ModelPropertyIdentifier.of(identifier, "targetBuildTypes"))
							.elementType(TargetBuildType.class)
							.axis(BuildType.BUILD_TYPE_COORDINATE_AXIS)
							.defaultValue(TargetBuildTypes.DEFAULT)
							.build());
						registry.register(dimensions.newAxisProperty(ModelPropertyIdentifier.of(identifier, "targetMachines"))
							.axis(TargetMachine.TARGET_MACHINE_COORDINATE_AXIS)
							.defaultValue(TargetMachines.host())
							.build());
					}
				}
			}))
			.action(new RegisterAssembleLifecycleTaskRule(identifier, PolymorphicDomainObjectRegistry.of(project.getTasks()), project.getExtensions().getByType(ModelRegistry.class), project.getProviders()))
			.action(ModelActionWithInputs.of(ModelComponentReference.of(ModelPathComponent.class), ModelComponentReference.of(ModelState.class), new ModelActionWithInputs.A2<ModelPathComponent, ModelState>() {
				private boolean alreadyExecuted = false;
				@Override
				@SuppressWarnings("unchecked")
				public void execute(ModelNode entity, ModelPathComponent path, ModelState state) {
					if (entityPath.equals(path.get()) && state.equals(ModelState.Registered) && !alreadyExecuted) {
						alreadyExecuted = true;
						ModelNodeUtils.get(entity, BaseComponent.class).getBaseName().convention(path.get().getName());
					}
				}
			}))
			.action(ModelActionWithInputs.of(ModelComponentReference.of(ModelPathComponent.class), ModelComponentReference.of(ModelState.IsAtLeastFinalized.class), (entity, path, ignored) -> {
				if (entityPath.equals(path.get())) {
					new CalculateNativeLibraryVariantAction(project).execute(entity, path);
				}
			}));

		if (identifier.isMainComponent()) {
			builder.withComponent(ExcludeFromQualifyingNameTag.tag());
		}

		return builder.build();
	}

	private static class CalculateNativeLibraryVariantAction extends ModelActionWithInputs.ModelAction1<ModelPathComponent> {
		private final Project project;

		private CalculateNativeLibraryVariantAction(Project project) {
			this.project = project;
		}

		@Override
		protected void execute(ModelNode entity, ModelPathComponent path) {
			val component = ModelNodeUtils.get(entity, ModelType.of(DefaultNativeLibraryComponent.class));

			val variants = ImmutableMap.<BuildVariant, ModelNode>builder();
			component.getBuildVariants().get().forEach(new Consumer<BuildVariant>() {
				private final ModelLookup modelLookup = project.getExtensions().getByType(ModelLookup.class);

				@Override
				public void accept(BuildVariant buildVariant) {
					val variantIdentifier = VariantIdentifier.builder().withBuildVariant((BuildVariantInternal) buildVariant).withComponentIdentifier(component.getIdentifier()).withType(DefaultNativeLibraryVariant.class).build();
					val variant = ModelNodeUtils.register(entity, nativeLibraryVariant(variantIdentifier, component, project));

					variants.put(buildVariant, ModelNodes.of(variant));
					onEachVariantDependencies(variant.as(NativeLibrary.class), ModelNodes.of(variant).getComponent(ModelComponentType.componentOf(VariantComponentDependencies.class)));
				}

				private void onEachVariantDependencies(DomainObjectProvider<NativeLibrary> variant, VariantComponentDependencies<?> dependencies) {
					if (NativeLibrary.class.isAssignableFrom(DefaultNativeLibraryVariant.class)) {
						if (modelLookup.anyMatch(ModelSpecs.of(withType(ModelType.of(SwiftSourceSet.class))))) {
							dependencies.getOutgoing().getExportedSwiftModule().convention(variant.flatMap(it -> {
								List<? extends Provider<RegularFile>> result = it.getBinaries().withType(NativeBinary.class).flatMap(binary -> {
									List<? extends Provider<RegularFile>> modules = binary.getCompileTasks().withType(SwiftCompileTask.class).map(task -> task.getModuleFile()).get();
									return modules;
								}).get();
								return one(result);
							}));
						}
						dependencies.getOutgoing().getExportedHeaders().from(sourceViewOf(component).filter(it -> (it instanceof NativeHeaderSet) && it.getName().equals("public")).map(transformEach(LanguageSourceSet::getSourceDirectories)));
					}
					dependencies.getOutgoing().getExportedBinary().convention(variant.flatMap(it -> it.getDevelopmentBinary()));
				}

				private <T> T one(Iterable<T> c) {
					Iterator<T> iterator = c.iterator();
					Preconditions.checkArgument(iterator.hasNext(), "collection needs to have one element, was empty");
					T result = iterator.next();
					Preconditions.checkArgument(!iterator.hasNext(), "collection needs to only have one element, more than one element found");
					return result;
				}
			});
			entity.addComponent(new Variants(variants.build()));

			component.finalizeExtension(null);
			component.getDevelopmentVariant().convention(project.provider(new BuildableDevelopmentVariantConvention<>(() -> component.getVariants().get())));
		}
	}
}
