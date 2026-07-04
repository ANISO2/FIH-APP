package com.fih.companion.badge;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


@Service
public class BadgePdfService {

    // ---- palette (matches the mock-up) -------------------------------------
    private static final Color BLUE = new Color(0x19, 0x3C, 0x8D);   // panel / stub
    private static final Color NAVY = new Color(0x16, 0x2E, 0x6E);   // text inside white cards
    private static final Color GREY = new Color(0x6E, 0x78, 0x87);   // instruction text
    private static final Color WHITE = Color.WHITE;

    private static final float MM = 72f / 25.4f;        // millimetres -> points
    private static final DateTimeFormatter DAY_MONTH_FR =
            DateTimeFormatter.ofPattern("d MMMM", Locale.FRENCH);   // -> "10 août"

    // ---- ticket-local geometry, in millimetres (origin: top-left) ----------
    private static final float T_W = 220f, T_H = 100f;  // logical ticket size used for scaling
    private static final float POSTER_W = 100f;          // poster panel width
    private static final float PANEL_RIGHT = 188f;       // blue panel ends here; white strip to the right
    private static final float PERF_X = 150f;            // perforation line
    private static final float CARD_L = 105f, CARD_R = 145f;  // white card column
    private static final float CARD_CX = (CARD_L + CARD_R) / 2f;
    private static final float CARD_R_RADIUS = 3f;       // card corner radius (mm)

    private final BadgeProperties props;

    private BaseFont bfBold;
    private BaseFont bfReg;
    private BaseFont bfObl;

