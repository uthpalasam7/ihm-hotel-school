package com.ihm.hotelschool.student.photo;

import com.ihm.hotelschool.common.config.ApplicationProperties;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
class StudentPhotoProcessor {

	private static final int MAX_FULL_EDGE = 1600;
	private static final int THUMBNAIL_EDGE = 96;
	private static final int MAX_SOURCE_EDGE = 10000;
	private static final long MAX_SOURCE_PIXELS = 40_000_000L;
	private static final long ABSOLUTE_MAX_SIZE = 5L * 1024 * 1024;

	private final long maxSize;

	StudentPhotoProcessor(ApplicationProperties properties) {
		this.maxSize = Math.min(properties.studentPhoto().maxSize().toBytes(), ABSOLUTE_MAX_SIZE);
	}

	ProcessedPhoto process(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("Student photo is required");
		}
		if (file.getSize() > maxSize) {
			throw new PhotoTooLargeException("Student photo must be 5 MiB or smaller");
		}
		String declaredType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
		if (!declaredType.equals("image/jpeg") && !declaredType.equals("image/png")) {
			throw new IllegalArgumentException("Student photo must be a JPEG or PNG image");
		}

		try {
			byte[] bytes = file.getBytes();
			ImageDetails details = inspect(bytes);
			String expectedType = details.extension().equals("png") ? "image/png" : "image/jpeg";
			if (!declaredType.equals(expectedType)) {
				throw new IllegalArgumentException("Student photo content does not match its MIME type");
			}
			BufferedImage source = ImageIO.read(new ByteArrayInputStream(bytes));
			if (source == null) {
				throw new IllegalArgumentException("Student photo could not be decoded");
			}
			BufferedImage full = resize(source, MAX_FULL_EDGE, details.extension());
			BufferedImage thumbnail = resize(source, THUMBNAIL_EDGE, details.extension());
			return new ProcessedPhoto(
					encode(full, details.extension()),
					encode(thumbnail, details.extension()),
					expectedType,
					details.extension());
		} catch (IOException exception) {
			throw new IllegalArgumentException("Student photo could not be processed", exception);
		}
	}

	private ImageDetails inspect(byte[] bytes) throws IOException {
		try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
			if (input == null) {
				throw new IllegalArgumentException("Student photo could not be decoded");
			}
			Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
			if (!readers.hasNext()) {
				throw new IllegalArgumentException("Student photo must contain valid image data");
			}
			ImageReader reader = readers.next();
			try {
				reader.setInput(input, true, true);
				String format = reader.getFormatName().toLowerCase(Locale.ROOT);
				String extension = format.equals("png") ? "png" : format.equals("jpeg") || format.equals("jpg") ? "jpg" : null;
				if (extension == null) {
					throw new IllegalArgumentException("Student photo must be a JPEG or PNG image");
				}
				int width = reader.getWidth(0);
				int height = reader.getHeight(0);
				if (width <= 0 || height <= 0 || width > MAX_SOURCE_EDGE || height > MAX_SOURCE_EDGE
						|| (long) width * height > MAX_SOURCE_PIXELS) {
					throw new IllegalArgumentException("Student photo dimensions are too large");
				}
				return new ImageDetails(extension);
			} finally {
				reader.dispose();
			}
		}
	}

	private BufferedImage resize(BufferedImage source, int maxEdge, String extension) {
		double scale = Math.min(1.0, (double) maxEdge / Math.max(source.getWidth(), source.getHeight()));
		int width = Math.max(1, (int) Math.round(source.getWidth() * scale));
		int height = Math.max(1, (int) Math.round(source.getHeight() * scale));
		int imageType = extension.equals("png") ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
		BufferedImage output = new BufferedImage(width, height, imageType);
		Graphics2D graphics = output.createGraphics();
		try {
			if (imageType == BufferedImage.TYPE_INT_RGB) {
				graphics.setColor(Color.WHITE);
				graphics.fillRect(0, 0, width, height);
			}
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			graphics.drawImage(source, 0, 0, width, height, null);
		} finally {
			graphics.dispose();
		}
		return output;
	}

	private byte[] encode(BufferedImage image, String extension) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		if (!ImageIO.write(image, extension, output)) {
			throw new IllegalArgumentException("Student photo could not be encoded");
		}
		return output.toByteArray();
	}

	private record ImageDetails(String extension) {
	}
}
