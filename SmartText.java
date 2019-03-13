package javaprojectview.graphics;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import javaprojectview.Settings;

// Text object consisting of multiple pieces of text with different colors and styles.
public class SmartText implements TextImagePainter {
    
    // Text parts that this text object consists of.
    private final ArrayList<SmartTextPart> parts;
    
    // Create text using smart text parts.
    public SmartText(SmartTextPart... parts) {
        this.parts = new ArrayList<>(Arrays.asList(parts));
    }
    
    // Append a text part. Returns this object so that you can append multiple times
    // in a row easily.
    public SmartText append(SmartTextPart part) {
        parts.add(part);
        return this;
    }
    
    // Append all the text parts from the other smart text object to this one.
    public SmartText append(SmartText other) {
        if (other != null) {
            for (SmartTextPart part : other.parts) {
                parts.add(part);
            }
        }
        return this;
    }
    
    // Seven different methods to create and append a text part (one for each
    // constructor of SmartTextPart).
    public SmartText append(String text) {
        return append(new SmartTextPart(text));
    }
    
    public SmartText append(String text, String colorNames) {
        return append(new SmartTextPart(text, colorNames));
    }
    
    public SmartText append(String text, String colorNames, boolean underlined) {
        return append(new SmartTextPart(text, colorNames, underlined));
    }
    
    public SmartText append(String text, String colorNames, String underlineColorNames) {
        return append(new SmartTextPart(text, colorNames, underlineColorNames));
    }
    
    public SmartText append(String text, int style, String colorNames) {
        return append(new SmartTextPart(text, style, colorNames));
    }
    
    public SmartText append(String text, int style, String colorNames, boolean underlined) {
        return append(new SmartTextPart(text, style, colorNames, underlined));
    }
    
    public SmartText append(String text, int style, String colorNames, String underlineColorNames) {
        return append(new SmartTextPart(text, style, colorNames, underlineColorNames));
    }
    
    // Create an image for this text using the given font size.
    @Override
    public Image paintImage(float fontSize) {
        Settings settings = Settings.getInstance();
        Image[] partImages = new Image[parts.size()];
        int width = 0;
        int height = 0;
        int i = 0;
        for (SmartTextPart part : parts) {
            // Draw the part with the font and add it to the list of images.
            Image partImage = part.paintImage(fontSize);
            partImages[i++] = partImage;
            int partWidth = partImage.getWidth(null);
            int partHeight = partImage.getHeight(null);
            // Add the widths of the parts together to get the total width.
            width += partImage.getWidth(null);
            if (partHeight > height) {
                // Make the overall image height the height of the tallest part.
                height = partHeight;
            }
        }
        if (width == 0 || height == 0) {
            // If the width or height is 0 then the image is empty. A BufferedImage then cannot
            // be created, so create a dummy image instead.
            return settings.getEmptyImage();
        }
        // Create image for this text.
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics graphics = image.getGraphics();
        settings.applyDesktopHints(graphics);
        int x = 0;
        for (Image partImage : partImages) {
            // Draw the text for the part using the image created earlier.
            graphics.drawImage(partImage, x, 0, null);
            // Then move to the right until after the part, so the next part can be drawn after it.
            x += partImage.getWidth(null);
        }
        // Finish drawing the image.
        image.flush();
        graphics.dispose();
        return image;
    }
    
    // Return the plain text of this part.
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        // Append all the text parts that this text consists of to get the whole text.
        for (SmartTextPart part : parts) {
            builder.append(part.toString());
        }
        return builder.toString();
    }
}
