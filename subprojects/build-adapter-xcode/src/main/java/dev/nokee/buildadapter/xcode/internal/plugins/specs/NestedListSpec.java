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

import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EqualsAndHashCode
public final class NestedListSpec implements XCBuildSpec {
	private final List<XCBuildSpec> specs;

	NestedListSpec(List<XCBuildSpec> specs) {
		this.specs = specs;
	}

	@Override
	public XCBuildPlan resolve(ResolveContext context) {
		final List<XCBuildPlan> values = new ArrayList<>(specs.size());
		for (XCBuildSpec spec : specs) {
			values.add(spec.resolve(context));
		}
		return new CompositeXCBuildPlan(values);
	}

	@Override
	public void visit(Visitor visitor) {
		for (int i = 0; i < specs.size(); i++) {
			XCBuildSpec it = specs.get(i);
			visitor.enterContext(String.valueOf(i));
			it.visit(visitor);
			visitor.exitContext();
		}
	}

	private static final class CompositeXCBuildPlan implements XCBuildPlan, Iterable<XCBuildPlan> {
		private final List<XCBuildPlan> values;

		public CompositeXCBuildPlan(List<XCBuildPlan> values) {
			this.values = values;
		}

		@Override
		public Iterator<XCBuildPlan> iterator() {
			return values.iterator();
		}
	}
}
