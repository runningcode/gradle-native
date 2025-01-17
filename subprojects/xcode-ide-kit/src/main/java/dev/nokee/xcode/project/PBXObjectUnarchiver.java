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
package dev.nokee.xcode.project;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Streams;
import dev.nokee.xcode.objects.PBXObject;
import dev.nokee.xcode.objects.PBXProject;
import lombok.val;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.Streams.stream;
import static java.util.function.Function.identity;

public final class PBXObjectUnarchiver {
	private final Map<String, PBXObjectCoder<?>> coders;

	public PBXObjectUnarchiver() {
		this(ImmutableList.copyOf(PBXObjectCoders.values()));
	}

	public PBXObjectUnarchiver(Iterable<PBXObjectCoder<?>> coders) {
		this.coders = stream(coders).collect(ImmutableMap.toImmutableMap(it -> it.getType().getSimpleName(), identity()));
	}

	public PBXProject decode(PBXProj proj) {
		return new PBXObjectDecoder(proj.getObjects()).decode(proj.getRootObject());
	}

	private final class PBXObjectDecoder {
		private final Map<String, PBXObject> decodedObjects = new HashMap<>();
		private final PBXObjects objects;

		public PBXObjectDecoder(PBXObjects objects) {
			this.objects = objects;
		}

		public <T extends PBXObject> T decode(String gid) {
			assert gid != null;
			return decode(objects.getById(gid));
		}

		@SuppressWarnings("unchecked")
		private <T extends PBXObject> T decode(PBXObjectReference objectRef) {
			// DO NOT USE computeIfAbsent as it's not reentrant
			T result = (T) decodedObjects.get(objectRef.getGlobalID());
			if (result == null) {
				result = (T) Objects.requireNonNull(coders.get(objectRef.isa()), "missing coder for '" + objectRef.isa() + "'").read(new BaseDecoder(this, objectRef.getFields()));
				decodedObjects.put(objectRef.getGlobalID(), result);
			}
			return result;
		}
	}

	private final class BaseDecoder implements PBXObjectCoder.Decoder {
		private final PBXObjectDecoder delegate;
		private final PBXObjectFields object;

		public BaseDecoder(PBXObjectDecoder delegate, PBXObjectFields object) {
			this.delegate = delegate;
			this.object = object;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <S> Optional<S> decodeObject(String key) {
			return Optional.ofNullable(object.get(key)).map(it -> (S) delegate.decode(it.toString()));
		}

		@Override
		public <S> Optional<S> decode(String key, Class<S> type) {
			return decode(key, (Type) type);
		}

		@SuppressWarnings("unchecked")
		public <S> Optional<S> decode(String key, Type type) {
			return Optional.ofNullable(object.get(key)).map(value -> {
				if (type instanceof ParameterizedType) {
					if (Iterable.class.equals(((ParameterizedType) type).getRawType())) {
						if (!(value instanceof Iterable)) throw new IllegalStateException();
						return (S) Streams.stream((Iterable<Object>) value).map(it -> this.<S>decode(it, ((ParameterizedType) type).getActualTypeArguments()[0])).collect(toImmutableList());
					} else if (Map.class.equals(((ParameterizedType) type).getRawType())) {
						// Usually the values is already a map
						if (!(value instanceof Map)) throw new IllegalStateException();
						return (S) value;
					} else {
						throw new UnsupportedOperationException();
					}
				} else {
					return this.<S>decode(value, type);
				}
			});
		}

		@Override
		@SuppressWarnings("unchecked")
		public <S> void decodeIfPresent(String key, Consumer<? super S> action) {
			Optional.ofNullable(object.get(key)).ifPresent(value -> {
				if (value instanceof Iterable) {
					action.accept((S) Streams.stream((Iterable<?>) value).map(it -> delegate.decode(it.toString())).collect(toImmutableList()));
				} else {
					action.accept((S) delegate.decode(value.toString()));
				}
			});
		}

		@Override
		@SuppressWarnings("unchecked")
		public <S> void decodeIfPresent(String key, Type type, Consumer<? super S> action) {
			Optional.ofNullable(object.get(key)).ifPresent(value -> {
				if (type instanceof ParameterizedType) {
					if (Iterable.class.equals(((ParameterizedType) type).getRawType())) {
						if (!(value instanceof Iterable)) throw new IllegalStateException();
						action.accept((S) Streams.stream((Iterable<Object>) value).map(it -> this.<S>decode(it, ((ParameterizedType) type).getActualTypeArguments()[0])).collect(toImmutableList()));
					} else if (Map.class.equals(((ParameterizedType) type).getRawType())) {
						// Usually the values is already a map
						if (!(value instanceof Map)) throw new IllegalStateException();
						action.accept((S) value);
					} else {
						throw new UnsupportedOperationException();
					}
				} else {
					action.accept(this.<S>decode(value, type));
				}
			});
		}

		@SuppressWarnings("unchecked")
		private <S> S decode(Object value, Type type) {
			if (type.equals(String.class)) {
				return (S) value.toString();
			} else if (type.equals(Integer.class)) {
				return (S) Integer.decode(value.toString());
			} else if (PBXObject.class.isAssignableFrom((Class<?>) type)) {
				val object = delegate.decode(value.toString());
				if (!((Class<?>) type).isInstance(object)) throw new IllegalStateException();
				return (S) object;
			} else if (coders.containsKey(((Class<?>) type).getSimpleName()) && value instanceof Map) {
				return (S) coders.get(((Class<?>) type).getSimpleName()).read(new BaseDecoder(delegate, PBXObjectFields.fromMap((Map<String, Object>) value)));
			} else {
				throw new UnsupportedOperationException();
			}
		}
	}
}
