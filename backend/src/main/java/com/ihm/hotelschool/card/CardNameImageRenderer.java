package com.ihm.hotelschool.card;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextAttribute;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.AttributedString;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Renders complex-script names with Java's text shaper at print resolution. */
@Component
class CardNameImageRenderer {
    private static final int SCALE = 5;
    private static final FontRenderContext CONTEXT = new FontRenderContext(new AffineTransform(), true, true);
    private final Font latin = load("fonts/NotoSans-Regular.ttf");
    private final Font sinhala = load("fonts/NotoSansSinhala-Regular.ttf");
    private final Font tamil = load("fonts/NotoSansTamil-Regular.ttf");

    RenderedName render(String value, float maxWidth) {
        String name = value == null ? "" : value.strip();
        if (name.isEmpty()) throw new IllegalArgumentException("Student name is required for the card");
        float size = 11;
        TextLayout layout = layout(name, size);
        while (layout.getAdvance() > maxWidth && size > 7) {
            size -= 0.5f;
            layout = layout(name, size);
        }
        if (layout.getAdvance() > maxWidth) {
            throw new IllegalArgumentException("Student name is too long for the printable card");
        }
        float baseline = layout.getAscent() + 1;
        int width = Math.max(1, (int)Math.ceil(layout.getAdvance() * SCALE) + 4);
        int height = Math.max(1, (int)Math.ceil((baseline + layout.getDescent() + layout.getLeading() + 1) * SCALE));
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setColor(new Color(41, 41, 41));
            graphics.scale(SCALE, SCALE);
            layout.draw(graphics, 0, baseline);
        } finally { graphics.dispose(); }
        return new RenderedName(image, width / (float) SCALE, height / (float) SCALE,
                (height / (float) SCALE) - baseline);
    }

    private TextLayout layout(String value, float size) {
        AttributedString attributed = new AttributedString(value);
        Font current = null;
        int start = 0;
        for (int offset = 0; offset < value.length();) {
            int codePoint = value.codePointAt(offset);
            Font selected = fontFor(codePoint, current);
            if (!selected.canDisplay(codePoint)) {
                throw new IllegalArgumentException("Student name contains characters unsupported by the printable card");
            }
            if (selected != current) {
                if (current != null) attributed.addAttribute(TextAttribute.FONT,
                        current.deriveFont(Font.BOLD, size), start, offset);
                current = selected;
                start = offset;
            }
            offset += Character.charCount(codePoint);
        }
        attributed.addAttribute(TextAttribute.FONT, current.deriveFont(Font.BOLD, size), start, value.length());
        return new TextLayout(attributed.getIterator(), CONTEXT);
    }

    private Font fontFor(int codePoint, Font previous) {
        if (codePoint == 0x200C || codePoint == 0x200D) return previous == null ? latin : previous;
        Character.UnicodeBlock block = Character.UnicodeBlock.of(codePoint);
        if (block == Character.UnicodeBlock.SINHALA) return sinhala;
        if (block == Character.UnicodeBlock.TAMIL) return tamil;
        return latin;
    }

    private Font load(String path) {
        try (var input = new ClassPathResource(path).getInputStream()) {
            return Font.createFont(Font.TRUETYPE_FONT, input);
        } catch (IOException | FontFormatException exception) {
            throw new IllegalStateException("Could not load card font: " + path, exception);
        }
    }

    record RenderedName(BufferedImage image, float width, float height, float baselineFromBottom) {}
}
