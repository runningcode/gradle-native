package dev.nokee.xcode;

import lombok.Getter;

import java.io.File;

class XCWorkspaceLayout {
	@Getter private final File location;

	public XCWorkspaceLayout(File location) {
		this.location = location;
	}

	public File getContentFile() {
		return new File(location, "contents.xcworkspacedata");
	}

	public File getBaseDirectory() {
		return location.getParentFile();
	}
}
