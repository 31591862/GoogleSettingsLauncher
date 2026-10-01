// Renders the generated launcher-icon layers the way a launcher really shows them,
// so mask clipping and *relative size* can be checked without navigating a device UI.
//
//   java Preview.java <resDir> <out.png>
//
// Geometry: the adaptive canvas is 108dp but only the middle 72dp is visible
// (18dp padding per side is masked away). Launchers therefore scale the layers to
// 1.5x the tile size and crop the centre -- drawing the canvas at 1x, which looks
// fine, silently shrinks everything by a third.

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Preview {

    static final int PX = 162;            // xxhdpi rendering of a 54dp tile
    static final double CANVAS_SCALE = 108.0 / 72.0;   // 108dp canvas -> 72dp visible

    public static void main(String[] args) throws IOException {
        File res = new File(args[0]);
        BufferedImage fg = ImageIO.read(new File(res, "drawable-xxhdpi/ic_launcher_foreground.png"));
        BufferedImage mono = ImageIO.read(new File(res, "drawable-xxhdpi/ic_launcher_monochrome.png"));
        BufferedImage legacy = ImageIO.read(new File(res, "mipmap-xxhdpi/ic_launcher.png"));
        BufferedImage round = ImageIO.read(new File(res, "mipmap-xxhdpi/ic_launcher_round.png"));

        BufferedImage adaptive = compose(fg, Color.WHITE);
        BufferedImage themed = compose(mono, new Color(0x1A73E8));

        int cell = PX + 34;
        BufferedImage out = new BufferedImage(cell * 5 + 40, PX * 2 + 150, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = g(out);
        g.setColor(new Color(0x4A6FA5));
        g.fillRect(0, 0, out.getWidth(), out.getHeight());
        g.setColor(Color.WHITE);
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.PLAIN, 22));

        String[] labels = {"\u5706\u89d2\u77e9\u5f62\u906e\u7f69", "\u5706\u5f62\u906e\u7f69",
                "\u65e7\u7248\u65b9\u5f62", "\u65e7\u7248\u5706\u5f62", "\u6df1\u8272\u4e3b\u9898"};
        BufferedImage[] tiles = {
                mask(adaptive, MaskKind.SQUIRCLE),
                mask(adaptive, MaskKind.CIRCLE),
                legacy,
                round,
                mask(themed, MaskKind.SQUIRCLE),
        };
        for (int i = 0; i < tiles.length; i++) {
            int x = 20 + i * cell;
            g.drawImage(tiles[i], x, 20, PX, PX, null);
            g.drawString(labels[i], x + 4, PX + 50);
        }

        // geometry diagram: the whole 108dp canvas, with the 72dp visible window marked
        int diagY = PX + 90;
        g.drawImage(fg, 20, diagY, PX, PX, null);
        g.setColor(new Color(0xF1F3F4));
        g.fillRect(20 + PX + 24, diagY + 8, 4, PX - 16);
        g.drawString("108dp \u753b\u5e03\u539f\u6837\uff08\u6846 = \u542f\u52a8\u5668\u5b9e\u9645\u663e\u793a\u7684 72dp\uff09",
                20 + PX + 46, diagY + 40);
        g.setColor(new Color(0xFBBC04));
        g.setStroke(new BasicStroke(3f));
        int inset = (int) Math.round(PX * (1 - 1 / CANVAS_SCALE) / 2);
        g.drawRect(20 + inset, diagY + inset, PX - 2 * inset, PX - 2 * inset);
        g.dispose();

        File target = new File(args[1]);
        ImageIO.write(out, "png", target);
        System.out.println("preview -> " + target.getAbsolutePath());
    }

    enum MaskKind { SQUIRCLE, CIRCLE }

    /** Background colour + foreground layer, both at launcher scale, on a tile-sized canvas. */
    static BufferedImage compose(BufferedImage fgLayer, Color background) {
        BufferedImage out = new BufferedImage(PX, PX, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = g(out);
        g.setColor(background);
        g.fillRect(0, 0, PX, PX);
        int big = (int) Math.round(PX * CANVAS_SCALE);
        int off = (PX - big) / 2;
        g.drawImage(fgLayer, off, off, big, big, null);
        g.dispose();
        return out;
    }

    static BufferedImage mask(BufferedImage src, MaskKind kind) {
        BufferedImage out = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = g(out);
        if (kind == MaskKind.CIRCLE) {
            g.fill(new Ellipse2D.Float(0, 0, src.getWidth(), src.getHeight()));
        } else {
            float r = src.getWidth() * 0.24f;
            g.fill(new RoundRectangle2D.Float(0, 0, src.getWidth(), src.getHeight(), r, r));
        }
        g.setComposite(AlphaComposite.SrcIn);
        g.drawImage(src, 0, 0, null);
        g.dispose();
        return out;
    }

    static Graphics2D g(BufferedImage img) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g;
    }
}
