package javaprojectview.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// Builder class for ClassInfo.
// Information about the class can be provided step by step using the methods,
// and then it can be converted to a ClassInfo object afterwards.
public class ClassBuilder {

    private final JavaParser parser;
    private final HashMap<String, String> imports;
    private final String packagePath;
    private final String name;
    private final String type;
    private String superClassName;
    private String outerClassName;
    private final ArrayList<String> interfaceNames;
    private final ArrayList<FieldInfo> fields;
    private final ArrayList<MethodInfo> methods;

    // Start building a class.
    // 'parser' is the parser that created this class.
    // 'path' is the full name of the class (package.className).
    // 'type' is a string representing whether this "class" is actually a class,
    // interface, or enum.
    public ClassBuilder(JavaParser parser, String path, String type) {
        this.parser = parser;
        imports = new HashMap<>();
        int dotIndex = path.lastIndexOf('.');
        packagePath = dotIndex == -1 ? "" : path.substring(0, dotIndex);
        name = path.substring(dotIndex + 1);
        this.type = type;
        interfaceNames = new ArrayList<>();
        fields = new ArrayList<>();
        methods = new ArrayList<>();
    }

    // Add a field.
    public void addField(Accessibility accessibility, String name, String typeName, boolean isFinal, boolean isStatic) {
        fields.add(new FieldInfo(accessibility, name, typeName, isFinal, isStatic));
    }

    // Add a method.
    public void addMethod(Accessibility accessibility, String name, String typeName, ArrayList<ParameterInfo> parameters, ValueInfo[] variables, boolean isFinal, boolean isStatic) {
        methods.add(new MethodInfo(accessibility, name, typeName, parameters.toArray(new ParameterInfo[parameters.size()]), variables, isFinal, isStatic));
    }
    
    // Add an import so that the short name means the full name when it comes to type names.
    public void addImport(String shortName, String fullName) {
        imports.put(shortName, fullName);
    }
    
    // Add all imports from another class builder.
    public void addImports(ClassBuilder other) {
        imports.putAll(other.imports);
    }
    
    // Add all imports from a map.
    public void addImports(Map<String, String> map) {
        imports.putAll(map);
    }
    
    // Add the name of an interface that this class implements
    public void addInterfaceName(String interfaceName) {
        interfaceNames.add(interfaceName);
    }
    
    // Set the name of the class that this class extends.
    public void setSuperClassName(String superClassName) {
        this.superClassName = superClassName;
    }
    
    // Set the name of the class that this class was defined in.
    public void setOuterClassName(String outerClassName) {
        this.outerClassName = outerClassName;
    }

    // Generate a ClassInfo object based on this builder.
    public ClassInfo toClassInfo() {
        return new ClassInfo(
                parser, imports, packagePath, name, type,
                superClassName, outerClassName, interfaceNames.toArray(new String[interfaceNames.size()]),
                fields.toArray(new FieldInfo[fields.size()]),
                methods.toArray(new MethodInfo[methods.size()]));
    }
    
    // Convert the fields in this class to regular values and return them.
    // This can be used to extract variable declarations from method bodies.
    public ValueInfo[] getFieldsAsVariables() {
        ValueInfo[] variables = new ValueInfo[fields.size()];
        for (int i = 0; i < variables.length; ++i) {
            variables[i] = fields.get(i).toValueInfo();
        }
        return variables;
    }
    
    // Return the name of the class (without the package name).
    public String getName() {
        return name;
    }
}
