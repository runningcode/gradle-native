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
package dev.nokee.testing.nativebase.internal;

import com.google.common.collect.ImmutableList;
import dev.nokee.language.base.LanguageSourceSet;
import dev.nokee.language.base.internal.IsLanguageSourceSet;
import dev.nokee.language.base.internal.LanguageSourceSetIdentifier;
import dev.nokee.language.base.internal.LanguageSourceSetRegistrationFactory;
import dev.nokee.language.nativebase.NativeHeaderSet;
import dev.nokee.language.nativebase.tasks.internal.NativeSourceCompileTask;
import dev.nokee.language.swift.SwiftSourceSet;
import dev.nokee.language.swift.tasks.internal.SwiftCompileTask;
import dev.nokee.model.KnownDomainObject;
import dev.nokee.model.internal.DomainObjectEventPublisher;
import dev.nokee.model.internal.core.*;
import dev.nokee.model.internal.registry.ModelLookup;
import dev.nokee.model.internal.registry.ModelNodeBackedKnownDomainObject;
import dev.nokee.model.internal.registry.ModelRegistry;
import dev.nokee.model.internal.state.ModelState;
import dev.nokee.model.internal.type.ModelType;
import dev.nokee.platform.base.Binary;
import dev.nokee.platform.base.BinaryView;
import dev.nokee.platform.base.ComponentSources;
import dev.nokee.platform.base.VariantView;
import dev.nokee.platform.base.internal.*;
import dev.nokee.platform.base.internal.tasks.TaskIdentifier;
import dev.nokee.platform.base.internal.tasks.TaskName;
import dev.nokee.platform.base.internal.tasks.TaskRegistry;
import dev.nokee.platform.base.internal.tasks.TaskViewFactory;
import dev.nokee.platform.nativebase.ExecutableBinary;
import dev.nokee.platform.nativebase.NativeBinary;
import dev.nokee.platform.nativebase.NativeComponentDependencies;
import dev.nokee.platform.nativebase.internal.BaseNativeComponent;
import dev.nokee.platform.nativebase.internal.DefaultNativeApplicationComponent;
import dev.nokee.platform.nativebase.internal.ExecutableBinaryInternal;
import dev.nokee.platform.nativebase.internal.rules.CreateVariantAssembleLifecycleTaskRule;
import dev.nokee.platform.nativebase.internal.rules.CreateVariantAwareComponentObjectsLifecycleTaskRule;
import dev.nokee.platform.nativebase.internal.rules.CreateVariantObjectsLifecycleTaskRule;
import dev.nokee.platform.nativebase.tasks.LinkExecutable;
import dev.nokee.platform.nativebase.tasks.internal.LinkExecutableTask;
import dev.nokee.runtime.core.CoordinateSet;
import dev.nokee.runtime.core.Coordinates;
import dev.nokee.runtime.nativebase.internal.TargetLinkages;
import dev.nokee.testing.base.TestSuiteComponent;
import dev.nokee.testing.nativebase.NativeTestSuite;
import groovy.lang.Closure;
import lombok.Getter;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.internal.Cast;
import org.gradle.language.nativeplatform.tasks.AbstractNativeSourceCompileTask;
import org.gradle.language.nativeplatform.tasks.UnexportMainSymbol;
import org.gradle.language.swift.tasks.SwiftCompile;
import org.gradle.nativeplatform.test.tasks.RunTestExecutable;
import org.gradle.util.ConfigureUtil;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.Callable;

import static com.google.common.base.Predicates.instanceOf;
import static dev.nokee.model.internal.core.ModelActions.once;
import static dev.nokee.model.internal.core.ModelComponentType.projectionOf;
import static dev.nokee.model.internal.core.ModelNodeUtils.applyTo;
import static dev.nokee.model.internal.core.ModelNodes.*;
import static dev.nokee.model.internal.core.NodePredicate.allDirectDescendants;
import static dev.nokee.model.internal.type.ModelType.of;
import static dev.nokee.platform.base.internal.SourceAwareComponentUtils.sourceViewOf;
import static dev.nokee.runtime.nativebase.BinaryLinkage.BINARY_LINKAGE_COORDINATE_AXIS;
import static dev.nokee.utils.TransformerUtils.transformEach;
import static java.util.stream.Collectors.toList;

