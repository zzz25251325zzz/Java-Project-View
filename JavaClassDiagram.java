package javaprojectview.uml;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import javaprojectview.Settings;
import javaprojectview.graphics.ClassInfoPainter;
import javaprojectview.graphics.PositionedImage;
import javaprojectview.parser.ClassInfo;
import javaprojectview.parser.FieldInfo;
import javaprojectview.parser.MethodInfo;
import javaprojectview.parser.ParameterInfo;
import javaprojectview.parser.ValueInfo;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

public class JavaClassDiagram extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener {
    
    private final float MIN_FONT_SIZE = 2.0f;
    private final float MAX_FONT_SIZE = 24.0f;
    private final float BASE_ARROW_LINE_WIDTH = 2.0f;
    private final float BASE_ARROW_HEAD_RADIUS = 10.0f;
    
    private final int RANDOMSORT_OFFSET_ITERATIONS = 16;
    
    // Virtual X and Y values of top left corner of the JPanel.
    private int viewX, viewY;
    // Size of the font used in class info panels.
    private float fontSize;
    // Internal counter to keep track of how many times the font size has been changed.
    private int resizeCount;
    
    private final ClassInfo[] classes;
    private final HashMap<ClassInfo, Integer> classIndexMap;
    private final PositionedImage[] classInfoImages;
    private final ClassInfoPainter[] classPainters;
    private final ArrayList<Relation> relations;
    
    
    public JavaClassDiagram(ClassInfo[] classes) {
        viewX = 0;
        viewY = 0;
        fontSize = 12.0f;
        resizeCount = 0;
        this.classes = classes;
        classIndexMap = new HashMap<>();
        classInfoImages = new PositionedImage[classes.length];
        classPainters = new ClassInfoPainter[classes.length];
        relations = new ArrayList<>();
        initialize();
        setFontSize(12.0f, 0, 0);
        autoSort();
    }
    
    public void separateBoxes() {
        float zoomRatio = fontSize / Settings.getInstance().getBaseFontSize();
        Random random = new Random();
        int maxOffset = (int) (100 * zoomRatio + 0.5f);
        int offsetIteration = 0;
        boolean valid;
        do {
            int zmOffset = random.nextInt(maxOffset) + 1;
            for (PositionedImage positionedImage : classInfoImages) {
                positionedImage.x = random.nextInt(zmOffset * 2 + 1) - zmOffset;
                positionedImage.y = random.nextInt(zmOffset * 2 + 1) - zmOffset;
            }
            if (++offsetIteration >= RANDOMSORT_OFFSET_ITERATIONS) {
                maxOffset += 1;
                offsetIteration = 0;
            }
            valid = true;
            for (int i = 0; i < classInfoImages.length; ++i) {
                ClassInfo class1 = classes[i];
                PositionedImage image1 = classInfoImages[i];
                Rectangle bounds1 = image1.getBounds();
                for (int j = i + 1; j < classInfoImages.length; ++j) {
                    ClassInfo class2 = classes[j];
                    PositionedImage image2 = classInfoImages[j];
                    Rectangle bounds2 = image2.getBounds();
                    if (bounds1.intersects(bounds2)) {
                        valid = false;
                        break;
                    }
                }
                if (!valid) {
                    break;
                }
            }
        } while (!valid);
    }
    
    public final void autoSort() {
        separateBoxes();
        // Find top left of the diagram.
        Point topLeft = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
        for (PositionedImage image : classInfoImages) {
            topLeft.x = Math.min(image.x - image.width / 2, topLeft.x);
            topLeft.y = Math.min(image.y - image.height / 2, topLeft.y);
        }
        if (topLeft.x == Integer.MAX_VALUE) {
            topLeft.x = 0;
            topLeft.y = 0;
        }
        // Move view to top left.
        viewX = topLeft.x;
        viewY = topLeft.y;
    }
    
