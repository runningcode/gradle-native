/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.nokee.buildadapter.xcode.internal.plugins.specs;

import dev.nokee.xcode.objects.files.PBXReference;
import dev.nokee.xcode.project.CodeablePBXAggregateTarget;
import dev.nokee.xcode.project.CodeablePBXBuildFile;
import dev.nokee.xcode.project.CodeablePBXContainerItemProxy;
import dev.nokee.xcode.project.CodeablePBXCopyFilesBuildPhase;
import dev.nokee.xcode.project.CodeablePBXFileReference;
import dev.nokee.xcode.project.CodeablePBXFrameworksBuildPhase;
import dev.nokee.xcode.project.CodeablePBXGroup;
import dev.nokee.xcode.project.CodeablePBXHeadersBuildPhase;
import dev.nokee.xcode.project.CodeablePBXLegacyTarget;
import dev.nokee.xcode.project.CodeablePBXNativeTarget;
import dev.nokee.xcode.project.CodeablePBXProject;
import dev.nokee.xcode.project.CodeablePBXReferenceProxy;
import dev.nokee.xcode.project.CodeablePBXResourcesBuildPhase;
import dev.nokee.xcode.project.CodeablePBXShellScriptBuildPhase;
import dev.nokee.xcode.project.CodeablePBXTargetDependency;
import dev.nokee.xcode.project.CodeablePBXVariantGroup;
import dev.nokee.xcode.project.CodeableProjectReference;
import dev.nokee.xcode.project.CodeableVersionRequirementBranch;
import dev.nokee.xcode.project.CodeableVersionRequirementExact;
import dev.nokee.xcode.project.CodeableVersionRequirementRange;
import dev.nokee.xcode.project.CodeableVersionRequirementRevision;
import dev.nokee.xcode.project.CodeableVersionRequirementUpToNextMajorVersion;
import dev.nokee.xcode.project.CodeableVersionRequirementUpToNextMinorVersion;
import dev.nokee.xcode.project.CodeableXCBuildConfiguration;
import dev.nokee.xcode.project.CodeableXCConfigurationList;
import dev.nokee.xcode.project.CodeableXCRemoteSwiftPackageReference;
import dev.nokee.xcode.project.CodeableXCSwiftPackageProductDependency;
import dev.nokee.xcode.project.CodeableXCVersionGroup;
import dev.nokee.xcode.project.CodingKey;
import dev.nokee.xcode.project.KeyedCoder;
import dev.nokee.xcode.project.KeyedCoders;
import dev.nokee.xcode.project.ValueEncoder;
import dev.nokee.xcode.project.coders.FieldCoder;
import dev.nokee.xcode.project.coders.ListEncoder;
import dev.nokee.xcode.project.coders.NoOpEncoder;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.spotify.hamcrest.optional.OptionalMatchers.emptyOptional;
import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class XCBuildSpecCodingKeyCodersTests {
	XCBuildSpecCodingKeyCoders subject = new XCBuildSpecCodingKeyCoders();

	@ParameterizedTest
	@ArgumentsSource(CodingKeysProvider.class)
	void checkCoderForCodingKeys(CodingKey key, Matcher<Optional<KeyedCoder<?>>> expected) {
		assertThat(subject.get(key), expected);
	}

	private static final class CodingKeysProvider implements ArgumentsProvider {
		private static final Collection<Arguments> ARGUMENTS = Collections.unmodifiableCollection(new ArrayList<Arguments>() {{
			add(arguments(KeyedCoders.ISA, ignore()));

			add(arguments(CodeablePBXAggregateTarget.CodingKeys.name, ignore()));
			add(arguments(CodeablePBXAggregateTarget.CodingKeys.productName, ignore()));
			add(arguments(CodeablePBXAggregateTarget.CodingKeys.productType, ignore()));
			add(arguments(CodeablePBXAggregateTarget.CodingKeys.productReference, ignore()));
			add(arguments(CodeablePBXAggregateTarget.CodingKeys.dependencies, ignore())); // not required (2)
			add(arguments(CodeablePBXAggregateTarget.CodingKeys.buildConfigurationList, ignore()));
			add(arguments(CodeablePBXAggregateTarget.CodingKeys.buildPhases, keyOf("buildPhases", listOf(object(/*PBXBuildPhase*/)))));

			add(arguments(CodeablePBXBuildFile.CodingKeys.fileRef, keyOf("fileRef", inputLocation(resolvablePaths()))));
			add(arguments(CodeablePBXBuildFile.CodingKeys.productRef, ignore()));
			add(arguments(CodeablePBXBuildFile.CodingKeys.settings, ignore()));

			add(arguments(CodeablePBXContainerItemProxy.CodingKeys.containerPortal, ignore()));
			add(arguments(CodeablePBXContainerItemProxy.CodingKeys.proxyType, ignore()));
			add(arguments(CodeablePBXContainerItemProxy.CodingKeys.remoteGlobalIDString, ignore()));
			add(arguments(CodeablePBXContainerItemProxy.CodingKeys.remoteInfo, ignore()));

			add(arguments(CodeablePBXCopyFilesBuildPhase.CodingKeys.files, keyOf("files", listOf(object(/*PBXBuildFile*/)))));
			add(arguments(CodeablePBXCopyFilesBuildPhase.CodingKeys.dstSubfolderSpec, ignore()));
			add(arguments(CodeablePBXCopyFilesBuildPhase.CodingKeys.dstPath, ignore()));

			add(arguments(CodeablePBXFileReference.CodingKeys.name, ignore()));
			add(arguments(CodeablePBXFileReference.CodingKeys.path, ignore()));
			add(arguments(CodeablePBXFileReference.CodingKeys.sourceTree, ignore()));
			add(arguments(CodeablePBXFileReference.CodingKeys.explicitFileType, ignore()));
			add(arguments(CodeablePBXFileReference.CodingKeys.lastKnownFileType, ignore()));

			add(arguments(CodeablePBXFrameworksBuildPhase.CodingKeys.files, keyOf("files", listOf(object(/*PBXBuildFile*/)))));

			add(arguments(CodeablePBXGroup.CodingKeys.name, ignore()));
			add(arguments(CodeablePBXGroup.CodingKeys.path, ignore()));
			add(arguments(CodeablePBXGroup.CodingKeys.sourceTree, ignore()));
			add(arguments(CodeablePBXGroup.CodingKeys.children, ignore()));

			add(arguments(CodeablePBXHeadersBuildPhase.CodingKeys.files, keyOf("files", listOf(object(/*PBXBuildFile*/)))));

			add(arguments(CodeablePBXLegacyTarget.CodingKeys.name, ignore()));
			add(arguments(CodeablePBXLegacyTarget.CodingKeys.productName, ignore()));
			add(arguments(CodeablePBXLegacyTarget.CodingKeys.productType, ignore()));
			add(arguments(CodeablePBXLegacyTarget.CodingKeys.productReference, ignore()));
			add(arguments(CodeablePBXLegacyTarget.CodingKeys.dependencies, ignore())); // not required (2)
			add(arguments(CodeablePBXLegacyTarget.CodingKeys.buildConfigurationList, ignore()));
			add(arguments(CodeablePBXLegacyTarget.CodingKeys.buildPhases, keyOf("buildPhases", listOf(object(/*PBXBuildPhase*/)))));
			add(arguments(CodeablePBXLegacyTarget.CodingKeys.buildToolPath, ignore()));
			add(arguments(CodeablePBXLegacyTarget.CodingKeys.buildArgumentsString, ignore()));
			add(arguments(CodeablePBXLegacyTarget.CodingKeys.buildWorkingDirectory, ignore()));
			add(arguments(CodeablePBXLegacyTarget.CodingKeys.passBuildSettingsInEnvironment, ignore()));

			add(arguments(CodeablePBXNativeTarget.CodingKeys.name, ignore()));
			add(arguments(CodeablePBXNativeTarget.CodingKeys.productName, ignore()));
			add(arguments(CodeablePBXNativeTarget.CodingKeys.productType, ignore()));
			add(arguments(CodeablePBXNativeTarget.CodingKeys.productReference, ignore()));
			add(arguments(CodeablePBXNativeTarget.CodingKeys.dependencies, ignore())); // not required (2)
			add(arguments(CodeablePBXNativeTarget.CodingKeys.buildConfigurationList, ignore()));
			add(arguments(CodeablePBXNativeTarget.CodingKeys.buildPhases, keyOf("buildPhases", listOf(object(/*PBXBuildPhase*/)))));
			add(arguments(CodeablePBXNativeTarget.CodingKeys.packageProductDependencies, ignore()));

			add(arguments(CodeablePBXProject.CodingKeys.mainGroup, ignore())); // not required (1)
			add(arguments(CodeablePBXProject.CodingKeys.projectReferences, ignore())); // not required (1)
			add(arguments(CodeablePBXProject.CodingKeys.buildConfigurationList, ignore())); // not required (1)
			add(arguments(CodeablePBXProject.CodingKeys.packageReferences, ignore())); // not required (1)
			add(arguments(CodeablePBXProject.CodingKeys.compatibilityVersion, ignore())); // not required (1)
			add(arguments(CodeablePBXProject.CodingKeys.targets, ignore())); // not required (1)

			add(arguments(CodeablePBXReferenceProxy.CodingKeys.name, ignore()));
			add(arguments(CodeablePBXReferenceProxy.CodingKeys.path, ignore()));
			add(arguments(CodeablePBXReferenceProxy.CodingKeys.sourceTree, ignore()));
			add(arguments(CodeablePBXReferenceProxy.CodingKeys.remoteRef, ignore()));
			add(arguments(CodeablePBXReferenceProxy.CodingKeys.fileType, ignore()));

			add(arguments(CodeablePBXResourcesBuildPhase.CodingKeys.files, keyOf("files", listOf(object(/*PBXBuildFile*/)))));

			add(arguments(CodeablePBXShellScriptBuildPhase.CodingKeys.files, keyOf("files", listOf(object(/*PBXBuildFile*/)))));
			add(arguments(CodeablePBXShellScriptBuildPhase.CodingKeys.name, ignore()));
			add(arguments(CodeablePBXShellScriptBuildPhase.CodingKeys.shellPath, ignore()));
			add(arguments(CodeablePBXShellScriptBuildPhase.CodingKeys.shellScript, ignore()));
			add(arguments(CodeablePBXShellScriptBuildPhase.CodingKeys.inputPaths, keyOf("inputPaths", listOf(inputLocation(resolvablePaths())))));
			add(arguments(CodeablePBXShellScriptBuildPhase.CodingKeys.inputFileListPaths, ignore())); // TODO: implement support
			add(arguments(CodeablePBXShellScriptBuildPhase.CodingKeys.outputPaths, ignore()));
			add(arguments(CodeablePBXShellScriptBuildPhase.CodingKeys.outputFileListPaths, ignore())); // TODO: implement support

			add(arguments(CodeablePBXResourcesBuildPhase.CodingKeys.files, keyOf("files", listOf(object(/*PBXBuildFile*/)))));

			add(arguments(CodeablePBXTargetDependency.CodingKeys.name, ignore())); // not required (2)
			add(arguments(CodeablePBXTargetDependency.CodingKeys.target, ignore())); // not required (2)
			add(arguments(CodeablePBXTargetDependency.CodingKeys.targetProxy, ignore())); // not required (2)

			add(arguments(CodeablePBXVariantGroup.CodingKeys.name, ignore()));
			add(arguments(CodeablePBXVariantGroup.CodingKeys.path, ignore()));
			add(arguments(CodeablePBXVariantGroup.CodingKeys.sourceTree, ignore()));
			add(arguments(CodeablePBXVariantGroup.CodingKeys.children, ignore()));

			add(arguments(CodeableProjectReference.CodingKeys.ProjectRef, ignore()));
			add(arguments(CodeableProjectReference.CodingKeys.ProductGroup, ignore()));

			add(arguments(CodeableVersionRequirementBranch.CodingKeys.kind, ignore()));
			add(arguments(CodeableVersionRequirementBranch.CodingKeys.branch, ignore()));

			add(arguments(CodeableVersionRequirementExact.CodingKeys.kind, ignore()));
			add(arguments(CodeableVersionRequirementExact.CodingKeys.version, ignore()));

			add(arguments(CodeableVersionRequirementRange.CodingKeys.kind, ignore()));
			add(arguments(CodeableVersionRequirementRange.CodingKeys.minimumVersion, ignore()));
			add(arguments(CodeableVersionRequirementRange.CodingKeys.maximumVersion, ignore()));

			add(arguments(CodeableVersionRequirementRevision.CodingKeys.kind, ignore()));
			add(arguments(CodeableVersionRequirementRevision.CodingKeys.revision, ignore()));

			add(arguments(CodeableVersionRequirementUpToNextMajorVersion.CodingKeys.kind, ignore()));
			add(arguments(CodeableVersionRequirementUpToNextMajorVersion.CodingKeys.minimumVersion, ignore()));

			add(arguments(CodeableVersionRequirementUpToNextMinorVersion.CodingKeys.kind, ignore()));
			add(arguments(CodeableVersionRequirementUpToNextMinorVersion.CodingKeys.minimumVersion, ignore()));

			add(arguments(CodeableXCBuildConfiguration.CodingKeys.name, ignore()));
			add(arguments(CodeableXCBuildConfiguration.CodingKeys.baseConfigurationReference, ignore()));
			add(arguments(CodeableXCBuildConfiguration.CodingKeys.buildSettings, ignore()));

			add(arguments(CodeableXCConfigurationList.CodingKeys.buildConfigurations, ignore()));
			add(arguments(CodeableXCConfigurationList.CodingKeys.defaultConfigurationIsVisible, ignore()));
			add(arguments(CodeableXCConfigurationList.CodingKeys.defaultConfigurationName, ignore()));

			add(arguments(CodeableXCRemoteSwiftPackageReference.CodingKeys.repositoryUrl, ignore()));
			add(arguments(CodeableXCRemoteSwiftPackageReference.CodingKeys.requirement, ignore()));

			add(arguments(CodeableXCSwiftPackageProductDependency.CodingKeys.productName, ignore()));
			add(arguments(CodeableXCSwiftPackageProductDependency.CodingKeys.packageReference, ignore()));

			add(arguments(CodeableXCVersionGroup.CodingKeys.name, ignore()));
			add(arguments(CodeableXCVersionGroup.CodingKeys.path, ignore()));
			add(arguments(CodeableXCVersionGroup.CodingKeys.sourceTree, ignore()));
			add(arguments(CodeableXCVersionGroup.CodingKeys.children, ignore()));
			add(arguments(CodeableXCVersionGroup.CodingKeys.currentVersion, ignore()));
			add(arguments(CodeableXCVersionGroup.CodingKeys.versionGroupType, ignore()));
			add(arguments(CodeableXCVersionGroup.CodingKeys.versionGroupType, ignore()));
		}});

		@Override
		public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
			return ARGUMENTS.stream();
		}
	}

	// Note 1: We don't need PBXProject objects for target encoding
	// Note 2: We track target dependencies differently (aka via input files)

	private static <T> Matcher<Optional<? extends KeyedCoder<T>>> keyOf(String key, ValueEncoder<XCBuildSpec, T> encoder) {
		return optionalWithValue(equalTo(new FieldCoder<>(key, new EncoderOnlyCoder<>(encoder))));
	}

	private static <T> Matcher<Optional<KeyedCoder<T>>> ignore() {
		return emptyOptional();
	}

	private static ValueEncoder<PBXReference, ?> resolvablePaths() {
		return new NormalizeStringAsPBXReferenceEncoder(new NormalizePBXBuildFileFileReferenceAsPBXReferenceEncoder(new ThrowingValueEncoder<>()));
	}

	private static ValueEncoder<XCBuildSpec, ?> inputLocation(ValueEncoder<PBXReference, ?> encoder) {
		return new InputLocationSpecEncoder<>(encoder);
	}

	private static ValueEncoder<XCBuildSpec, ?> object() {
		return new MapSpecEncoder<>(new NoOpEncoder<>());
	}

	public static <E> ValueEncoder<XCBuildSpec, List<E>> listOf(ValueEncoder<XCBuildSpec, E> elementEncoder) {
		return new ListSpecEncoder<>(new ListEncoder<>(elementEncoder));
	}
}
