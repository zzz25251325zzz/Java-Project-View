package javaprojectview.parser;


import javaprojectview.graphics.SmartText;

// This class contains information about a method of a parsed class.
public class MethodInfo extends MemberInfo {
    
    // List of parameters that the method has.
    private final ParameterInfo[] parameters;
    private final ValueInfo[] variables;

    // Create the object.
    public MethodInfo(Accessibility accessibility, String name, String returnTypeName, ParameterInfo[] parameters, ValueInfo[] variables, boolean isFinal, boolean isStatic) {
        super(accessibility, name, returnTypeName, isFinal, isStatic);
        this.parameters = parameters;
        this.variables = variables;
    }
    
    public ParameterInfo[] getParameters() {
        return parameters;
    }
    
    // Return variable info.
    public ValueInfo[] getVariables() {
        return variables;
    }
    
    // Convert the information about the method to a SmartText string.
    @Override
    public SmartText toSmartText() {
        // Convert the parameters to a string like "(param1, param2, param3)".
        SmartText parameterText = new SmartText();
        parameterText.append("(");
        boolean isFirst = true;
        for (ParameterInfo parameter : parameters) {
            if (isFirst) {
                isFirst = false;
            } else {
                parameterText.append(", ");
            }
            parameterText.append(parameter.toSmartText());
        }
        parameterText.append(")");
        // Use MemberInfo's method to convert this object into a string using
        // the parameter list.
        return toSmartText(parameterText, false);
    }
}
