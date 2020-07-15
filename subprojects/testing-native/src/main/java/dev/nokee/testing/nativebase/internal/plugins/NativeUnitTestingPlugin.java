package dev.nokee.testing.nativebase.internal.plugins;

import dev.nokee.language.c.internal.CSourceSet;
import dev.nokee.language.cpp.internal.CppSourceSet;
import dev.nokee.language.objectivec.internal.ObjectiveCSourceSet;
import dev.nokee.language.objectivecpp.internal.ObjectiveCppSourceSet;
import dev.nokee.language.swift.internal.SwiftSourceSet;
import dev.nokee.platform.base.Variant;
import dev.nokee.platform.base.internal.NamingScheme;
import dev.nokee.platform.nativebase.internal.ExecutableBinaryInternal;
import dev.nokee.platform.nativebase.tasks.LinkExecutable;
import dev.nokee.testing.base.TestSuiteComponent;
import dev.nokee.testing.base.internal.plugins.TestingBasePlugin;
import dev.nokee.testing.nativebase.NativeTestSuite;
import dev.nokee.testing.nativebase.internal.DefaultNativeTestSuiteComponent;
import lombok.val;
import org.gradle.api.ExtensiblePolymorphicDomainObjectContainer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.TaskContainer;
import org.gradle.nativeplatform.test.tasks.RunTestExecutable;

import javax.inject.Inject;

public abstract class NativeUnitTestingPlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		project.getPluginManager().apply("lifecycle-base");
		project.getPluginManager().apply(TestingBasePlugin.class);

		ExtensiblePolymorphicDomainObjectContainer<TestSuiteComponent> extension = (ExtensiblePolymorphicDomainObjectContainer<TestSuiteComponent>) project.getExtensions().getByName("testSuites");
		extension.registerFactory(NativeTestSuite.class, this::createNativeTestSuite);

		extension.withType(DefaultNativeTestSuiteComponent.class).configureEach(testSuite -> {
			// TODO: Move these generic source set creation to the respective language plugin
			if (project.getPluginManager().hasPlugin("dev.nokee.c-language")) {
				testSuite.getSourceCollection().add(getObjects().newInstance(CSourceSet.class, "c").from(testSuite.getNames().getSourceSetPath("c")));
			}
			if (project.getPluginManager().hasPlugin("dev.nokee.cpp-language")) {
				testSuite.getSourceCollection().add(getObjects().newInstance(CppSourceSet.class, "cpp").from(testSuite.getNames().getSourceSetPath("cpp")));
			}
			if (project.getPluginManager().hasPlugin("dev.nokee.objective-c-language")) {
				testSuite.getSourceCollection().add(getObjects().newInstance(ObjectiveCSourceSet.class, "objc").from(testSuite.getNames().getSourceSetPath("objc")));
			}
			if (project.getPluginManager().hasPlugin("dev.nokee.objective-cpp-language")) {
				testSuite.getSourceCollection().add(getObjects().newInstance(ObjectiveCppSourceSet.class, "objcpp").from(testSuite.getNames().getSourceSetPath("objcpp")));
			}
			if (project.getPluginManager().hasPlugin("dev.nokee.swift-language")) {
				testSuite.getSourceCollection().add(getObjects().newInstance(SwiftSourceSet.class, "swift").from(testSuite.getNames().getSourceSetPath("swift")));
			}
			val testTask = getTasks().register(testSuite.getName(), RunTestExecutable.class, task -> {
				task.setOutputDir(task.getTemporaryDir());
				task.dependsOn(testSuite.getDevelopmentVariant().flatMap(Variant::getDevelopmentBinary));
				task.commandLine(new Object() {
					@Override
					public String toString() {
						return testSuite.getDevelopmentVariant().flatMap(it -> ((ExecutableBinaryInternal)it.getDevelopmentBinary().get()).getLinkTask().flatMap(LinkExecutable::getLinkedFile)).get().getAsFile().getAbsolutePath();
					}
				});
			});
			getTasks().named("check", task -> {
				task.dependsOn(testTask);
			});
		});

		project.afterEvaluate(proj -> {
			extension.withType(DefaultNativeTestSuiteComponent.class).forEach(it -> it.finalizeExtension(proj));
		});
	}

	@Inject
	protected abstract ObjectFactory getObjects();

	@Inject
	protected abstract TaskContainer getTasks();

	private NativeTestSuite createNativeTestSuite(String name) {
		return getObjects().newInstance(DefaultNativeTestSuiteComponent.class, NamingScheme.asComponent(name, name).withComponentDisplayName("Test Suite"));
	}
}
