package javaprojectview.parser;

import javaprojectview.graphics.SmartText;

// This class contains information about a field of a parsed class.
public class FieldInfo extends MemberInfo {

    // Creates the object.
    public FieldInfo(Accessibility accessibility, String name, String typeName, boolean isFinal, boolean isStatic) {
        super(accessibility, name, typeName, isFinal, isStatic);
    }
    
    @Override
    public SmartText toSmartText() {
        // By default, do not exclude the type name.
        return toSmartText(false);
    }
    
    // Use MemberInfo's toString(parameterString, excludeTypeName) method to convert the
    // information to a SmartText string.
    public SmartText toSmartText(boolean excludeTypeName) {
        // The argument is null because fields don't have parameters.
        return toSmartText(null, excludeTypeName);
    }
}
