package dev.nokee.platform.objectivec;

import dev.nokee.internal.testing.utils.TestUtils;
import dev.nokee.language.base.FunctionalSourceSet;
import dev.nokee.language.base.testers.FileSystemWorkspace;
import dev.nokee.language.nativebase.NativeHeaderSet;
import dev.nokee.language.objectivec.ObjectiveCSourceSet;
import dev.nokee.platform.base.testers.SourceAwareComponentTester;
import dev.nokee.platform.nativebase.internal.plugins.NativeComponentBasePlugin;
import lombok.Getter;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import spock.lang.Subject;

import java.io.File;
import java.util.stream.Stream;

import static dev.nokee.model.fixtures.ModelRegistryTestUtils.create;
import static dev.nokee.model.fixtures.ModelRegistryTestUtils.registry;
import static dev.nokee.platform.objectivec.internal.plugins.ObjectiveCApplicationPlugin.objectiveCApplication;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

@Subject(ObjectiveCApplication.class)
class ObjectiveCApplicationTest implements SourceAwareComponentTester<ObjectiveCApplication> {
	@Getter @TempDir File testDirectory;

	@Override
	public ObjectiveCApplication createSubject(String componentName) {
		val project = TestUtils.createRootProject(getTestDirectory());
		project.getPluginManager().apply(NativeComponentBasePlugin.class);
		val component = create(registry(project.getObjects()), objectiveCApplication(componentName, project));
		((FunctionalSourceSet) component.getSources()).get(); // force realize all source set
		return component;
	}

	@Override
	public Stream<SourcesUnderTest> provideSourceSetUnderTest() {
		return Stream.of(
			new SourcesUnderTest("objectiveC", ObjectiveCSourceSet.class, "objectiveCSources"),
			new SourcesUnderTest("headers", NativeHeaderSet.class, "privateHeaders"));
	}

	@Test
	public void hasAdditionalConventionOnObjectiveCSourceSet() throws Throwable {
		val a = new FileSystemWorkspace(getTestDirectory());
		assertThat(createSubject("main").getObjectiveCSources().getSourceDirectories(),
			hasItem(a.file("src/main/objc")));
		assertThat(createSubject("test").getObjectiveCSources().getSourceDirectories(),
			hasItem(a.file("src/test/objc")));
	}
}
