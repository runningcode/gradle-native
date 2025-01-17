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

import dev.nokee.xcode.XCWorkspaceReference;
import lombok.val;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.logging.ConsoleRenderer;
import org.gradle.process.ExecOperations;
import org.gradle.process.ExecResult;

import javax.inject.Inject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static dev.nokee.buildadapter.xcode.internal.plugins.XCBuildSettingsUtils.codeSigningDisabled;
import static dev.nokee.utils.ProviderUtils.ifPresent;

public abstract class XcodeSchemeExecTask extends DefaultTask implements XcodebuildExecTask {
	@Inject
	protected abstract ExecOperations getExecOperations();

	@Internal
	public abstract Property<XCWorkspaceReference> getXcodeWorkspace();

	@Internal
	public abstract Property<String> getSchemeName();

	@TaskAction
	private void doExec() throws IOException {
		ExecResult result = null;
		try (val outStream = new FileOutputStream(new File(getTemporaryDir(), "outputs.txt"))) {
			result = getExecOperations().exec(spec -> {
				spec.commandLine("xcodebuild", "-workspace", getXcodeWorkspace().get().getLocation(), "-scheme", getSchemeName().get());
				ifPresent(getDerivedDataPath(), it -> spec.args("-derivedDataPath", it.getAsFile()));
				ifPresent(getSdk(), sdk -> spec.args("-sdk", sdk));
				ifPresent(getConfiguration(), buildType -> spec.args("-configuration", buildType));
				spec.args(codeSigningDisabled());
				spec.workingDir(getWorkingDirectory().map(Directory::getAsFile)
					.orElse(getXcodeWorkspace().map(it -> it.getLocation().getParent().toFile())));
				spec.setStandardOutput(outStream);
				spec.setErrorOutput(outStream);
				spec.setIgnoreExitValue(true);
			});
		}


		if (result.getExitValue() != 0) {
			throw new RuntimeException(String.format("Process '%s' finished with non-zero exit value %d, see %s for more information.", "xcodebuild", result.getExitValue(), new ConsoleRenderer().asClickableFileUrl(new File(getTemporaryDir(), "outputs.txt"))));
		}
	}
}
