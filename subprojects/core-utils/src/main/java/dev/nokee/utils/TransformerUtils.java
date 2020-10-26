package dev.nokee.utils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import lombok.EqualsAndHashCode;
import org.gradle.api.Action;
import org.gradle.api.Transformer;

import java.util.List;
import java.util.Set;

public final class TransformerUtils {
	private TransformerUtils() {}

	@SuppressWarnings("unchecked")
	public static <T> Transformer<T, T> noOpTransformer() {
		return (Transformer<T, T>) NoOpTransformer.INSTANCE;
	}

	private enum NoOpTransformer implements Transformer<Object, Object> {
		INSTANCE;

		@Override
		public Object transform(Object o) {
			return o;
		}

		@Override
		public String toString() {
			return "TransformerUtils.noOpTransformer()";
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> Transformer<List<T>, Iterable<T>> toListTransformer() {
		return (Transformer<List<T>, Iterable<T>>) (Transformer<? extends List<T>, ? super Iterable<T>>) ToListTransformer.INSTANCE;
	}

	private enum ToListTransformer implements Transformer<List<? extends Object>, Iterable<? extends Object>> {
		INSTANCE;

		@Override
		public List<?> transform(Iterable<?> objects) {
			return ImmutableList.copyOf(objects);
		}


		@Override
		public String toString() {
			return "TransformerUtils.toListTransformer()";
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> Transformer<Set<T>, Iterable<T>> toSetTransformer() {
		return (Transformer<Set<T>, Iterable<T>>) (Transformer<? extends Set<T>, ? super Iterable<T>>) ToSetTransformer.INSTANCE;
	}

	@SuppressWarnings("unchecked")
	public static <OUT, IN> Transformer<Set<OUT>, Iterable<IN>> toSetTransformer(Class<OUT> type) {
		return (Transformer<Set<OUT>, Iterable<IN>>) (Transformer<? extends Set<OUT>, ? super Iterable<IN>>) ToSetTransformer.INSTANCE;
	}

	private enum ToSetTransformer implements Transformer<Set<? extends Object>, Iterable<? extends Object>> {
		INSTANCE;

		@Override
		public Set<?> transform(Iterable<?> objects) {
			return ImmutableSet.copyOf(objects);
		}


		@Override
		public String toString() {
			return "TransformerUtils.toSetTransformer()";
		}
	}

	@SuppressWarnings("unchecked")
	public static <T, U> Transformer<T, U> constant(T value) {
		return (Transformer<T, U>) new ConstantTransformer<>(value);
	}

	@EqualsAndHashCode
	private static final class ConstantTransformer<T> implements Transformer<T, Object> {
		private final T value;

		public ConstantTransformer(T value) {
			this.value = value;
		}

		@Override
		public T transform(Object o) {
			return value;
		}

		@Override
		public String toString() {
			return "TransformerUtils.constant(" + value + ")";
		}
	}

	public static <T> Transformer<T, T> configureInPlace(Action<? super T> action) {
		return new ConfigureInPlaceTransformer<>(action);
	}

	@EqualsAndHashCode
	private static final class ConfigureInPlaceTransformer<T> implements Transformer<T, T> {
		private final Action<? super T> action;

		public ConfigureInPlaceTransformer(Action<? super T> action) {
			this.action = action;
		}

		@Override
		public T transform(T t) {
			action.execute(t);
			return t;
		}

		@Override
		public String toString() {
			return "TransformerUtils.configureInPlace(" + action + ")";
		}
	}
}
