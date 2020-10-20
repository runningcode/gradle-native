package dev.nokee.platform.cpp.internal.plugins

import dev.nokee.fixtures.*
import dev.nokee.language.cpp.internal.CppHeaderSetImpl
import dev.nokee.language.cpp.internal.CppSourceSetImpl
import dev.nokee.platform.base.Variant
import dev.nokee.platform.cpp.CppApplicationExtension
import dev.nokee.platform.nativebase.ExecutableBinary
import dev.nokee.platform.nativebase.NativeApplication
import dev.nokee.platform.nativebase.internal.DefaultNativeApplicationComponent
import org.gradle.api.Project
import spock.lang.Subject

trait CppApplicationPluginTestFixture {
	abstract Project getProjectUnderTest()

	String getPluginId() {
		return 'dev.nokee.cpp-application'
	}

	void applyPluginUnderTest() {
		projectUnderTest.apply plugin: pluginId
	}

	def getExtensionUnderTest() {
		return projectUnderTest.application
	}

	String getExtensionNameUnderTest() {
		return 'application'
	}

	Class getExtensionType() {
		return CppApplicationExtension
	}

	Class getVariantType() {
		return NativeApplication
	}

	String[] getExpectedVariantAwareTaskNames() {
		return ['objects', 'executable']
	}

	void configureMultipleVariants() {
		extensionUnderTest.targetMachines = [extensionUnderTest.machines.macOS, extensionUnderTest.machines.windows, extensionUnderTest.machines.linux]
	}
}

@Subject(CppApplicationPlugin)
class CppApplicationPluginTest extends AbstractPluginTest implements CppApplicationPluginTestFixture {
	final String pluginIdUnderTest = pluginId
}

@Subject(CppApplicationPlugin)
class CppApplicationComponentPluginTest extends AbstractComponentPluginTest {
	@Override
	protected Class getExtensionTypeUnderTest() {
		return CppApplicationExtension
	}

	@Override
	protected Class getComponentTypeUnderTest() {
		return DefaultNativeApplicationComponent
	}

	@Override
	protected void applyPluginUnderTests(Project project) {
		project.apply plugin: 'dev.nokee.cpp-application'
	}

	@Override
	protected List<ExpectedLanguageSourceSet> getExpectedLanguageSourceSets() {
		return [newExpectedSourceSet('cpp', CppSourceSetImpl), newExpectedSourceSet('headers', CppHeaderSetImpl, 'privateHeaders')]
	}
}

@Subject(CppApplicationPlugin)
class CppApplicationTargetMachineAwarePluginTest extends AbstractTargetMachineAwarePluginTest implements CppApplicationPluginTestFixture {
}

@Subject(CppApplicationPlugin)
class CppApplicationTaskPluginTest extends AbstractTaskPluginTest implements CppApplicationPluginTestFixture {
}

@Subject(CppApplicationPlugin)
class CppApplicationVariantPluginTest extends AbstractVariantPluginTest implements CppApplicationPluginTestFixture {
}

@Subject(CppApplicationPlugin)
class CppApplicationBinaryPluginTest extends AbstractBinaryPluginTest implements CppApplicationPluginTestFixture {
	@Override
	boolean hasExpectedBinaries(Variant variant) {
		variant.binaries.get().with { binaries ->
			assert binaries.size() == 1
			assert binaries.any { it instanceof ExecutableBinary }
		}
		return true
	}

	@Override
	boolean hasExpectedBinaries(Object extension) {
		if (extension.targetMachines.get().size() == 1) {
			extension.binaries.get().with { binaries ->
				assert binaries.size() == 1
				assert binaries.any { it instanceof ExecutableBinary }
			}
		} else {
			extension.binaries.get().with { binaries ->
				assert binaries.size() == 3
				assert binaries.count { it instanceof ExecutableBinary } == 3
			}
		}
		return true
	}
}
