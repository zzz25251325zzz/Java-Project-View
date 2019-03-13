package javaprojectview.parser;

import java.awt.Font;
import javaprojectview.graphics.SmartText;

// Class for storing information about a class member (field or method).
public class MemberInfo extends ValueInfo {
    
    // Accessibility (public, private, protected) of the member.
    protected final Accessibility accessibility;
    // Whether or not the member is static.
    protected final boolean isStatic;

    // Create the object.
    protected MemberInfo(Accessibility accessibility, String name, String typeName, boolean isFinal, boolean isStatic) {
        // Set name, typeName, and whether or not the member is final.
        super(name, typeName, isFinal);
        this.accessibility = accessibility;
        this.isStatic = isStatic;
    }
    
    // Convert information about the member to a SmartText string.
    protected SmartText toSmartText(SmartText parameterText, boolean excludeTypeName) {
        SmartText smartText = new SmartText();
        smartText.append(accessibility.toString(), "access-modifier");
        // If a parameter string is provided then it will be dislayed as a
        // method,  otherwise it will be displayed as a field.
        if (parameterText != null) {
            smartText.append(name, isStatic ? Font.ITALIC : Font.PLAIN, "method-name", isStatic);
            smartText.append(parameterText);
        } else {
            smartText.append(name, isStatic ? Font.ITALIC : Font.PLAIN, typeName == null ? "enum-value" : "field-name", isStatic);
        }
        // Constructors and enum values don't have an explicit type name.
        // Just leave the type name out if that is the case.
        // Also leave it out if it is explicitly decided to leave it out.
        if (typeName != null && !excludeTypeName) {
            smartText.append(" : ")
                     .append(formatTypeName());
        }
        if (isFinal)
            smartText.append(formatComment(" (final)"));
        return smartText;
    }
}
