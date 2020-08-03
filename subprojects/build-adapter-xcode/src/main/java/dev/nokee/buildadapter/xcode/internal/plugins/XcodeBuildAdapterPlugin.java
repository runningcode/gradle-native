package dev.nokee.buildadapter.xcode.internal.plugins;

import com.google.gson.Gson;
import dev.nokee.core.exec.CommandLine;
import dev.nokee.core.exec.ProcessBuilderEngine;
import dev.nokee.platform.nativebase.internal.BaseTargetBuildType;
import dev.nokee.platform.nativebase.internal.NamedTargetBuildType;
import dev.nokee.runtime.base.internal.tools.ToolRepository;
import dev.nokee.runtime.darwin.internal.locators.XcodebuildLocator;
import lombok.Value;
import lombok.val;
import org.apache.commons.io.FilenameUtils;
import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.initialization.Settings;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.plugins.ide.idea.model.Workspace;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class XcodeBuildAdapterPlugin implements Plugin<Settings> {
	private static final Logger LOGGER = Logging.getLogger(XcodeBuildAdapterPlugin.class);
	@Override
	public void apply(Settings settings) {
		val repository = new ToolRepository();
		repository.register("xcodebuild", new XcodebuildLocator());

		val workspaceFile = findXcodeWorkspace(settings.getSettingsDir());
		val projectFiles = findXcodeProjects(workspaceFile);
		projectFiles.forEach(projectFile -> {
			val projectPath = asProjectPath(settings.getSettingsDir(), projectFile);
			settings.include(projectPath);

			settings.getGradle().rootProject(rootProject -> {
				rootProject.project(projectPath, project -> configureProject(project, repository));
			});
		});
	}

	private void configureProject(Project project, ToolRepository repository) {
		val projectInfo = CommandLine.of(repository.findAll("xcodebuild").iterator().next().getPath().getAbsolutePath(), "-project", project.getProjectDir() + "/" + project.getName() + ".xcodeproj", "-list", "-json").newInvocation().workingDirectory(project.getRootDir()).buildAndSubmit(new ProcessBuilderEngine()).waitFor().assertNormalExitValue().getStandardOutput().parse(this::asProjectData);

		if (projectInfo.getProject().getSchemes().size() > 1) {
			LOGGER.warn(String.format("Multiple schemes found in project '%s', which are currently unsupported", project.getPath()));
			return;
		}

		projectInfo.getProject().getSchemes().forEach(schemeName -> {
			projectInfo.getProject().getConfigurations().forEach(configurationName -> {
				// TODO: Capitalize configurationName
				project.getConfigurations().create(schemeName + configurationName, configuration -> {
					configuration.setCanBeResolved(false);
					configuration.setCanBeConsumed(true);
					configuration.getAttributes().attribute(BaseTargetBuildType.BUILD_TYPE_ATTRIBUTE, configurationName);
				});
			});
		});
	}

	private ProjectData asProjectData(String output) {
		return new Gson().fromJson(output, ProjectData.class);
	}

	@Value
	private static class ProjectData {
		ProjectInfo project;

		@Value
		public static class ProjectInfo {
			List<String> configurations;
			String name;
			List<String> schemes;
			List<String> targets;
		}
	}

	private String asProjectPath(File settingDirectory, File projectFile) {
		val relativePath = settingDirectory.toPath().relativize(projectFile.toPath());
		return FilenameUtils.removeExtension(relativePath.toString()).replace('/', ':');
	}

	private File findXcodeWorkspace(File directory) {
		try (val stream = Files.newDirectoryStream(directory.toPath(), this::filterXcodeWorkspace)) {
			val iter = stream.iterator();
			if (!iter.hasNext()) {
				throw new IllegalStateException("No response from cmake");
			}

			return iter.next().toFile();
		} catch (IOException e) {
			throw new IllegalArgumentException("Unable to locate Xcode workspace.", e);
		}
	}

	private boolean filterXcodeWorkspace(Path entry) {
		return entry.getFileName().toString().endsWith(".xcworkspace");
	}

	private List<File> findXcodeProjects(File workspaceFile) {
		val workspaceContentFile = new File(workspaceFile, "contents.xcworkspacedata");

		Serializer serializer = new Persister();
		try {
			val workspace = serializer.read(Workspace.class, workspaceContentFile);
			return workspace.getFileRefs().stream().map(fileRef -> {
				if (fileRef.getLocation().startsWith("group:")) {
					return new File(workspaceFile.getParentFile(), fileRef.getLocation().substring(6));
				} else if (fileRef.getLocation().startsWith("absolute:")) {
					return new File(workspaceFile.getParentFile(), fileRef.getLocation().substring(9));
				}
				throw new IllegalArgumentException(String.format("Unknown Xcode workspace file reference '%s'.", fileRef.getLocation()));
			}).collect(Collectors.toList());
		} catch (Exception e) {
			throw new IllegalArgumentException("Unable to read Xcode workspace content.");
		}
	}

	@Value
	@Root(name = "Workspace", strict = false)
	private static class Workspace {
		@ElementList(name = "FileRef", inline = true)
		List<FileRef> fileRefs;

		@Value
		public static class FileRef {
			@Attribute(name = "location")
			String location;
		}
	}
}
