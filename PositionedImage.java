package javaprojectview.graphics;

import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;

public class PositionedImage {
    
    public int x, y;
    public int width, height;
    
    private Image image;
    
    public PositionedImage() {
        this(null);
    }
    
    public PositionedImage(Image image) {
        this(image, 0, 0);
    }
    
    public PositionedImage(Image image, int x, int y) {
        this.x = x;
        this.y = y;
        setImage(image);
    }

    public Rectangle getBounds() {
        return new Rectangle(x - width / 2, y - height / 2, width, height);
    }
    
    public Point getConnectionPoint(PositionedImage targetImage) {
        return getConnectionPoint(getBounds(), targetImage.getBounds());
    }
    
    public static Point getConnectionPoint(Rectangle bounds, Rectangle targetBounds) {
        int x = bounds.x;
        int y = bounds.y;
        int width = bounds.width;
        int height = bounds.height;
        int x1 = x;
        int x2 = x + width;
        int y1 = y;
        int y2 = y + height;
        int targetX = targetBounds.x + targetBounds.width / 2;
        int targetY = targetBounds.y + targetBounds.height / 2;
        // Make (x, y) equal to the center of bounds.
        x += width / 2;
        y += height / 2;
        int deltaX = targetX - x;
        int deltaY = targetY - y;
        Point[] points = new Point[4];
        if (deltaX == 0.0) {
            // The line is vertical.
            return new Point(x, deltaY > 0 ? y2 : y1);
        } else {
            // The line is not vertical.
            double slope = (double) deltaY / (double) deltaX;
            points[0] = new Point(x1, (int) (y - 0.5 * width * slope + 0.5));
            points[1] = new Point(x2, (int) (y + 0.5 * width * slope + 0.5));
        }
        if (deltaY == 0.0) {
            // The line is horizontal.
            return new Point(deltaX > 0 ? x2 : x1, y);
        } else {
            // The line is not horizontal.
            double slope = (double) deltaX / (double) deltaY;
            points[2] = new Point((int) (x - height * 0.5 * slope + 0.5), y1);
            points[3] = new Point((int) (x + height * 0.5 * slope + 0.5), y2);
        }
        Point targetPoint = new Point(targetX, targetY);
        Point closestPoint = null;
        double minDistance = Double.POSITIVE_INFINITY;
        for (Point point : points) {
            // Only accept points that lie on the rectangle.
            if (x1 <= point.x && point.x <= x2 && y1 <= point.y && point.y <= y2) {
                double distance = point.distanceSq(targetPoint);
                if (distance < minDistance) {
                    minDistance = distance;
                    closestPoint = point;
                }
            }
        }
        return closestPoint;
    }
    
    public Image getImage() {
        return image;
    }
    
    public final void setImage(Image image) {
        this.image = image;
        if (image == null) {
            width = 0;
            height = 0;
        } else {
            width = image.getWidth(null);
            height = image.getHeight(null);
        }
    }
}
