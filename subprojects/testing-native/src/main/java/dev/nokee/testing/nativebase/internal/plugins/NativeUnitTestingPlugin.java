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
package dev.nokee.testing.nativebase.internal.plugins;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import dev.nokee.language.base.LanguageSourceSet;
import dev.nokee.language.objectivec.ObjectiveCSourceSet;
import dev.nokee.language.objectivecpp.ObjectiveCppSourceSet;
import dev.nokee.language.swift.SwiftSourceSet;
import dev.nokee.model.DependencyFactory;
import dev.nokee.model.DomainObjectProvider;
import dev.nokee.model.NamedDomainObjectRegistry;
import dev.nokee.model.PolymorphicDomainObjectRegistry;
import dev.nokee.model.internal.DomainObjectEventPublisher;
import dev.nokee.model.internal.FullyQualifiedNameComponent;
import dev.nokee.model.internal.ModelPropertyIdentifier;
import dev.nokee.model.internal.ProjectIdentifier;
import dev.nokee.model.internal.core.*;
import dev.nokee.model.internal.registry.ModelLookup;
import dev.nokee.model.internal.registry.ModelRegistry;
import dev.nokee.model.internal.state.ModelState;
import dev.nokee.model.internal.state.ModelStates;
import dev.nokee.model.internal.type.TypeOf;
import dev.nokee.platform.base.BuildVariant;
import dev.nokee.platform.base.Component;
import dev.nokee.platform.base.internal.*;
import dev.nokee.platform.base.internal.binaries.BinaryConfigurer;
import dev.nokee.platform.base.internal.binaries.BinaryRepository;
import dev.nokee.platform.base.internal.binaries.BinaryViewFactory;
import dev.nokee.platform.base.internal.dependencies.*;
import dev.nokee.platform.base.internal.tasks.TaskIdentifier;
import dev.nokee.platform.base.internal.tasks.TaskName;
import dev.nokee.platform.base.internal.tasks.TaskRegistry;
import dev.nokee.platform.base.internal.tasks.TaskViewFactory;
import dev.nokee.platform.nativebase.NativeApplication;
import dev.nokee.platform.nativebase.NativeComponentDependencies;
import dev.nokee.platform.nativebase.internal.ExecutableBinaryInternal;
import dev.nokee.platform.nativebase.internal.dependencies.*;
import dev.nokee.platform.nativebase.internal.rules.BuildableDevelopmentVariantConvention;
import dev.nokee.platform.nativebase.internal.rules.RegisterAssembleLifecycleTaskRule;
import dev.nokee.platform.objectivec.ObjectiveCApplicationSources;
import dev.nokee.platform.objectivec.internal.plugins.ObjectiveCApplicationSourcesAdapter;
import dev.nokee.runtime.nativebase.*;
import dev.nokee.runtime.nativebase.internal.TargetBuildTypes;
import dev.nokee.runtime.nativebase.internal.TargetLinkages;
import dev.nokee.runtime.nativebase.internal.TargetMachines;
import dev.nokee.testing.base.TestSuiteContainer;
import dev.nokee.testing.base.internal.IsTestComponent;
import dev.nokee.testing.base.internal.plugins.TestingBasePlugin;
import dev.nokee.testing.nativebase.NativeTestSuite;
import dev.nokee.testing.nativebase.NativeTestSuiteVariant;
import dev.nokee.testing.nativebase.internal.DefaultNativeTestSuiteComponent;
import dev.nokee.testing.nativebase.internal.DefaultNativeTestSuiteVariant;
import dev.nokee.utils.ProviderUtils;
import lombok.val;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Usage;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.SetProperty;

import java.util.Optional;

