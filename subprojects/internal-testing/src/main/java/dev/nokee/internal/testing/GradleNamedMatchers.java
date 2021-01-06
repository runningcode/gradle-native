package dev.nokee.internal.testing;

import org.gradle.api.Named;
import org.gradle.api.artifacts.Configuration;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import static java.util.Objects.requireNonNull;
import static org.hamcrest.Matchers.equalTo;

public final class GradleNamedMatchers {
	private GradleNamedMatchers() {}

	public static <T> Matcher<T> named(String name) {
		return new FeatureMatcher<T, String>(equalTo(requireNonNull(name)), "", "Named's name") {
			@Override
			protected String featureValueOf(T actual) {
				if (actual instanceof Named) {
					return ((Named) actual).getName();

				// Configuration class somewhat behave like a Named class
				} else if (actual instanceof Configuration) {
					return ((Configuration) actual).getName();
				}
				throw new UnsupportedOperationException();
			}
		};
	}
}
