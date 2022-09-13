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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.nokee.core.exec.CommandLineTool;
import dev.nokee.core.exec.CommandLineToolInvocation;
import dev.nokee.utils.FileSystemLocationUtils;
import dev.nokee.xcode.AsciiPropertyListReader;
import dev.nokee.xcode.XCBuildSettings;
import dev.nokee.xcode.XCProjectReference;
import dev.nokee.xcode.project.PBXObjectReference;
import dev.nokee.xcode.project.PBXProj;
import dev.nokee.xcode.project.PBXProjReader;
import dev.nokee.xcode.project.PBXProjWriter;
import lombok.val;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.DuplicatesStrategy;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecOperations;
import org.gradle.workers.WorkAction;
import org.gradle.workers.WorkParameters;
import org.gradle.workers.WorkerExecutor;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static dev.nokee.buildadapter.xcode.internal.plugins.XCBuildSettingsUtils.codeSigningDisabled;
import static dev.nokee.core.exec.CommandLineToolExecutionEngine.execOperations;
import static dev.nokee.core.exec.CommandLineToolInvocationOutputRedirection.toFile;
import static dev.nokee.core.exec.CommandLineToolInvocationOutputRedirection.toNullStream;
import static dev.nokee.core.exec.CommandLineToolInvocationOutputRedirection.toStandardStream;
import static dev.nokee.utils.ProviderUtils.disallowChanges;
import static dev.nokee.utils.ProviderUtils.finalizeValueOnRead;
import static dev.nokee.utils.ProviderUtils.ifPresent;

public abstract class XcodeTargetExecTask extends DefaultTask implements XcodebuildExecTask {
	private final WorkerExecutor workerExecutor;

	@Inject
	protected abstract ExecOperations getExecOperations();

	@Internal
	public abstract Property<XCProjectReference> getXcodeProject();

	@Input
	public abstract Property<String> getTargetName();

	@InputFiles
	public abstract ConfigurableFileCollection getInputFiles();

	@InputFiles
	public abstract ConfigurableFileCollection getInputDerivedData();

	@OutputDirectory
	public abstract DirectoryProperty getOutputDirectory();

	@Internal
	public abstract MapProperty<String, String> getBuildSettings();

	@Inject
	public XcodeTargetExecTask(ProviderFactory providers, WorkerExecutor workerExecutor) {
		this.workerExecutor = workerExecutor;
		finalizeValueOnRead(disallowChanges(getBuildSettings().value(providers.provider(() -> {
			return CommandLineTool.of("xcodebuild").withArguments(it -> {
				it.args("-project", getXcodeProject().get().getLocation());
				it.args("-target", getTargetName());
				it.args(getDerivedDataPath().map(FileSystemLocationUtils::asPath).map(derivedDataPath -> {
					return ImmutableList.of("PODS_BUILD_DIR=" + derivedDataPath.resolve("Build/Products"));
				}));
				it.args(getDerivedDataPath().map(FileSystemLocationUtils::asPath)
					.map(new DerivedDataPathAsBuildSettings()).map(this::asFlags));
				ifPresent(getSdk(), sdk -> it.args("-sdk", sdk));
				ifPresent(getConfiguration(), buildType -> it.args("-configuration", buildType));
				it.args(codeSigningDisabled());
				it.args("-showBuildSettings", "-json");
			}).newInvocation(it -> {
				ifPresent(getWorkingDirectory(), it::workingDirectory);
				it.redirectStandardOutput(toNullStream());
				it.redirectErrorOutput(toNullStream());
			}).submitTo(execOperations(getExecOperations())).result()
				.getStandardOutput().parse(output -> {
					@SuppressWarnings("unchecked")
					val parsedOutput = (List<ShowBuildSettingsEntry>) new Gson().fromJson(output, new TypeToken<List<ShowBuildSettingsEntry>>() {}.getType());
					return parsedOutput.get(0).getBuildSettings();
				});
		}))));
	}

	private static final class ShowBuildSettingsEntry {
		private final Map<String, String> buildSettings;

		private ShowBuildSettingsEntry(Map<String, String> buildSettings) {
			this.buildSettings = buildSettings;
		}

		public Map<String, String> getBuildSettings() {
			return buildSettings;
		}
	}

	@Inject
	protected abstract FileSystemOperations getFileOperations();

