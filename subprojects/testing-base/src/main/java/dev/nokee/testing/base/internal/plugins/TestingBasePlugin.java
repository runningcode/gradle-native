package dev.nokee.testing.base.internal.plugins;

import dev.nokee.platform.base.internal.DefaultDomainObjectStore;
import dev.nokee.platform.base.internal.DomainObjectStore;
import dev.nokee.platform.base.internal.plugins.ProjectStorePlugin;
import dev.nokee.testing.base.TestSuiteContainer;
import dev.nokee.testing.base.internal.DefaultTestSuiteContainer;
import lombok.val;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.tasks.TaskContainer;

import javax.inject.Inject;

public abstract class TestingBasePlugin implements Plugin<Project> {
	@Override
	public void apply(Project project) {
		project.getPluginManager().apply(ProjectStorePlugin.class);
		val store = project.getExtensions().getByType(DomainObjectStore.class);
		val extension = getObjects().newInstance(DefaultTestSuiteContainer.class, store);
		project.getExtensions().add(TestSuiteContainer.class, "testSuites", extension);
	}

	@Inject
	protected abstract ObjectFactory getObjects();

	@Inject
	protected abstract TaskContainer getTasks();
}
