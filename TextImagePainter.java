package javaprojectview.graphics;

import java.awt.Image;

// Objects of this type can create an image object when supplied with a font size.
public interface TextImagePainter {

    // Create an image object based on the font size.
    public abstract Image paintImage(float fontSize);
}
