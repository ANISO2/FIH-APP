package com.fih.companion.badge;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.Chunk;
import com.lowagie.text.pdf.Barcode128;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Builds badge PDFs with OpenPDF. Each badge is a small PdfPTable (header / photo
 * + info / footer with barcode + QR), so it composes cleanly both as a full page
 * and inside an A4 sheet grid. We write only to an output stream — never the DB.
 */
@Service
public class BadgePdfService {

    private static final Color PRIMARY = new Color(0x1f, 0x5f, 0x8b);
    private static final Color INK = new Color(0x1a, 0x22, 0x30);
    private static final Color MUTED = new Color(0x5b, 0x64, 0x70);
    private static final Color LINE = new Color(0xe2, 0xe6, 0xec);
    private static final Color PLACEHOLDER = new Color(0xed, 0xf1, 0xf5);
    private static final Color ZONE_PUBLIC = new Color(0x0a, 0x7c, 0x4a);  // green
    private static final Color ZONE_VIP = new Color(0xb5, 0x47, 0x08);     // orange
    private static final Color ZONE_PRESS = new Color(0x5b, 0x3a, 0xa6);   // purple

    private static final float MM = 72f / 25.4f;        // millimetres -> points
    private static final float MARGIN = 14f;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");

    private final BadgeProperties props;

    public BadgePdfService(BadgeProperties props) {
        this.props = props;
    }

    private Rectangle badgeRect() {
        return new Rectangle((float) (props.getWidthMm() * MM), (float) (props.getHeightMm() * MM));
    }