    public BadgePdfService(BadgeProperties props) {
        this.props = props;
        try {
            bfBold = BaseFont.createFont(BaseFont.HELVETICA_BOLD, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            bfReg = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
            bfObl = BaseFont.createFont(BaseFont.HELVETICA_OBLIQUE, BaseFont.WINANSI, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new IllegalStateException("Could not load the base PDF fonts", e);
        }
    }

    private Rectangle ticketPage() {
        return new Rectangle((float) (props.getTicketWidthMm() * MM), (float) (props.getTicketHeightMm() * MM));
    }

    // ----------------------------------------------------------- public API
    /** One ticket, sized to the ticket page. */
    public byte[] single(BadgeRecord rec) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Document doc = new Document(ticketPage(), 0, 0, 0, 0);
        PdfWriter writer = PdfWriter.getInstance(doc, os);
        doc.open();
        Rectangle p = doc.getPageSize();
        drawTicket(writer.getDirectContent(), writer, rec, 0, 0, p.getWidth(), p.getHeight());
        doc.close();
        return os.toByteArray();
    }


    public byte[] batchZipPerAffectee(List<BadgeRecord> recs) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Map<String, Integer> used = new HashMap<>();
        try (ZipOutputStream zip = new ZipOutputStream(os)) {
            for (BadgeRecord rec : recs) {
                String entry = uniqueEntryName(fileBaseName(rec), rec, used);
                zip.putNextEntry(new ZipEntry(entry));
                zip.write(single(rec));
                zip.closeEntry();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to build badge ZIP", e);
        }
        return os.toByteArray();
    }

    /** Base file name (no extension) for one invitation: affecteeA -> holder -> SANS-NOM_code. */
    private String fileBaseName(BadgeRecord rec) {
        if (rec.affecteeA() != null && !rec.affecteeA().isBlank()) {
            return sanitizeFilename(rec.affecteeA());
        }
        if (rec.holderName() != null && !rec.holderName().isBlank()) {
            return sanitizeFilename(rec.holderName());
        }
        String code = (rec.codebarre() != null && !rec.codebarre().isBlank())
                ? rec.codebarre() : rec.numeroserie();
        return "SANS-NOM_" + sanitizeFilename(code);
    }

    /** Ensure ZIP entry names are unique (case-insensitively) within one archive. */
    private String uniqueEntryName(String base, BadgeRecord rec, Map<String, Integer> used) {
        String candidate = base + ".pdf";
        if (used.putIfAbsent(candidate.toLowerCase(Locale.ROOT), 1) == null) {
            return candidate;
        }
        String withSerial = base + "_" + sanitizeFilename(rec.numeroserie()) + ".pdf";
        if (used.putIfAbsent(withSerial.toLowerCase(Locale.ROOT), 1) == null) {
            return withSerial;
        }
        int n = used.merge(withSerial.toLowerCase(Locale.ROOT), 1, Integer::sum);
        return base + "_" + sanitizeFilename(rec.numeroserie()) + "_" + n + ".pdf";
    }


    private String sanitizeFilename(String s) {
        if (s == null) return "badge";
        String folded = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")                 // drop diacritics
                .replace(' ', '_')
                .replaceAll("[^a-zA-Z0-9._-]+", "-")       // unsafe -> dash
                .replaceAll("[-_]{2,}", "_")               // collapse repeats
                .replaceAll("(^[._-]+|[._-]+$)", "");      // trim separators
        if (folded.isBlank()) return "badge";
        return folded.length() > 120 ? folded.substring(0, 120) : folded;
    }

    public int zipThreshold() {
        return props.getZipThreshold();
    }

    // ------------------------------------------------------ one ticket drawing

    private void drawTicket(PdfContentByte cb, PdfWriter writer, BadgeRecord rec,
                            float x0, float y0, float w, float h) {
        final float s = w / (T_W * MM);   // points-per-(logical mm*MM); 1.0 at full size

        // local mm -> page point helpers (origin top-left of the logical ticket)
        // X: distance from the left edge; Yt: distance measured DOWN from the top edge.
        // PDF y grows upward, so we translate the top-anchored mm into page space.
        // (kept as small lambdas via private methods below)

        // 1) blue info/stub panel (100 .. 188 mm), full height
        cb.setColorFill(BLUE);
        cb.rectangle(X(x0, 100f, s), y0, (PANEL_RIGHT - 100f) * MM * s, h);
        cb.fill();

        // 2) poster, cover-fitted into the left panel (0 .. 100 mm)
        drawPoster(cb, rec, x0, y0, h, s);

        // 3) holder name — RELOCATED. The name is no longer printed above the
        // cards; it is now set vertically in the white strip on the far right
        // (see step 9), in black, like the event title. Nothing else changed.

        // 4) date + time cards
        card(cb, x0, y0, h, s, CARD_L, 22f, CARD_R, 36f);
        card(cb, x0, y0, h, s, CARD_L, 39f, CARD_R, 53f);
        String dateStr = rec.eventDate() == null ? "" : capitalizeMonth(rec.eventDate().format(DAY_MONTH_FR));
        textCenteredInBand(cb, bfBold, 14f * s, NAVY, dateStr, X(x0, CARD_CX, s), y0, h, 29f, s);
        String time = props.resolveShowTime(rec.eventDate());
        textCenteredInBand(cb, bfBold, 14f * s, NAVY, time, X(x0, CARD_CX, s), y0, h, 46f, s);

        // 5) QR card + QR image
        card(cb, x0, y0, h, s, CARD_L, 55f, CARD_R, 93f);
        try {
            Image qr = Image.getInstance(qrImage(rec.codebarre(), 320), null);
            float qs = 30f * MM * s;
            qr.scaleAbsolute(qs, qs);
            qr.setAbsolutePosition(X(x0, CARD_CX, s) - qs / 2f, Yt(y0, h, 74f, s) - qs / 2f);
            cb.addImage(qr);
        } catch (Exception ignore) {
            // QR carries the code; if it fails the ticket prints without it
        }

        // 6) perforation: dashed line + punch notches
        cb.saveState();
        cb.setColorStroke(WHITE);
        cb.setLineWidth(0.9f * s);
        cb.setLineDash(3.2f * MM * s, 2.6f * MM * s, 0f);
        cb.moveTo(X(x0, PERF_X, s), Yt(y0, h, 8f, s));
        cb.lineTo(X(x0, PERF_X, s), Yt(y0, h, 92f, s));
        cb.stroke();
        cb.restoreState();
        cb.setColorFill(WHITE);
        cb.circle(X(x0, PERF_X, s), y0 + h, 3.4f * MM * s);   // top edge notch
        cb.fill();
        cb.circle(X(x0, PERF_X, s), y0, 3.4f * MM * s);       // bottom edge notch
        cb.fill();

        // 7) event title, set vertically in the stub
        String title = safe(rec.eventTitle(), "FIH 2025");
        float stubCx = (PERF_X + PANEL_RIGHT) / 2f;
        float titleSize = fitFont(bfBold, title, 26f * s, 11f * s, h - 14f * MM * s);
        textRotated(cb, bfBold, titleSize, WHITE, Element.ALIGN_CENTER, title,
                X(x0, stubCx, s), y0 + h / 2f, 90);

        // 8) rotated instruction text near the right edge
        textRotated(cb, bfObl, 6.5f * s, GREY, Element.ALIGN_CENTER,
                "Ceci est votre e-ticket \u00e0 pr\u00e9senter au contr\u00f4le d'acc\u00e8s",
                X(x0, 217f, s), y0 + h / 2f, 90);

        // 9) holder name in the far-right white strip (the red-circled spot
        //    where the barcode used to be). Set VERTICALLY like the event
        //    title, but in BLACK so it shows on the white background. Same
        //    bold font and the same fit-to-height sizing as the event title,
        //    so the two read identically — only the colour and position differ.
        String name = displayName(rec);
        if (name != null) {
            float nameCx = (PANEL_RIGHT + 217f) / 2f;   // mid white strip, left of the instruction
            float nameSize = fitFont(bfBold, name, 26f * s, 11f * s, h - 14f * MM * s);
            textRotated(cb, bfBold, nameSize, Color.BLACK, Element.ALIGN_CENTER, name,
                    X(x0, nameCx, s), y0 + h / 2f, 90);
        }
    }

    // --------------------------------------------------------------- helpers
    private void drawPoster(PdfContentByte cb, BadgeRecord rec, float x0, float y0, float h, float s) {
        float pw = POSTER_W * MM * s;
        Path poster = resolvePoster(rec.eventTitle());
        if (poster != null) {
            try {
                Image img = Image.getInstance(poster.toString());
                float iw = img.getWidth(), ih = img.getHeight();
                float scale = Math.max(pw / iw, h / ih);        // cover
                float dw = iw * scale, dh = ih * scale;
                float dx = x0 - (dw - pw) / 2f;                 // centre-crop
                float dy = y0 - (dh - h) / 2f;
                cb.saveState();
                cb.rectangle(x0, y0, pw, h);
                cb.clip();
                cb.newPath();
                img.scaleAbsolute(dw, dh);
                img.setAbsolutePosition(dx, dy);
                cb.addImage(img);
                cb.restoreState();
                return;
            } catch (Exception ignore) {
                // fall through to the coloured placeholder
            }
        }
        // placeholder: blue panel + centred title so the ticket is still usable
        cb.setColorFill(BLUE);
        cb.rectangle(x0, y0, pw, h);
        cb.fill();
        text(cb, bfBold, 18f * s, WHITE, Element.ALIGN_CENTER,
                safe(rec.eventTitle(), "FIH 2025"), x0 + pw / 2f, y0 + h / 2f);
    }

    /** Filled white rounded card from (lmm,tmm) to (rmm,bmm) in top-anchored mm. */
    private void card(PdfContentByte cb, float x0, float y0, float h, float s,
                      float lmm, float tmm, float rmm, float bmm) {
        float x = X(x0, lmm, s);
        float yTop = Yt(y0, h, tmm, s);
        float yBot = Yt(y0, h, bmm, s);
        cb.setColorFill(WHITE);
        cb.roundRectangle(x, yBot, (rmm - lmm) * MM * s, (yTop - yBot), CARD_R_RADIUS * MM * s);
        cb.fill();
    }

    /** Horizontal text; (x,y) is the baseline anchor for the given alignment. */
    private void text(PdfContentByte cb, BaseFont bf, float size, Color color,
                      int align, String txt, float x, float yBaseline) {
        cb.beginText();
        cb.setFontAndSize(bf, size);
        cb.setColorFill(color);
        cb.showTextAligned(align, txt, x, yBaseline, 0);
        cb.endText();
    }

    /** Horizontal text vertically centred inside a top-anchored band centre (cymm). */
    private void textCenteredInBand(PdfContentByte cb, BaseFont bf, float size, Color color,
                                    String txt, float x, float y0, float h, float cymm, float s) {
        float cy = Yt(y0, h, cymm, s);
        float baseline = cy - size * 0.33f;   // rough optical centring for Helvetica
        text(cb, bf, size, color, Element.ALIGN_CENTER, txt, x, baseline);
    }

    /** Rotated text (rotation in degrees, CCW) centred on (x,y). */
    private void textRotated(PdfContentByte cb, BaseFont bf, float size, Color color,
                             int align, String txt, float x, float y, float rotation) {
        cb.beginText();
        cb.setFontAndSize(bf, size);
        cb.setColorFill(color);
        cb.showTextAligned(align, txt, x, y, rotation);
        cb.endText();
    }

    /** Largest size in [min,start] whose rendered width fits maxWidth. */
    private float fitFont(BaseFont bf, String txt, float start, float min, float maxWidth) {
        float size = start;
        while (size > min && bf.getWidthPoint(txt, size) > maxWidth) {
            size -= 0.5f;
        }
        return size;
    }

    // local-mm -> page-point conversions (top-anchored)
    private float X(float x0, float mm, float s) {
        return x0 + mm * MM * s;
    }

    private float Yt(float y0, float h, float mmFromTop, float s) {
        return y0 + h - mmFromTop * MM * s;
    }

    /** Build a QR code as a black/white image (OpenPDF has no QR generator of its own). */
    private BufferedImage qrImage(String text, int size) throws Exception {
        BitMatrix matrix = new QRCodeWriter().encode(text, BarcodeFormat.QR_CODE, size, size);
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                img.setRGB(x, y, matrix.get(x, y) ? 0x000000 : 0xFFFFFF);
            }
        }
        return img;
    }

    /**
     * Resolve the poster file for an event title from the configured poster dir:
     * first {@code <slug(title)>.<ext>}, then the configured default file. Returns
     * null when nothing is found (the caller draws a placeholder).
     */
    private Path resolvePoster(String title) {
        String dir = props.getPosterDir();
        if (dir == null || dir.isBlank()) return null;
        Path base = Paths.get(dir);
        if (title != null && !title.isBlank()) {
            String slug = slugify(title);
            for (String ext : props.getPosterExtensions()) {
                Path p = base.resolve(slug + "." + ext);
                if (Files.isReadable(p)) return p;
            }
        }
        String def = props.getPosterDefault();
        if (def != null && !def.isBlank()) {
            Path p = base.resolve(def);
            if (Files.isReadable(p)) return p;
        }
        return null;
    }

    /** "Salif Keïta" -> "salif-keita" (accent-folded, lower-case, dash-joined). */
    private String slugify(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        return n;
    }

    /** Capitalise the month word in a "10 août" string -> "10 Août". */
    private String capitalizeMonth(String s) {
        int sp = s.indexOf(' ');
        if (sp < 0 || sp + 1 >= s.length()) return s;
        return s.substring(0, sp + 1)
                + Character.toUpperCase(s.charAt(sp + 1))
                + s.substring(sp + 2);
    }

    private String displayName(BadgeRecord rec) {
        if (rec.affecteeA() != null && !rec.affecteeA().isBlank()) return rec.affecteeA().trim();
        if (rec.holderName() != null && !rec.holderName().isBlank()) return rec.holderName().trim();
        return null;
    }

    private String safe(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }
}
