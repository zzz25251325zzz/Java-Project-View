package javaprojectview.graphics;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javaprojectview.Settings;
import javaprojectview.parser.FieldInfo;
import javaprojectview.parser.MethodInfo;

public class ClassInfoPainter implements TextImagePainter {
    
    private static final float BASE_TITLE_FONT_SIZE = 13.0f;
    private static final float BASE_TITLE_MARGIN_PIXELS = 5.0f;
    private static final float BASE_BORDER_PIXELS = 3.0f;
    private static final float BASE_MARGIN_PIXELS = 10.0f;
    private static final float BASE_SEPARATOR_HEIGHT = 7.0f;
    private static final float BASE_SEPARATOR_THICKNESS = 1.0f;
    
    private final SmartText title;
    private final ArrayList<SmartText> fieldTexts;
    private final ArrayList<SmartText> methodTexts;
    
    public ClassInfoPainter(SmartText title) {
        this.title = title;
        fieldTexts = new ArrayList<>();
        methodTexts = new ArrayList<>();
    }
    
    public void add(FieldInfo fieldInfo) {
        fieldTexts.add(fieldInfo.toSmartText());
    }
    
    public void add(MethodInfo methodInfo) {
        methodTexts.add(methodInfo.toSmartText());
    }
    
    // Create an image based on the information about this class. This image may be
    // used in a class diagram.
    @Override
    public Image paintImage(float fontSize) {
        Settings settings = Settings.getInstance();
        float zoomRatio = fontSize / settings.getBaseFontSize();
        int numFields = fieldTexts.size();
        int numMethods = methodTexts.size();
        boolean useSeparator = numFields != 0 && numMethods != 0;
        Image titleImage = title.paintImage(Math.max(Math.round(zoomRatio * BASE_TITLE_FONT_SIZE), fontSize + 1.0f));
        int titleWidth = titleImage.getWidth(null);
        int titleHeight = titleImage.getHeight(null);
        int titleMargin = (int) (zoomRatio * BASE_TITLE_MARGIN_PIXELS + 0.5f);
        Image[] images = new Image[numFields + numMethods + (useSeparator ? 1 : 0)];
        int imageIndex = 0;
        int width = titleWidth + 2 * titleMargin;
        int height = 0;
        int separatorHeight = 0;
        for (int listIndex = 0; listIndex < 2; ++listIndex) {
            // Use the field texts in the first iteration, and the method texts in the second iteration.
            ArrayList<SmartText> memberTexts = listIndex == 0 ? fieldTexts : methodTexts;
            // Prepare the images for the field/method texts and measure them.
            for (SmartText text : memberTexts) {
                Image image = text.paintImage(fontSize);
                images[imageIndex++] = image;
                int textWidth = image.getWidth(null);
                int textHeight = image.getHeight(null);
                if (textWidth > width) {
                    // Store the widest text's width.
                    width = textWidth;
                }
                // Add all heights together to get the total height.
                height += textHeight;
            }
            // If a separator needs to be drawn, then put it right after the fields and before the methods.
            if (useSeparator && listIndex == 0) {
                // Use null as a placeholder for the separator.
                images[imageIndex++] = null;
                // Calculate the height of the separator (including empty space) and add it to the total height.
                // Must be at least 1 pixel, so use Math.max.
                separatorHeight = Math.max((int) (zoomRatio * BASE_SEPARATOR_HEIGHT + 0.5), 1);
                height += separatorHeight;
            }
        }
        if (width == 0 || height == 0) {
            return settings.getEmptyImage();
        }
        int separatorWidth = width;
        // Compute the border thickness and margin for this zoomRatio.
        int borderPixels = Math.max((int) (zoomRatio * BASE_BORDER_PIXELS + 0.5), 1);
        int marginPixels = (int) (zoomRatio * BASE_MARGIN_PIXELS + 0.5);
        int sidePadding = borderPixels + marginPixels;
        // Add the width of the border and the margin to the width and height twice, since it's on the left and
        // right side, and on the top and bottom sides.
        width += sidePadding * 2;
        height += sidePadding * 2;
        // Compute how many pixels of the title (class name) stick out at the top and add them to the height.
        int titleSpace = Math.max((titleHeight - borderPixels) / 2, 0);
        height += titleSpace;
        // Create image for this panel.
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics graphics = image.getGraphics();
        settings.applyDesktopHints(graphics);
        // Draw brackground.
        graphics.setColor(settings.getColor("info-background"));
        int halfBorderThickness = borderPixels / 2;
        graphics.fillRect(halfBorderThickness, halfBorderThickness + titleSpace, width - halfBorderThickness, height - halfBorderThickness - titleSpace);
        // Draw title.
        int titleX = (width - titleWidth) / 2;
        graphics.drawImage(titleImage, titleX, -1, null);
        // Draw border.
        graphics.setColor(settings.getColor("info-border"));
        graphics.fillRect(0, titleSpace, titleX - titleMargin, borderPixels); // left part of top border
        graphics.fillRect(width - titleX + titleMargin, titleSpace, titleX - titleMargin, borderPixels); // right part of top border
        graphics.fillRect(0, titleSpace, borderPixels, height - titleSpace); // left border
        graphics.fillRect(width - borderPixels, titleSpace, borderPixels, height - titleSpace); // right border
        graphics.fillRect(0, height - borderPixels, width, borderPixels); // bottom border
        int y = sidePadding + titleSpace;
        for (Image partImage : images) {
            if (partImage != null) {
                // If the image for the current line of text isn't null, then draw it.
                graphics.drawImage(partImage, sidePadding, y, null);
                y += partImage.getHeight(null);
            } else {
                // Otherwise, draw a separator instead.
                graphics.setColor(settings.getColor("separator"));
                // Calculate the thickness of the separator (at least 1).
                int separatorThickness = Math.max((int) (zoomRatio * BASE_SEPARATOR_THICKNESS + 0.5), 1);
                graphics.fillRect(sidePadding, y + (int) ((separatorHeight - separatorThickness) * 0.5f), separatorWidth, separatorThickness);
                y += separatorHeight;
            }
        }
        // Finish drawing the image.
        graphics.dispose();
        image.flush();
        return image;
    }
}
