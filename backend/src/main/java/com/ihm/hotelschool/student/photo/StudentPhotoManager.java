package com.ihm.hotelschool.student.photo;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class StudentPhotoManager {

	private final StudentPhotoProcessor processor;
	private final StudentPhotoStorage storage;

	StudentPhotoManager(StudentPhotoProcessor processor, StudentPhotoStorage storage) {
		this.processor = processor;
		this.storage = storage;
	}

	public String store(MultipartFile file) {
		return storage.store(processor.process(file));
	}

	public StoredPhotoContent load(String storageKey, PhotoVariant variant) {
		return storage.load(storageKey, variant);
	}

	public void delete(String storageKey) {
		storage.delete(storageKey);
	}
}
