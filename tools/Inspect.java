import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class Inspect {
    public static void main(String[] a) throws Exception {
        BufferedImage img = ImageIO.read(new File(a[0]));
        System.out.println("size=" + img.getWidth() + "x" + img.getHeight() + " type=" + img.getType());
        int minx = img.getWidth(), miny = img.getHeight(), maxx = -1, maxy = -1;
        System.out.println("corner TL argb=" + Integer.toHexString(img.getRGB(2, 2))
            + " TR=" + Integer.toHexString(img.getRGB(img.getWidth() - 3, 2))
            + " center=" + Integer.toHexString(img.getRGB(img.getWidth() / 2, img.getHeight() / 2)));
        int opaque = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int al = (img.getRGB(x, y) >>> 24) & 0xff;
                if (al > 8) { opaque++; if (x<minx)minx=x; if(x>maxx)maxx=x; if(y<miny)miny=y; if(y>maxy)maxy=y; }
            }
        }
        System.out.println("opaquePx=" + opaque + " bounds=" + minx + "," + miny + " - " + maxx + "," + maxy);
    }
}
