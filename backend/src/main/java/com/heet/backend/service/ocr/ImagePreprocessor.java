package com.heet.backend.service.ocr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;

@Slf4j
@Component
public class ImagePreprocessor {

    private static final int TARGET_MIN_DIMENSION = 1500;
    private static final int MAX_DIMENSION = 3200;

    /**
     * Preprocess image file for OCR: scales, converts to grayscale, applies
     * histogram-based auto-contrast stretching and unsharp edge sharpening.
     */
    public BufferedImage preprocess(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        try {
            BufferedImage original = ImageIO.read(file);
            return preprocess(original);
        } catch (IOException ex) {
            log.warn("Failed to preprocess image for OCR: {}", ex.getMessage());
            return null;
        }
    }

    /**
     * Enhanced preprocessing pipeline suitable for photographed and scanned paper receipts.
     */
    public BufferedImage preprocess(BufferedImage original) {
        if (original == null) {
            return null;
        }

        int width = original.getWidth();
        int height = original.getHeight();

        // 1. Resizing/upscaling: ensure sufficient pixel density for small character OCR
        double scale = 1.0;
        if (width < TARGET_MIN_DIMENSION || height < TARGET_MIN_DIMENSION) {
            double scaleW = (double) TARGET_MIN_DIMENSION / width;
            double scaleH = (double) TARGET_MIN_DIMENSION / height;
            scale = Math.max(scaleW, scaleH);
        } else if (width > MAX_DIMENSION || height > MAX_DIMENSION) {
            double scaleW = (double) MAX_DIMENSION / width;
            double scaleH = (double) MAX_DIMENSION / height;
            scale = Math.min(scaleW, scaleH);
        }

        int targetWidth = (int) Math.round(width * scale);
        int targetHeight = (int) Math.round(height * scale);

        // 2. Grayscale conversion with bicubic interpolation
        BufferedImage gray = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g2d = gray.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
        g2d.dispose();

        // 3. Contrast enhancement: Auto-contrast histogram stretching
        applyAutoContrast(gray);

        // 4. Edge-preserving sharpening to clarify faint thermal print and dot-matrix characters
        return applySharpen(gray);
    }

    private void applyAutoContrast(BufferedImage gray) {
        byte[] pixels = ((DataBufferByte) gray.getRaster().getDataBuffer()).getData();
        int[] hist = new int[256];
        for (byte b : pixels) {
            hist[b & 0xFF]++;
        }

        int totalPixels = pixels.length;
        int lowCut = (int) (totalPixels * 0.02);
        int highCut = (int) (totalPixels * 0.98);

        int count = 0;
        int minVal = 0;
        for (int i = 0; i < 256; i++) {
            count += hist[i];
            if (count >= lowCut) {
                minVal = i;
                break;
            }
        }

        count = 0;
        int maxVal = 255;
        for (int i = 255; i >= 0; i--) {
            count += hist[i];
            if (count >= (totalPixels - highCut)) {
                maxVal = i;
                break;
            }
        }

        if (maxVal > minVal) {
            float range = (float) (maxVal - minVal);
            for (int i = 0; i < pixels.length; i++) {
                int val = pixels[i] & 0xFF;
                if (val <= minVal) {
                    pixels[i] = 0;
                } else if (val >= maxVal) {
                    pixels[i] = (byte) 255;
                } else {
                    pixels[i] = (byte) Math.round(((val - minVal) / range) * 255f);
                }
            }
        }
    }

    private BufferedImage applySharpen(BufferedImage gray) {
        float[] kernel = {
             0.0f, -0.4f,  0.0f,
            -0.4f,  2.6f, -0.4f,
             0.0f, -0.4f,  0.0f
        };
        BufferedImageOp op = new ConvolveOp(new Kernel(3, 3, kernel), ConvolveOp.EDGE_NO_OP, null);
        BufferedImage sharpened = new BufferedImage(gray.getWidth(), gray.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        return op.filter(gray, sharpened);
    }
}