    public final synchronized void setFontSize(float fontSize, int centerX, int centerY) {
        int resizeId = ++resizeCount;
        float zoomRatio = fontSize / this.fontSize;
        this.fontSize = fontSize;
        Thread[] resizeThreads = new Thread[classPainters.length];
        Rectangle viewBounds = new Rectangle(viewX, viewY, getWidth(), getHeight());
        for (int i = 0; i < classPainters.length; ++i) {
            PositionedImage positionedImage = classInfoImages[i];
            // Scale the distance between the image's center and the zoom center along with the zoom factor.
            positionedImage.x = (int) ((positionedImage.x - centerX) * zoomRatio + centerX + 0.5f);
            positionedImage.y = (int) ((positionedImage.y - centerY) * zoomRatio + centerY + 0.5f);
            ClassInfoPainter painter = classPainters[i];
            Image image = painter.paintImage(fontSize);
                // Only update image if no other resize processes started in the meantime.
            if (resizeCount == resizeId) {
                positionedImage.setImage(image);
            }
        }
        // Repaint the diagram to apply the changes to class info panel positions.
        repaint();
    }
    
    private void initialize() {
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
        Settings settings = Settings.getInstance();
        setBackground(settings.getColor("diagram-background"));
        for (int i = 0; i < classes.length; ++i) {
            ClassInfo classInfo = classes[i];
            classIndexMap.put(classInfo, i);
            // Add relation for "extends".
            ClassInfo superClass = classInfo.getSuperClass();
            if (superClass != null) {
                addRelation(new Relation(Relation.Type.GENERALIZATION, classInfo, superClass));
            }
            // Add relations for "implements".
            for (ClassInfo interfaceInfo : classInfo.getInterfaces()) {
                addRelation(new Relation(Relation.Type.REALIZATION, classInfo, interfaceInfo));
            }
            // Add weak relation for classes defined in other classes.
            ClassInfo outerClass = classInfo.getOuterClass();
            if (outerClass != null) {
                addRelation(new Relation(Relation.Type.DEPENDENCY, classInfo, outerClass));
            }
            ClassInfoPainter painter = new ClassInfoPainter(classInfo.getSmartTextName());
            for (FieldInfo info : classInfo.getFields()) {
                ClassInfo fieldType = classInfo.resolveClass(info.getTypeName());
                if (fieldType != null) {
                    // Show fields of custom types as associations.
                    addRelation(new Relation(Relation.Type.ASSOCIATION, classInfo, fieldType)); //, info.toSmartText(true)));
                }
                painter.add(info);
            }
            // Add return type, parameter types and variable types of methods as "dependency".
            for (MethodInfo info : classInfo.getMethods()) {
                painter.add(info);
                ClassInfo returnType = classInfo.resolveClass(info.getTypeName());
                if (returnType != null) {
                    addRelation(new Relation(Relation.Type.DEPENDENCY, classInfo, returnType));
                }
                for (ParameterInfo parameterInfo : info.getParameters()) {
                    ClassInfo parameterType = classInfo.resolveClass(parameterInfo.getTypeName());
                    if (parameterType != null) {
                        addRelation(new Relation(Relation.Type.DEPENDENCY, classInfo, parameterType));
                    }
                }
                for (ValueInfo variableInfo : info.getVariables()) {
                    ClassInfo variableType = classInfo.resolveClass(variableInfo.getTypeName());
                    if (variableType != null) {
                        addRelation(new Relation(Relation.Type.DEPENDENCY, classInfo, variableType)); //, new SmartText().append(variableInfo.getName())));
                    }
                }
            }
            classInfoImages[i] = new PositionedImage();
            classPainters[i] = painter;
        }
    }
    
    private void addRelation(Relation relation) {
        for (int i = 0, numRelations = relations.size(); i < numRelations; ++i) {
            Relation existingRelation = relations.get(i);
            if (relation.hasSameConnection(existingRelation)) {
                // If a relationship already exists with the same objects in the same
                // direction as the new relation, and the new relationship is of a more
                // important type than the existing one, then replace it.
                if (relation.isMoreImportantThan(existingRelation)) {
                    relations.set(i, relation);
                }
                // If a relationship between the given objects in the same direction
                // already exists, then don't create a new entry; just exit the function
                // here.
                return;
            }
        }
        relations.add(relation);
    }
    
