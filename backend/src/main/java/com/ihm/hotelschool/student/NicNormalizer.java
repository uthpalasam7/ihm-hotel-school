package com.ihm.hotelschool.student;

import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
class NicNormalizer {

	String displayValue(String value) {
		String uppercase = value.trim().toUpperCase(Locale.ROOT);
		StringBuilder display = new StringBuilder(uppercase.length());
		uppercase.codePoints()
				.filter(codePoint -> !Character.isWhitespace(codePoint) && !Character.isSpaceChar(codePoint))
				.forEach(display::appendCodePoint);
		return display.toString();
	}

	String normalizedValue(String value) {
		return displayValue(value);
	}
}
