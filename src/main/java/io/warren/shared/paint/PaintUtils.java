package io.warren.shared.paint;

import java.awt.*;
import java.util.List;
import org.rspeer.game.position.Position;
import org.rspeer.game.position.area.Area;
import org.rspeer.game.scene.Projection;

public class PaintUtils {
    private static final int STROKE_WIDTH = 2;
    private static final Font LABEL_FONT = new Font("Arial", Font.BOLD, 12);
    private static final double FILL_OPACITY = 0.3;
    private static final double DARK_FACTOR = 0.4;

    /**
     * Draws a single tile with automatic fill and outline
     */
    public static void drawTile(Graphics2D g, Position pos, Color color) {
        drawTile(g, pos, color, null);
    }

    /**
     * Draws a single tile with automatic fill, outline, and optional label
     */
    public static void drawTile(Graphics2D g, Position pos, Color color, String label) {
        if (pos == null || g == null) return;

        Polygon poly = Projection.getPositionPolygon(Projection.Canvas.VIEWPORT, pos);
        if (poly == null || poly.npoints == 0) return;

        // Draw fill (darker, semi-transparent)
        g.setColor(new Color(
            (int)(color.getRed() * DARK_FACTOR),
            (int)(color.getGreen() * DARK_FACTOR),
            (int)(color.getBlue() * DARK_FACTOR),
            (int)(255 * FILL_OPACITY)
        ));
        g.fillPolygon(poly);

        // Draw outline (full color)
        Stroke oldStroke = g.getStroke();
        g.setStroke(new BasicStroke(STROKE_WIDTH));
        g.setColor(color);
        g.drawPolygon(poly);
        g.setStroke(oldStroke);

        // Draw label if provided
        if (label != null && !label.isEmpty()) {
            drawLabel(g, pos, label, Color.WHITE);
        }
    }

    /**
     * Draws an area (all tiles)
     */
    public static void drawArea(Graphics2D g, Area area, Color color) {
        drawArea(g, area, color, null);
    }

    /**
     * Draws an area with optional label on each tile
     */
    public static void drawArea(Graphics2D g, Area area, Color color, String label) {
        if (area == null || g == null) return;

        for (Position pos : area.getTiles()) {
            drawTile(g, pos, color, label);
        }
    }

    /**
     * Draws multiple tiles
     */
    public static void drawTiles(Graphics2D g, List<Position> tiles, Color color) {
        if (tiles == null || g == null) return;

        for (Position pos : tiles) {
            drawTile(g, pos, color);
        }
    }

    /**
     * Draws multiple tiles with sequential numbers
     */
    public static void drawNumberedTiles(Graphics2D g, List<Position> tiles, Color color) {
        if (tiles == null || g == null) return;

        for (int i = 0; i < tiles.size(); i++) {
            drawTile(g, tiles.get(i), color, String.valueOf(i + 1));
        }
    }

    /**
     * Helper to draw a label at a position
     */
    private static void drawLabel(Graphics2D g, Position pos, String label, Color textColor) {
        Point center = getTileCenter(pos);
        if (center == null) return;

        Font oldFont = g.getFont();
        g.setFont(LABEL_FONT);
        g.setColor(textColor);

        FontMetrics fm = g.getFontMetrics();
        int textWidth = fm.stringWidth(label);
        g.drawString(label, center.x - textWidth / 2, center.y + fm.getAscent() / 2 - 2);

        g.setFont(oldFont);
    }

    /**
     * Gets the visual center of a tile
     */
    private static Point getTileCenter(Position pos) {
        if (pos == null) return null;

        Polygon poly = Projection.getPositionPolygon(Projection.Canvas.VIEWPORT, pos);
        if (poly == null || poly.npoints == 0) return null;

        int centerX = 0, centerY = 0;
        for (int i = 0; i < poly.npoints; i++) {
            centerX += poly.xpoints[i];
            centerY += poly.ypoints[i];
        }

        return new Point(centerX / poly.npoints, centerY / poly.npoints);
    }
}