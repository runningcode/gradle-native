package dev.nokee.buildadapter.xcode.internal.tasks;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.nokee.core.exec.CommandLineTool;
import dev.nokee.core.exec.CommandLineToolInvocationStandardOutputRedirect;
import dev.nokee.core.exec.ProcessBuilderEngine;
import dev.nokee.xcode.internal.XBShowBuildSettingsResponse;
import lombok.val;
import org.gradle.api.DefaultTask;
import org.gradle.api.Transformer;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.*;

import javax.inject.Inject;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static dev.nokee.core.exec.CommandLineToolInvocationStandardOutputRedirect.duplicateToSystemOutput;

public class XcodebuildTask extends DefaultTask {
	private final Property<CommandLineTool> xcodebuildTool;
	private final Property<FileSystemLocation> workspaceLocation;
	private final Property<String> schemeName;
	private final Property<String> configurationName;
	private final DirectoryProperty derivedDataPath;
	private final Property<File> productLocation;

	@Internal
	public Property<CommandLineTool> getXcodebuildTool() {
		return xcodebuildTool;
	}

	@InputDirectory
	public Property<FileSystemLocation> getWorkspaceLocation() {
		return workspaceLocation;
	}

	@Input
	public Property<String> getSchemeName() {
		return schemeName;
	}

	@Input
	public Property<String> getConfigurationName() {
		return configurationName;
	}

	@OutputDirectory
	public DirectoryProperty getDerivedDataPath() {
		return derivedDataPath;
	}

	@Internal
	public Provider<File> getProductLocation() {
		return productLocation;
	}

	@Inject
	public XcodebuildTask(ObjectFactory objectFactory, ProviderFactory providerFactory) {
		this.xcodebuildTool = objectFactory.property(CommandLineTool.class);
		this.workspaceLocation = objectFactory.property(FileSystemLocation.class);
		this.schemeName = objectFactory.property(String.class);
		this.configurationName = objectFactory.property(String.class);
		this.derivedDataPath = objectFactory.directoryProperty();
		this.productLocation = objectFactory.property(File.class);
		productLocation.convention(providerFactory.provider(new FindProductPath()));
	}

	@TaskAction
	private void doBuild() {
		xcodebuildTool.get().withArguments(getStandardXcodebuildFlags()).newInvocation().redirectStandardOutput(duplicateToSystemOutput()).buildAndSubmit(new ProcessBuilderEngine()).waitFor().assertNormalExitValue().getStandardOutput();
	}

	private Iterable<Object> getStandardXcodebuildFlags() {
		return ImmutableList.of("-workspace", workspaceLocation.get(), "-scheme", schemeName.get(), "-configuration", configurationName.get(), "-derivedDataPath", derivedDataPath.get().getAsFile());
	}

	private class FindProductPath implements Callable<File> {
		@Override
		public File call() throws Exception {
			val buildSettings = xcodebuildTool.get().withArguments(Iterables.concat(getStandardXcodebuildFlags(), ImmutableList.of("-showBuildSettings", "-json"))).newInvocation().redirectStandardOutput(duplicateToSystemOutput()).buildAndSubmit(new ProcessBuilderEngine()).waitFor().assertNormalExitValue().getStandardOutput().parse(this::asBuildSettings);
			return new File(buildSettings.get("BUILT_PRODUCTS_DIR") + "/" + buildSettings.get("FULL_PRODUCT_NAME"));
		}

		private Map<String, String> asBuildSettings(String output) {
			val responseType = new TypeToken<List<XBShowBuildSettingsResponse>>() {}.getType();
			List<XBShowBuildSettingsResponse> responses = new Gson().fromJson(output, responseType);
			return assumeOne(responses).getBuildSettings();
		}

		private XBShowBuildSettingsResponse assumeOne(Iterable<XBShowBuildSettingsResponse> responses) {
			val iter = responses.iterator();
			Preconditions.checkArgument(iter.hasNext());
			val result = iter.next();
			Preconditions.checkArgument(!iter.hasNext());
			return result;
		}
	}
}
