package dev.nokee.platform.jni


import dev.nokee.language.cpp.CppTaskNames
import dev.nokee.platform.jni.fixtures.JavaJniCppGreeterLib

import static org.hamcrest.CoreMatchers.containsString

class JavaCppJniLibraryFunctionalTest extends AbstractJavaJniLibraryFunctionalTest implements CppTaskNames {
	def "build fails when C++ compilation fails"() {
        given:
        makeSingleProject()

        and:
        file("src/main/cpp/broken.cpp") << "broken!"

        expect:
        fails "assemble"
        failure.assertHasDescription("Execution failed for task '${tasks.compile}'.")
        failure.assertHasCause("A build operation failed.")
        failure.assertThatCause(containsString("C++ compiler failed while compiling broken.cpp"))
    }


//    def "can compile and link against a library"() {
// TODO: JNI Plugin should allow to work alone by embedding the runtime native dependencies into a JAR. It should also create an empty JAR when no language (not even Java). This Jar should be exposed via API outgoing dependencies.

	@Override
	protected void makeSingleProject() {
        buildFile << '''
            plugins {
                id 'java'
                id 'dev.nokee.jni-library'
                id 'dev.nokee.cpp-language'
            }
        '''
		settingsFile << "rootProject.name = 'jni-greeter'"
	}

	@Override
	JavaJniCppGreeterLib getComponentUnderTest() {
        return new JavaJniCppGreeterLib('jni-greeter')
    }

	@Override
	protected String getNativePluginId() {
		return 'dev.nokee.cpp-language'
	}
}
