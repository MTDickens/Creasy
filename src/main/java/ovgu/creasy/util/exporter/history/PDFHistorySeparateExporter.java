package ovgu.creasy.util.exporter.history;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.util.Matrix;

import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D;
import javafx.scene.Parent;
import javafx.stage.FileChooser;
import ovgu.creasy.origami.basic.CreasePattern;
import ovgu.creasy.ui.elements.CreasePatternCanvas;
import ovgu.creasy.util.exporter.base.AbstractHistoryExporter;

public class PDFHistorySeparateExporter extends AbstractHistoryExporter {

    public PDFHistorySeparateExporter(List<CreasePatternCanvas> history) {
        this.history = history;
    }

    @Override
    public Optional<File> open(Parent root) {
        FileChooser exportFolder = new FileChooser();
        exportFolder.setTitle("Save as folder");
        exportFolder.setInitialFileName("history");

        File target = exportFolder.showSaveDialog(root.getScene().getWindow());
        return target != null ? Optional.of(target) : Optional.empty();
    }

    @Override
    public boolean export(File file) {
        String base = file.getName();
        File dir = file.getParentFile();
        if (dir == null) {
            dir = new File(".");
        }

        File baseDir = new File(dir, base);
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            return false;
        }

        List<CreasePatternCanvas> resizableCanvas = this.history;
        for (int i = 0; i < this.history.size(); i++) {
            CreasePatternCanvas canvas = resizableCanvas.get((resizableCanvas.size() - 1) - i);
            File out = new File(baseDir, i + ".pdf");

            try (PDDocument document = new PDDocument()) {
                PDPage page = new PDPage(PDRectangle.A4);
                PdfBoxGraphics2D pdfBoxGraphics2D = new PdfBoxGraphics2D(document, 400, 400);

                // first draw normal crease pattern
                canvas.getCp().drawOnGraphics2D(pdfBoxGraphics2D);
                // then draw thick difference over it
                if (i > 0) {
                    pdfBoxGraphics2D.setStroke(new BasicStroke(5));

                    CreasePattern prev = resizableCanvas.get(resizableCanvas.size() - 1 - i + 1).getCp();
                    CreasePattern diff = canvas.getCp().getDifference(prev);

                    diff.drawOnGraphics2D(pdfBoxGraphics2D);
                }

                pdfBoxGraphics2D.dispose();

                PDFormXObject xObject = pdfBoxGraphics2D.getXFormObject();
                try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                    Matrix translate = new Matrix();
                    translate.translate(100, 200);

                    contentStream.transform(translate);
                    contentStream.drawForm(xObject);

                    drawText("step " + (i + 1), 25, 500, contentStream);

                    if (i == 0) {
                        drawText("Made with Creasy", 300, 500, contentStream);
                        drawText("Mountain Fold", 320, 460, contentStream);
                        drawText("Valley Fold", 320, 440, contentStream);

                        contentStream.setNonStrokingColor(Color.RED);
                        contentStream.addRect(300, 460, 10, 10);
                        contentStream.fill();

                        contentStream.setNonStrokingColor(Color.BLUE);
                        contentStream.addRect(300, 440, 10, 10);
                        contentStream.fill();
                    }
                }

                document.addPage(page);
                document.save(out.getPath());
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    private void drawText(String text, int x, int y, PDPageContentStream contentStream) throws IOException {
        contentStream.beginText();
        contentStream.setFont(PDType1Font.HELVETICA, 12);

        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);

        contentStream.endText();
    }
}
