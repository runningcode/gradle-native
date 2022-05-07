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
package dev.nokee.model.internal.names;

import dev.nokee.model.internal.core.ModelNode;
import lombok.val;

import java.util.function.BiConsumer;

import static dev.nokee.model.internal.tags.ModelTags.typeOf;

final class QualifyingNameAccumulator implements BiConsumer<QualifyingName.Builder, ModelNode> {
	@Override
	public void accept(QualifyingName.Builder builder, ModelNode parentEntity) {
		val parentElementName = parentEntity.find(ElementNameComponent.class).map(ElementNameComponent::get).map(ElementName::toString);
		if (!parentEntity.hasComponent(typeOf(ExcludeFromQualifyingNameTag.class))) {
			parentElementName.ifPresent(builder::prepend);
		}
	}
}
