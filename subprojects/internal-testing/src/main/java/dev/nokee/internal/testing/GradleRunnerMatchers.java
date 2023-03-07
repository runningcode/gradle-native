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
package dev.nokee.internal.testing;

import dev.gradleplugins.runnerkit.BuildTask;
import dev.gradleplugins.runnerkit.TaskOutcome;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

// TODO: Should move to Gradle Runner in toolbox
public final class GradleRunnerMatchers {
	public static Matcher<BuildTask> skipped() {
		return outcome(not(anyOf(equalTo(TaskOutcome.SUCCESS), equalTo(TaskOutcome.FAILED))));
	}

	public static Matcher<BuildTask> succeed() {
		return outcome(equalTo(TaskOutcome.SUCCESS));
	}

	public static Matcher<BuildTask> upToDate() {
		return outcome(equalTo(TaskOutcome.UP_TO_DATE));
	}

	public static Matcher<BuildTask> outOfDate() {
		return outcome(equalTo(TaskOutcome.SUCCESS));
	}

	public static Matcher<BuildTask> outcome(Matcher<? super TaskOutcome> matcher) {
		return new FeatureMatcher<BuildTask, TaskOutcome>(matcher, "", "") {
			@Override
			protected TaskOutcome featureValueOf(BuildTask actual) {
				return actual.getOutcome();
			}
		};
	}
}
