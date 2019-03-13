package javaprojectview.parser;


import javaprojectview.graphics.SmartText;

// Class for storing information about a method parameter.
public class ParameterInfo extends ValueInfo {
    
    // Create the object.
    public ParameterInfo(String name, String typeName, boolean isFinal) {
        super(name, typeName, isFinal);
    }
    
    // Convert the information about the parameter to a SmartText string.
    @Override
    public SmartText toSmartText() {
        return new SmartText().append(name, "parameter").append(" : ").append(formatTypeName()).append(formatComment(isFinal ? " (final)" : ""));
    }
}
