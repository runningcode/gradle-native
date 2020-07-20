package dev.nokee.platform.c.internal.plugins;

import dev.nokee.platform.base.internal.DomainObjectStore;
import dev.nokee.platform.base.internal.NamingSchemeFactory;
import dev.nokee.platform.base.internal.plugins.ProjectStorePlugin;
import dev.nokee.platform.c.CApplicationExtension;
import dev.nokee.platform.c.internal.DefaultCApplicationExtension;
import dev.nokee.platform.nativebase.internal.DefaultNativeApplicationComponent;
import dev.nokee.platform.nativebase.internal.TargetMachineRule;
import lombok.val;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.nativeplatform.toolchain.internal.plugins.StandardToolChainsPlugin;

import javax.inject.Inject;

public abstract class CApplicationPlugin implements Plugin<Project> {
	private static final String EXTENSION_NAME = "application";

	@Inject
	protected abstract ObjectFactory getObjects();

	@Override
	public void apply(Project project) {
		project.getPluginManager().apply(StandardToolChainsPlugin.class);
		project.getPluginManager().apply(ProjectStorePlugin.class);

		val store = project.getExtensions().getByType(DomainObjectStore.class);
		val component = store.register(DefaultNativeApplicationComponent.newMain(getObjects(), new NamingSchemeFactory(project.getName())));
		component.configure(it -> it.getBaseName().convention(project.getName()));
		DefaultCApplicationExtension extension = getObjects().newInstance(DefaultCApplicationExtension.class, component.get());

		project.afterEvaluate(getObjects().newInstance(TargetMachineRule.class, extension.getTargetMachines(), EXTENSION_NAME));
		project.afterEvaluate(extension::finalizeExtension);

		project.getExtensions().add(CApplicationExtension.class, EXTENSION_NAME, extension);
	}
}
