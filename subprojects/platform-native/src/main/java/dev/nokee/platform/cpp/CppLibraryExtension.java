package dev.nokee.platform.cpp;

import dev.nokee.language.cpp.CppHeaderSet;
import dev.nokee.language.cpp.CppSourceSet;
import dev.nokee.platform.base.BinaryAwareComponent;
import dev.nokee.platform.base.DependencyAwareComponent;
import dev.nokee.platform.base.VariantAwareComponent;
import dev.nokee.platform.nativebase.*;

/**
 * Configuration for a library written in C++, defining the dependencies that make up the library plus other settings.
 *
 * <p>An instance of this type is added as a project extension by the C++ Library Plugin.</p>
 *
 * @since 0.4
 */
public interface CppLibraryExtension extends DependencyAwareComponent<NativeLibraryComponentDependencies>, VariantAwareComponent<NativeLibrary>, BinaryAwareComponent, TargetMachineAwareComponent, TargetLinkageAwareComponent, TargetBuildTypeAwareComponent {
	/**
	 * Defines the source files or directories of this library.
	 * You can add files or directories to this collection.
	 * When a directory is added, all source files are included for compilation.
	 *
	 * <p>When this collection is empty, the directory {@code src/main/cpp} is used by default.</p>
	 *
	 * @since 0.5
	 */
	CppSourceSet getCppSources();

	/**
	 * Defines the private headers search directories of this library.
	 *
	 * <p>When this collection is empty, the directory {@code src/main/headers} is used by default.</p>
	 *
	 * @since 0.5
	 */
	CppHeaderSet getPrivateHeaders();

	/**
	 * Defines the public header file directories of this library.
	 *
	 * <p>When this collection is empty, the directory {@code src/main/public} is used by default.</p>
	 *
	 * @since 0.5
	 */
	CppHeaderSet getPublicHeaders();
}
