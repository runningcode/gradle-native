package dev.nokee.xcode;

import dev.nokee.xcode.internal.XCWorkspaceDataSerializer;
import lombok.val;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

class XCWorkspaceImpl implements XCWorkspace {
	private final XCWorkspaceLayout layout;

	public XCWorkspaceImpl(File workspaceLocation) {
		this.layout = new XCWorkspaceLayout(workspaceLocation);
	}

	@Override
	public File getLocation() {
		return layout.getLocation();
	}

	@Override
	public List<File> getProjectLocations() {
		val workspace = new XCWorkspaceDataSerializer().read(layout.getContentFile());
		val resolver = new XCFileReferenceResolver(layout.getBaseDirectory());
		return workspace.getFileRefs().stream().map(resolver::resolve).collect(Collectors.toList());
	}
}
