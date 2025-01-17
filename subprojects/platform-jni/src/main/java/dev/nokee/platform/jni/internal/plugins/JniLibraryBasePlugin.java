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
package dev.nokee.platform.jni.internal.plugins;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import dev.nokee.language.base.internal.plugins.LanguageBasePlugin;
import dev.nokee.language.c.internal.plugins.SupportCSourceSetTag;
import dev.nokee.language.cpp.internal.plugins.SupportCppSourceSetTag;
import dev.nokee.language.jvm.GroovySourceSet;
import dev.nokee.language.jvm.JavaSourceSet;
import dev.nokee.language.jvm.KotlinSourceSet;
import dev.nokee.language.jvm.internal.CompileTaskComponent;
import dev.nokee.language.jvm.internal.GroovyLanguageSourceSetComponent;
import dev.nokee.language.jvm.internal.GroovySourceSetSpec;
import dev.nokee.language.jvm.internal.JavaLanguageSourceSetComponent;
import dev.nokee.language.jvm.internal.JavaSourceSetSpec;
import dev.nokee.language.jvm.internal.JvmSourceSetTag;
import dev.nokee.language.jvm.internal.KotlinLanguageSourceSetComponent;
import dev.nokee.language.jvm.internal.KotlinSourceSetSpec;
import dev.nokee.language.jvm.internal.SourceSetComponent;
import dev.nokee.language.jvm.internal.plugins.JvmLanguageBasePlugin;
import dev.nokee.language.nativebase.HasObjectFiles;
import dev.nokee.language.nativebase.internal.HasConfigurableHeadersPropertyComponent;
import dev.nokee.language.nativebase.internal.NativeLanguagePlugin;
import dev.nokee.language.nativebase.internal.NativePlatformFactory;
import dev.nokee.language.nativebase.internal.ToolChainSelectorInternal;
import dev.nokee.language.nativebase.internal.toolchains.NokeeStandardToolChainsPlugin;
import dev.nokee.language.nativebase.tasks.internal.NativeSourceCompileTask;
import dev.nokee.language.objectivec.internal.plugins.SupportObjectiveCSourceSetTag;
import dev.nokee.language.objectivecpp.internal.plugins.SupportObjectiveCppSourceSetTag;
import dev.nokee.model.internal.ModelElementFactory;
import dev.nokee.model.internal.core.GradlePropertyComponent;
import dev.nokee.model.internal.core.IdentifierComponent;
import dev.nokee.model.internal.core.ModelActionWithInputs;
import dev.nokee.model.internal.core.ModelComponentReference;
import dev.nokee.model.internal.core.ModelElements;
import dev.nokee.model.internal.core.ModelNode;
import dev.nokee.model.internal.core.ModelNodeUtils;
import dev.nokee.model.internal.core.ModelNodes;
import dev.nokee.model.internal.core.ModelProperties;
import dev.nokee.model.internal.core.ModelProperty;
import dev.nokee.model.internal.core.ModelPropertyRegistrationFactory;
import dev.nokee.model.internal.core.ParentComponent;
import dev.nokee.model.internal.core.ParentUtils;
import dev.nokee.model.internal.names.ElementNameComponent;
import dev.nokee.model.internal.names.ExcludeFromQualifyingNameTag;
import dev.nokee.model.internal.registry.ModelConfigurer;
import dev.nokee.model.internal.registry.ModelRegistry;
import dev.nokee.model.internal.state.ModelState;
import dev.nokee.model.internal.state.ModelStates;
import dev.nokee.model.internal.tags.ModelTags;
import dev.nokee.platform.base.Binary;
import dev.nokee.platform.base.BuildVariant;
import dev.nokee.platform.base.DependencyAwareComponent;
import dev.nokee.platform.base.VariantView;
import dev.nokee.platform.base.internal.BinaryIdentifier;
import dev.nokee.platform.base.internal.BinaryIdentity;
import dev.nokee.platform.base.internal.BuildVariantComponent;
import dev.nokee.platform.base.internal.BuildVariantInternal;
import dev.nokee.platform.base.internal.IsBinary;
import dev.nokee.platform.base.internal.IsVariant;
import dev.nokee.platform.base.internal.VariantIdentifier;
import dev.nokee.platform.base.internal.Variants;
import dev.nokee.platform.base.internal.assembletask.AssembleTaskComponent;
import dev.nokee.platform.base.internal.dependencies.ConsumableDependencyBucketSpec;
import dev.nokee.platform.base.internal.dependencies.DeclarableDependencyBucketSpec;
import dev.nokee.platform.base.internal.dependencies.DependencyBuckets;
import dev.nokee.platform.base.internal.dependencies.ExtendsFromParentConfigurationAction;
import dev.nokee.platform.base.internal.dependencybuckets.CompileOnlyConfigurationComponent;
import dev.nokee.platform.base.internal.dependencybuckets.ImplementationConfigurationComponent;
import dev.nokee.platform.base.internal.dependencybuckets.LinkOnlyConfigurationComponent;
import dev.nokee.platform.base.internal.dependencybuckets.RuntimeOnlyConfigurationComponent;
import dev.nokee.platform.base.internal.developmentbinary.DevelopmentBinaryPropertyComponent;
import dev.nokee.platform.base.internal.developmentvariant.DevelopmentVariantPropertyComponent;
import dev.nokee.platform.base.internal.plugins.OnDiscover;
import dev.nokee.platform.base.internal.util.PropertyUtils;
import dev.nokee.platform.jni.JavaNativeInterfaceLibrary;
import dev.nokee.platform.jni.JniJarBinary;
import dev.nokee.platform.jni.JniLibrary;
import dev.nokee.platform.jni.JvmJarBinary;
import dev.nokee.platform.jni.internal.ApiElementsConfiguration;
import dev.nokee.platform.jni.internal.ConfigureJniHeaderDirectoryOnJavaCompileAction;
import dev.nokee.platform.jni.internal.GeneratedJniHeadersComponent;
import dev.nokee.platform.jni.internal.JarTaskComponent;
import dev.nokee.platform.jni.internal.JavaNativeInterfaceLibraryComponentRegistrationFactory;
import dev.nokee.platform.jni.internal.JavaNativeInterfaceLibraryVariantRegistrationFactory;
import dev.nokee.platform.jni.internal.JniJarArtifactComponent;
import dev.nokee.platform.jni.internal.JniJarArtifactTag;
import dev.nokee.platform.jni.internal.JniJarBinaryRegistrationFactory;
import dev.nokee.platform.jni.internal.JniLibraryComponentInternal;
import dev.nokee.platform.jni.internal.JniLibraryComponentTag;
import dev.nokee.platform.jni.internal.JniLibraryInternal;
import dev.nokee.platform.jni.internal.JniLibraryVariantTag;
import dev.nokee.platform.jni.internal.JvmJarArtifactComponent;
import dev.nokee.platform.jni.internal.JvmJarBinaryRegistrationFactory;
import dev.nokee.platform.jni.internal.ModelBackedJniJarBinary;
import dev.nokee.platform.jni.internal.ModelBackedJvmJarBinary;
import dev.nokee.platform.jni.internal.MultiVariantTag;
import dev.nokee.platform.jni.internal.RuntimeElementsConfiguration;
import dev.nokee.platform.jni.internal.actions.OnceAction;
import dev.nokee.platform.jni.internal.actions.WhenPlugin;
import dev.nokee.platform.nativebase.SharedLibraryBinary;
import dev.nokee.platform.nativebase.internal.DependentRuntimeLibraries;
import dev.nokee.platform.nativebase.internal.SharedLibraryBinaryInternal;
import dev.nokee.platform.nativebase.internal.TargetLinkagesPropertyComponent;
import dev.nokee.platform.nativebase.internal.dependencies.FrameworkAwareDependencyBucketTag;
import dev.nokee.platform.nativebase.internal.plugins.NativeComponentBasePlugin;
import dev.nokee.platform.nativebase.internal.rules.BuildableDevelopmentVariantConvention;
import dev.nokee.platform.nativebase.internal.services.UnbuildableWarningService;
import dev.nokee.platform.nativebase.tasks.LinkSharedLibrary;
import dev.nokee.runtime.nativebase.TargetLinkage;
import dev.nokee.runtime.nativebase.internal.TargetLinkages;
import dev.nokee.runtime.nativebase.internal.TargetMachines;
import dev.nokee.utils.ProviderUtils;
import lombok.val;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.PublishArtifact;
import org.gradle.api.attributes.LibraryElements;
import org.gradle.api.attributes.Usage;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.internal.artifacts.dsl.LazyPublishArtifact;
import org.gradle.api.logging.Logger;
import org.gradle.api.plugins.AppliedPlugin;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.SetProperty;
import org.gradle.api.tasks.bundling.Jar;
import org.gradle.language.nativeplatform.tasks.AbstractNativeCompileTask;
import org.gradle.language.swift.tasks.SwiftCompile;
import org.gradle.nativeplatform.platform.NativePlatform;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static dev.nokee.language.nativebase.internal.NativePlatformFactory.platformNameFor;
import static dev.nokee.model.internal.actions.ModelAction.configure;
import static dev.nokee.model.internal.actions.ModelAction.configureEach;
import static dev.nokee.model.internal.actions.ModelAction.configureMatching;
import static dev.nokee.model.internal.actions.ModelSpec.descendantOf;
import static dev.nokee.model.internal.actions.ModelSpec.ownedBy;
import static dev.nokee.model.internal.actions.ModelSpec.subtypeOf;
import static dev.nokee.model.internal.core.ModelRegistration.builder;
import static dev.nokee.model.internal.state.ModelStates.realize;
import static dev.nokee.model.internal.tags.ModelTags.tag;
import static dev.nokee.model.internal.type.GradlePropertyTypes.property;
import static dev.nokee.model.internal.type.ModelType.of;
import static dev.nokee.platform.base.internal.DomainObjectEntities.newEntity;
import static dev.nokee.platform.base.internal.util.PropertyUtils.from;
import static dev.nokee.platform.base.internal.util.PropertyUtils.set;
import static dev.nokee.platform.base.internal.util.PropertyUtils.wrap;
import static dev.nokee.platform.jni.internal.actions.WhenPlugin.any;
import static dev.nokee.platform.jni.internal.plugins.JvmIncludeRoots.jvmIncludes;
import static dev.nokee.platform.jni.internal.plugins.NativeCompileTaskProperties.includeRoots;
import static dev.nokee.runtime.nativebase.TargetMachine.TARGET_MACHINE_COORDINATE_AXIS;
import static dev.nokee.utils.ConfigurationUtils.configureAttributes;
import static dev.nokee.utils.ConfigurationUtils.configureExtendsFrom;
import static dev.nokee.utils.TaskUtils.configureBuildGroup;
import static dev.nokee.utils.TaskUtils.configureDependsOn;
import static dev.nokee.utils.TaskUtils.configureDescription;
import static dev.nokee.utils.TransformerUtils.transformEach;
import static java.util.Collections.emptyList;

