package com.ihm.hotelschool.card;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ihm.hotelschool.student.Student;
import com.ihm.hotelschool.student.photo.PhotoVariant;
import com.ihm.hotelschool.student.photo.StoredPhotoContent;
import com.ihm.hotelschool.student.photo.StudentPhotoManager;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

class StudentCardRendererTests {
    @Test
    void embedsTheStoredFullSizePhotoInsteadOfTheListThumbnail() throws Exception {
        StudentPhotoManager photos = mock(StudentPhotoManager.class);
        Student student = mock(Student.class);
        StudentCard card = mock(StudentCard.class);
        when(card.getStudent()).thenReturn(student);
        when(student.getId()).thenReturn(5L);
        when(student.getFullName()).thenReturn("Nimal Perera");
        when(student.getPhotoStorageKey()).thenReturn("student-photo");

        BufferedImage image = new BufferedImage(320, 400, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bytes);
        when(photos.load("student-photo", PhotoVariant.FULL))
                .thenReturn(new StoredPhotoContent(bytes.toByteArray(), "image/png", "png"));

        byte[] pdf = new StudentCardRenderer(photos, mock(CardNameImageRenderer.class),
                "Return lost cards to IHM Hotel School").pdf(card, "test-card-token");

        assertTrue(pdf.length > 0);
        verify(photos).load("student-photo", PhotoVariant.FULL);
        boolean fullResolutionEmbedded = false;
        try (var document = Loader.loadPDF(pdf)) {
            var resources = document.getPage(0).getResources();
            for (var name : resources.getXObjectNames()) {
                if (resources.getXObject(name) instanceof PDImageXObject embedded
                        && embedded.getWidth() == 320 && embedded.getHeight() == 400) {
                    fullResolutionEmbedded = true;
                }
            }
        }
        assertTrue(fullResolutionEmbedded, "The PDF should embed the full photo pixels");
    }
}
