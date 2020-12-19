package dev.nokee.model.internal.core;

import com.google.common.testing.EqualsTester;
import lombok.val;
import org.junit.jupiter.api.Test;

import static com.google.common.base.Predicates.alwaysFalse;
import static com.spotify.hamcrest.optional.OptionalMatchers.emptyOptional;
import static com.spotify.hamcrest.optional.OptionalMatchers.optionalWithValue;
import static dev.nokee.model.internal.core.ModelPath.path;
import static dev.nokee.model.internal.core.ModelTestUtils.node;
import static dev.nokee.model.internal.core.NodePredicate.allDirectDescendants;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasToString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NodePredicate_AllDirectDescendantsTest {
	@Test
	void canCreateSpecMatchingAllDirectDescendants() {
		val spec = allDirectDescendants().scope(path("foo"));
		assertThat(spec.getPath(), emptyOptional());
		assertThat(spec.getParent(), optionalWithValue(equalTo(path("foo"))));
		assertThat(spec.getAncestor(), emptyOptional());
		assertTrue(spec.isSatisfiedBy(node("foo.bar")));
	}

	@Test
	void canCreateSpecMatchingAllDirectDescendantsAndPredicate() {
		val spec = allDirectDescendants(alwaysFalse()).scope(path("bar"));
		assertThat(spec.getPath(), emptyOptional());
		assertThat(spec.getParent(), optionalWithValue(equalTo(path("bar"))));
		assertThat(spec.getAncestor(), emptyOptional());
		assertFalse(spec.isSatisfiedBy(node("bar.foo")));
	}

	@Test
	@SuppressWarnings("UnstableApiUsage")
	void canEquals() {
		new EqualsTester()
			.addEqualityGroup(allDirectDescendants(), allDirectDescendants())
			.addEqualityGroup(allDirectDescendants(alwaysFalse()), allDirectDescendants(alwaysFalse()))
			.addEqualityGroup(allDirectDescendants(node -> true))
			.testEquals();
	}

	@Test
	@SuppressWarnings("UnstableApiUsage")
	void canEqualsScopedPredicate() {
		new EqualsTester()
			.addEqualityGroup(allDirectDescendants().scope(path("foo")), allDirectDescendants().scope(path("foo")))
			.addEqualityGroup(allDirectDescendants().scope(path("bar")))
			.addEqualityGroup(allDirectDescendants(alwaysFalse()).scope(path("bar")), allDirectDescendants(alwaysFalse()).scope(path("bar")))
			.addEqualityGroup(allDirectDescendants(alwaysFalse()).scope(path("foo")))
			.addEqualityGroup(allDirectDescendants(node -> true).scope(path("foo")))
			.addEqualityGroup(allDirectDescendants(node -> true).scope(path("bar")))
			.testEquals();
	}

	@Test
	void checkToString() {
		assertThat(allDirectDescendants(), hasToString("NodePredicate.allDirectDescendants()"));
		assertThat(allDirectDescendants(alwaysFalse()), hasToString("NodePredicate.allDirectDescendants(Predicates.alwaysFalse())"));
	}


}
