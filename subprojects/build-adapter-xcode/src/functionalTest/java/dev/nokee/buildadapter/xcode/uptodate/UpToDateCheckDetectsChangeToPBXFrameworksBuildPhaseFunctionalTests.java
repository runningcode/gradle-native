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
package dev.nokee.buildadapter.xcode.uptodate;

import dev.nokee.xcode.objects.buildphase.PBXFrameworksBuildPhase;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import java.io.IOException;
import java.nio.file.Path;

import static com.google.common.base.Predicates.instanceOf;
import static dev.nokee.buildadapter.xcode.PBXProjectTestUtils.add;
import static dev.nokee.buildadapter.xcode.PBXProjectTestUtils.buildFileToProduct;
import static dev.nokee.buildadapter.xcode.PBXProjectTestUtils.buildPhases;
import static dev.nokee.buildadapter.xcode.PBXProjectTestUtils.dependencies;
import static dev.nokee.buildadapter.xcode.PBXProjectTestUtils.files;
import static dev.nokee.buildadapter.xcode.PBXProjectTestUtils.matching;
import static dev.nokee.buildadapter.xcode.PBXProjectTestUtils.mutateProject;
import static dev.nokee.buildadapter.xcode.PBXProjectTestUtils.targetDependencyTo;
import static dev.nokee.buildadapter.xcode.PBXProjectTestUtils.targetNamed;
import static dev.nokee.internal.testing.GradleRunnerMatchers.outOfDate;
import static java.nio.file.Files.delete;
import static org.hamcrest.MatcherAssert.assertThat;

@EnabledOnOs(OS.MAC)
class UpToDateCheckDetectsChangeToPBXFrameworksBuildPhaseFunctionalTests extends UpToDateCheckSpec {
	void setup(Path location) {
		mutateProject(targetNamed("App", dependencies(add(targetDependencyTo("Common"))))).accept(location);
		mutateProject(targetNamed("App", buildPhases(matching(instanceOf(PBXFrameworksBuildPhase.class), files(add(buildFileToProduct("Common.framework"))))))).accept(location);
	}

	@Test
	void outOfDateWhenConsumedFrameworkRebuild() throws IOException {
		appendMeaningfulChangeToCFile(testDirectory.resolve("Common/Common.c"));

		assertThat(executer.build().task(":UpToDateCheck:AppDebug"), outOfDate());
	}

	@Disabled("outputs are not yet tracked")
	@Test
	void outOfDateWhenConsumedFrameworkChange() throws IOException {
		delete(appDebugProductsDirectory().resolve("Common.framework/Common"));

		assertThat(executer.build().task(":UpToDateCheck:AppDebug"), outOfDate());
	}
}