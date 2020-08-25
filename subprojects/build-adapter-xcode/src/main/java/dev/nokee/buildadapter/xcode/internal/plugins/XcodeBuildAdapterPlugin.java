package dev.nokee.buildadapter.xcode.internal.plugins;

import com.google.gson.Gson;
import dev.nokee.buildadapter.xcode.internal.tasks.XcodebuildTask;
import dev.nokee.core.exec.CommandLine;
import dev.nokee.core.exec.CommandLineTool;
import dev.nokee.core.exec.ProcessBuilderEngine;
import dev.nokee.platform.nativebase.internal.BaseTargetBuildType;
import dev.nokee.runtime.base.internal.tools.ToolRepository;
import dev.nokee.runtime.darwin.internal.locators.XcodebuildLocator;
import dev.nokee.xcode.XCWorkspaceLocator;
import dev.nokee.xcode.internal.XBProjectListResponse;
import lombok.val;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jgit.util.StringUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.io.File;

public class XcodeBuildAdapterPlugin implements Plugin<Settings> {
	private static final Logger LOGGER = Logging.getLogger(XcodeBuildAdapterPlugin.class);
	@Override
	public void apply(Settings settings) {
		val repository = new ToolRepository();
		repository.register("xcodebuild", new XcodebuildLocator());

		val workspaces = new XCWorkspaceLocator().findWorkspaces(settings.getSettingsDir());
		if (workspaces.size() != 1) {
			throw new IllegalStateException("Currently not supporting more than one workspace");
		}
		val projectFiles = workspaces.iterator().next().getProjectLocations();
		projectFiles.forEach(projectFile -> {
			val relativePath = settings.getSettingsDir().toPath().relativize(projectFile.toPath());
			val projectPath = asProjectPath(settings.getSettingsDir(), projectFile);
			LOGGER.info(String.format("Mapping Xcode project '%s' to Gradle project '%s'.", relativePath.toString(), projectPath));
			settings.include(projectPath);

			settings.getGradle().rootProject(rootProject -> {
				rootProject.project(projectPath, project -> configureProject(project, workspaces.iterator().next().getLocation(), projectFile, repository));
			});
		});
	}

	private void configureProject(Project project, File workspaceLocation, File projectFile, ToolRepository repository) {
		val projectInfo = CommandLine.of(repository.findAll("xcodebuild").iterator().next().getPath().getAbsolutePath(), "-project", projectFile, "-list", "-json").newInvocation().workingDirectory(project.getRootDir()).buildAndSubmit(new ProcessBuilderEngine()).waitFor().assertNormalExitValue().getStandardOutput().parse(this::asProjectData);

		if (projectInfo.getProject().getSchemes().size() > 1) {
			LOGGER.warn(String.format("Multiple schemes found in project '%s', which are currently unsupported", project.getPath()));
			return;
		}

		projectInfo.getProject().getSchemes().forEach(schemeName -> {
			projectInfo.getProject().getConfigurations().forEach(configurationName -> {
				val buildTask = project.getTasks().register("build" + schemeName + configurationName, XcodebuildTask.class, task -> {
					task.getConfigurationName().set(configurationName);
					task.getSchemeName().set(schemeName);
					val w = project.getObjects().directoryProperty();
					w.set(workspaceLocation);
					task.getWorkspaceLocation().set(w);
					task.getXcodebuildTool().set(project.getProviders().provider(() -> CommandLineTool.of(repository.findAll("xcodebuild").iterator().next().getPath())));
					task.getDerivedDataPath().set(project.getLayout().getBuildDirectory().dir("derived-data/" + schemeName + "/" + configurationName));
				});

				project.getConfigurations().create(schemeName + StringUtils.capitalize(configurationName) + "Elements", configuration -> {
					configuration.setCanBeResolved(false);
					configuration.setCanBeConsumed(true);
					configuration.getAttributes().attribute(BaseTargetBuildType.BUILD_TYPE_ATTRIBUTE, configurationName);
					configuration.getOutgoing().artifact(buildTask.map(it -> it.getProductLocation().get()));
				});
			});
		});
	}

	private XBProjectListResponse asProjectData(String output) {
		return new Gson().fromJson(output, XBProjectListResponse.class);
	}

	private String asProjectPath(File settingDirectory, File projectFile) {
		val relativePath = settingDirectory.toPath().relativize(projectFile.toPath());
		return FilenameUtils.removeExtension(relativePath.toString()).replace('/', ':');
	}
}
