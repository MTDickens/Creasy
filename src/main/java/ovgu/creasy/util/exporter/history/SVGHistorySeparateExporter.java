package ovgu.creasy.util.exporter.history;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.jfree.svg.SVGGraphics2D;

import javafx.scene.Parent;
import javafx.stage.FileChooser;
import ovgu.creasy.ui.elements.CreasePatternCanvas;
import ovgu.creasy.util.exporter.base.AbstractHistoryExporter;

public class SVGHistorySeparateExporter extends AbstractHistoryExporter {

    public SVGHistorySeparateExporter(List<CreasePatternCanvas> history) {
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

    private String stripExtension(String name) {
        int dot = name.lastIndexOf('.');
        return (dot > 0) ? name.substring(0, dot) : name;
    }

    private boolean writeSvg(SVGGraphics2D g, File out) {
        try (FileWriter fileWriter = new FileWriter(out)) {
            fileWriter.write(g.getSVGDocument());
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean export(File file) {
        // String base = stripExtension(file.getName());
        String base = file.getName();
        File dir = file.getParentFile();
        if (dir == null) {
            dir = new File(".");
        }

        File baseDir = new File(dir, base);
        if (!baseDir.exists() && !baseDir.mkdirs()) {
            return false;
        }

        for (int i = 0; i < history.size(); i++) {
            SVGGraphics2D g = new SVGGraphics2D(400, 400);
            CreasePatternCanvas canvas = history.get((history.size() - 1) - i);
            canvas.getCp().drawOnGraphics2D(g);

            File out = new File(baseDir, i + ".svg");
            if (!writeSvg(g, out)) {
                return false;
            }
        }
        return true;
    }



    private void drawArrow(SVGGraphics2D svgGraphics2D) {
        svgGraphics2D.setColor(Color.BLACK);
        svgGraphics2D.fillRect(400, 200, 45, 10);
        svgGraphics2D.fillPolygon(new int[] {445, 450, 445}, new int[] {195, 205, 215}, 3);
    }
}
