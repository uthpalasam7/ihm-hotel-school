package com.ihm.hotelschool.student;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NicNormalizerTests {

	private final NicNormalizer normalizer = new NicNormalizer();

	@Test
	void trimsUppercasesAndRemovesWhitespace() {
		assertThat(normalizer.displayValue("  2000 1234v  ")).isEqualTo("20001234V");
		assertThat(normalizer.normalizedValue("  2000\u00a01234v\t ")).isEqualTo("20001234V");
	}
}