	@TaskAction
	private void doExec() throws IOException {
		getFileOperations().sync(spec -> {
			spec.from(getInputDerivedData());
			spec.into(getDerivedDataPath());
			spec.setDuplicatesStrategy(DuplicatesStrategy.INCLUDE);
		});

		val isolatedProjectLocation = new File(getTemporaryDir(), getXcodeProject().get().getLocation().getFileName().toString()).toPath();
		getFileOperations().sync(spec -> {
			spec.from(getXcodeProject().get().getLocation());
			spec.into(isolatedProjectLocation);
		});

		PBXProj proj;
		try (val reader = new PBXProjReader(new AsciiPropertyListReader(Files.newBufferedReader(isolatedProjectLocation.resolve("project.pbxproj"))))) {
			proj = reader.read();
		}
		val builder = PBXProj.builder();
		val isolatedProject = builder.rootObject(proj.getRootObject()).objects(o -> {
			for (PBXObjectReference object : proj.getObjects()) {
				if (ImmutableSet.of("PBXNativeTarget", "PBXAggregateTarget", "PBXLegacyTarget").contains(object.isa()) && getTargetName().get().equals(object.getFields().get("name"))) {
					o.add(PBXObjectReference.of(object.getGlobalID(), entryBuilder -> {
						for (Map.Entry<String, Object> entry : object.getFields().entrySet()) {
							if (!entry.getKey().equals("dependencies")) {
								entryBuilder.putField(entry.getKey(), entry.getValue());
							}
						}
					}));
				} else if ("PBXProject".equals(object.isa())) {
					o.add(PBXObjectReference.of(object.getGlobalID(), entryBuilder -> {
						entryBuilder.putField("projectDirPath", getXcodeProject().get().getLocation().getParent().toString());
						for (Map.Entry<String, Object> entry : object.getFields().entrySet()) {
							if (!entry.getKey().equals("projectDirPath")) {
								entryBuilder.putField(entry.getKey(), entry.getValue());
							}
						}
					}));
				} else {
					o.add(object);
				}
			}
		}).build();
		try (val writer = new PBXProjWriter(Files.newBufferedWriter(isolatedProjectLocation.resolve("project.pbxproj")))) {
			writer.write(isolatedProject);
		}

		val invocation = CommandLineTool.of("xcodebuild").withArguments(it -> {
			it.args("-project", isolatedProjectLocation);
			it.args("-target", getTargetName());
			it.args(getDerivedDataPath().map(FileSystemLocationUtils::asPath).map(derivedDataPath -> {
				return ImmutableList.of("PODS_BUILD_DIR=" + derivedDataPath.resolve("Build/Products"));
			}));
			it.args(getDerivedDataPath().map(FileSystemLocationUtils::asPath)
				.map(new DerivedDataPathAsBuildSettings()).map(this::asFlags));
			ifPresent(getSdk(), sdk -> it.args("-sdk", sdk));
			ifPresent(getConfiguration(), buildType -> it.args("-configuration", buildType));
			it.args(codeSigningDisabled());
		}).newInvocation(it -> {
			ifPresent(getWorkingDirectory(), it::workingDirectory);
			it.redirectStandardOutput(toFile(new File(getTemporaryDir(), "outputs.txt")));
			it.redirectErrorOutput(toStandardStream());
		});
		workerExecutor.noIsolation().submit(XcodebuildExec.class, spec -> {
			spec.getInvocation().set(invocation);
			spec.getOutputDirectory().set(getOutputDirectory());
			spec.getDerivedDataPath().set(getDerivedDataPath());
		});
	}

	public static abstract class XcodebuildExec implements WorkAction<XcodebuildExec.Parameters> {
		interface Parameters extends WorkParameters {
			Property<CommandLineToolInvocation> getInvocation();
			DirectoryProperty getDerivedDataPath();
			DirectoryProperty getOutputDirectory();
		}

		@Inject
		protected abstract ExecOperations getExecOperations();

		@Inject
		protected abstract FileSystemOperations getFileOperations();

		@Override
		public void execute() {
			getParameters().getInvocation().get().submitTo(execOperations(getExecOperations())).result().assertNormalExitValue();

			getFileOperations().sync(spec -> {
				spec.from(getParameters().getDerivedDataPath(), it -> it.include("Build/Products/**/*"));
				spec.into(getParameters().getOutputDirectory());
			});
		}
	}

	private List<String> asFlags(XCBuildSettings buildSettings) {
		val builder = ImmutableList.<String>builder();
		buildSettings.forEach((k, v) -> builder.add(k + "=" + v));
		return builder.build();
	}
}
