package dev.nokee.utils;

import com.google.common.testing.NullPointerTester;
import org.junit.jupiter.api.Test;

class SpecUtils_NullTest {
	@Test
	@SuppressWarnings("UnstableApiUsage")
	void checkNulls() {
		new NullPointerTester().testAllPublicStaticMethods(SpecUtils.class);
	}
}
