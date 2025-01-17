/*
 * Copyright 2020 the original author or authors.
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
package dev.nokee.core.exec;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

enum CommandLineToolInvocationEnvironmentVariablesEmptyImpl implements CommandLineToolInvocationEnvironmentVariables {
	EMPTY_ENVIRONMENT_VARIABLES;

	@Override
	public Map<String, String> getAsMap() {
		return ImmutableMap.of();
	}

	@Override
	public List<String> getAsList() {
		return ImmutableList.of();
	}

	@Override
	public CommandLineToolInvocationEnvironmentVariables plus(CommandLineToolInvocationEnvironmentVariables environmentVariables) {
		return requireNonNull(environmentVariables);
	}
}
