package dev.nokee.xcode;

import lombok.val;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class XCWorkspaceLocator {
	public List<XCWorkspace> findWorkspaces(File searchDirectory) {
		// TODO: Check directory exists
		try (val stream = Files.newDirectoryStream(searchDirectory.toPath(), this::filterXcodeWorkspace)) {
			return StreamSupport.stream(stream.spliterator(), false).map(Path::toFile).map(XCWorkspaceImpl::new).collect(Collectors.toList());
		} catch (IOException e) {
			throw new IllegalArgumentException("Unable to locate Xcode workspace.", e);
		}
	}

	private boolean filterXcodeWorkspace(Path entry) {
		return entry.getFileName().toString().endsWith(".xcworkspace");
	}
}
