package javaprojectview.parser;

import java.awt.Font;
import javaprojectview.graphics.SmartText;
import javaprojectview.graphics.SmartTextPart;

// Class representing anything with an identifier (name) and a type.
public class ValueInfo {
    
    // Name of the value.
    protected final String name;
    // Name of the type of the value.
    protected final String typeName;
    // Whether or not the value is marked as final.
    protected final boolean isFinal;

    // Creates the object.
    protected ValueInfo(String name, String typeName, boolean isFinal) {
        int nameLength = name.length();
        int index;
        // Move "[]" from the name to the type if it's an array.
        // This is to support things like "String args[]".
        for (index = nameLength - 1; index >= 0; --index) {
            char character = name.charAt(index);
            if (character != '[' && character != ']') {
                break;
            }
        }
        this.name = name.substring(0, index + 1);
        this.typeName = typeName == null ? null : typeName + name.substring(index + 1);
        this.isFinal = isFinal;
    }
    
    protected SmartTextPart formatComment(String text) {
        return new SmartTextPart(text, Font.ITALIC, "comment");
    }
    
    protected SmartText formatTypeName() {
        switch (typeName) {
            case "boolean":
            case "byte":
            case "char":
            case "double":
            case "float":
            case "int":
            case "long":
            case "short":
                return new SmartText(new SmartTextPart(typeName, Font.ITALIC, "builtin-type, type-name"));
            case "void":
                return new SmartText(new SmartTextPart(typeName, "void-type, builtin-type, type-name"));
            default:
                SmartText smartText = new SmartText();
                int currentIdentifierIndex = 0;
                for (int index = 0, typeNameLength = typeName.length(); index < typeNameLength; ++index) {
                    char character = typeName.charAt(index);
                    switch (character) {
                        case '<':
                        case '>':
                        case '[':
                        case ']':
                        case ',':
                        case '?':
                            smartText.append(typeName.substring(currentIdentifierIndex, index), Font.ITALIC, "type-name")
                                     .append(String.valueOf(character));
                            currentIdentifierIndex = index + 1;
                            break;
                    }
                }
                return smartText.append(typeName.substring(currentIdentifierIndex), Font.ITALIC, "type-name");
        }
    }
    
    public String getName() {
        return name;
    }
    
    public String getTypeName() {
        return typeName;
    }
    
    public boolean isFinal() {
        return isFinal;
    }
    
    // Convert the information about the value to a SmartText string.
    public SmartText toSmartText() {
        return new SmartText().append(name).append(" : ").append(formatTypeName()).append(formatComment(isFinal ? " (final)" : ""));
    }
    
    // Return a simple String representation of the information.
    @Override
    public String toString() {
        return toSmartText().toString();
    }
    
    // Returns a copy of this object with only the name, typeName, and whether
    // or not it is final. Can be used on classes that inherit this class to
    // simplify them.
    public ValueInfo toValueInfo() {
        return new ValueInfo(name, typeName, isFinal);
    }
}
