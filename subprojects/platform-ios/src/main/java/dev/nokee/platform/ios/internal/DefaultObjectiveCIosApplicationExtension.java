package dev.nokee.platform.ios.internal;

import dev.nokee.language.base.internal.LanguageSourceSetIdentifier;
import dev.nokee.language.base.internal.LanguageSourceSetName;
import dev.nokee.language.base.internal.LanguageSourceSetRegistry;
import dev.nokee.language.c.internal.CHeaderSetImpl;
import dev.nokee.language.objectivec.internal.ObjectiveCSourceSetImpl;
import dev.nokee.platform.base.Component;
import dev.nokee.platform.base.VariantView;
import dev.nokee.platform.ios.IosApplication;
import dev.nokee.platform.ios.ObjectiveCIosApplicationExtension;
import dev.nokee.platform.nativebase.NativeComponentDependencies;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.ProviderFactory;

import javax.inject.Inject;

public class DefaultObjectiveCIosApplicationExtension extends BaseIosExtension<DefaultIosApplicationComponent> implements ObjectiveCIosApplicationExtension, Component {
	@Inject
	public DefaultObjectiveCIosApplicationExtension(DefaultIosApplicationComponent component, ObjectFactory objects, ProviderFactory providers, LanguageSourceSetRegistry languageSourceSetRegistry) {
		super(component, objects, providers);
		languageSourceSetRegistry.create(LanguageSourceSetIdentifier.of(LanguageSourceSetName.of("objc"), ObjectiveCSourceSetImpl.class, component.getIdentifier()));
		languageSourceSetRegistry.create(LanguageSourceSetIdentifier.of(LanguageSourceSetName.of("headers"), CHeaderSetImpl.class, component.getIdentifier()));
	}

	@Override
	public NativeComponentDependencies getDependencies() {
		return getComponent().getDependencies();
	}

	public void finalizeExtension(Project project) {
		getComponent().finalizeExtension(project);
	}

	@Override
	public VariantView<IosApplication> getVariants() {
		return getComponent().getVariantCollection().getAsView(IosApplication.class);
	}
}
