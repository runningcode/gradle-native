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

import java.util.Arrays;
import java.util.List;

import static org.apache.commons.lang3.SystemUtils.IS_OS_FREE_BSD;
import static org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS;

final class CommandLineUtils {
	private CommandLineUtils() {}

	public static List<Object> getScriptCommandLine() {
		if (IS_OS_WINDOWS) {
			return Arrays.asList("cmd", "/c");
		} else if (IS_OS_FREE_BSD) {
			return Arrays.asList("/bin/sh", "-c");
		}
		return Arrays.asList("/bin/bash", "-c");
	}
}
