package com.ihm.hotelschool.student.photo;

public interface StudentPhotoStorage {
	String store(ProcessedPhoto photo);
	StoredPhotoContent load(String storageKey, PhotoVariant variant);
	void delete(String storageKey);
}
