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
package dev.nokee.utils;

import groovy.lang.Closure;
import lombok.EqualsAndHashCode;
import org.gradle.api.Action;
import org.gradle.util.GradleVersion;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

@EqualsAndHashCode
public final class ClosureWrappedConfigureAction<T> implements Action<T> {
	@SuppressWarnings("rawtypes") private final Closure configureClosure;

	public ClosureWrappedConfigureAction(@SuppressWarnings("rawtypes") Closure configureClosure) {
		this.configureClosure = Objects.requireNonNull(configureClosure);
	}

	@Override
	public void execute(T t) {
		try {
			final Class<?> ConfigureUtil = findConfigureUtilClass();
			final Method configure = ConfigureUtil.getMethod("configure", Closure.class, Object.class);
			configure.invoke(null, configureClosure, t);
		} catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}

	private static Class<?> findConfigureUtilClass() throws ClassNotFoundException {
		if (GradleVersion.current().compareTo(GradleVersion.version("7.1")) > 0) {
			return Class.forName("org.gradle.util.internal.ConfigureUtil");
		} else {
			return Class.forName("org.gradle.util.ConfigureUtil");
		}
	}

	@SuppressWarnings("rawtypes")
	public Closure getConfigureClosure() {
		return configureClosure;
	}
}
