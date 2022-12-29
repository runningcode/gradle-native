/*
 * Copyright 2022 the original author or authors.
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
package dev.nokee.xcode.project.coders;

import com.google.common.collect.Iterators;
import dev.nokee.internal.testing.invocations.HasInvocationResults;
import dev.nokee.internal.testing.invocations.InvocationResult2;
import dev.nokee.xcode.project.ValueDecoder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class ConstantDecoder<OUT, IN> implements ValueDecoder<OUT, IN>, HasInvocationResults<InvocationResult2<IN, ValueDecoder.Context>> {
	private final List<InvocationResult2<IN, Context>> invocations = new ArrayList<>();
	private final OUT returnValue;

	public ConstantDecoder(OUT returnValue) {
		this.returnValue = returnValue;
	}

	@Override
	public List<InvocationResult2<IN, Context>> getAllInvocations() {
		return invocations;
	}

	@Override
	public OUT decode(IN ignored, Context context) {
		invocations.add(new InvocationResult2<IN, Context>() {
			@Override
			public Iterator<Object> iterator() {
				return Iterators.forArray(ignored, context);
			}
		});
		return returnValue;
	}

	@Override
	public CoderType<?> getDecodeType() {
		return CoderType.of(returnValue.getClass());
	}
}
