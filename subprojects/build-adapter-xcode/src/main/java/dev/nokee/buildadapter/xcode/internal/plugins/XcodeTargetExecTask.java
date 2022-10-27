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
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
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
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.skip;
import static dev.nokee.buildadapter.xcode.internal.plugins.XCBuildSettingsUtils.codeSigningDisabled;
import static dev.nokee.core.exec.CommandLineToolExecutionEngine.execOperations;
import static dev.nokee.core.exec.CommandLineToolInvocationEnvironmentVariables.inherit;
import static dev.nokee.core.exec.CommandLineToolInvocationOutputRedirection.toFile;
import static dev.nokee.core.exec.CommandLineToolInvocationOutputRedirection.toNullStream;
import static dev.nokee.core.exec.CommandLineToolInvocationOutputRedirection.toStandardStream;
import static dev.nokee.utils.ProviderUtils.disallowChanges;
import static dev.nokee.utils.ProviderUtils.finalizeValueOnRead;
import static dev.nokee.utils.ProviderUtils.ifPresent;

public abstract class XcodeTargetExecTask extends DefaultTask implements XcodebuildExecTask, HasConfigurableXcodeInstallation {
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
	public abstract ListProperty<String> getAllArguments();

	@Internal
	public abstract MapProperty<String, String> getAllBuildSettings();

