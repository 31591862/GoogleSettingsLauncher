// Generates Android launcher icon assets from a flat source PNG.
//
//   java MakeIcons.java <source.png> <resDir>
//
// Source art is a full-bleed tile (white background, black outside corners).
// This tool extracts only the colourful artwork (gear + G) as the adaptive-icon
// foreground layer, shrinks it into the 108dp safe zone, and derives:
//   mipmap-<dpi>/ic_launcher.png          legacy square icon (API < 26)
//   mipmap-<dpi>/ic_launcher_round.png    legacy round icon
//   drawable-<dpi>/ic_launcher_foreground.png   adaptive foreground
//   drawable-<dpi>/ic_launcher_monochrome.png   themed-icon silhouette
//   mipmap-anydpi-v26/ic_launcher(.round).xml   adaptive icon descriptors
// The adaptive background is a solid colour resource (@color/ic_launcher_background).

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class MakeIcons {

    // icon canvas size in dp per density bucket, and the adaptive-canvas dp
    static final int[] DENS = {160, 240, 320, 480, 640};
    static final String[] DENS_NAME = {"mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"};
    static final int LEGACY_DP = 48;
    static final int FOREGROUND_DP = 108;

    // fraction of the 108dp canvas the artwork may cover. The launcher shows only the
    // middle 72dp (18dp padding per side is masked away) and scales the canvas up 1.5x
    // to fill it, so 0.46 (=50dp art) leaves the gear at ~69% of the visible tile,
    // which is what neighbouring icons use. 0.60 (=65dp) filled 90% and looked oversized.
    static final double ART_COVERAGE = 0.46;
    // fraction of the 48dp legacy tile the artwork covers (legacy has no 1.5x canvas)
    static final double LEGACY_ART_COVERAGE = 0.68;

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("usage: java MakeIcons.java <source.png> <resDir>");
            System.exit(2);
        }
        File src = new File(args[0]);
        File res = new File(args[1]);
        BufferedImage in = ImageIO.read(src);
        System.out.println("source: " + src + " " + in.getWidth() + "x" + in.getHeight());

        BufferedImage art = extractArtwork(in);
        Rectangle bounds = contentBounds(art);
        BufferedImage cropped = crop(art, bounds);
        System.out.println("artwork bounds: " + bounds + " -> " + cropped.getWidth() + "x" + cropped.getHeight());

        for (int i = 0; i < DENS.length; i++) {
            String dir = DENS_NAME[i];
            int fgPx = dp(FOREGROUND_DP, DENS[i]);
            int legacyPx = dp(LEGACY_DP, DENS[i]);

            BufferedImage fg = fitOnCanvas(cropped, fgPx, ART_COVERAGE);
            write(fg, new File(res, "drawable-" + dir + "/ic_launcher_foreground.png"));

            BufferedImage mono = silhouette(fg);
            write(mono, new File(res, "drawable-" + dir + "/ic_launcher_monochrome.png"));

            write(tile(fg, legacyPx, false), new File(res, "mipmap-" + dir + "/ic_launcher.png"));
            write(tile(fg, legacyPx, true), new File(res, "mipmap-" + dir + "/ic_launcher_round.png"));
            System.out.println(dir + ": legacy " + legacyPx + "px, foreground " + fgPx + "px");
        }

        File any = new File(res, "mipmap-anydpi-v26");
        any.mkdirs();
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<adaptive-icon xmlns:android=\"http://schemas.android.com/apk/res/android\">\n"
                + "    <background android:drawable=\"@color/ic_launcher_background\" />\n"
                + "    <foreground android:drawable=\"@drawable/ic_launcher_foreground\" />\n"
                + "    <monochrome android:drawable=\"@drawable/ic_launcher_monochrome\" />\n"
                + "</adaptive-icon>\n";
        writeText(new File(any, "ic_launcher.xml"), xml);
        writeText(new File(any, "ic_launcher_round.xml"), xml);

        writeText(new File(res, "values/ic_launcher_background.xml"),
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<resources>\n"
                        + "    <color name=\"ic_launcher_background\">#FFFFFFFF</color>\n</resources>\n");

        System.out.println("done");
    }

    static int dp(int dps, int density) {
        return Math.round(dps * density / 160f);
    }

    /** Keep only chromatic pixels; white tile, black corners and their AA grey rim go transparent. */
    static BufferedImage extractArtwork(BufferedImage src) {
        int w = src.getWidth(), h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = src.getRGB(x, y);
                int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
                int mx = Math.max(r, Math.max(g, b));
                int mn = Math.min(r, Math.min(g, b));
                // chroma-based alpha: neutral pixels (white/black/grey rim) -> 0, brand colours -> 255
                double chroma = mx - mn;
                double a = Math.max(0, Math.min(1, (chroma - 22.0) / 48.0));
                int alpha = (int) Math.round(a * 255);
                out.setRGB(x, y, (alpha << 24) | (rgb & 0x00FFFFFF));
            }
        }
        return out;
    }

    static Rectangle contentBounds(BufferedImage img) {
        int minX = img.getWidth(), minY = img.getHeight(), maxX = -1, maxY = -1;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if (((img.getRGB(x, y) >>> 24) & 255) >= 20) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < 0) throw new IllegalStateException("no artwork found in source");
        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    static BufferedImage crop(BufferedImage img, Rectangle b) {
        BufferedImage out = new BufferedImage(b.width, b.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.drawImage(img, 0, 0, b.width, b.height, b.x, b.y, b.x + b.width, b.y + b.height, null);
        g.dispose();
        return out;
    }

    /** Centred on a transparent canvas, artwork covers `coverage` of the canvas width. */
    static BufferedImage fitOnCanvas(BufferedImage art, int canvasPx, double coverage) {
        BufferedImage out = new BufferedImage(canvasPx, canvasPx, BufferedImage.TYPE_INT_ARGB);
        int target = (int) Math.round(canvasPx * coverage);
        int offset = (canvasPx - target) / 2;
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(art, offset, offset, target, target, null);
        g.dispose();
        return out;
    }

    /** Composite the foreground onto an opaque legacy tile (rounded square or circle). */
    static BufferedImage tile(BufferedImage fg, int px, boolean round) {
        BufferedImage out = new BufferedImage(px, px, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        if (round) {
            g.fill(new Ellipse2D.Float(0, 0, px, px));
        } else {
            float rr = px * 0.22f;
            g.fill(new RoundRectangle2D.Float(0, 0, px, px, rr, rr));
        }
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        int target = (int) Math.round(px * (LEGACY_ART_COVERAGE / ART_COVERAGE));
        int offset = (px - target) / 2;
        g.drawImage(fg, offset, offset, target, target, null);
        g.dispose();
        return out;
    }

    /** Solid near-black silhouette for <monochrome> (themed / dark launcher icons). */
    static BufferedImage silhouette(BufferedImage fg) {
        BufferedImage out = new BufferedImage(fg.getWidth(), fg.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < fg.getHeight(); y++) {
            for (int x = 0; x < fg.getWidth(); x++) {
                int a = (fg.getRGB(x, y) >>> 24) & 255;
                out.setRGB(x, y, (a << 24) | 0x00202020);
            }
        }
        return out;
    }

    static void write(BufferedImage img, File file) throws IOException {
        file.getParentFile().mkdirs();
        ImageIO.write(img, "png", file);
    }

    static void writeText(File file, String text) throws IOException {
        java.nio.file.Files.writeString(file.toPath(), text);
    }

    static class Rectangle {
        final int x, y, width, height;
        Rectangle(int x, int y, int w, int h) { this.x = x; this.y = y; width = w; height = h; }
        public String toString() { return x + "," + y + " " + width + "x" + height; }
    }
}
