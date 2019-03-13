package javaprojectview;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

// Singleton containing global variables and useful functions.
public class Settings {
    
    private final Image emptyImage;
    private final Map desktopHints;
    
    private HashMap<String, Color> colors;
    
    private Font regularFont, boldFont, italicFont, boldItalicFont, semiboldFont, semiboldItalicFont;
    
    private float baseFontSize;
    
    private static Settings instance = null;
    
    public static Settings getInstance() {
        if (instance == null) {
            instance = new Settings();
        }
        return instance;
    }
    
    private Settings() {
        baseFontSize = 12.0f;
        emptyImage = new BufferedImage(1, 1, BufferedImage.TYPE_4BYTE_ABGR_PRE) {
            
            @Override
            public int getWidth(ImageObserver observer) {
                return 0;
            }
            
            @Override
            public int getHeight(ImageObserver observer) {
                return 0;
            }
        };
        desktopHints = (Map) Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints");
        try {
            regularFont = loadFont("fonts/SourceCodePro-Regular.ttf");
            boldFont = loadFont("fonts/SourceCodePro-Bold.ttf");
            italicFont = loadFont("fonts/SourceCodePro-It.ttf");
            boldItalicFont = loadFont("fonts/SourceCodePro-BoldIt.ttf");
            semiboldFont = loadFont("fonts/SourceCodePro-Semibold.ttf");
            semiboldItalicFont = loadFont("fonts/SourceCodePro-SemiboldIt.ttf");
        } catch (FontFormatException | IOException ex) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
        }
        colors = new HashMap<>();
        try {
            loadColors();
        } catch (IOException | SAXException | ParserConfigurationException ex) {
            Logger.getLogger(Settings.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private Font loadFont(String uri) throws FontFormatException, IOException {
        Font font = Font.createFont(Font.TRUETYPE_FONT, getClass().getResourceAsStream(uri));
        GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
        return font.deriveFont(12.0f);
    }
    
    private void loadColors() throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = documentBuilder.parse(getClass().getResourceAsStream("colors.xml"));
        document.getDocumentElement().normalize();
        putElementColors(colors, document.getElementsByTagName("colors").item(0).getChildNodes());
    }
    
    private void putElementColors(HashMap<String, Color> colors, NodeList elements) {
        for (int i = 0, length = elements.getLength(); i < length; ++i) {
            Node node = elements.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) node;
                colors.put(element.getTagName(), new Color(Integer.parseInt(element.getTextContent(), 16)));
            }
        }
    }

    // Apply default desktop rendering hints to a graphics object for better quality.
    public void applyDesktopHints(Graphics graphics) {
        if (desktopHints != null) {
            ((Graphics2D) graphics).addRenderingHints(desktopHints);
        }
    }
    
    /**
     * Find and returns the first color that is available based on a list of
     * color names. If none of the given colors are available, return the
     * foreground color of the map.
     * @param names List of names of the colors to pick from. This is given as
     * a string, seperating names by '/', ',', ';', ':' or whitespace. For example:
     * "color1, color2" will prioritize "color1" over "color2".
     * @return First available color in the list, or the "foreground" color.
     */
    public Color getColor(String names) {
        if (names != null) {
            // Split the string up based on the separators.
            for (String name : names.split("[/,;:\\s]")) {
                Color color = colors.get(name);
                if (color != null) {
                    // If the color exists, return it.
                    return color;
                }
            }
        }
        // If none of the colors were found or names is null, then return the
        // foreground color.
        return colors.get("foreground");
    }
    
    public Image getEmptyImage() {
        return emptyImage;
    }
    
    public FontMetrics getFontMetrics(Font font) {
        Graphics2D graphics = (Graphics2D) emptyImage.getGraphics();
        applyDesktopHints(graphics);
        return graphics.getFontMetrics(font);
    }
    
    public Font getFont(float fontSize, boolean bold, boolean italic) {
        Font baseFont;
        if (bold) {
            baseFont = italic ? boldItalicFont : boldFont;
        } else if (fontSize <= 8.0f) {
            // Use semibold fonts for small text for increased readability.
            baseFont = italic ? semiboldItalicFont : semiboldFont;
        } else {
            baseFont = italic ? italicFont : regularFont;
        }
        return baseFont.deriveFont(fontSize);
    }

    public Font getRegularFont() {
        return regularFont;
    }
    
    public Font getBoldFont() {
        return boldFont;
    }
    
    public Font getItalicFont() {
        return italicFont;
    }
    
    public Font getBoldItalicFont() {
        return boldItalicFont;
    }
    
    public float getBaseFontSize() {
        return baseFontSize;
    }
}
