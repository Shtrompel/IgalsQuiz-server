package com.igalblech.igalsquizserver;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class Utils {

    public static String imageToBase64String(BufferedImage image, String type) {
        String ret = null;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, type, bos);
            byte[] bytes = bos.toByteArray();
            ret = new String(Base64.getEncoder().encode(bytes));
        } catch (IOException e) {
            throw new RuntimeException();
        }
        return ret;
    }

    public static BufferedImage fromFXImage(Image fxImage) {
        int width = (int) fxImage.getWidth();
        int height = (int) fxImage.getHeight();
        BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        PixelReader pixelReader = fxImage.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color fxColor = pixelReader.getColor(x, y);
                int argb = ((int) (fxColor.getOpacity() * 255) << 24) |
                        ((int) (fxColor.getRed() * 255) << 16) |
                        ((int) (fxColor.getGreen() * 255) << 8) |
                        ((int) (fxColor.getBlue() * 255));
                bufferedImage.setRGB(x, y, argb);
            }
        }
        return bufferedImage;
    }

    public static String encodeImageToBase64(Image image) {
        try {
            // Convert JavaFX Image to BufferedImage
            BufferedImage bufferedImage = fromFXImage(image);

            // Write image to byte array output stream
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", outputStream); // Replace "png" with your image format if needed

            // Encode byte array to Base64
            return Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Image decodeBase64ToImage(String base64String) {
        try {
            if (base64String.startsWith("data:image")) {
                base64String = base64String.substring(base64String.indexOf(",") + 1);
            }

            // Decode Base64 string to byte array
            byte[] imageBytes = Base64.getDecoder().decode(base64String);

            // Convert byte array into an InputStream
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);

            // Create and return JavaFX Image from InputStream
            return new Image(inputStream);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }


    public static int[] findClosestProduct(int x)
    {
        int index = (int)Math.sqrt(x);
        int factorA = 1, factorB = x;

        for (int i = index; i >= 1; i--)
        {
            if (x % i == 0)
            {
                factorA = i;
                factorB = x / i;
                break;
            }
        }
        return new int[]{
                Math.min(factorA, factorB),
                Math.max(factorA, factorB)};
    }


    public static int[] oklabToSRGB(double L, double a, double b) {
        double l = L + a * +0.3963377774 + b * +0.2158037573;
        double m = L + a * -0.1055613458 + b * -0.0638541728;
        double s = L + a * -0.0894841775 + b * -1.2914855480;

        l = Math.pow(l, 3);
        m = Math.pow(m, 3);
        s = Math.pow(s, 3);

        double r = l * +4.0767416621 + m * -3.3077115913 + s * +0.2309699292;
        double g = l * -1.2684380046 + m * +2.6097574011 + s * -0.3413193965;
        double b_ = l * -0.0041960863 + m * -0.7034186147 + s * +1.7076147010;

        r = 255 * linearToGamma(r);
        g = 255 * linearToGamma(g);
        b_ = 255 * linearToGamma(b_);

        r = clamp(r, 0, 255);
        g = clamp(g, 0, 255);
        b_ = clamp(b_, 0, 255);

        return new int[]{(int) Math.round(r), (int) Math.round(g), (int) Math.round(b_)};
    }

    private static double linearToGamma(double c) {
        return c >= 0.0031308 ? 1.055 * Math.pow(c, 1.0 / 2.4) - 0.055 : 12.92 * c;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
