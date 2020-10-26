package dev.nokee.platform.base.internal

import dev.nokee.model.internal.ProjectIdentifier
import dev.nokee.platform.base.Component
import dev.nokee.platform.base.Variant
import spock.lang.Specification

class VariantIdentifierTest extends Specification {
	def "can build identifier using factory method"() {
		given:
		def ownerIdentifier = ComponentIdentifier.ofMain(TestableComponent, ProjectIdentifier.of('root'))

		when:
		def identifier = VariantIdentifier.of('macosDebug', TestableVariant, ownerIdentifier)

		then:
		identifier.unambiguousName == 'macosDebug'
		identifier.type == TestableVariant
		identifier.componentIdentifier == ownerIdentifier
	}

	def "can build identifier from single ambiguous variant dimension using the builder"() {
		given:
		def ownerIdentifier = ComponentIdentifier.ofMain(TestableComponent, ProjectIdentifier.of('root'))

		when:
		def identifier = VariantIdentifier.builder().withComponentIdentifier(ownerIdentifier)
			.withType(TestableVariant)
			.withVariantDimension({'debug'}, [{'debug'}, {'release'}])
			.build()

		then:
		identifier.unambiguousName == 'debug'
		identifier.type == TestableVariant
		identifier.componentIdentifier == ownerIdentifier
	}

	def "can build identifier from multiple ambiguous variant dimension using the builder"() {
		given:
		def ownerIdentifier = ComponentIdentifier.ofMain(TestableComponent, ProjectIdentifier.of('root'))

		when:
		def identifier = VariantIdentifier.builder().withComponentIdentifier(ownerIdentifier)
			.withType(TestableVariant)
			.withVariantDimension({'macos'}, [{'macos'}, {'windows'}])
			.withVariantDimension({'debug'}, [{'debug'}, {'release'}])
			.build()

		then:
		identifier.unambiguousName == 'macosDebug'
		identifier.type == TestableVariant
		identifier.componentIdentifier == ownerIdentifier
	}

	def "can build identifier from only one ambiguous variant dimension using the builder"() {
		given:
		def ownerIdentifier = ComponentIdentifier.ofMain(TestableComponent, ProjectIdentifier.of('root'))

		when:
		def identifier = VariantIdentifier.builder().withComponentIdentifier(ownerIdentifier)
			.withType(TestableVariant)
			.withVariantDimension({'macos'}, [{'macos'}])
			.withVariantDimension({'debug'}, [{'debug'}, {'release'}])
			.build()

		then:
		identifier.unambiguousName == 'debug'
		identifier.type == TestableVariant
		identifier.componentIdentifier == ownerIdentifier
	}

	def "can query the full identifier name"() {
		given:
		def ownerIdentifier = ComponentIdentifier.ofMain(TestableComponent, ProjectIdentifier.of('root'))

		expect:
		VariantIdentifier.builder().withComponentIdentifier(ownerIdentifier)
			.withType(TestableVariant)
			.withVariantDimension({'macos'}, [{'macos'}])
			.withVariantDimension({'debug'}, [{'debug'}, {'release'}])
			.build().fullName == 'macosDebug'

		VariantIdentifier.builder().withComponentIdentifier(ownerIdentifier)
			.withType(TestableVariant)
			.withVariantDimension({'macos'}, [{'macos'}, {'windows'}])
			.withVariantDimension({'debug'}, [{'debug'}, {'release'}])
			.build().fullName == 'macosDebug'

		VariantIdentifier.builder().withComponentIdentifier(ownerIdentifier)
			.withType(TestableVariant)
			.build().fullName == ''
	}

