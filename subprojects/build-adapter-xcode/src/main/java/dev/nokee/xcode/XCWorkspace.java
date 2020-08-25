package dev.nokee.xcode;

import java.io.File;
import java.util.List;

public interface XCWorkspace {
	List<File> getProjectLocations();

	File getLocation();

	static XCWorkspace open(File workspaceLocation) {
//		if (workspaceLocation.exists() && workspaceLocation.isDirectory() && new File(workspaceLocation, "contents.xcworkspacedata").exists() && new File(workspaceLocation, "contents.xcworkspacedata").isFile()) {
			return new XCWorkspaceImpl(workspaceLocation);
//		}
//		throw new IllegalArgumentException("Invalid workspace");
	}
}
