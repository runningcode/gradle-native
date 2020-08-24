package dev.nokee.platform.objectivec.internal;

import dev.nokee.language.c.internal.CHeaderSet;
import dev.nokee.language.objectivec.internal.ObjectiveCSourceSet;
import dev.nokee.platform.base.VariantView;
import dev.nokee.platform.nativebase.NativeApplication;
import dev.nokee.platform.nativebase.NativeApplicationComponentDependencies;
import dev.nokee.platform.nativebase.internal.BaseNativeExtension;
import dev.nokee.platform.nativebase.internal.DefaultNativeApplicationComponent;
import dev.nokee.platform.objectivec.ObjectiveCApplicationExtension;
import dev.nokee.runtime.nativebase.TargetBuildType;
import dev.nokee.runtime.nativebase.TargetMachine;
import lombok.Getter;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.provider.SetProperty;

import javax.inject.Inject;

public class DefaultObjectiveCApplicationExtension extends BaseNativeExtension<DefaultNativeApplicationComponent> implements ObjectiveCApplicationExtension {
	@Getter private final ConfigurableFileCollection sources;
	@Getter private final ConfigurableFileCollection privateHeaders;
	@Getter private final SetProperty<TargetMachine> targetMachines;
	@Getter private final SetProperty<TargetBuildType> targetBuildTypes;

	@Inject
	public DefaultObjectiveCApplicationExtension(DefaultNativeApplicationComponent component, ObjectFactory objects, ProviderFactory providers, ProjectLayout layout) {
		super(component, objects, providers, layout);
		this.sources = objects.fileCollection();
		this.privateHeaders = objects.fileCollection();
		this.targetMachines = objects.setProperty(TargetMachine.class);
		this.targetBuildTypes = objects.setProperty(TargetBuildType.class);

		getComponent().getSourceCollection().add(getObjects().newInstance(ObjectiveCSourceSet.class, "objc").from(getSources().getElements().map(toIfEmpty("src/main/objc"))));
		getComponent().getSourceCollection().add(getObjects().newInstance(CHeaderSet.class, "headers").srcDir(getPrivateHeaders().getElements().map(toIfEmpty("src/main/headers"))));
	}

	@Override
	public NativeApplicationComponentDependencies getDependencies() {
		return getComponent().getDependencies();
	}

	@Override
	public void dependencies(Action<? super NativeApplicationComponentDependencies> action) {
		getComponent().dependencies(action);
	}

	public void finalizeExtension(Project project) {
		getComponent().finalizeExtension(project);
	}

	@Override
	public VariantView<NativeApplication> getVariants() {
		return getComponent().getVariantCollection().getAsView(NativeApplication.class);
	}
}
