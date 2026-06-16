package com.parking.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.awt.image.BufferedImage;
import java.util.Map;

/**
 * Génération de QR Code pour les tickets (§8 CDC — ZXing).
 */
public final class QrCodeGenerator {

    private QrCodeGenerator() {
    }

    public static Image generateFxImage(String content, int size) {
        try {
            return toFxImage(generateBufferedImage(content, size));
        } catch (WriterException e) {
            throw new IllegalStateException("Impossible de générer le QR Code : " + e.getMessage(), e);
        }
    }

    private static WritableImage toFxImage(BufferedImage bufferedImage) {
        WritableImage image = new WritableImage(bufferedImage.getWidth(), bufferedImage.getHeight());
        PixelWriter writer = image.getPixelWriter();
        for (int y = 0; y < bufferedImage.getHeight(); y++) {
            for (int x = 0; x < bufferedImage.getWidth(); x++) {
                writer.setArgb(x, y, bufferedImage.getRGB(x, y));
            }
        }
        return image;
    }

    public static BufferedImage generateBufferedImage(String content, int size) throws WriterException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size,
                Map.of(EncodeHintType.CHARACTER_SET, "UTF-8"));

        int width = matrix.getWidth();
        int height = matrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        return image;
    }
}
