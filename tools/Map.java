import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class Map {
    public static void main(String[] a) throws Exception {
        BufferedImage img = ImageIO.read(new File(a[0]));
        int W = img.getWidth(), H = img.getHeight();
        for (int gy = 0; gy < 24; gy++) {
            StringBuilder sb = new StringBuilder();
            for (int gx = 0; gx < 48; gx++) {
                int rgb = img.getRGB(gx * W / 48, gy * H / 24);
                int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
                int mx = Math.max(r, Math.max(g, b)), mn = Math.min(r, Math.min(g, b));
                char c;
                if (mx < 70) c = '#';
                else if (mx > 200 && mn > 200) c = '.';
                else if (b > r && b > g) c = 'B';
                else if (r > g && r > b) c = 'R';
                else if (g > r && g >= b) c = 'G';
                else if (r > 200 && g > 140 && b < 100) c = 'Y';
                else c = '?';
                sb.append(c);
            }
            System.out.println(sb);
        }
    }
}
