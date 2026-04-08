package com.roofingcrm.service.accounting;

import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.List;

@Component
public class ReceiptSummaryRegionExtractor {

    /**
     * Bottom-right summary crops. Slightly larger than legacy ratios so totals labels/digits are not clipped;
     * shiftUp pulls the window upward to include Subtotal/Tax lines above the bottom edge.
     */
    public List<SummaryRegionCrop> extractVariants(BufferedImage image) {
        return List.of(
                crop(image, "tight", 3, 0.48d, 0.44d, 0d, 0.08d),
                crop(image, "large", 2, 0.58d, 0.54d, 0d, 0.05d),
                crop(image, "expanded", 2, 0.54d, 0.50d, 0.04d, 0.08d)
        );
    }

    private SummaryRegionCrop crop(BufferedImage image,
                                   String id,
                                   int weight,
                                   double widthRatio,
                                   double heightRatio,
                                   double shiftLeftRatio,
                                   double shiftUpRatio) {
        int cropWidth = Math.max(1, (int) Math.round(image.getWidth() * widthRatio));
        int cropHeight = Math.max(1, (int) Math.round(image.getHeight() * heightRatio));
        int x = Math.max(0, image.getWidth() - cropWidth - (int) Math.round(image.getWidth() * shiftLeftRatio));
        int y = Math.max(0, image.getHeight() - cropHeight - (int) Math.round(image.getHeight() * shiftUpRatio));
        cropWidth = Math.min(cropWidth, image.getWidth() - x);
        cropHeight = Math.min(cropHeight, image.getHeight() - y);
        BufferedImage cropped = image.getSubimage(x, y, cropWidth, cropHeight);
        return new SummaryRegionCrop(id, weight, x, y, cropWidth, cropHeight, cropped);
    }

    public record SummaryRegionCrop(
            String id,
            int weight,
            int x,
            int y,
            int width,
            int height,
            BufferedImage image
    ) {
    }
}
