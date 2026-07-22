package com.ihm.hotelschool.student.photo;

public enum PhotoVariant {
	FULL("full"),
	THUMBNAIL("thumbnail");

	private final String fileName;

	PhotoVariant(String fileName) {
		this.fileName = fileName;
	}

	String fileName() {
		return fileName;
	}

	public static PhotoVariant from(String value) {
		if (value == null || value.isBlank() || value.equalsIgnoreCase("full")) {
			return FULL;
		}
		if (value.equalsIgnoreCase("thumbnail")) {
			return THUMBNAIL;
		}
		throw new IllegalArgumentException("Photo variant must be full or thumbnail");
	}
}
