package javaprojectview.graphics;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javaprojectview.Settings;

// Piece of text with a specific color and underline color.
public class SmartTextPart implements TextImagePainter {
    
    private final String text;
    private final boolean bold;
    private final boolean italic;
    private final String colorNames;
    private final String underlineColorNames;
    
    public SmartTextPart(String text) {
        this(text, 0, null, null);
    }
    
    public SmartTextPart(String text, String colorNames) {
        this(text, 0, colorNames, null);
    }
    
    public SmartTextPart(String text, String colorNames, boolean underlined) {
        this(text, 0, colorNames, underlined);
    }
    
    public SmartTextPart(String text, String colorNames, String underlineColorNames) {
        this(text, 0, colorNames, underlineColorNames);
    }
    
    public SmartTextPart(String text, int style, String colorNames) {
        this(text, style, colorNames, null);
    }
    
    public SmartTextPart(String text, int style, String colorNames, boolean underlined) {
        this(text, style, colorNames, underlined ? colorNames == null ? "foreground" : colorNames : null);
    }
    
    public SmartTextPart(String text, int style, String colorNames, String underlineColorNames) {
        this.text = text;
        this.bold = (style & Font.BOLD) != 0;
        this.italic = (style & Font.ITALIC) != 0;
        this.colorNames = colorNames;
        this.underlineColorNames = underlineColorNames;
    }
    
    // Create an image for this text part using the given font size.
    @Override
    public Image paintImage(float fontSize) {
        Settings settings = Settings.getInstance();
        Font font = settings.getFont(fontSize, bold, italic);
        FontMetrics metrics = settings.getFontMetrics(font);
        // Measure the size of the text.
        int width = metrics.stringWidth(text);
        int height = metrics.getHeight();
        if (width == 0 || height == 0) {
            // If the width or height is 0 then the image is empty. A BufferedImage then cannot
            // be created, so create a dummy image instead.
            return settings.getEmptyImage();
        }
        // Create a buffer for the image.
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
        Graphics2D graphics = (Graphics2D) image.getGraphics();
        // Apply default desktop rendering hints for better quality.
        settings.applyDesktopHints(graphics);
        // If the underlineColor isn't null, then draw a line under the text.
        if (underlineColorNames != null) {
            float underlineThickness = fontSize / settings.getBaseFontSize();
            Color underlineColor = settings.getColor(underlineColorNames);
            if (underlineThickness < 1.0f) {
                // If the underline is less than one pixel thick, then draw a one pixel thick,
                // semi-transparent line.
                underlineColor = new Color(((int) (0xFF * underlineThickness) << 24) | (underlineColor.getRGB() & 0xFFFFFF), true);
                underlineThickness = 1.0f;
            }
            graphics.setColor(underlineColor);
            int underlineY = metrics.getAscent() + 1;
            graphics.fillRect(0, underlineY, width, (int) underlineThickness);
        }
        // Set the font and color for the text.
        graphics.setColor(settings.getColor(colorNames));
        graphics.setFont(font);
        // Draw the text.
        graphics.drawString(text, 0, metrics.getMaxAscent());
        // Finish drawing the image.
        image.flush();
        graphics.dispose();
        return image;
    }
    
    // Return the plain text of this part.
    @Override
    public String toString() {
        return text;
    }
}
