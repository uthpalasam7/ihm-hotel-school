package com.ihm.hotelschool.card;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.ihm.hotelschool.student.Student;
import com.ihm.hotelschool.student.photo.PhotoVariant;
import com.ihm.hotelschool.student.photo.StudentPhotoManager;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import javax.imageio.ImageIO;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class StudentCardRenderer {
    private static final float WIDTH = 242.65f; // ISO/IEC 7810 ID-1: 85.6 mm
    private static final float HEIGHT = 153.07f; // 53.98 mm
    private static final Color GOLD = new Color(176,133,44);
    private static final Color CHARCOAL = new Color(41,41,41);
    private static final PDType1Font REGULAR = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private final StudentPhotoManager photos;
    private final CardNameImageRenderer names;
    private final String contactLine;

    StudentCardRenderer(StudentPhotoManager photos, CardNameImageRenderer names,
            @Value("${app.card.contact-line}") String contactLine) {
        this.photos=photos; this.names=names; this.contactLine=contactLine;
    }

    public byte[] qrPng(String token) {
        try {
            var matrix = new QRCodeWriter().encode(token, BarcodeFormat.QR_CODE, 360, 360,
                    Map.of(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M, EncodeHintType.MARGIN, 4));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(MatrixToImageWriter.toBufferedImage(matrix), "PNG", out);
            return out.toByteArray();
        } catch (IOException | WriterException exception) { throw new IllegalStateException("Could not render card QR", exception); }
    }

    public byte[] pdf(StudentCard card, String token) {
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Student student = card.getStudent();
            PDPage front = new PDPage(new PDRectangle(WIDTH, HEIGHT)); document.addPage(front);
            try (PDPageContentStream stream = new PDPageContentStream(document, front)) {
                base(stream);
                var logo = ImageIO.read(new ClassPathResource("branding/ihm-logo.jpg").getInputStream());
                if (logo != null) stream.drawImage(LosslessFactory.createFromImage(document, logo), 18, 103, 36, 36);
                text(stream, "IHM HOTEL SCHOOL", 60, 124, BOLD, 10, CHARCOAL, 165);
                text(stream, "STUDENT ID CARD", 60, 110, REGULAR, 7, GOLD, 165);
                stream.setNonStrokingColor(new Color(235,232,225)); stream.addRect(18, 96, WIDTH-36, 0.6f); stream.fill();
                text(stream, "STUDENT", 18, 81, BOLD, 6.5f, GOLD, 110);
                drawName(document, stream, student.getFullName());
                text(stream, "IHM STUDENT ID", 18, 42, REGULAR, 6.5f, CHARCOAL, 120);
                text(stream, identifier(student), 18, 28, BOLD, 9, CHARCOAL, 145);
                drawPhoto(document, stream, student);
            }
            PDPage back = new PDPage(new PDRectangle(WIDTH, HEIGHT)); document.addPage(back);
            try (PDPageContentStream stream = new PDPageContentStream(document, back)) {
                base(stream);
                var qr = ImageIO.read(new ByteArrayInputStream(qrPng(token)));
                stream.drawImage(LosslessFactory.createFromImage(document, qr), 13, 18, 115, 115);
                text(stream, "SCAN FOR CLASS", 135, 119, BOLD, 8, CHARCOAL, 94);
                text(stream, "ATTENDANCE", 135, 107, BOLD, 8, CHARCOAL, 94);
                text(stream, identifier(student), 135, 87, BOLD, 7, GOLD, 94);
                text(stream, "Staff-supervised", 135, 68, REGULAR, 7, CHARCOAL, 94);
                text(stream, "check-in only", 135, 57, REGULAR, 7, CHARCOAL, 94);
                String[] contact = contactLine.trim().split("\\s+");
                String line=""; int lineNumber=0;
                for (String word : contact) {
                    String next=line.isEmpty()?word:line+" "+word;
                    if (!line.isEmpty() && REGULAR.getStringWidth(next)/1000*6 > 92 && lineNumber < 2) {
                        text(stream,line,135,38-lineNumber*10,REGULAR,6,CHARCOAL,92);
                        line=word; lineNumber++;
                    } else line=next;
                }
                text(stream,line,135,38-lineNumber*10,REGULAR,6,CHARCOAL,92);
            }
            document.save(out); return out.toByteArray();
        } catch (IOException exception) { throw new IllegalStateException("Could not render student card PDF", exception); }
    }

    private void drawPhoto(PDDocument document, PDPageContentStream stream, Student student) throws IOException {
        String key = student.getPhotoStorageKey();
        if (key == null) return;
        var content = photos.load(key, PhotoVariant.FULL);
        BufferedImage photo = ImageIO.read(new ByteArrayInputStream(content.content()));
        if (photo == null) return;
        float x = 169, y = 26, frameWidth = 55, frameHeight = 55;
        float scale = Math.max(frameWidth/photo.getWidth(), frameHeight/photo.getHeight()) * 1.2f;
        float width = photo.getWidth()*scale, height = photo.getHeight()*scale;
        stream.saveGraphicsState();
        stream.addRect(x, y, frameWidth, frameHeight);
        stream.clip();
        stream.drawImage(LosslessFactory.createFromImage(document, photo),
                x+(frameWidth-width)/2, y+(frameHeight-height)*0.6f, width, height);
        stream.restoreGraphicsState();
        stream.setStrokingColor(GOLD);
        stream.setLineWidth(0.5f);
        stream.addRect(x, y, frameWidth, frameHeight);
        stream.stroke();
    }

    private void drawName(PDDocument document, PDPageContentStream stream, String name) throws IOException {
        boolean standardFontSupported = name != null && name.codePoints().allMatch(codePoint -> {
            try { return BOLD.hasGlyph(codePoint); }
            catch (IOException exception) { return false; }
        });
        if (standardFontSupported) {
            text(stream, name, 18, 64, BOLD, 11, CHARCOAL, 150);
            return;
        }
        var rendered = names.render(name, 150);
        stream.drawImage(LosslessFactory.createFromImage(document, rendered.image()),
                18, 64 - rendered.baselineFromBottom(), rendered.width(), rendered.height());
    }

    private void base(PDPageContentStream stream) throws IOException {
        stream.setNonStrokingColor(Color.WHITE); stream.addRect(0, 0, WIDTH, HEIGHT); stream.fill();
        stream.setNonStrokingColor(GOLD); stream.addRect(0, HEIGHT-6, WIDTH, 6); stream.fill();
    }
    private String identifier(Student student) { return "IHM-ST-" + String.format(java.util.Locale.ROOT, "%06d", student.getId()); }
    private void text(PDPageContentStream stream, String value, float x, float y, PDType1Font font,
            float size, Color color, float maxWidth) throws IOException {
        String safe = value == null ? "" : value;
        for (int codePoint : safe.codePoints().toArray()) {
            if (!font.hasGlyph(codePoint)) {
                throw new IllegalArgumentException("Card text contains characters unsupported by the printable font");
            }
        }
        while (!safe.isEmpty() && font.getStringWidth(safe)/1000*size > maxWidth) safe=safe.substring(0,safe.length()-1);
        if (safe.length() < (value == null ? 0 : value.length()) && safe.length()>3) safe=safe.substring(0,safe.length()-3)+"...";
        stream.setNonStrokingColor(color); stream.beginText(); stream.setFont(font,size);
        stream.newLineAtOffset(x,y); stream.showText(safe); stream.endText();
    }
}
