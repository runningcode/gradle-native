package dev.nokee.xcode.internal;

import lombok.Data;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

import java.util.List;

@Data
@Root(name = "Workspace")
public final class XCWorkspaceData {
	@Attribute
	String version;

	@ElementList(inline = true, required = false, empty = false)
	List<XCFileReference> fileRefs;
}
