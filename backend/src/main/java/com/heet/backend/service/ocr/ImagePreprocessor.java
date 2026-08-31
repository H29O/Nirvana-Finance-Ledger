package com.heet.backend.service.ocr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Slf4j
@Component
public class ImagePreprocessor {

    private static final int TARGET_MIN_DIMENSION = 1400;
    private static final int MAX_DIMENSION = 3500;

    /**
     * Preprocess image for OCR: upscale small images and convert to clean grayscale.
     */
    public BufferedImage preprocess(File file) {
        try {
            BufferedImage original = ImageIO.read(file);
            if (original == null) {
                return null;
            }

            int width = original.getWidth();
            int height = original.getHeight();

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

            BufferedImage grayscale = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_BYTE_GRAY);
            Graphics2D g2d = grayscale.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.drawImage(original, 0, 0, targetWidth, targetHeight, null);
            g2d.dispose();

            return grayscale;
        } catch (IOException ex) {
            log.warn("Failed to preprocess image for OCR: {}", ex.getMessage());
            return null;
        }
    }
}