    /** One badge, sized to the badge page. */
    public byte[] single(BadgeRecord rec) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Document doc = new Document(badgeRect(), MARGIN, MARGIN, MARGIN, MARGIN);
        PdfWriter writer = PdfWriter.getInstance(doc, os);
        doc.open();
        doc.add(badgeTable(rec, writer));
        doc.close();
        return os.toByteArray();
    }

    /** Multi-page PDF, one badge per page. */
    public byte[] batchSingle(List<BadgeRecord> recs) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Document doc = new Document(badgeRect(), MARGIN, MARGIN, MARGIN, MARGIN);
        PdfWriter writer = PdfWriter.getInstance(doc, os);
        doc.open();
        for (int i = 0; i < recs.size(); i++) {
            if (i > 0) doc.newPage();
            doc.add(badgeTable(recs.get(i), writer));
        }
        doc.close();
        return os.toByteArray();
    }

    /** A4 sheet, 2 columns x 4 rows = 8 badges per page. */
    public byte[] batchSheet(List<BadgeRecord> recs) {
        final int cols = 2;
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        Document doc = new Document(PageSize.A4, 22, 22, 22, 22);
        PdfWriter writer = PdfWriter.getInstance(doc, os);
        doc.open();
        PdfPTable grid = new PdfPTable(cols);
        grid.setWidthPercentage(100);
        for (BadgeRecord rec : recs) {
            PdfPCell cell = new PdfPCell(badgeTable(rec, writer));
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setPadding(8);
            grid.addCell(cell);
        }
        // pad the final row so the table is rectangular
        int remainder = recs.size() % cols;
        if (remainder != 0) {
            for (int i = 0; i < cols - remainder; i++) {
                PdfPCell empty = new PdfPCell();
                empty.setBorder(Rectangle.NO_BORDER);
                grid.addCell(empty);
            }
        }
        doc.add(grid);
        doc.close();
        return os.toByteArray();
    }

    /** ZIP of one-page PDFs, used for very large batches. */
    public byte[] batchZip(List<BadgeRecord> recs) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(os)) {
            for (BadgeRecord rec : recs) {
                zip.putNextEntry(new ZipEntry("badge_" + rec.codebarre() + ".pdf"));
                zip.write(single(rec));
                zip.closeEntry();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to build badge ZIP", e);
        }
        return os.toByteArray();
    }

    public int zipThreshold() {
        return props.getZipThreshold();
    }

    // -------------------------------------------------------- one badge as a table
    private PdfPTable badgeTable(BadgeRecord rec, PdfWriter writer) {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        try {
            t.setWidths(new float[]{1f, 1.5f});
        } catch (DocumentException ignored) {
        }

        // Header band
        PdfPCell header = new PdfPCell();
        header.setColspan(2);
        header.setBackgroundColor(PRIMARY);
        header.setBorder(Rectangle.NO_BORDER);
        header.setPadding(8);
        Paragraph title = new Paragraph(safe(rec.eventTitle(), "FIH 2025"),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.WHITE));
        header.addElement(title);
        if (rec.eventDate() != null) {
            header.addElement(new Paragraph(rec.eventDate().format(DATE),
                    FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(255, 255, 255, 210))));
        }
        t.addCell(header);

        // Photo cell
        PdfPCell photoCell = new PdfPCell();
        photoCell.setBorderColor(LINE);
        photoCell.setPadding(6);
        photoCell.setFixedHeight(46 * MM);
        photoCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        photoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        boolean placed = false;
        if (rec.photo() != null) {
            try {
                Image img = Image.getInstance(rec.photo().toString());
                img.scaleToFit(36 * MM, 42 * MM);
                photoCell.addElement(img);
                placed = true;
            } catch (Exception ignore) {
                // fall through to placeholder
            }
        }
        if (!placed) {
            photoCell.setBackgroundColor(PLACEHOLDER);
            Paragraph ph = new Paragraph("No photo", FontFactory.getFont(FontFactory.HELVETICA, 8, MUTED));
            ph.setAlignment(Element.ALIGN_CENTER);
            photoCell.addElement(ph);
        }
        t.addCell(photoCell);

        // Info cell (name / model / zone chips)
        PdfPCell info = new PdfPCell();
        info.setBorderColor(LINE);
        info.setPadding(8);
        info.setVerticalAlignment(Element.ALIGN_MIDDLE);
        // Name printed on the badge: the "Affectée à" name from badge_affectation
        // if it has been set, otherwise the legacy holder name as a fallback.
        boolean fromAffectee = rec.affecteeA() != null && !rec.affecteeA().isBlank();
        String displayName = fromAffectee ? rec.affecteeA() : rec.holderName();
        if (displayName != null && !displayName.isBlank()) {
            if (fromAffectee) {
                Paragraph caption = new Paragraph("Affectée à",
                        FontFactory.getFont(FontFactory.HELVETICA, 7, MUTED));
                caption.setSpacingAfter(1);
                info.addElement(caption);
            }
            Paragraph name = new Paragraph(displayName, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, INK));
            name.setSpacingAfter(4);
            info.addElement(name);
        }
        Paragraph model = new Paragraph(safe(rec.modelName(), "Badge"),
                FontFactory.getFont(FontFactory.HELVETICA, 9, MUTED));
        model.setSpacingAfter(6);
        info.addElement(model);
        if (rec.zones() != null && !rec.zones().isEmpty()) {
            Paragraph chips = new Paragraph();
            Font chipFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, Color.WHITE);
            for (String z : rec.zones()) {
                Chunk c = new Chunk("  " + z + "  ", chipFont);
                c.setBackground(zoneColor(z), 1f, 2f, 1f, 2f);
                chips.add(c);
                chips.add(new Chunk("  "));
            }
            info.addElement(chips);
        }
        t.addCell(info);

        // Footer: barcode + QR + numeroserie
        PdfPCell footer = new PdfPCell();
        footer.setColspan(2);
        footer.setBorder(Rectangle.NO_BORDER);
        footer.setPaddingTop(10);
        try {
            Barcode128 code128 = new Barcode128();
            code128.setCode(rec.codebarre());
            code128.setCodeType(Barcode128.CODE128);
            Image bc = code128.createImageWithBarcode(writer.getDirectContent(), null, null);

            Image qr = Image.getInstance(qrImage(rec.codebarre(), 240), null);

            PdfPTable codes = new PdfPTable(2);
            codes.setWidthPercentage(100);
            codes.setWidths(new float[]{2.2f, 1f});

            PdfPCell bcCell = new PdfPCell(bc, true);
            bcCell.setBorder(Rectangle.NO_BORDER);
            bcCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            bcCell.setFixedHeight(18 * MM);
            codes.addCell(bcCell);

            PdfPCell qrCell = new PdfPCell(qr, true);
            qrCell.setBorder(Rectangle.NO_BORDER);
            qrCell.setFixedHeight(18 * MM);
            codes.addCell(qrCell);

            footer.addElement(codes);
        } catch (Exception ignore) {
            // if barcode rendering fails, still print the serial below
        }
        Paragraph serial = new Paragraph(rec.numeroserie() + "  ·  " + rec.codebarre(),
                FontFactory.getFont(FontFactory.HELVETICA, 7, MUTED));
        serial.setAlignment(Element.ALIGN_CENTER);
        serial.setSpacingBefore(4);
        footer.addElement(serial);
        t.addCell(footer);

        return t;
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

    private Color zoneColor(String zone) {
        if (zone == null) return MUTED;
        String z = zone.trim().toLowerCase();
        if (z.startsWith("vip")) return ZONE_VIP;
        if (z.startsWith("press")) return ZONE_PRESS;
        return ZONE_PUBLIC;
    }

    private String safe(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }
}