    @Override
    public void paint(Graphics graphics) {
        super.paint(graphics);
        Settings settings = Settings.getInstance();
        Graphics2D graphics2d = (Graphics2D) graphics;
        graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        float zoomRatio = fontSize / settings.getBaseFontSize();
        float strokeWidth = zoomRatio * BASE_ARROW_LINE_WIDTH;
        Stroke defaultStroke = graphics2d.getStroke();
        Stroke solidStroke = new BasicStroke(strokeWidth);
        Stroke dashedStroke = new BasicStroke(strokeWidth, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[] {zoomRatio * 9.0f}, 0);
        graphics2d.setColor(settings.getColor("diagram-arrow"));
        for (Relation relation : relations) {
            Relation.Type relationType = relation.getType();
            ClassInfo from = relation.getFrom();
            ClassInfo to = relation.getTo();
            if (from == to) {
                continue;
            }
            int fromIndex = classIndexMap.get(from);
            int toIndex = classIndexMap.get(to);
            PositionedImage fromImage = classInfoImages[fromIndex];
            PositionedImage toImage = classInfoImages[toIndex];
            Point fromPoint = fromImage.getConnectionPoint(toImage);
            Point toPoint = toImage.getConnectionPoint(fromImage);
            switch (relationType) {
                case DEPENDENCY:
                case REALIZATION:
                    // Dependency and realization (implements) use dashed lines.
                    graphics2d.setStroke(dashedStroke);
                    break;
                case ASSOCIATION:
                case GENERALIZATION:
                    // Association and generalization (extends) use solid lines.
                    graphics2d.setStroke(solidStroke);
                    break;
            }
            float distance = (float) fromPoint.distance(toPoint);
            float directionX = (toPoint.x - fromPoint.x) / distance;
            float directionY = (toPoint.y - fromPoint.y) / distance;
            float headRadius = zoomRatio * BASE_ARROW_HEAD_RADIUS;
            Point headCenter = new Point(toPoint.x - (int) (directionX * headRadius), toPoint.y - (int) (directionY * headRadius));
            int[] arrowHeadPointsX = new int[3];
            int[] arrowHeadPointsY = new int[3];
            for (int i = 0; i < 3; ++i) {
                double angle = Math.PI * 2.0 / 3.0 * (i - 1);
                double sin = Math.sin(angle);
                double cos = Math.cos(angle);
                arrowHeadPointsX[i] = (int) (headCenter.x + headRadius * (directionX * cos - directionY * sin)) - viewX;
                arrowHeadPointsY[i] = (int) (headCenter.y + headRadius * (directionX * sin + directionY * cos)) - viewY;
            }
            graphics2d.drawLine(fromPoint.x - viewX, fromPoint.y - viewY,
                                toPoint.x - viewX - (int) (directionX * strokeWidth), toPoint.y - viewY - (int) (directionY * strokeWidth));
            switch (relationType) {
                case DEPENDENCY:
                case ASSOCIATION:
                    graphics2d.setStroke(solidStroke);
                    for (int i = 0; i < 2; ++i) {
                        graphics2d.drawLine(arrowHeadPointsX[i], arrowHeadPointsY[i], arrowHeadPointsX[i + 1], arrowHeadPointsY[i + 1]);
                    }
                    break;
                case GENERALIZATION:
                case REALIZATION:
                    graphics2d.fillPolygon(arrowHeadPointsX, arrowHeadPointsY, 3);
                    break;
            }
        }
        graphics2d.setStroke(defaultStroke);
        for (PositionedImage positionedImage : classInfoImages) {
            graphics2d.drawImage(positionedImage.getImage(),
                                 positionedImage.x - positionedImage.width / 2 - viewX,
                                 positionedImage.y - positionedImage.height / 2 - viewY,
                                 null);
        }
    }
    
