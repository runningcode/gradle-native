package dev.nokee.xcode

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class XCWorkspaceLocatorTest extends Specification {
	@Rule
	TemporaryFolder temporaryFolder = new TemporaryFolder()

	def "returns empty list when no workspace found"() {
		given:
		def subject = new XCWorkspaceLocator()

		expect:
		subject.findWorkspaces(temporaryFolder.root).empty
	}

	def "can find a workspace"() {
		given:
		def subject = new XCWorkspaceLocator()

		and:
		writeWorkspace()

		expect:
		subject.findWorkspaces(temporaryFolder.root).size()
	}

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
}
