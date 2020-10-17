package dev.nokee.language.base.internal;

import dev.nokee.language.base.LanguageSourceSet;
import dev.nokee.model.DomainObjectIdentifier;
import dev.nokee.model.internal.AbstractDomainObjectViewFactory;

import static java.util.Objects.requireNonNull;

public final class LanguageSourceSetViewFactory extends AbstractDomainObjectViewFactory<LanguageSourceSet> {
	private final LanguageSourceSetRepository repository;
	private final LanguageSourceSetConfigurer configurer;
	private final KnownLanguageSourceSetFactory knownFactory;

	public LanguageSourceSetViewFactory(LanguageSourceSetRepository repository, LanguageSourceSetConfigurer configurer, KnownLanguageSourceSetFactory knownFactory) {
		super(LanguageSourceSet.class);
		this.repository = repository;
		this.configurer = configurer;
		this.knownFactory = knownFactory;
	}

	@Override
	public LanguageSourceSetViewImpl<LanguageSourceSet> create(DomainObjectIdentifier viewOwner) {
		return (LanguageSourceSetViewImpl<LanguageSourceSet>) super.create(viewOwner);
	}

	@Override
	public <S extends LanguageSourceSet> LanguageSourceSetViewImpl<S> create(DomainObjectIdentifier viewOwner, Class<S> elementType) {
		return new LanguageSourceSetViewImpl<>(requireNonNull(viewOwner), elementType, repository, configurer, this, knownFactory);
	}
}