    public boolean exportPng(File outputFile) throws IOException {
        Settings settings = Settings.getInstance();
        // Back-up the view.
        Point oldView = new Point(viewX, viewY);
        // Measure the diagram.
        Point topLeft = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
        Point bottomRight = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);
        for (PositionedImage image : classInfoImages) {
            int width = image.width;
            int height = image.height;
            topLeft.x = Math.min(image.x - width / 2, topLeft.x);
            topLeft.y = Math.min(image.y - height / 2, topLeft.y);
            // Adding it and then subtracting half ensures that rounding errors
            // don't occur even with odd dimensions.
            bottomRight.x = Math.max(image.x + width - width / 2, bottomRight.x);
            bottomRight.y = Math.max(image.y + height - height / 2, bottomRight.y);
        }
        if (topLeft.x == Integer.MAX_VALUE) {
            topLeft.x = 0;
            topLeft.y = 0;
            bottomRight.x = 0;
            bottomRight.y = 0;
        }
        // Measure diagram.
        int diagramWidth = bottomRight.x - topLeft.x;
        int diagramHeight = bottomRight.y - topLeft.y;
        if (diagramWidth > 0 && diagramHeight > 0) {
            // Move view to top left of diagram.
            viewX = topLeft.x;
            viewY = topLeft.y;
            // Create graphics and paint diagram.
            BufferedImage image = new BufferedImage(diagramWidth, diagramHeight, BufferedImage.TYPE_INT_RGB);
            Graphics graphics = image.getGraphics();
            // Draw background.
            graphics.setColor(settings.getColor("diagram-background"));
            graphics.fillRect(0, 0, diagramWidth, diagramHeight);
            // Draw contents.
            paint(graphics);
            image.flush();
            graphics.dispose();
            // Write image to file.
            ImageIO.write(image, "png", outputFile);
            // Restore view from backup.
            viewX = oldView.x;
            viewY = oldView.y;
            return true;
        }
        return false;
    }
    
    // Scrolling and dragging.
    private int prevMouseX, prevMouseY;
    private boolean draggingAnImage;
    private PositionedImage dragTarget;

    @Override
    public void mousePressed(MouseEvent event) {
        // Store current mouse position as previous position.
        prevMouseX = event.getX();
        prevMouseY = event.getY();
        dragTarget = null;
        // If shift is held down when clicking, then drag a panel instead of scrolling.
        draggingAnImage = event.isShiftDown();
        if (draggingAnImage) {
            // Calculate virtual mouse position according to view.
            int x = prevMouseX + viewX;
            int y = prevMouseY + viewY;
            // Look for panels that the mouse is over.
            for (int i = classInfoImages.length - 1; i >= 0; --i) {
                PositionedImage image = classInfoImages[i];
                if (image.getBounds().contains(x, y)) {
                    dragTarget = image;
                    break;
                }
            }
            draggingAnImage = dragTarget != null;
        }
    }
    
    @Override
    public void mouseDragged(MouseEvent event) {
        // Read current mouse position.
        int mouseX = event.getX();
        int mouseY = event.getY();
        // Calculate difference compared to previous position.
        int deltaX = mouseX - prevMouseX;
        int deltaY = mouseY - prevMouseY;
        if (draggingAnImage) {
            // Move the selected panel.
            dragTarget.x += deltaX;
            dragTarget.y += deltaY;
        } else {
            // Move the view.
            viewX -= deltaX;
            viewY -= deltaY;
        }
        // Store current mouse position as previous position.
        prevMouseX = mouseX;
        prevMouseY = mouseY;
        // Repaint this panel to apply the changes.
        repaint();
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent event) {
        int suggestedChange = 1 << (((int) fontSize) / 16);
        float newFontSize = fontSize;
        if (event.getWheelRotation() < 0) {
            newFontSize += suggestedChange;
            if (newFontSize > MAX_FONT_SIZE)
                newFontSize = MAX_FONT_SIZE;
        } else {
            newFontSize -= 1 << (((int) fontSize - suggestedChange) / 16);
            if (newFontSize < MIN_FONT_SIZE)
                newFontSize = MIN_FONT_SIZE;
        }
        if (newFontSize != fontSize) {
            setFontSize(newFontSize, event.getX() + viewX, event.getY() + viewY);
        }
        // setFontSize calls repaint already.
    }
    
    @Override
    public void mouseMoved(MouseEvent event) {
    }
    
    @Override
    public void mouseReleased(MouseEvent event) {
    }

    @Override
    public void mouseClicked(MouseEvent event) {
    }

    @Override
    public void mouseEntered(MouseEvent event) {
    }

    @Override
    public void mouseExited(MouseEvent event) {
    }
}
