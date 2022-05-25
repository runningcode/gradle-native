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
package dev.nokee.buildadapter.xcode.internal.plugins;

import lombok.val;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public abstract class XcodeTargetExecTask extends DefaultTask {
	@Inject
	protected abstract ExecOperations getExecOperations();

	@Internal
	public abstract RegularFileProperty getProjectLocation();

	@Internal
	public abstract Property<String> getTargetName();

	@TaskAction
	private void doExec() throws IOException {
		try (val outStream = new FileOutputStream(new File(getTemporaryDir(), "outputs.txt"))) {
			getExecOperations().exec(spec -> {
				spec.commandLine("xcodebuild", "-project", getProjectLocation().get().getAsFile(), "-target", getTargetName().get(),
					// Disable code signing, see https://stackoverflow.com/a/39901677/13624023
					"CODE_SIGN_IDENTITY=\"\"", "CODE_SIGNING_REQUIRED=NO", "CODE_SIGN_ENTITLEMENTS=\"\"", "CODE_SIGNING_ALLOWED=\"NO\"");
				spec.workingDir(getProjectLocation().get().getAsFile().getParentFile()); // TODO: Test execution on nested projects
				spec.setStandardOutput(outStream);
				spec.setErrorOutput(outStream);
			});
		}
	}
}