import static dev.nokee.model.internal.core.ModelActions.once;
import static dev.nokee.model.internal.core.ModelComponentType.componentOf;
import static dev.nokee.model.internal.core.ModelNodeUtils.applyTo;
import static dev.nokee.model.internal.core.ModelNodes.discover;
import static dev.nokee.model.internal.core.ModelNodes.stateAtLeast;
import static dev.nokee.model.internal.core.ModelProjections.createdUsing;
import static dev.nokee.model.internal.core.NodePredicate.allDirectDescendants;
import static dev.nokee.model.internal.core.NodePredicate.self;
import static dev.nokee.model.internal.type.ModelType.of;
import static dev.nokee.platform.base.internal.LanguageSourceSetConventionSupplier.*;
import static dev.nokee.platform.base.internal.dependencies.DependencyBucketIdentity.*;
import static dev.nokee.utils.ConfigurationUtils.configureAttributes;
import static dev.nokee.utils.ConfigurationUtils.configureExtendsFrom;
import static org.gradle.language.base.plugins.LifecycleBasePlugin.ASSEMBLE_TASK_NAME;

public class NativeUnitTestingPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		project.getPluginManager().apply("lifecycle-base");
		project.getPluginManager().apply(TestingBasePlugin.class);

		val testSuites = project.getExtensions().getByType(TestSuiteContainer.class);
		val registry = ModelNodeUtils.get(ModelNodes.of(testSuites), NodeRegistrationFactoryRegistry.class);
		registry.registerFactory(of(NativeTestSuite.class), (ModelRegistrationFactory) name -> nativeTestSuite(name, project));

		project.afterEvaluate(proj -> {
			// TODO: We delay as late as possible to "fake" a finalize action.
			testSuites.configureEach(DefaultNativeTestSuiteComponent.class, it -> {
				ModelStates.finalize(it.getNode());
			});
		});
	}

	public static ModelRegistration nativeTestSuite(String name, Project project) {
		val identifier = ComponentIdentifier.builder().name(ComponentName.of(name)).displayName("native test suite").withProjectIdentifier(ProjectIdentifier.of(project)).build();
		val entityPath = ModelPath.path(identifier.getName().get());
		return ModelRegistration.builder()
			.withComponent(entityPath)
			.withComponent(createdUsing(of(DefaultNativeTestSuiteComponent.class), () -> new DefaultNativeTestSuiteComponent(identifier, project.getObjects(), project.getProviders(), project.getTasks(), project.getExtensions().getByType(DomainObjectEventPublisher.class), project.getExtensions().getByType(TaskRegistry.class), project.getExtensions().getByType(TaskViewFactory.class), project.getExtensions().getByType(ModelLookup.class))))
			.withComponent(IsTestComponent.tag())
			.withComponent(IsComponent.tag())
			.withComponent(identifier)
			.withComponent(new FullyQualifiedNameComponent(ComponentNamer.INSTANCE.determineName(identifier)))
			.action(ModelActionWithInputs.of(ModelComponentReference.of(ModelPath.class), ModelComponentReference.ofProjection(LanguageSourceSet.class).asDomainObject(), ModelComponentReference.of(ModelState.IsAtLeastRealized.class), (entity, path, sourceSet, ignored) -> {
				if (entityPath.isDescendant(path)) {
					withConventionOf(maven(identifier.getName())).accept(sourceSet);
				}
			}))
			.action(ModelActionWithInputs.of(ModelComponentReference.of(ModelPath.class), ModelComponentReference.ofProjection(ObjectiveCSourceSet.class).asDomainObject(), ModelComponentReference.of(ModelState.IsAtLeastRealized.class), (entity, path, sourceSet, ignored) -> {
				if (entityPath.isDescendant(path)) {
					withConventionOf(maven(identifier.getName()), defaultObjectiveCGradle(identifier.getName())).accept(sourceSet);
				}
			}))
			.action(ModelActionWithInputs.of(ModelComponentReference.of(ModelPath.class), ModelComponentReference.ofProjection(ObjectiveCppSourceSet.class).asDomainObject(), ModelComponentReference.of(ModelState.IsAtLeastRealized.class), (entity, path, sourceSet, ignored) -> {
				if (entityPath.isDescendant(path)) {
					withConventionOf(maven(identifier.getName()), defaultObjectiveCppGradle(identifier.getName())).accept(sourceSet);
				}
			}))
			// TODO: Choose a better component sources
			.action(ModelActionWithInputs.of(ModelComponentReference.of(ModelPath.class), ModelComponentReference.of(ModelState.class), new ModelActionWithInputs.A2<ModelPath, ModelState>() {
				private boolean alreadyExecuted = false;
				@Override
				public void execute(ModelNode entity, ModelPath path, ModelState state) {
					if (entityPath.equals(path) && state.equals(ModelState.Registered) && !alreadyExecuted) {
						alreadyExecuted = true;
						val registry = project.getExtensions().getByType(ModelRegistry.class);

						registry.register(project.getExtensions().getByType(ComponentSourcesPropertyRegistrationFactory.class).create(ModelPropertyIdentifier.of(identifier, "sources"), ObjectiveCApplicationSources.class, ObjectiveCApplicationSourcesAdapter::new));

						registry.register(project.getExtensions().getByType(ComponentBinariesPropertyRegistrationFactory.class).create(ModelPropertyIdentifier.of(identifier, "binaries")));

						registry.register(project.getExtensions().getByType(ComponentTasksPropertyRegistrationFactory.class).create(ModelPropertyIdentifier.of(identifier, "tasks")));

						val bucketFactory = new DeclarableDependencyBucketRegistrationFactory(NamedDomainObjectRegistry.of(project.getConfigurations()), new FrameworkAwareDependencyBucketFactory(project.getObjects(), new DefaultDependencyBucketFactory(NamedDomainObjectRegistry.of(project.getConfigurations()), DependencyFactory.forProject(project))));
						registry.register(project.getExtensions().getByType(ComponentDependenciesPropertyRegistrationFactory.class).create(ModelPropertyIdentifier.of(identifier, "dependencies"), NativeComponentDependencies.class, ModelBackedNativeComponentDependencies::new));

						registry.register(bucketFactory.create(DependencyBucketIdentifier.of(declarable("implementation"), identifier)));
						registry.register(bucketFactory.create(DependencyBucketIdentifier.of(declarable("compileOnly"), identifier)));
						registry.register(bucketFactory.create(DependencyBucketIdentifier.of(declarable("linkOnly"), identifier)));
						registry.register(bucketFactory.create(DependencyBucketIdentifier.of(declarable("runtimeOnly"), identifier)));

						registry.register(project.getExtensions().getByType(ComponentVariantsPropertyRegistrationFactory.class).create(ModelPropertyIdentifier.of(identifier, "variants"), NativeTestSuiteVariant.class));

						registry.register(ModelRegistration.builder()
							.withComponent(path.child("developmentVariant"))
							.withComponent(IsModelProperty.tag())
							.withComponent(ModelPropertyTag.instance())
							.withComponent(new ModelPropertyTypeComponent(of(NativeTestSuiteVariant.class)))
							.withComponent(createdUsing(of(new TypeOf<Property<NativeTestSuiteVariant>>() {}), () -> project.getObjects().property(NativeTestSuiteVariant.class)))
							.build());

						val testedComponentProperty = registry.register(project.getExtensions().getByType(ModelPropertyRegistrationFactory.class).createProperty(ModelPropertyIdentifier.of(identifier, "testedComponent"), Component.class));

						val dimensions = project.getExtensions().getByType(DimensionPropertyRegistrationFactory.class);
						val buildVariants = entity.addComponent(new BuildVariants(entity, project.getProviders(), project.getObjects()));
						entity.addComponent(new ModelBackedVariantDimensions(identifier, registry, dimensions));
						registry.register(dimensions.newAxisProperty(ModelPropertyIdentifier.of(identifier, "targetLinkages"))
							.elementType(TargetLinkage.class)
							.axis(BinaryLinkage.BINARY_LINKAGE_COORDINATE_AXIS)
							.defaultValue(TargetLinkages.EXECUTABLE)
							.build());
						registry.register(dimensions.newAxisProperty(ModelPropertyIdentifier.of(identifier, "targetBuildTypes"))
							.elementType(TargetBuildType.class)
							.axis(BuildType.BUILD_TYPE_COORDINATE_AXIS)
							.defaultValues(testedComponentProperty.as(Component.class).flatMap(component -> {
								val property = ModelProperties.findProperty(component, "targetBuildTypes");
								if (property.isPresent()) {
									return property.get().as(SetProperty.class).get();
								} else {
									return ProviderUtils.notDefined();
								}
							}).orElse(ImmutableSet.of(TargetBuildTypes.DEFAULT)))
							.build());
						registry.register(dimensions.newAxisProperty(ModelPropertyIdentifier.of(identifier, "targetMachines"))
							.axis(TargetMachine.TARGET_MACHINE_COORDINATE_AXIS)
							.defaultValues(testedComponentProperty.as(Component.class).flatMap(component -> {
								val property = ModelProperties.findProperty(component, "targetMachines");
								if (property.isPresent()) {
									return property.get().as(SetProperty.class).get();
								} else {
									return ProviderUtils.notDefined();
								}
							}).orElse(ImmutableSet.of(TargetMachines.host())))
							.build());
						registry.register(dimensions.buildVariants(ModelPropertyIdentifier.of(identifier, "buildVariants"), buildVariants.get()));
					}
				}
			}))
			.action(new RegisterAssembleLifecycleTaskRule(identifier, PolymorphicDomainObjectRegistry.of(project.getTasks()), project.getExtensions().getByType(ModelRegistry.class), project.getProviders()))
			.action(ModelActionWithInputs.of(ModelComponentReference.of(ModelPath.class), ModelComponentReference.of(ModelState.IsAtLeastFinalized.class), (entity, path, ignored) -> {
				if (entityPath.equals(path)) {
					val registry = project.getExtensions().getByType(ModelRegistry.class);
					val component = ModelNodeUtils.get(entity, DefaultNativeTestSuiteComponent.class);
					component.finalizeExtension(project);
					component.getDevelopmentVariant().convention(project.getProviders().provider(new BuildableDevelopmentVariantConvention<>(() -> component.getVariants().get())));

					val variants = ImmutableMap.<BuildVariant, ModelNode>builder();
					component.getBuildVariants().get().forEach(buildVariant -> {
						val variantIdentifier = VariantIdentifier.builder().withBuildVariant((BuildVariantInternal) buildVariant).withComponentIdentifier(component.getIdentifier()).withType(DefaultNativeTestSuiteVariant.class).build();

						val variant = ModelNodeUtils.register(entity, nativeTestSuiteVariant(variantIdentifier, component, project));
						variants.put(buildVariant, ModelNodes.of(variant));
						onEachVariantDependencies(variant.as(DefaultNativeTestSuiteVariant.class), ModelNodes.of(variant).getComponent(componentOf(VariantComponentDependencies.class)));
					});
					entity.addComponent(new Variants(variants.build()));
				}
			}))
			.build()
			;
	}

	private static NodeRegistration nativeTestSuiteVariant(VariantIdentifier<DefaultNativeTestSuiteVariant> identifier, DefaultNativeTestSuiteComponent component, Project project) {
		val taskRegistry = project.getExtensions().getByType(TaskRegistry.class);
		val assembleTask = taskRegistry.registerIfAbsent(TaskIdentifier.of(TaskName.of(ASSEMBLE_TASK_NAME), identifier));
		return NodeRegistration.unmanaged(identifier.getUnambiguousName(), of(DefaultNativeTestSuiteVariant.class), () -> {
			return project.getObjects().newInstance(DefaultNativeTestSuiteVariant.class, identifier, project.getObjects(), project.getProviders(), assembleTask, project.getExtensions().getByType(BinaryViewFactory.class));
		})
			.withComponent(IsVariant.tag())
			.withComponent(identifier)
			.withComponent(new FullyQualifiedNameComponent(VariantNamer.INSTANCE.determineName(identifier)))
			.action(self().apply(once(ModelActionWithInputs.of(ModelComponentReference.of(ModelPath.class), (entity, path) -> {
				entity.addComponent(new ModelBackedNativeIncomingDependencies(path, project.getObjects(), project.getProviders(), project.getExtensions().getByType(ModelLookup.class)));
			}))))
			.action(self(discover()).apply(ModelActionWithInputs.of(ModelComponentReference.of(ModelPath.class), (entity, path) -> {
				val registry = project.getExtensions().getByType(ModelRegistry.class);

				val binaryRepository = project.getExtensions().getByType(BinaryRepository.class);
				val binaryIdentifier = BinaryIdentifier.of(BinaryName.of("executable"), ExecutableBinaryInternal.class, identifier);
				val binaryEntity = registry.register(ModelRegistration.builder()
					.withComponent(path.child("executable"))
					.withComponent(IsBinary.tag())
					.withComponent(binaryIdentifier)
					.withComponent(new FullyQualifiedNameComponent(BinaryNamer.INSTANCE.determineName(binaryIdentifier)))
					.withComponent(createdUsing(of(ExecutableBinaryInternal.class), () -> binaryRepository.get(binaryIdentifier)))
					.build());
				project.getExtensions().getByType(BinaryConfigurer.class)
					.configure(binaryIdentifier, binary -> ModelStates.realize(ModelNodes.of(binaryEntity)));

				registry.register(project.getExtensions().getByType(ComponentBinariesPropertyRegistrationFactory.class).create(ModelPropertyIdentifier.of(identifier, "binaries")));

				registry.register(project.getExtensions().getByType(ComponentTasksPropertyRegistrationFactory.class).create(ModelPropertyIdentifier.of(identifier, "tasks")));

				val dependencies = registry.register(project.getExtensions().getByType(ComponentDependenciesPropertyRegistrationFactory.class).create(ModelPropertyIdentifier.of(identifier, "dependencies"), NativeComponentDependencies.class, ModelBackedNativeComponentDependencies::new));

				val bucketFactory = project.getExtensions().getByType(DeclarableDependencyBucketRegistrationFactory.class);
				val implementation = registry.register(bucketFactory.create(DependencyBucketIdentifier.of(declarable("implementation"), identifier)));
				val compileOnly = registry.register(bucketFactory.create(DependencyBucketIdentifier.of(declarable("compileOnly"), identifier)));
				val linkOnly = registry.register(bucketFactory.create(DependencyBucketIdentifier.of(declarable("linkOnly"), identifier)));
				val runtimeOnly = registry.register(bucketFactory.create(DependencyBucketIdentifier.of(declarable("runtimeOnly"), identifier)));

				val resolvableFactory = project.getExtensions().getByType(ResolvableDependencyBucketRegistrationFactory.class);
				boolean hasSwift = project.getExtensions().getByType(ModelLookup.class).anyMatch(ModelSpecs.of(ModelNodes.withType(of(SwiftSourceSet.class))));
				if (hasSwift) {
					val importModules = registry.register(resolvableFactory.create(DependencyBucketIdentifier.of(resolvable("importSwiftModules"), identifier)));
					importModules.configure(Configuration.class, configureExtendsFrom(implementation.as(Configuration.class), compileOnly.as(Configuration.class))
						.andThen(configureAttributes(it -> it.usage(project.getObjects().named(Usage.class, Usage.SWIFT_API))))
						.andThen(ConfigurationUtilsEx.configureIncomingAttributes((BuildVariantInternal) identifier.getBuildVariant(), project.getObjects()))
						.andThen(ConfigurationUtilsEx::configureAsGradleDebugCompatible));
				} else {
					val headerSearchPaths = registry.register(resolvableFactory.create(DependencyBucketIdentifier.of(resolvable("headerSearchPaths"), identifier)));
					headerSearchPaths.configure(Configuration.class, configureExtendsFrom(implementation.as(Configuration.class), compileOnly.as(Configuration.class))
						.andThen(configureAttributes(it -> it.usage(project.getObjects().named(Usage.class, Usage.C_PLUS_PLUS_API))))
						.andThen(ConfigurationUtilsEx.configureIncomingAttributes((BuildVariantInternal) identifier.getBuildVariant(), project.getObjects()))
						.andThen(ConfigurationUtilsEx::configureAsGradleDebugCompatible));
				}
				val linkLibraries = registry.register(resolvableFactory.create(DependencyBucketIdentifier.of(resolvable("linkLibraries"), identifier)));
				linkLibraries.configure(Configuration.class, configureExtendsFrom(implementation.as(Configuration.class), linkOnly.as(Configuration.class))
					.andThen(configureAttributes(it -> it.usage(project.getObjects().named(Usage.class, Usage.NATIVE_LINK))))
					.andThen(ConfigurationUtilsEx.configureIncomingAttributes((BuildVariantInternal) identifier.getBuildVariant(), project.getObjects()))
					.andThen(ConfigurationUtilsEx::configureAsGradleDebugCompatible));
				val runtimeLibraries = registry.register(resolvableFactory.create(DependencyBucketIdentifier.of(resolvable("runtimeLibraries"), identifier)));
				runtimeLibraries.configure(Configuration.class, configureExtendsFrom(implementation.as(Configuration.class), runtimeOnly.as(Configuration.class))
					.andThen(configureAttributes(it -> it.usage(project.getObjects().named(Usage.class, Usage.NATIVE_RUNTIME))))
					.andThen(ConfigurationUtilsEx.configureIncomingAttributes((BuildVariantInternal) identifier.getBuildVariant(), project.getObjects()))
					.andThen(ConfigurationUtilsEx::configureAsGradleDebugCompatible));

				val consumableFactory = project.getExtensions().getByType(ConsumableDependencyBucketRegistrationFactory.class);
				val runtimeElements = registry.register(consumableFactory.create(DependencyBucketIdentifier.of(consumable("runtimeElements"), identifier)));
				runtimeElements.configure(Configuration.class, configureExtendsFrom(implementation.as(Configuration.class), runtimeOnly.as(Configuration.class))
					.andThen(configureAttributes(it -> it.usage(project.getObjects().named(Usage.class, Usage.NATIVE_RUNTIME))))
					.andThen(ConfigurationUtilsEx.configureOutgoingAttributes((BuildVariantInternal) identifier.getBuildVariant(), project.getObjects())));
				val outgoing = entity.addComponent(new NativeApplicationOutgoingDependencies(ModelNodeUtils.get(ModelNodes.of(runtimeElements), Configuration.class), project.getObjects()));
				val incoming = entity.getComponent(NativeIncomingDependencies.class);
				entity.addComponent(new VariantComponentDependencies<NativeComponentDependencies>(dependencies.as(NativeComponentDependencies.class)::get, incoming, outgoing));

				whenElementKnown(entity, ModelActionWithInputs.of(ModelComponentReference.ofProjection(Configuration.class).asConfigurableProvider(), ModelComponentReference.of(ModelPath.class), (e, configurationProvider, p) -> {
					configurationProvider.configure(configuration -> {
						val parentConfigurationResult = project.getExtensions().getByType(ModelLookup.class).query(ModelSpecs.of(ModelNodes.withPath(path.getParent().get().child(p.getName()))));
						Optional.ofNullable(Iterables.getOnlyElement(parentConfigurationResult.get(), null)).ifPresent(parentConfigurationEntity -> {
							val parentConfiguration = ModelNodeUtils.get(parentConfigurationEntity, Configuration.class);
							if (!parentConfiguration.getName().equals(configuration.getName())) {
								configuration.extendsFrom(parentConfiguration);
							}
						});
					});
				}));
			})))
			;
	}

	private static void whenElementKnown(Object target, ModelAction action) {
		applyTo(ModelNodes.of(target), allDirectDescendants(stateAtLeast(ModelState.Created)).apply(once(action)));
	}

	private static void onEachVariantDependencies(DomainObjectProvider<DefaultNativeTestSuiteVariant> variant, VariantComponentDependencies<?> dependencies) {
		dependencies.getOutgoing().getExportedBinary().convention(variant.flatMap(it -> it.getDevelopmentBinary()));
	}
}
