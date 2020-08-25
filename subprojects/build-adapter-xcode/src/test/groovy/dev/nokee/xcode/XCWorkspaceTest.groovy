package dev.nokee.xcode

import dev.nokee.buildadapter.xcode.internal.plugins.XcodeBuildAdapterPlugin
import dev.nokee.xcode.internal.XCWorkspaceData
import dev.nokee.xcode.internal.XCWorkspaceDataSerializer
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.simpleframework.xml.Serializer
import org.simpleframework.xml.core.Persister
import org.simpleframework.xml.stream.Format
import spock.lang.Specification

class XCWorkspaceTest extends Specification {
	@Rule
	TemporaryFolder temporaryFolder = new TemporaryFolder()

	private File writeWorkspace() {
		def workspace = temporaryFolder.newFolder('Foo.xcworkspace')
		def content = temporaryFolder.newFile('Foo.xcworkspace/contents.xcworkspacedata')
		content << '''<?xml version="1.0" encoding="UTF-8"?>
			|<Workspace
			|   version = "1.0">
			|   <FileRef
			|      location = "group:Foo.xcodeproj">
			|   </FileRef>
			|   <FileRef
			|      location = "group:Pods/Pods.xcodeproj">
			|   </FileRef>
			|</Workspace>
			|'''.stripMargin()

		return workspace
	}

	def "can get workspace location"() {
		given:
		def workspaceLocation = writeWorkspace()
		def workspace = XCWorkspace.open(workspaceLocation)

		expect:
		workspace.location.absolutePath == "${temporaryFolder.root}/Foo.xcworkspace"
	}

	def "can file absolute file location of Xcode project"() {
		given:
		def workspaceLocation = writeWorkspace()
		def workspace = XCWorkspace.open(workspaceLocation)

		expect:
		workspace.projectLocations*.absolutePath == ["${temporaryFolder.root}/Foo.xcodeproj", "${temporaryFolder.root}/Pods/Pods.xcodeproj"]*.toString()
	}


	def "can deserialize empty xcworkspace data"() {
		given:
		temporaryFolder.newFolder('Boogie.xcworkspace')
		def content = temporaryFolder.newFile('Boogie.xcworkspace/contents.xcworkspacedata')
		content << '''<?xml version="1.0" encoding="UTF-8"?>
			|<Workspace
			|   version = "1.0">
			|</Workspace>
			|'''.stripMargin()

		when:
		def workspace = new XCWorkspaceDataSerializer().read(content)

		then:
		noExceptionThrown()

		and:
		workspace.fileRefs.empty
	}

	def "can deserialize xcworkspace data"() {
		given:
		temporaryFolder.newFolder('Boogie.xcworkspace')
		def content = temporaryFolder.newFile('Boogie.xcworkspace/contents.xcworkspacedata')
		content << '''<?xml version="1.0" encoding="UTF-8"?>
			|<Workspace
			|   version = "1.0">
			|   <FileRef
			|      location = "group:Foo.xcodeproj">
			|   </FileRef>
			|   <FileRef
			|      location = "group:Pods/Pods.xcodeproj">
			|   </FileRef>
			|</Workspace>
			|'''.stripMargin()

		when:
		def workspace = new XCWorkspaceDataSerializer().read(content)

		then:
		noExceptionThrown()

		and:
		workspace.fileRefs*.location == ['group:Foo.xcodeproj', 'group:Pods/Pods.xcodeproj']
	}
}
