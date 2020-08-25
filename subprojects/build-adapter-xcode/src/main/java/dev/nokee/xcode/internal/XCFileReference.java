package dev.nokee.xcode.internal;

import lombok.Data;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Data
@Root(name = "FileRef")
public final class XCFileReference {
	@Attribute
	String location;
}