	def "can query the ambiguous dimension list of identifier"() {
		given:
		def ownerIdentifier = ComponentIdentifier.ofMain(TestableComponent, ProjectIdentifier.of('root'))

		expect:
		VariantIdentifier.builder().withComponentIdentifier(ownerIdentifier)
			.withType(TestableVariant)
			.withVariantDimension({'macos'}, [{'macos'}])
			.withVariantDimension({'debug'}, [{'debug'}, {'release'}])
			.build().ambiguousDimensions.get() == ['debug']

		VariantIdentifier.builder().withComponentIdentifier(ownerIdentifier)
			.withType(TestableVariant)
			.withVariantDimension({'macos'}, [{'macos'}, {'windows'}])
			.withVariantDimension({'debug'}, [{'debug'}, {'release'}])
			.build().ambiguousDimensions.get() == ['macos', 'debug']

		VariantIdentifier.builder().withComponentIdentifier(ownerIdentifier)
			.withType(TestableVariant)
			.build().ambiguousDimensions.get() == []
	}

	def "can build identifier from only one unambiguous variant dimension using the builder"() {
		given:
		def ownerIdentifier = ComponentIdentifier.ofMain(TestableComponent, ProjectIdentifier.of('root'))

		when:
		def identifier = VariantIdentifier.builder().withComponentIdentifier(ownerIdentifier)
			.withType(TestableVariant)
			.withVariantDimension({'macos'}, [{'macos'}])
			.build()

		then:
		identifier.unambiguousName == ''
		identifier.type == TestableVariant
		identifier.componentIdentifier == ownerIdentifier
	}

	def "two identifiers with the same unambiguous name built from different dimension are not equals"() {
		given:
		def ownerIdentifier = ComponentIdentifier.ofMain(TestableComponent, ProjectIdentifier.of('root'))
		def singleDimension = VariantIdentifier.builder().withComponentIdentifier(ownerIdentifier)
			.withType(TestableVariant)
			.withVariantDimension({'debug'}, [{'debug'}, {'release'}])
			.build()
		def multipleDimension = VariantIdentifier.builder().withComponentIdentifier(ownerIdentifier)
			.withType(TestableVariant)
			.withVariantDimension({'macos'}, [{'macos'}])
			.withVariantDimension({'debug'}, [{'debug'}, {'release'}])
			.build()

		expect:
		singleDimension != multipleDimension
	}

	def "two identifier with the same unambiguous name built from the same dimension are equals"() {
		given:
		def ownerIdentifier = ComponentIdentifier.ofMain(TestableComponent, ProjectIdentifier.of('root'))
		def identifier1 = VariantIdentifier.builder().withComponentIdentifier(ownerIdentifier)
			.withType(TestableVariant)
			.withVariantDimension({'macos'}, [{'macos'}])
			.withVariantDimension({'debug'}, [{'debug'}, {'release'}])
			.build()
		def identifier2 = VariantIdentifier.builder().withComponentIdentifier(ownerIdentifier)
			.withType(TestableVariant)
			.withVariantDimension({'macos'}, [{'macos'}])
			.withVariantDimension({'debug'}, [{'debug'}, {'release'}])
			.build()

		expect:
		identifier1 == identifier2
	}

	def "two identifier build from names with the same unambiguous name are considered equals"() {
		given:
		def ownerIdentifier = ComponentIdentifier.ofMain(TestableComponent, ProjectIdentifier.of('root'))
		def identifier1 = VariantIdentifier.of('macosDebug', TestableVariant, ownerIdentifier)
		def identifier2 = VariantIdentifier.of('macosDebug', TestableVariant, ownerIdentifier)

		expect:
		identifier1 == identifier2
	}

	def "two identifiers resulting to the same unambiguous name where one has all dimensions participated to the name and the other is built directly from name are considered equals"() {
		given:
		def ownerIdentifier = ComponentIdentifier.ofMain(TestableComponent, ProjectIdentifier.of('root'))
		def identifier1 = VariantIdentifier.of('macosDebug', TestableVariant, ownerIdentifier)
		def identifier2 = VariantIdentifier.builder().withComponentIdentifier(ownerIdentifier)
			.withType(TestableVariant)
			.withVariantDimension({'macos'}, [{'macos'}, {'windows'}])
			.withVariantDimension({'debug'}, [{'debug'}, {'release'}])
			.build()

		expect:
		identifier1 == identifier2
	}

