package javaprojectview.parser;

import java.awt.Font;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import javaprojectview.graphics.SmartText;

// This class contains information about a parsed class.
public class ClassInfo {
    
    private final JavaParser parser;
    private final HashMap<String, String> imports;
    private final String packagePath;
    private final String name;
    private final String type;
    private final String superClassName;
    private final String outerClassName;
    private final String[] interfaceNames;
    private final FieldInfo[] fields;
    private final MethodInfo[] methods;
    
    // Create the object.
    public ClassInfo(JavaParser parser, HashMap<String, String> imports, String packagePath, String name, String type, String superClassName, String outerClassName, String[] interfaceNames, FieldInfo[] fields, MethodInfo[] methods) {
        this.parser = parser;
        this.imports = imports;
        this.packagePath = packagePath;
        this.name = name;
        this.type = type;
        this.superClassName = superClassName;
        this.outerClassName = outerClassName;
        this.interfaceNames = interfaceNames;
        this.fields = fields;
        this.methods = methods;
    }
    
    public ClassInfo resolveClass(String shortName) {
        ClassInfo scope = this;
        ClassInfo classFound = null;
        do {
            String fullName = scope.imports.get(shortName);
            if (fullName == null) {
                classFound = parser.getClassInfo(scope.packagePath + "." + shortName);
            } else {
                classFound = parser.getClassInfo(fullName);
            }
        } while (classFound == null && (scope = scope.getOuterClass()) != null);
        return classFound;
    }
    
    // Return the class name.
    public String getName() {
        return name;
    }
        
    // Return what kind of class-like structure this is (class, interface or enum).
    public String getType() {
        return type;
    }

    // Return the fields.
    public FieldInfo[] getFields() {
        return fields;
    }

    // Return the methods.
    public MethodInfo[] getMethods() {
        return methods;
    }
    
    // Return the full name of the class, including package name.
    public String getFullName() {
        return packagePath + "." + getName();
    }
    
    // Return the full name of the class as highlighted text.
    public SmartText getSmartTextName() {
        ClassInfo outermostClass = this;
        ClassInfo outerClass;
        while ((outerClass = outermostClass.getOuterClass()) != null) {
            outermostClass = outerClass;
        }
        String realPackagePath = outermostClass.packagePath + ".";
        String extendedClassName = getFullName().substring(realPackagePath.length());
        return new SmartText().append(realPackagePath, Font.ITALIC, "package-path")
                              .append(extendedClassName, Font.BOLD, "class-name");
    }
    
    // Return the information about the class that this class extends, if such a
    // class exists and it was parsed by the same parser. Return null otherwise.
    public ClassInfo getSuperClass() {
        return resolveClass(superClassName);
    }
    
    // Return the information about the class that this class was defined in, if
    // such a class exists and it was parsed by the same parser. Return null
    // otherwise.
    public ClassInfo getOuterClass() {
        return parser.getClassInfo(packagePath);
    }
    
    // Return the information about the interfaces that this class implements,
    // if they have been parsed by the same parser.
    public Collection<ClassInfo> getInterfaces() {
        ArrayList<ClassInfo> interfaces = new ArrayList<>();
        for (String interfaceName : interfaceNames) {
            // Resolve interfaces using their name.
            ClassInfo interfaceInfo = resolveClass(interfaceName);
            // Only add the ones that were found.
            if (interfaceInfo != null)
                interfaces.add(interfaceInfo);
        }
        return interfaces;
    }
    
    // Write the information about the class to a BufferedWriter.
    // This includes the class name, extended class, implemented interfaces,
    // outer class, fields, and methods.
    public void writeTo(BufferedWriter writer) throws IOException {
        // Write whether it's a class, enum or interface.
        writer.write(type);
        writer.write(' ');
        // Write the name.
        writer.write(name);
        if (superClassName != null) {
            // Write the name of the super class (extends) if there is any.
            writer.write(" extends ");
            writer.write(superClassName);
        }
        if (interfaceNames.length > 0) {
            // Write the names of interfaces (implements) if there is any.
            writer.write(" implements ");
            int i = 0;
            for (String interfaceName : interfaceNames) {
                if (i++ != 0) {
                    writer.write(", ");
                }
                writer.write(interfaceName);
            }
        }
        if (outerClassName != null) {
            // Write the name of the class that this class was defined in, if any.
            writer.write(" inside ");
            writer.write(outerClassName);
        }
        writer.newLine();
        // List the fields of the class.
        for (FieldInfo field : fields) {
            writer.write(' ');
            writer.write(field.toString());
            writer.newLine();
        }
        // List the methods of the class.
        for (MethodInfo method : methods) {
            writer.write(' ');
            writer.write(method.toString());
            writer.newLine();
            // List the variables that are defined in the method.
            for (ValueInfo variable : method.getVariables()) {
                writer.write("   * ");
                writer.write(variable.toString());
                writer.newLine();
            }
        }
    }
}
