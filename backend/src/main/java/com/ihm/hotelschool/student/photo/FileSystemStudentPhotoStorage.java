package com.ihm.hotelschool.student.photo;

import com.ihm.hotelschool.common.config.ApplicationProperties;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
class FileSystemStudentPhotoStorage implements StudentPhotoStorage {

	private static final Pattern SAFE_KEY = Pattern.compile("[0-9a-f-]{36}\\.(jpg|png)");
	private final Path root;

	FileSystemStudentPhotoStorage(ApplicationProperties properties) {
		this.root = Path.of(properties.studentPhoto().storagePath()).toAbsolutePath().normalize();
	}

	@PostConstruct
	void initialize() {
		try {
			Files.createDirectories(root);
		} catch (IOException exception) {
			throw new PhotoStorageException("Student photo storage could not be initialized", exception);
		}
	}

	@Override
	public String store(ProcessedPhoto photo) {
		String key = UUID.randomUUID() + "." + photo.extension();
		Path destination = resolveKey(key);
		Path temporary = null;
		try {
			temporary = Files.createTempDirectory(root, ".upload-");
			Files.write(temporary.resolve("full." + photo.extension()), photo.fullImage());
			Files.write(temporary.resolve("thumbnail." + photo.extension()), photo.thumbnail());
			moveDirectory(temporary, destination);
			return key;
		} catch (IOException exception) {
			deleteDirectoryQuietly(temporary);
			throw new PhotoStorageException("Student photo could not be stored", exception);
		}
	}

	@Override
	public StoredPhotoContent load(String storageKey, PhotoVariant variant) {
		Path directory = resolveKey(storageKey);
		String extension = extension(storageKey);
		Path file = directory.resolve(variant.fileName() + "." + extension).normalize();
		if (!file.startsWith(directory)) {
			throw new IllegalArgumentException("Invalid photo storage key");
		}
		try {
			if (!Files.isRegularFile(file)) {
				throw new PhotoStorageException("Student photo was not found", null);
			}
			return new StoredPhotoContent(
					Files.readAllBytes(file),
					extension.equals("png") ? "image/png" : "image/jpeg",
					extension);
		} catch (IOException exception) {
			throw new PhotoStorageException("Student photo could not be read", exception);
		}
	}

	@Override
	public void delete(String storageKey) {
		Path directory = resolveKey(storageKey);
		try {
			if (!Files.exists(directory)) {
				return;
			}
			String extension = extension(storageKey);
			Files.deleteIfExists(directory.resolve("full." + extension));
			Files.deleteIfExists(directory.resolve("thumbnail." + extension));
			Files.deleteIfExists(directory);
		} catch (IOException exception) {
			throw new PhotoStorageException("Student photo could not be deleted", exception);
		}
	}

	private Path resolveKey(String storageKey) {
		if (storageKey == null || !SAFE_KEY.matcher(storageKey).matches()) {
			throw new IllegalArgumentException("Invalid photo storage key");
		}
		Path resolved = root.resolve(storageKey).normalize();
		if (!resolved.startsWith(root)) {
			throw new IllegalArgumentException("Invalid photo storage key");
		}
		return resolved;
	}

	private String extension(String storageKey) {
		return storageKey.substring(storageKey.lastIndexOf('.') + 1);
	}

	private void moveDirectory(Path source, Path destination) throws IOException {
		try {
			Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
		} catch (AtomicMoveNotSupportedException exception) {
			Files.move(source, destination);
		}
	}

	private void deleteDirectoryQuietly(Path directory) {
		if (directory == null) {
			return;
		}
		try {
			try (var files = Files.list(directory)) {
				files.forEach(path -> {
					try {
						Files.deleteIfExists(path);
					} catch (IOException ignored) {
						// Best-effort cleanup of an incomplete upload.
					}
				});
			}
			Files.deleteIfExists(directory);
		} catch (IOException ignored) {
			// Best-effort cleanup of an incomplete upload.
		}
	}
}