	@Inject
	public XcodeTargetExecTask(WorkerExecutor workerExecutor) {
		this.workerExecutor = workerExecutor;

		getAllArguments().addAll(getXcodeProject().map(it -> of("-project", it.getLocation().toString())));
		getAllArguments().addAll(getTargetName().map(it -> of("-target", it)));
		getAllArguments().addAll(getSdk().map(sdk -> of("-sdk", sdk)).orElse(of()));
		getAllArguments().addAll(getConfiguration().map(buildType -> of("-configuration", buildType)).orElse(of()));
		getAllArguments().addAll(getDerivedDataPath().map(FileSystemLocationUtils::asPath)
			.map(derivedDataPath -> of("PODS_BUILD_DIR=" + derivedDataPath.resolve("Build/Products"))));
		getAllArguments().addAll(getDerivedDataPath().map(FileSystemLocationUtils::asPath)
			.map(new DerivedDataPathAsBuildSettings()).map(this::asFlags));
		getAllArguments().addAll(codeSigningDisabled());

		finalizeValueOnRead(disallowChanges(getAllArguments()));

		finalizeValueOnRead(disallowChanges(getAllBuildSettings().value(getAllArguments().map(allArguments -> {
			return CommandLineTool.of("xcodebuild").withArguments(it -> {
				it.args(allArguments);
				it.args("-showBuildSettings", "-json");
			}).newInvocation(it -> {
				it.withEnvironmentVariables(inherit().putOrReplace("DEVELOPER_DIR", getXcodeInstallation().get().getDeveloperDirectory()));
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

	@TaskAction
	private void doExec() {
		val isolatedProjectLocation = new File(getTemporaryDir(), getXcodeProject().get().getLocation().getFileName().toString());

		val invocation = CommandLineTool.of("xcodebuild").withArguments(it -> {
			it.args(getAllArguments().map(allArguments -> concat(of("-project", isolatedProjectLocation.getAbsolutePath()), skip(allArguments, 2))));
		}).newInvocation(it -> {
			it.withEnvironmentVariables(inherit().putOrReplace("DEVELOPER_DIR", getXcodeInstallation().get().getDeveloperDirectory()));
			ifPresent(getWorkingDirectory(), it::workingDirectory);
			it.redirectStandardOutput(toFile(new File(getTemporaryDir(), "outputs.txt")));
			it.redirectErrorOutput(toStandardStream());
		});
		workerExecutor.noIsolation().submit(XcodebuildExec.class, spec -> {
			spec.getOutgoingDerivedDataPath().set(getOutputDirectory());
			spec.getXcodeDerivedDataPath().set(getDerivedDataPath());
			spec.getIncomingDerivedDataPaths().setFrom(getInputDerivedData());

			spec.getOriginalProjectLocation().set(getXcodeProject().get().getLocation().toFile());
			spec.getIsolatedProjectLocation().set(isolatedProjectLocation);
			spec.getTargetNameToIsolate().set(getTargetName());

			spec.getInvocation().set(invocation);
		});
	}

	interface IsolationParameters {
		DirectoryProperty getOriginalProjectLocation();
		DirectoryProperty getIsolatedProjectLocation();
		Property<String> getTargetNameToIsolate();
	}

	interface ExecutionParameters {
		Property<CommandLineToolInvocation> getInvocation();
	}

	interface DerivedDataParameters {
		ConfigurableFileCollection getIncomingDerivedDataPaths();
		DirectoryProperty getXcodeDerivedDataPath();
		DirectoryProperty getOutgoingDerivedDataPath();
	}

	public static abstract class XcodebuildExec implements WorkAction<XcodebuildExec.Parameters> {
		interface Parameters extends WorkParameters, DerivedDataParameters, ExecutionParameters, IsolationParameters {}

		@Inject
		protected abstract ExecOperations getExecOperations();

		@Inject
		protected abstract FileSystemOperations getFileOperations();

		@Override
		public void execute() {
			derivedDataPath(getParameters(), () -> {
				isolateProject(getParameters(), () -> {
					getParameters().getInvocation().get().submitTo(execOperations(getExecOperations())).result().assertNormalExitValue();
				});
			});
		}

		private void isolateProject(IsolationParameters parameters, Runnable action) {
			val originalProjectLocation = parameters.getOriginalProjectLocation().get().getAsFile().toPath();
			val isolatedProjectLocation = parameters.getIsolatedProjectLocation().get().getAsFile().toPath();
			getFileOperations().sync(spec -> {
				spec.from(originalProjectLocation);
				spec.into(isolatedProjectLocation);
			});

			try {
				PBXProj proj;
				try (val reader = new PBXProjReader(new AsciiPropertyListReader(Files.newBufferedReader(isolatedProjectLocation.resolve("project.pbxproj"))))) {
					proj = reader.read();
				}
				val builder = PBXProj.builder();
				val isolatedProject = builder.rootObject(proj.getRootObject()).objects(o -> {
					for (PBXObjectReference object : proj.getObjects()) {
						if (ImmutableSet.of("PBXNativeTarget", "PBXAggregateTarget", "PBXLegacyTarget").contains(object.isa()) && parameters.getTargetNameToIsolate().get().equals(object.getFields().get("name"))) {
							o.add(PBXObjectReference.of(object.getGlobalID(), entryBuilder -> {
								for (Map.Entry<String, Object> entry : object.getFields().entrySet()) {
									if (!entry.getKey().equals("dependencies")) {
										entryBuilder.putField(entry.getKey(), entry.getValue());
									}
								}
							}));
						} else if ("PBXProject".equals(object.isa())) {
							o.add(PBXObjectReference.of(object.getGlobalID(), entryBuilder -> {
								entryBuilder.putField("projectDirPath", originalProjectLocation.getParent().toString());
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
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}

			action.run();
		}

		private void derivedDataPath(DerivedDataParameters parameters, Runnable action) {
			getFileOperations().sync(spec -> {
				spec.from(parameters.getIncomingDerivedDataPaths());
				spec.into(parameters.getXcodeDerivedDataPath());
				spec.setDuplicatesStrategy(DuplicatesStrategy.INCLUDE);
			});

			action.run();

			getFileOperations().sync(spec -> {
				spec.from(parameters.getXcodeDerivedDataPath(), it -> it.include("Build/Products/**/*"));
				spec.into(parameters.getOutgoingDerivedDataPath());
			});
		}
	}

	private List<String> asFlags(Map<String, String> buildSettings) {
		val builder = ImmutableList.<String>builder();
		buildSettings.forEach((k, v) -> builder.add(k + "=" + v));
		return builder.build();
	}
}