public class JniLibraryBasePlugin implements Plugin<Project> {
	@Override
	@SuppressWarnings({"unchecked", "rawtypes"})
	public void apply(Project project) {
		project.getPluginManager().apply("lifecycle-base");
		project.getPluginManager().apply(LanguageBasePlugin.class);
		project.getPluginManager().apply(NativeComponentBasePlugin.class);
		project.getPluginManager().apply(JvmLanguageBasePlugin.class);
		project.getPluginManager().apply(NokeeStandardToolChainsPlugin.class);

		project.getExtensions().add("__nokee_jniJarBinaryFactory", new JniJarBinaryRegistrationFactory());
		project.getExtensions().add("__nokee_jvmJarBinaryFactory", new JvmJarBinaryRegistrationFactory());
		project.getExtensions().add("__nokee_jniLibraryComponentFactory", new JavaNativeInterfaceLibraryComponentRegistrationFactory());
		project.getExtensions().add("__nokee_jniLibraryVariantFactory", new JavaNativeInterfaceLibraryVariantRegistrationFactory());

		// Component rules
		project.getExtensions().getByType(ModelConfigurer.class).configure(new OnDiscover(ModelActionWithInputs.of(ModelComponentReference.of(IdentifierComponent.class), ModelComponentReference.ofProjection(JniLibraryComponentInternal.class), (entity, identifier, tag) -> {
			val registry = project.getExtensions().getByType(ModelRegistry.class);

			val api = registry.register(newEntity("api", DeclarableDependencyBucketSpec.class, it -> it.ownedBy(entity)));
			val implementation = registry.register(newEntity("jvmImplementation", DeclarableDependencyBucketSpec.class, it -> it.ownedBy(entity)));
			val runtimeOnly = registry.register(newEntity("jvmRuntimeOnly", DeclarableDependencyBucketSpec.class, it -> it.ownedBy(entity)));
			project.getPlugins().withType(NativeLanguagePlugin.class, new Action<NativeLanguagePlugin>() {
				private boolean alreadyExecuted = false;

				@Override
				public void execute(NativeLanguagePlugin appliedPlugin) {
					if (!alreadyExecuted) {
						alreadyExecuted = true;
						val nativeCompileOnly = registry.register(newEntity("nativeCompileOnly", DeclarableDependencyBucketSpec.class, it -> it.ownedBy(entity).withTag(FrameworkAwareDependencyBucketTag.class)));
						entity.addComponent(new CompileOnlyConfigurationComponent(ModelNodes.of(nativeCompileOnly)));
					}
				}
			});
			val nativeImplementation = registry.register(newEntity("nativeImplementation", DeclarableDependencyBucketSpec.class, it -> it.ownedBy(entity).withTag(FrameworkAwareDependencyBucketTag.class)));
			val nativeLinkOnly = registry.register(newEntity("nativeLinkOnly", DeclarableDependencyBucketSpec.class, it -> it.ownedBy(entity).withTag(FrameworkAwareDependencyBucketTag.class)));
			val nativeRuntimeOnly = registry.register(newEntity("nativeRuntimeOnly", DeclarableDependencyBucketSpec.class, it -> it.ownedBy(entity).withTag(FrameworkAwareDependencyBucketTag.class)));
			implementation.configure(Configuration.class, configureExtendsFrom(api.as(Configuration.class)));

			entity.addComponent(new ImplementationConfigurationComponent(ModelNodes.of(nativeImplementation)));
			entity.addComponent(new RuntimeOnlyConfigurationComponent(ModelNodes.of(nativeRuntimeOnly)));
			entity.addComponent(new LinkOnlyConfigurationComponent(ModelNodes.of(nativeLinkOnly)));

			val apiElements = registry.register(newEntity("apiElements", ConsumableDependencyBucketSpec.class, it -> it.ownedBy(entity)));
			apiElements.configure(Configuration.class, configureAttributes(builder -> builder.usage(project.getObjects().named(Usage.class, Usage.JAVA_API)).attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, LibraryElements.JAR))));
			apiElements.configure(Configuration.class, configureExtendsFrom(api.as(Configuration.class)));
			entity.addComponent(new ApiElementsConfiguration(ModelNodes.of(apiElements)));
			val runtimeElements = registry.register(newEntity("runtimeElements", ConsumableDependencyBucketSpec.class, it -> it.ownedBy(entity)));
			runtimeElements.configure(Configuration.class, configureAttributes(builder -> builder.usage(project.getObjects().named(Usage.class, Usage.JAVA_RUNTIME)).attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.getObjects().named(LibraryElements.class, LibraryElements.JAR))));
			runtimeElements.configure(Configuration.class, configureExtendsFrom(api.as(Configuration.class)));
			entity.addComponent(new RuntimeElementsConfiguration(ModelNodes.of(runtimeElements)));

			// TODO: This is an external dependency meaning we should go through the component dependencies.
			//  We can either add an file dependency or use the, yet-to-be-implemented, shim to consume system libraries
			//  We aren't using a language source set as the files will be included inside the IDE projects which is not what we want.
			registry.instantiate(configureEach(descendantOf(entity.getId()), NativeSourceCompileTask.class, includeRoots(from(jvmIncludes()))));

			project.getPluginManager().withPlugin("groovy", ignored -> {
				val sourceSet = registry.register(newEntity("groovy", GroovySourceSetSpec.class, it -> it.ownedBy(entity)));
				entity.addComponent(new GroovyLanguageSourceSetComponent(ModelNodes.of(sourceSet)));
			});
			project.getPluginManager().withPlugin("java", ignored -> {
				val sourceSet = registry.register(newEntity("java", JavaSourceSetSpec.class, it -> it.ownedBy(entity)));

				sourceSet.as(JavaSourceSet.class).configure(it -> {
					it.getCompileTask().configure(new ConfigureJniHeaderDirectoryOnJavaCompileAction(identifier.get(), project.getLayout()));
				});

				entity.addComponent(new GeneratedJniHeadersComponent(project.getObjects().fileCollection().from((Callable<?>) () -> {
					return sourceSet.as(JavaSourceSet.class).flatMap(ss -> ss.getCompileTask().flatMap(it -> it.getOptions().getHeaderOutputDirectory()));
				})));
				entity.addComponent(new JavaLanguageSourceSetComponent(ModelNodes.of(sourceSet)));
			});
			project.getPluginManager().withPlugin("org.jetbrains.kotlin.jvm", ignored -> {
				val sourceSet = registry.register(newEntity("kotlin", KotlinSourceSetSpec.class, it -> it.ownedBy(entity)));
				entity.addComponent(new KotlinLanguageSourceSetComponent(ModelNodes.of(sourceSet)));
			});

			project.getPluginManager().withPlugin("java", appliedPlugin -> {
				registry.register(newEntity("implementation", DeclarableDependencyBucketSpec.class, it -> it.ownedBy(entity)))
					.configure(Configuration.class, configureExtendsFrom(implementation.as(Configuration.class)));
				registry.register(newEntity("runtimeOnly", DeclarableDependencyBucketSpec.class, it -> it.ownedBy(entity)))
					.configure(Configuration.class, configureExtendsFrom(runtimeOnly.as(Configuration.class)));
			});
		})));
		// TODO: When discovery will be a real feature, we shouldn't need this anymore
		project.getExtensions().getByType(ModelConfigurer.class).configure(new OnDiscover(ModelActionWithInputs.of(ModelComponentReference.ofProjection(JniLibraryComponentInternal.class), (entity, ignored) -> {
			project.getPluginManager().withPlugin("dev.nokee.c-language", __ -> entity.addComponent(tag(SupportCSourceSetTag.class)));
			project.getPluginManager().withPlugin("dev.nokee.cpp-language", __ -> entity.addComponent(tag(SupportCppSourceSetTag.class)));
			project.getPluginManager().withPlugin("dev.nokee.objective-c-language", __ -> entity.addComponent(tag(SupportObjectiveCSourceSetTag.class)));
			project.getPluginManager().withPlugin("dev.nokee.objective-cpp-language", __ -> entity.addComponent(tag(SupportObjectiveCppSourceSetTag.class)));
		})));
		// ComponentFromEntity<GradlePropertyComponent> read-write on DevelopmentVariantPropertyComponent
		project.getExtensions().getByType(ModelConfigurer.class).configure(ModelActionWithInputs.of(ModelComponentReference.of(DevelopmentVariantPropertyComponent.class), ModelTags.referenceOf(JniLibraryComponentTag.class), (entity, developmentVariant, ignored1) -> {
			((Property<JniLibrary>) developmentVariant.get().get(GradlePropertyComponent.class).get())
				.convention((Provider<? extends JniLibrary>) project.provider(new BuildableDevelopmentVariantConvention(() -> ModelElements.of(entity).property("variants").as(of(VariantView.class)).as(VariantView.class).flatMap(VariantView::getElements).get())));
		}));
		project.getExtensions().getByType(ModelConfigurer.class).configure(ModelActionWithInputs.of(ModelTags.referenceOf(JvmSourceSetTag.class), ModelComponentReference.of(SourceSetComponent.class), ModelComponentReference.of(CompileTaskComponent.class), (entity, ignored1, sourceSet, compileTask) -> {
			sourceSet.get().configure(it -> {
				project.getExtensions().getByType(ModelRegistry.class).instantiate(configure(compileTask.get().getId(), Task.class, configureDependsOn((Callable<?>) () -> DependencyBuckets.finalize(project.getConfigurations().getByName(it.getCompileClasspathConfigurationName())))));
			});
		}));
		project.getExtensions().getByType(ModelConfigurer.class).configure(ModelActionWithInputs.of(ModelComponentReference.of(JvmJarArtifactComponent.class), ModelComponentReference.of(ApiElementsConfiguration.class), (entity, jvmJar, apiElements) -> {
			val registry = project.getExtensions().getByType(ModelRegistry.class);
			registry.instantiate(configure(apiElements.get().getId(), Configuration.class, configuration -> {
				configuration.getOutgoing().artifact(jvmJar.getJarTask());
			}));
		}));
		project.getExtensions().getByType(ModelConfigurer.class).configure(ModelActionWithInputs.of(ModelComponentReference.of(JvmJarArtifactComponent.class), ModelComponentReference.of(RuntimeElementsConfiguration.class), (entity, jvmJar, runtimeElements) -> {
			val registry = project.getExtensions().getByType(ModelRegistry.class);
			registry.instantiate(configure(runtimeElements.get().getId(), Configuration.class, configuration -> {
				configuration.getOutgoing().artifact(jvmJar.getJarTask());
			}));
		}));
		project.getExtensions().getByType(ModelConfigurer.class).configure(ModelActionWithInputs.of(ModelComponentReference.of(AssembleTaskComponent.class), ModelComponentReference.ofProjection(JavaNativeInterfaceLibrary.class), (entity, assembleTask, projection) -> {
			val component = project.provider(() -> ModelNodeUtils.get(entity, of(JavaNativeInterfaceLibrary.class)));
			Provider<List<JniLibrary>> allBuildableVariants = component.flatMap(it -> it.getVariants().filter(v -> v.getSharedLibrary().isBuildable()));
			project.getExtensions().getByType(ModelRegistry.class).instantiate(configure(assembleTask.get().getId(), Task.class, configureDependsOn(component.flatMap(JavaNativeInterfaceLibrary::getDevelopmentVariant).map(JniLibrary::getJavaNativeInterfaceJar).map(Collections::singletonList).orElse(Collections.emptyList()))));
			project.getExtensions().getByType(ModelRegistry.class).instantiate(configure(assembleTask.get().getId(), Task.class, task -> {
				task.dependsOn((Callable<Object>) () -> {
					val buildVariants = component.get().getBuildVariants().get();
					val firstBuildVariant = Iterables.getFirst(buildVariants, null);
					if (buildVariants.size() == 1 && allBuildableVariants.get().isEmpty() && firstBuildVariant.hasAxisOf(TargetMachines.host().getOperatingSystemFamily())) {
						throw new RuntimeException(String.format("No tool chain is available to build for platform '%s'", platformNameFor(((BuildVariantInternal) firstBuildVariant).getAxisValue(TARGET_MACHINE_COORDINATE_AXIS))));
					}
					return ImmutableList.of();
				});
			}));
		}));
		project.getExtensions().getByType(ModelConfigurer.class).configure(ModelActionWithInputs.of(ModelComponentReference.of(RuntimeElementsConfiguration.class), ModelComponentReference.ofProjection(JavaNativeInterfaceLibrary.class), (entity, runtimeElements, projection) -> {
			val component = project.provider(() -> projection.get(of(JavaNativeInterfaceLibrary.class)));
			val toolChainSelector = project.getObjects().newInstance(ToolChainSelectorInternal.class);
			val values = project.getObjects().listProperty(PublishArtifact.class);
			Provider<List<JniLibrary>> allBuildableVariants = component.flatMap(it -> it.getVariants().filter(v -> toolChainSelector.canBuild(v.getTargetMachine())));
			Provider<Iterable<JniJarBinary>> allJniJars = allBuildableVariants.map(transformEach(v -> v.getJavaNativeInterfaceJar()));
			val allArtifacts = project.getObjects().listProperty(PublishArtifact.class);
			allArtifacts.set(allJniJars.flatMap(binaries -> {
				val result = project.getObjects().listProperty(PublishArtifact.class);
				for (JniJarBinary binary : binaries) {
					result.add(new LazyPublishArtifact(binary.getJarTask()));
				}
				return result;
			}));
			allArtifacts.finalizeValueOnRead();
			values.addAll(allArtifacts);
			runtimeElements.addAll(values);
		}));
		project.getExtensions().getByType(ModelConfigurer.class).configure(ModelActionWithInputs.of(ModelComponentReference.of(IdentifierComponent.class), ModelComponentReference.of(ElementNameComponent.class), ModelComponentReference.of(ModelState.IsAtLeastFinalized.class), ModelComponentReference.ofProjection(JniLibraryComponentInternal.class), (entity, identifier, elementName, ignored, projection) -> {
			val variantFactory = new JavaNativeInterfaceLibraryVariantRegistrationFactory();
			val component = ModelNodeUtils.get(entity, JniLibraryComponentInternal.class);

			val variants = ImmutableMap.<BuildVariant, ModelNode>builder();
			component.getBuildVariants().get().forEach(buildVariant -> {
				val variantIdentifier = VariantIdentifier.builder().withBuildVariant((BuildVariantInternal) buildVariant).withComponentIdentifier(component.getIdentifier()).build();
				val variant = project.getExtensions().getByType(ModelRegistry.class).register(variantFactory.create(variantIdentifier));

				// See https://github.com/nokeedev/gradle-native/issues/543
				if (component.getBuildVariants().get().size() > 1) {
					variant.configure(JniLibrary.class, it -> {
						it.getJavaNativeInterfaceJar().getJarTask().configure(task -> {
							task.getDestinationDirectory().set(project.getLayout().getBuildDirectory().dir("libs/" + elementName.get()));
						});
					});
				}

				variants.put(buildVariant, ModelNodes.of(variant));
			});
			entity.addComponent(new Variants(variants.build()));
		}));
		project.getExtensions().getByType(ModelConfigurer.class).configure(ModelActionWithInputs.of(ModelComponentReference.of(JvmJarArtifactComponent.class), ModelComponentReference.of(JavaLanguageSourceSetComponent.class), (entity, jvmJar, javaSourceSet) -> {
			project.getExtensions().getByType(ModelRegistry.class).instantiate(configure(jvmJar.get().getId(), JvmJarBinary.class, binary -> {
				binary.getJarTask().configure(configureDependsOn(entity.get(ModelElementFactory.class).createObject(javaSourceSet.get(), of(JavaSourceSet.class)).flatMap(JavaSourceSet::getCompileTask)));
			}));
		}));
		project.getExtensions().getByType(ModelConfigurer.class).configure(ModelActionWithInputs.of(ModelComponentReference.of(JvmJarArtifactComponent.class), ModelComponentReference.of(GroovyLanguageSourceSetComponent.class), (entity, jvmJar, groovySourceSet) -> {
			project.getExtensions().getByType(ModelRegistry.class).instantiate(configure(jvmJar.get().getId(), JvmJarBinary.class, binary -> {
				binary.getJarTask().configure(configureDependsOn(entity.get(ModelElementFactory.class).createObject(groovySourceSet.get(), of(GroovySourceSet.class)).flatMap(GroovySourceSet::getCompileTask)));
			}));
		}));
		project.getExtensions().getByType(ModelConfigurer.class).configure(ModelActionWithInputs.of(ModelComponentReference.of(JvmJarArtifactComponent.class), ModelComponentReference.of(KotlinLanguageSourceSetComponent.class), (entity, jvmJar, kotlinSourceSet) -> {
			project.getExtensions().getByType(ModelRegistry.class).instantiate(configure(jvmJar.get().getId(), JvmJarBinary.class, binary -> {
				binary.getJarTask().configure(configureDependsOn(entity.get(ModelElementFactory.class).createObject(kotlinSourceSet.get(), of(KotlinSourceSet.class)).flatMap(KotlinSourceSet::getCompileTask)));
			}));
		}));

		project.getExtensions().getByType(ModelConfigurer.class).configure(new OnDiscover(ModelActionWithInputs.of(ModelComponentReference.ofProjection(ModelBackedJniJarBinary.class), ModelComponentReference.of(IdentifierComponent.class), ModelTags.referenceOf(IsBinary.class), ModelComponentReference.of(ElementNameComponent.class), (entity, projection, identifier, tag, elementName) -> {
			val registry = project.getExtensions().getByType(ModelRegistry.class);
			val jarTask = registry.instantiate(newEntity("jar", Jar.class, it -> it.ownedBy(entity)));
			registry.instantiate(configure(jarTask.getId(), Jar.class, configureBuildGroup()));
			registry.instantiate(configure(jarTask.getId(), Jar.class, task -> {
				task.getDestinationDirectory().convention(task.getProject().getLayout().getBuildDirectory().dir("libs"));
			}));
			registry.instantiate(configure(jarTask.getId(), Jar.class, task -> {
				task.getArchiveBaseName().convention(elementName.get().toString());
			}));
			registry.instantiate(configure(jarTask.getId(), Jar.class, task -> {
				if (task.getDescription() == null) {
					task.setDescription(String.format("Assembles a JAR archive containing the shared library for %s.", identifier.get()));
				}
			}));
			ModelStates.register(jarTask);
			entity.addComponent(new JarTaskComponent(jarTask));
		})));

		project.getExtensions().getByType(ModelConfigurer.class).configure(new OnDiscover(ModelActionWithInputs.of(ModelComponentReference.ofProjection(ModelBackedJvmJarBinary.class), ModelComponentReference.of(IdentifierComponent.class), ModelTags.referenceOf(IsBinary.class), ModelComponentReference.of(ElementNameComponent.class), (entity, projection, identifier, tag, elementName) -> {
			val registry = project.getExtensions().getByType(ModelRegistry.class);
			val jarTask = registry.instantiate(newEntity("jar", Jar.class, it -> it.ownedBy(entity)));
			registry.instantiate(configure(jarTask.getId(), Jar.class, configureBuildGroup()));
			registry.instantiate(configure(jarTask.getId(), Jar.class, task -> {
				task.getDestinationDirectory().convention(task.getProject().getLayout().getBuildDirectory().dir("libs"));
			}));
			registry.instantiate(configure(jarTask.getId(), Jar.class, task -> task.getArchiveBaseName().convention(elementName.get().toString())));
			registry.instantiate(configure(jarTask.getId(), Jar.class, task -> {
				if (task.getDescription() == null) {
					task.setDescription(String.format("Assembles a JAR archive containing the classes for %s.", identifier.get()));
				}
			}));
			ModelStates.register(jarTask);
			entity.addComponent(new JarTaskComponent(jarTask));
		})));


		project.getExtensions().getByType(ModelConfigurer.class).configure(ModelActionWithInputs.of(ModelComponentReference.of(Variants.class), ModelComponentReference.of(JvmJarArtifactComponent.class), (entity, variants, jvmJar) -> {
			if (Iterables.size(variants) == 1) {
				project.getExtensions().getByType(ModelRegistry.class).instantiate(configure(jvmJar.get().getId(), JvmJarBinary.class, binary -> {
					binary.getJarTask().configure(configureDescription("Assembles a JAR archive containing the classes and shared library for %s.", ModelNodes.of(binary).get(IdentifierComponent.class).get()));
				}));
			}
		}));

		val registerJvmJarBinaryAction = new Action<AppliedPlugin>() {
			@Override
			public void execute(AppliedPlugin ignored) {
				project.getExtensions().getByType(ModelConfigurer.class).configure(new OnDiscover(ModelActionWithInputs.of(ModelComponentReference.ofProjection(JniLibraryComponentInternal.class), ModelComponentReference.of(IdentifierComponent.class), (entity, projection, identifier) -> {
					val registry = project.getExtensions().getByType(ModelRegistry.class);
					val binaryIdentifier = BinaryIdentifier.of(identifier.get(), BinaryIdentity.ofMain("jvmJar", "JVM JAR binary"));
					val jvmJar = registry.instantiate(project.getExtensions().getByType(JvmJarBinaryRegistrationFactory.class).create(binaryIdentifier)
						.withComponent(new ParentComponent(entity))
						.withComponent(tag(ExcludeFromQualifyingNameTag.class))
						.build());
					registry.instantiate(configure(jvmJar.getId(), JvmJarBinary.class, binary -> {
						binary.getJarTask().configure(task -> task.getArchiveBaseName().set(project.provider(() -> {
							return ModelProperties.getProperty(entity, "baseName").as(String.class).get();
						})));
					}));
					ModelStates.register(jvmJar);
					entity.addComponent(new JvmJarArtifactComponent(jvmJar));
				})));
			}
		};
		new WhenPlugin(any("java", "groovy", "org.jetbrains.kotlin.jvm"), registerJvmJarBinaryAction).execute(project);

		// Assemble task configuration
		project.getExtensions().getByType(ModelConfigurer.class).configure(ModelActionWithInputs.of(ModelComponentReference.of(JvmJarArtifactComponent.class), ModelComponentReference.of(AssembleTaskComponent.class), (entity, jvmJar, assembleTask) -> {
			val registry = project.getExtensions().getByType(ModelRegistry.class);
			registry.instantiate(configure(assembleTask.get().getId(), Task.class, configureDependsOn((Callable<Object>) () -> ModelNodeUtils.get(jvmJar.get(), JvmJarBinary.class))));
		}));

		project.getExtensions().getByType(ModelConfigurer.class).configure(ModelActionWithInputs.of( ModelComponentReference.of(JniJarArtifactComponent.class), ModelComponentReference.of(AssembleTaskComponent.class), ModelTags.referenceOf(MultiVariantTag.class), (entity, jniJar, assembleTask, tag) -> {
			val registry = project.getExtensions().getByType(ModelRegistry.class);
			registry.instantiate(configure(assembleTask.get().getId(), Task.class, configureDependsOn((Callable<Object>) () -> ModelNodeUtils.get(jniJar.get(), JniJarBinary.class))));
		}));

		new WhenPlugin(any("java", "groovy", "org.jetbrains.kotlin.jvm"), ignored -> {
			// ComponentFromEntity<JvmJarArtifactComponent.class> read-only from ParentComponent
			project.getExtensions().getByType(ModelConfigurer.class).configure(ModelActionWithInputs.of(ModelComponentReference.of(ParentComponent.class), ModelComponentReference.of(JniJarArtifactComponent.class), ModelComponentReference.of(AssembleTaskComponent.class), (entity, parent, jniJar, assembleTask) -> {
				project.getExtensions().getByType(ModelRegistry.class).instantiate(configure(assembleTask.get().getId(), Task.class, configureDependsOn((Callable<Object>) () -> ModelNodeUtils.get(parent.get().get(JvmJarArtifactComponent.class).get(), JvmJarBinary.class))));
			}));
		}).execute(project);

		project.getExtensions().getByType(ModelConfigurer.class).configure(ModelActionWithInputs.of(ModelComponentReference.ofProjection(JavaNativeInterfaceLibrary.class), ModelComponentReference.of(TargetLinkagesPropertyComponent.class), (entity, tag, targetLinkages) -> {
			((SetProperty<TargetLinkage>) targetLinkages.get().get(GradlePropertyComponent.class).get()).convention(Collections.singletonList(TargetLinkages.SHARED));
		}));

		// Variant rules
		// TODO: We should limit to JNILibrary variant
		project.getExtensions().getByType(ModelConfigurer.class).configure(new OnDiscover(ModelActionWithInputs.of(ModelComponentReference.of(IdentifierComponent.class), ModelTags.referenceOf(IsVariant.class), (entity, id, tag) -> {
			val identifier = (VariantIdentifier) id.get();
			val registry = project.getExtensions().getByType(ModelRegistry.class);

			if (!((VariantIdentifier) id.get()).getUnambiguousName().isEmpty()) {
				entity.addComponent(tag(MultiVariantTag.class));
			}

			val implementation = registry.register(newEntity("nativeImplementation", DeclarableDependencyBucketSpec.class, it -> it.ownedBy(entity).withTag(FrameworkAwareDependencyBucketTag.class)));
			val linkOnly = registry.register(newEntity("nativeLinkOnly", DeclarableDependencyBucketSpec.class, it -> it.ownedBy(entity).withTag(FrameworkAwareDependencyBucketTag.class)));
			val runtimeOnly = registry.register(newEntity("nativeRuntimeOnly", DeclarableDependencyBucketSpec.class, it -> it.ownedBy(entity).withTag(FrameworkAwareDependencyBucketTag.class)));

			entity.addComponent(new ImplementationConfigurationComponent(ModelNodes.of(implementation)));
			entity.addComponent(new LinkOnlyConfigurationComponent(ModelNodes.of(linkOnly)));
			entity.addComponent(new RuntimeOnlyConfigurationComponent(ModelNodes.of(runtimeOnly)));

			val resourcePathProperty = registry.register(builder().withComponent(new ElementNameComponent("resourcePath")).withComponent(new ParentComponent(entity)).mergeFrom(project.getExtensions().getByType(ModelPropertyRegistrationFactory.class).createProperty(String.class)).build());
			((ModelProperty<String>) resourcePathProperty).asProperty(property(of(String.class))).convention(identifier.getAmbiguousDimensions().getAsKebabCase().orElse(""));

			val sharedLibrary = registry.register(newEntity("sharedLibrary", SharedLibraryBinaryInternal.class, it -> it.ownedBy(entity)
				.displayName("shared library binary")
				.withComponent(new IdentifierComponent(BinaryIdentifier.of(identifier, BinaryIdentity.ofMain("sharedLibrary", "shared library binary"))))
				.withComponent(new BuildVariantComponent(identifier.getBuildVariant()))
				.withTag(ExcludeFromQualifyingNameTag.class)
			));
			val sharedLibraryTask = registry.register(newEntity("sharedLibrary", Task.class, it -> it.ownedBy(entity)));
			sharedLibraryTask.configure(Task.class, configureBuildGroup());
			sharedLibraryTask.configure(Task.class, configureDescription("Assembles the shared library binary of %s.", identifier));
			sharedLibraryTask.configure(Task.class, configureDependsOn(sharedLibrary.as(SharedLibraryBinary.class)));

			sharedLibrary.configure(SharedLibraryBinary.class, binary -> {
				binary.getCompileTasks().configureEach(configureTargetPlatform(set(fromBuildVariant(identifier.getBuildVariant()))));
			});

			val objectsTask = registry.register(newEntity("objects", Task.class, it -> it.ownedBy(entity)));
			objectsTask.configure(Task.class, configureDependsOn(sharedLibrary.as(SharedLibraryBinary.class).flatMap(binary -> binary.getCompileTasks().filter(it -> it instanceof HasObjectFiles))));
			objectsTask.configure(Task.class, configureBuildGroup());
			objectsTask.configure(Task.class, configureDescription("Assembles the object files of %s.", identifier));

			val nativeRuntimeFiles = registry.register(builder().withComponent(new ElementNameComponent("nativeRuntimeFiles")).withComponent(new ParentComponent(entity)).mergeFrom(project.getExtensions().getByType(ModelPropertyRegistrationFactory.class).createFileCollectionProperty()).build());
			((ModelProperty<Set<File>>) nativeRuntimeFiles).asProperty(of(ConfigurableFileCollection.class)).from(sharedLibrary.as(SharedLibraryBinary.class).flatMap(SharedLibraryBinary::getLinkTask).flatMap(LinkSharedLibrary::getLinkedFile));
			((ModelProperty<Set<File>>) nativeRuntimeFiles).asProperty(of(ConfigurableFileCollection.class)).from((Callable<Object>) () -> ModelNodes.of(sharedLibrary).get(DependentRuntimeLibraries.class));

			registry.instantiate(configureMatching(ownedBy(entity.getId()).and(subtypeOf(of(Configuration.class))), new ExtendsFromParentConfigurationAction()));
		})));
		// ComponentFromEntity<GradlePropertyComponent> read-write on DevelopmentBinaryPropertyComponent
		project.getExtensions().getByType(ModelConfigurer.class).configure(ModelActionWithInputs.of(ModelComponentReference.of(DevelopmentBinaryPropertyComponent.class), ModelComponentReference.of(JniJarArtifactComponent.class), ModelTags.referenceOf(JniLibraryVariantTag.class), (entity, developmentBinary, jniJarArtifact , ignored1) -> {
			((Property<Binary>) developmentBinary.get().get(GradlePropertyComponent.class).get()).convention(project.provider(() -> ModelNodeUtils.get(realize(jniJarArtifact.get()), JniJarBinary.class)));
		}));
		project.getExtensions().getByType(ModelConfigurer.class).configure(new OnDiscover(ModelActionWithInputs.of(ModelComponentReference.ofProjection(JniLibraryInternal.class), ModelComponentReference.of(IdentifierComponent.class), ModelTags.referenceOf(IsVariant.class), (entity, projection, identifier, tag) -> {
			val registry = project.getExtensions().getByType(ModelRegistry.class);
			val binaryIdentifier = BinaryIdentifier.of(identifier.get(), BinaryIdentity.ofMain("jniJar", "JNI JAR binary"));
			val jniJar = registry.instantiate(project.getExtensions().getByType(JniJarBinaryRegistrationFactory.class).create(binaryIdentifier)
				.withComponent(new ParentComponent(entity))
				.withComponent(tag(JniJarArtifactTag.class))
				.withComponent(tag(ExcludeFromQualifyingNameTag.class))
				.build());
			entity.addComponent(new JniJarArtifactComponent(jniJar));
			ModelStates.register(jniJar);
			registry.instantiate(configure(jniJar.getId(), JniJarBinary.class, binary -> {
				binary.getJarTask().configure(task -> {
					task.getArchiveBaseName().set(project.provider(() -> {
						val baseName = ModelProperties.getProperty(entity, "baseName").as(String.class).get();
						return baseName + ((VariantIdentifier) identifier.get()).getAmbiguousDimensions().getAsKebabCase().map(it -> "-" + it).orElse("");
					}));
				});
			}));
		})));

		val unbuildableWarningService = (Provider<UnbuildableWarningService>) project.getGradle().getSharedServices().getRegistrations().getByName(UnbuildableWarningService.class.getSimpleName()).getService();

		// ComponentFromEntity<IdentifierComponent> read-only
		project.getExtensions().getByType(ModelConfigurer.class).configure(ModelActionWithInputs.of(ModelComponentReference.of(JarTaskComponent.class), ModelTags.referenceOf(JniJarArtifactTag.class), ModelComponentReference.of(ParentComponent.class), (entity, jarTask, tag, parent) -> {
			val registry = project.getExtensions().getByType(ModelRegistry.class);

			val identifier = (VariantIdentifier) parent.get().get(IdentifierComponent.class).get();
			registry.instantiate(configure(jarTask.get().getId(), Jar.class, configureJarTaskUsing(project.provider(() -> ModelNodeUtils.get(parent.get(), JniLibrary.class)), unbuildableWarningService.map(it -> {
				it.warn(identifier.getComponentIdentifier());
				return null;
			}))));
		}));

		project.getPlugins().withType(NativeLanguagePlugin.class, new OnceAction<>(ignored -> {
			project.getExtensions().getByType(ModelConfigurer.class).configure(new OnDiscover(ModelActionWithInputs.of(ModelComponentReference.ofProjection(DependencyAwareComponent.class), ModelComponentReference.of(IdentifierComponent.class), (entity, tag, identifier) -> {
				val registry = project.getExtensions().getByType(ModelRegistry.class);
				val compileOnly = registry.register(newEntity("nativeCompileOnly", DeclarableDependencyBucketSpec.class, it -> it.ownedBy(entity).withTag(FrameworkAwareDependencyBucketTag.class)));
				entity.addComponent(new CompileOnlyConfigurationComponent(ModelNodes.of(compileOnly)));
			})));
		}));

		project.getExtensions().getByType(ModelConfigurer.class).configure(ModelActionWithInputs.of(ModelComponentReference.of(HasConfigurableHeadersPropertyComponent.class), ModelComponentReference.of(ParentComponent.class), (entity, headers, parent) -> {
			// Attach generated JNI headers
			// FIXME shoul not stream parent in a callable...
			((ConfigurableFileCollection) headers.get().get(GradlePropertyComponent.class).get()).from((Callable<?>) () -> {
				return ParentUtils.stream(parent).filter(it -> it.has(GeneratedJniHeadersComponent.class)).map(it -> it.get(GeneratedJniHeadersComponent.class).get()).collect(Collectors.toList());
			});
		}));
	}

	// NOTE(daniel): I added the diagnostic because I lost about 2 hours debugging missing files from the generated JAR file.
	//  The concept of diagnostic is something I want to push forward throughout Nokee to inform the user of possible configuration problems.
	//  I'm still hesitant at this stage to throw an exception vs warning in the console.
	//  Some of the concept here should be shared with the incompatible plugin usage (and vice-versa).
	private static class MissingFileDiagnostic {
		private boolean hasAlreadyRan = false;
		private final List<File> missingFiles = new ArrayList<>();

		public void logTo(Logger logger) {
			if (!missingFiles.isEmpty()) {
				StringBuilder builder = new StringBuilder();
				builder.append("The following file");
				if (missingFiles.size() > 1) {
					builder.append("s are");
				} else {
					builder.append(" is");
				}
				builder.append(" missing and will be absent from the JAR file:").append(System.lineSeparator());
				for (File file : missingFiles) {
					builder.append(" * ").append(file.getPath()).append(System.lineSeparator());
				}
				builder.append("We recommend taking the following actions:").append(System.lineSeparator());
				builder.append(" - Verify 'nativeRuntimeFile' property configuration for each variants").append(System.lineSeparator());
				builder.append("Missing files from the JAR file can lead to runtime errors such as 'NoClassDefFoundError'.");
				logger.warn(builder.toString());
			}
		}

		public void missingFiles(List<File> missingFiles) {
			this.missingFiles.addAll(missingFiles);
		}

		public void run(Consumer<MissingFileDiagnostic> action) {
			if (!hasAlreadyRan) {
				action.accept(this);
				hasAlreadyRan = false;
			}
		}
	}

	private static Action<Jar> configureJarTaskUsing(Provider<JniLibrary> library, Provider<Void> logger) {
		return task -> {
			MissingFileDiagnostic diagnostic = new MissingFileDiagnostic();
			task.doFirst(new Action<Task>() {
				@Override
				public void execute(Task task) {
					diagnostic.run(warnAboutMissingFiles(task.getInputs().getSourceFiles()));
					diagnostic.logTo(task.getLogger());
				}

				private Consumer<MissingFileDiagnostic> warnAboutMissingFiles(Iterable<File> files) {
					return diagnostic -> {
						ImmutableList.Builder<File> builder = ImmutableList.builder();
						File linkedFile = library.map(it -> it.getSharedLibrary().getLinkTask().get().getLinkedFile().get().getAsFile()).get();
						for (File file : files) {
							if (!file.exists() && !file.equals(linkedFile)) {
								builder.add(file);
							}
						}
						diagnostic.missingFiles(builder.build());
					};
				}
			});
			task.from(library.flatMap(it -> {
				// TODO: The following is debt that we accumulated from gradle/gradle.
				//  The real condition to check is, do we know of a way to build the target machine on the current host.
				//  If yes, we crash the build by attaching the native file which will tell the user how to install the right tools.
				//  If no, we can "silently" ignore the build by saying you can't build on this machine.
				//  One consideration is to deactivate publishing so we don't publish a half built jar.
				//  TL;DR:
				//    - Single variant where no toolchain could ever build the binary (unavailable) => print a warning
				//    - Single variant where no toolchain is found to build the binary (unbuildable) => fail
				//    - Single variant where toolchain is found to build the binary (buildable) => build (and hopefully succeed)
				if (task.getName().equals("jar")) {
					// TODO: Test this scenario in a functional test
					if (it.getSharedLibrary().isBuildable()) {
						return it.getNativeRuntimeFiles().getElements();
					} else {
						logger.getOrNull();
						return ProviderUtils.fixed(emptyList());
					}
				}
				return it.getNativeRuntimeFiles().getElements();
			}), spec -> {
				// Don't resolve the resourcePath now as the JVM Kotlin plugin (as of 1.3.72) was resolving the `jar` task early.
				spec.into(library.map(JniLibrary::getResourcePath));
			});
		};
	}

	//region Target platform
	public static <SELF extends Task> Action<SELF> configureTargetPlatform(BiConsumer<? super SELF, ? super PropertyUtils.Property<? extends NativePlatform>> action) {
		return task -> {
			action.accept(task, wrap(targetPlatformProperty(task)));
		};
	}

	private static NativePlatform fromBuildVariant(BuildVariant buildVariant) {
		return NativePlatformFactory.create(buildVariant).get();
	}

	private static Property<NativePlatform> targetPlatformProperty(Task task) {
		if (task instanceof AbstractNativeCompileTask) {
			return ((AbstractNativeCompileTask) task).getTargetPlatform();
		} else if (task instanceof SwiftCompile) {
			return ((SwiftCompile) task).getTargetPlatform();
		} else {
			throw new IllegalArgumentException();
		}
	}
	//endregion
}
