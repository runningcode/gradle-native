package dev.nokee.xcode.internal;

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

import java.io.File;

public final class XCWorkspaceDataSerializer {
	public XCWorkspaceData read(File workspaceDataFile) {
		try {
			Serializer serializer = new Persister();
			return serializer.read(XCWorkspaceData.class, workspaceDataFile, false);
		} catch (Exception e) {
			throw new RuntimeException(String.format("Could not read Xcode workspace data at '%s' because of a parsing error.", workspaceDataFile.getAbsolutePath()), e);
		}
	}
}