public class DefaultNativeTestSuiteComponent extends BaseNativeComponent<DefaultNativeTestSuiteVariant> implements NativeTestSuite
	, ModelBackedSourceAwareComponentMixIn<ComponentSources>
	, ModelBackedVariantAwareComponentMixIn<DefaultNativeTestSuiteVariant>
	, ModelBackedHasDevelopmentVariantMixIn<DefaultNativeTestSuiteVariant>
	, ModelBackedNamedMixIn
{
	private final ObjectFactory objects;
	private final ProviderFactory providers;
	@Getter Property<BaseComponent<?>> testedComponent;
	private final TaskRegistry taskRegistry;
	private final TaskContainer tasks;
	private final ModelLookup modelLookup;
	private final SetProperty<BuildVariantInternal> buildVariants;

	@Inject
	public DefaultNativeTestSuiteComponent(ComponentIdentifier identifier, ObjectFactory objects, ProviderFactory providers, TaskContainer tasks, DomainObjectEventPublisher eventPublisher, TaskRegistry taskRegistry, TaskViewFactory taskViewFactory, ModelLookup modelLookup) {
		super(identifier, DefaultNativeTestSuiteVariant.class, objects, tasks, eventPublisher, taskRegistry, taskViewFactory);
		this.objects = objects;
		this.providers = providers;
		this.tasks = tasks;
		this.modelLookup = modelLookup;
		this.buildVariants = objects.setProperty(BuildVariantInternal.class);

		this.testedComponent = Cast.uncheckedCast(objects.property(BaseComponent.class));

		getDimensions().addAll(providers.provider(() -> {
			if (getTestedComponent().isPresent()) {
				return getTestedComponent().get().getDimensions().get().stream().map(it -> (CoordinateSet<?>) it).map((CoordinateSet<?> set) -> {
					if (set.getAxis().equals(BINARY_LINKAGE_COORDINATE_AXIS)) {
						return CoordinateSet.of(Coordinates.of(TargetLinkages.EXECUTABLE));
					}
					return set;
				}).collect(toList());
			}
			throw new UnsupportedOperationException();
		}));
		this.getBaseName().convention(BaseNameUtils.from(identifier).getAsString());

		this.taskRegistry = taskRegistry;

		this.getBuildVariants().convention(getFinalSpace().map(DefaultBuildVariant::fromSpace));
		this.getBuildVariants().finalizeValueOnRead();
		this.getBuildVariants().disallowChanges(); // Let's disallow changing them for now.
	}

	@Override
	public NativeComponentDependencies getDependencies() {
		return ModelProperties.getProperty(this, "dependencies").as(NativeComponentDependencies.class).get();
	}

	@Override
	public void dependencies(Action<? super NativeComponentDependencies> action) {
		action.execute(getDependencies());
	}

	@Override
	public void dependencies(@SuppressWarnings("rawtypes") Closure closure) {
		dependencies(ConfigureUtil.configureUsing(closure));
	}

	@Override
	public SetProperty<BuildVariantInternal> getBuildVariants() {
		return buildVariants;
	}

	@Override
	public Property<DefaultNativeTestSuiteVariant> getDevelopmentVariant() {
		return ModelProperties.getProperty(this, "developmentVariant").as(Property.class).get();
	}

	@Override
	public BinaryView<Binary> getBinaries() {
		return ModelProperties.getProperty(this, "binaries").as(BinaryView.class).get();
	}

	@Override
	public VariantView<DefaultNativeTestSuiteVariant> getVariants() {
		return ModelProperties.getProperty(this, "variants").as(VariantView.class).get();
	}

	@Override
	public VariantCollection<DefaultNativeTestSuiteVariant> getVariantCollection() {
		throw new UnsupportedOperationException("Use 'variants' property instead.");
	}

	@Override
	public TestSuiteComponent testedComponent(Object component) {
		if (component instanceof BaseComponent) {
			getTestedComponent().set((BaseComponent) component);
		} else if (ModelNodeUtils.canBeViewedAs(ModelNodes.of(component), of(BaseComponent.class))) {
			getTestedComponent().set(ModelNodeUtils.get(ModelNodes.of(component), BaseComponent.class));
		} else {
			throw new UnsupportedOperationException();
		}
		return this;
	}

	public void finalizeExtension(Project project) {
		whenElementKnown(this, ModelActionWithInputs.of(ModelComponentReference.of(VariantIdentifier.class), ModelComponentReference.ofProjection(DefaultNativeTestSuiteVariant.class).asKnownObject(), (entity, variantIdentifier, knownVariant) -> {
			createBinaries(knownVariant);
		}));
		whenElementKnown(this, ModelActionWithInputs.of(ModelComponentReference.of(VariantIdentifier.class), ModelComponentReference.ofProjection(DefaultNativeTestSuiteVariant.class).asKnownObject(), (entity, variantIdentifier, knownVariant) -> {
			new CreateVariantObjectsLifecycleTaskRule(taskRegistry).accept(knownVariant);
		}));
		new CreateVariantAwareComponentObjectsLifecycleTaskRule(taskRegistry).execute(this);
		whenElementKnown(this, ModelActionWithInputs.of(ModelComponentReference.of(VariantIdentifier.class), ModelComponentReference.ofProjection(DefaultNativeTestSuiteVariant.class).asKnownObject(), (entity, variantIdentifier, knownVariant) -> {
			new CreateVariantAssembleLifecycleTaskRule(taskRegistry).accept(knownVariant);
		}));

		// HACK: This should really be solve using the variant whenElementKnown API
		getBuildVariants().get().forEach(buildVariant -> {
			val variantIdentifier = VariantIdentifier.builder().withType(DefaultNativeTestSuiteVariant.class).withComponentIdentifier(getIdentifier()).withBuildVariant(buildVariant).build();

			// TODO: The variant should have give access to the testTask
			val runTask = taskRegistry.register(TaskIdentifier.of(TaskName.of("run"), RunTestExecutable.class, variantIdentifier), task -> {
				// TODO: Use a provider of the variant here
				task.dependsOn((Callable) () -> getVariants().filter(it -> it.getBuildVariant().equals(buildVariant)).flatMap(it -> it.get(0).getDevelopmentBinary()));
				task.setOutputDir(task.getTemporaryDir());
				task.commandLine(new Object() {
					@Override
					public String toString() {
						val binary = (ExecutableBinaryInternal) getVariants().get().stream().filter(it -> it.getBuildVariant().equals(buildVariant)).findFirst().get().getDevelopmentBinary().get();
						return binary.getLinkTask().flatMap(LinkExecutable::getLinkedFile).get().getAsFile().getAbsolutePath();
					}
				});
			});
			// TODO: The following is a gap is how we declare task, it should be possible to register a lifecycle task for a entity
			val testTask = taskRegistry.register(TaskIdentifier.ofLifecycle(variantIdentifier), task -> {
				task.dependsOn(runTask);
			});
		});

		// Ensure the task is registered before configuring
		taskRegistry.registerIfAbsent("check").configure(task -> {
			// TODO: To eliminate access to the TaskContainer, we should have a getter on the variant for the relevant task in question
			task.dependsOn(getDevelopmentVariant().flatMap(it -> tasks.named(TaskIdentifier.ofLifecycle(it.getIdentifier()).getTaskName())));
		});


		getTestedComponent().disallowChanges();
		if (getTestedComponent().isPresent()) {
			val component = getTestedComponent().get();

			// TODO: Map name to something close to what is expected
			getBaseName().convention(component.getBaseName().map(it -> {
				// if the tested component has a SwiftSourceSet
				if (!modelLookup.anyMatch(ModelSpecs.of(descendantOf(ModelNodeUtils.getPath(component.getNode())).and(withType(of(SwiftSourceSet.class)))))) {
					return it + "-" + getIdentifier().getName().get();
				}
				return it + StringUtils.capitalize(getIdentifier().getName().get());
			}));

			val registry = project.getExtensions().getByType(ModelRegistry.class);
			whenElementKnown(component, ModelActionWithInputs.of(ModelComponentReference.of(ModelState.IsAtLeastCreated.class), ModelComponentReference.of(IsLanguageSourceSet.class), ModelComponentReference.ofProjection(LanguageSourceSet.class), (entity, a, b, c) -> {
				// TODO: should have a way to report the public type of the "main" projection
				//   The known and provider should use the public type of the projection... instead of the "assumed type"
				//   BUT should it... seems a bit hacky... check what Software Model did.
				val sourceSetType = ModelNodeUtils.get(entity, LanguageSourceSet.class).getClass();
				// If source set don't already exists on test suite
				if (!modelLookup.anyMatch(ModelSpecs.of(descendantOf(ModelNodeUtils.getPath(getNode())).and(withType(of(sourceSetType)))))) {
					// HACK: SourceSet in this world are quite messed up, the refactor around the source management that will be coming soon don't have this problem.
					val identifier = getNode().getComponent(ComponentIdentifier.class);
					if (NativeHeaderSet.class.isAssignableFrom(sourceSetType)) {
						// NOTE: Ensure we are using the "headers" name as the tested component may also contains "public"
						registry.register(project.getExtensions().getByType(LanguageSourceSetRegistrationFactory.class).create(LanguageSourceSetIdentifier.of(identifier, "headers"), sourceSetType).build());
					} else {
						registry.register(project.getExtensions().getByType(LanguageSourceSetRegistrationFactory.class).create(LanguageSourceSetIdentifier.of(identifier, entity.getComponent(LanguageSourceSetIdentifier.class).getName().toString()), sourceSetType).build());
					}
				}
			}));
			if (component instanceof BaseNativeComponent) {
				val testedComponentDependencies = ((BaseNativeComponent<?>) component).getDependencies();
				getDependencies().getImplementation().getAsConfiguration().extendsFrom(testedComponentDependencies.getImplementation().getAsConfiguration());
				getDependencies().getLinkOnly().getAsConfiguration().extendsFrom(testedComponentDependencies.getLinkOnly().getAsConfiguration());
				getDependencies().getRuntimeOnly().getAsConfiguration().extendsFrom(testedComponentDependencies.getRuntimeOnly().getAsConfiguration());
			}
			getVariants().configureEach(variant -> {
				variant.getBinaries().configureEach(ExecutableBinaryInternal.class, binary -> {
					Provider<List<? extends FileTree>> componentObjects = component.getVariants().filter(it -> ((BuildVariantInternal)it.getBuildVariant()).withoutDimension(BINARY_LINKAGE_COORDINATE_AXIS).equals(variant.getBuildVariant().withoutDimension(BINARY_LINKAGE_COORDINATE_AXIS))).map(it -> {
						ImmutableList.Builder<FileTree> result = ImmutableList.builder();
						it.stream().flatMap(v -> v.getBinaries().withType(NativeBinary.class).get().stream()).forEach(testedBinary -> {
							result.addAll(testedBinary.getCompileTasks().withType(NativeSourceCompileTask.class).getElements().map(t -> {
								return t.stream().map(a -> {
									return ((AbstractNativeSourceCompileTask) a).getObjectFileDir().getAsFileTree().matching(pattern -> pattern.include("**/*.o", "**/*.obj"));
								}).collect(toList());
							}).get());

							result.addAll(testedBinary.getCompileTasks().withType(SwiftCompileTask.class).getElements().map(t -> {
								return t.stream().map(a -> {
									return ((SwiftCompileTask) a).getObjectFileDir().getAsFileTree().matching(pattern -> pattern.include("**/*.o", "**/*.obj"));
								}).collect(toList());
							}).get());
						});
						return result.build();
					});
//					Provider<List<? extends FileTree>> componentObjects = component.getBinaries().withType(NativeBinary.class).flatMap(it -> {
//						ImmutableList.Builder<FileTree> result = ImmutableList.builder();
//						result.addAll(it.getCompileTasks().withType(NativeSourceCompileTask.class).getElements().map(t -> {
//							return t.stream().map(a -> {
//								return ((AbstractNativeSourceCompileTask) a).getObjectFileDir().getAsFileTree().matching(UTTypeUtils.onlyIf(UTTypeObjectCode.INSTANCE));
//							}).collect(Collectors.toList());
//						}).get());
//
//						result.addAll(it.getCompileTasks().withType(SwiftCompileTask.class).getElements().map(t -> {
//							return t.stream().map(a -> {
//								return ((SwiftCompileTask) a).getObjectFileDir().getAsFileTree().matching(UTTypeUtils.onlyIf(UTTypeObjectCode.INSTANCE));
//							}).collect(Collectors.toList());
//						}).get());
//
//						return result.build();
//					});

					ConfigurableFileCollection objects = this.objects.fileCollection();
					objects.from(componentObjects);
					if (component instanceof DefaultNativeApplicationComponent) {
						val relocateTask = taskRegistry.register(TaskIdentifier.of(TaskName.of("relocateMainSymbolFor"), UnexportMainSymbol.class, variant.getIdentifier()), task -> {
							task.getObjects().from(componentObjects);
							task.getOutputDirectory().set(project.getLayout().getBuildDirectory().dir(binary.getIdentifier().getOutputDirectoryBase("objs/for-test")));
						});
						objects.setFrom(relocateTask.map(UnexportMainSymbol::getRelocatedObjects));
					}
					binary.getLinkTask().configure(task -> {
						val taskInternal = (LinkExecutableTask) task;
						taskInternal.source(objects);
					});
				});
			});

			getBinaries().configureEach(ExecutableBinary.class, binary -> {
				binary.getCompileTasks().configureEach(SwiftCompileTask.class, task -> {
					task.getModules().from(component.getDevelopmentVariant().map(it -> it.getBinaries().withType(NativeBinary.class).getElements().get().stream().flatMap(b -> b.getCompileTasks().withType(SwiftCompileTask.class).get().stream()).map(SwiftCompile::getModuleFile).collect(toList())));
				});
				binary.getCompileTasks().configureEach(NativeSourceCompileTask.class, task -> {
					((AbstractNativeSourceCompileTask)task).getIncludes().from(sourceViewOf(component).filter(instanceOf(NativeHeaderSet.class)::test).map(transformEach(LanguageSourceSet::getSourceDirectories)));
				});
			});
		}
	}

	private static void whenElementKnown(Object target, ModelAction action) {
		applyTo(ModelNodes.of(target), allDirectDescendants(stateAtLeast(ModelState.Created)).apply(once(action)));
	}
}
