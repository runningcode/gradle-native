/*
 * Copyright 2022 the original author or authors.
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
package dev.nokee.xcode.project;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PBXTypeSafety {
	public static <T> Optional<T> orEmpty(@Nullable T value) {
		return Optional.ofNullable(value);
	}

	public static <E> List<E> orEmptyList(@Nullable List<E> value) {
		return value == null ? ImmutableList.of() : value;
	}

	public static boolean orFalse(@Nullable Boolean value) {
		return value == null ? false : value.booleanValue();
	}

	public static <K> Map<K, ?> orEmptyMap(@Nullable Map<K, ?> value) {
		return value == null ? ImmutableMap.of() : value;
	}
}