	def "computes display name from unambiguous name and component's display name for non-main component"() {
		given:
		def mainOwnerIdentifier = ComponentIdentifier.ofMain(TestableComponent, ProjectIdentifier.of('root'))
		def testOwnerIdentifier = ComponentIdentifier.of(ComponentName.of('test'), TestableComponent, ProjectIdentifier.of('root'))
		def integTestOwnerIdentifier = ComponentIdentifier.of(ComponentName.of('integTest'), TestableComponent, ProjectIdentifier.of('root'))

		expect:
        VariantIdentifier.of('macosDebug', TestableVariant, mainOwnerIdentifier).displayName == "variant 'macosDebug'"
        VariantIdentifier.of('macosDebug', TestableVariant, testOwnerIdentifier).displayName == "variant 'macosDebug' of component 'test'"
        VariantIdentifier.of('macosDebug', TestableVariant, integTestOwnerIdentifier).displayName == "variant 'macosDebug' of component 'integTest'"
        VariantIdentifier.of('debug', TestableVariant, mainOwnerIdentifier).displayName == "variant 'debug'"
	}

	def "includes component custom display name for main component"() {
		given:
		def jniOwnerIdentifier = ComponentIdentifier.builder().withName(ComponentName.of('main')).withType(TestableComponent).withProjectIdentifier(ProjectIdentifier.of('root')).withDisplayName('JNI library').build()
		def iosOwnerIdentifier = ComponentIdentifier.builder().withName(ComponentName.of('main')).withType(TestableComponent).withProjectIdentifier(ProjectIdentifier.of('root')).withDisplayName('iOS application').build()

		expect:
        VariantIdentifier.of('macosDebug', TestableVariant, jniOwnerIdentifier).displayName == "variant 'macosDebug' of JNI library"
        VariantIdentifier.of('debug', TestableVariant, iosOwnerIdentifier).displayName == "variant 'debug' of iOS application"
	}

	def "uses component display name for empty unambiguous name"() {
		def mainOwnerIdentifier = ComponentIdentifier.ofMain(TestableComponent, ProjectIdentifier.of('root'))
		def testOwnerIdentifier = ComponentIdentifier.of(ComponentName.of('test'), TestableComponent, ProjectIdentifier.of('root'))
		def jniOwnerIdentifier = ComponentIdentifier.builder().withName(ComponentName.of('main')).withType(TestableComponent).withProjectIdentifier(ProjectIdentifier.of('root')).withDisplayName('JNI library').build()
		def iosOwnerIdentifier = ComponentIdentifier.builder().withName(ComponentName.of('main')).withType(TestableComponent).withProjectIdentifier(ProjectIdentifier.of('root')).withDisplayName('iOS application').build()

        VariantIdentifier.of('', TestableVariant, mainOwnerIdentifier).displayName == "main component"
        VariantIdentifier.of('', TestableVariant, testOwnerIdentifier).displayName == "component 'test'"
        VariantIdentifier.of('', TestableVariant, jniOwnerIdentifier).displayName == "JNI library"
        VariantIdentifier.of('', TestableVariant, jniOwnerIdentifier).displayName == "iOS application"
	}

	def "has meaningful toString() implementation"() {
		given:
		def ownerIdentifier = ComponentIdentifier.ofMain(TestableComponent, ProjectIdentifier.of('root'))
		def singleDimension = VariantIdentifier.builder().withComponentIdentifier(ownerIdentifier)
			.withType(TestableVariant)
			.withVariantDimension({'debug'}, [{'debug'}, {'release'}])
			.build()
		def multipleDimension = VariantIdentifier.builder().withComponentIdentifier(ownerIdentifier)
			.withType(TestableVariant)
			.withVariantDimension({'macos'}, [{'macos'}])
			.withVariantDimension({'debug'}, [{'debug'}, {'release'}])
			.build()

		expect:
		singleDimension.toString() == "variant ':root:main:debug' (${TestableVariant.simpleName})"
		multipleDimension.toString() == "variant ':root:main:macosDebug' (${TestableVariant.simpleName})"
	}

	interface TestableComponent extends Component {}
	interface TestableVariant extends Variant {}
}
