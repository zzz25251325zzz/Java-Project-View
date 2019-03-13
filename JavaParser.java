package javaprojectview.parser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

// Class for obtaining information about Java classes by parsing Java source
// files.
public class JavaParser {

    // Set of keywords that should be ignored.
    private static final HashSet<String> IGNORED_KEYWORDS = new HashSet<>(Arrays.asList(
            "abstract",
            "assert",
            "break",
            "case",
            "continue",
            "default",
            "do",
            "else",
            "finally",
            "for",
            "if",
            "native",
            "new",
            "return",
            "strictfp",
            "switch",
            "synchronized",
            "throw",
            "transient",
            "try",
            "volatile",
            "while"
    ));
    
    // Source code that is currently being parsed.
    private String sourceCode;
    // Current position in the source code.
    private int index;
    // Position to jump to when the "class" keyword is found.
    private int insideBracketsIndex;
    // Keeps track how deep the code is inside curly brackets inside a method.
    private int curlyBracketsLevel;
    // Package or class that the parser is currently in.
    private String currentPackage;
    // Builder for the class that is currently being parsed.
    private ClassBuilder currentClass;
    // Whether or not a method is currently being scanned for variables.
    private boolean inMethod;
    // Imports defined in current file.
    private HashMap<String, String> imports;

    // List of parsed classes.
    private final ArrayList<ClassInfo> classes;
    
    // Map of parsed classes with their full name (with package) as the key.
    private final HashMap<String, ClassInfo> classMap;
    
    // Create the object.
    public JavaParser() {
        classes = new ArrayList<>();
        classMap = new HashMap<>();
    }
    
    // Find information about a parsed class using its full class name (including
    // package). Returns null if it was not found.
    public ClassInfo getClassInfo(String fullClassName) {
        return classMap.get(fullClassName);
    }

    // Returns the information about all the parsed classes.
    public ClassInfo[] getClasses() {
        return classes.toArray(new ClassInfo[classes.size()]);
    }

    // Parse a source file. If 'file' is a directory, then all java files inside
    // the directory are parsed.
    public void parseFile(File file) throws IOException {
        if (file.isDirectory()) {
            for (File subFile : file.listFiles()) {
                if (subFile.isDirectory() || subFile.getName().endsWith(".java")) {
                    parseFile(subFile);
                }
            }
        } else {
            parseSourceCode(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
        }
    }

    // Parse information about classes inside the source code.
    public void parseSourceCode(String code) {
        sourceCode = code;
        index = 0;
        curlyBracketsLevel = 0;
        currentPackage = "";
        currentClass = null;
        inMethod = false;
        imports = new HashMap<>();
        parse();
    }

    // Parse the current source code at the current position until the end of
    // the file or the end of the class has been detected.
    private void parse() {
        for (int length = sourceCode.length(); index < length; ++index) {
            char c = sourceCode.charAt(index);
            if (!Character.isWhitespace(c)) {
                ArrayList<String> tokens = tokenizeStatement(sourceCode);
                if (tokens != null) {
                    parseStatementTokens(tokens);
                } else {
                    break;
                }
            }
        }
    }
    
    // Automatically detects the comment type and skips the comment if there is any.
    // If it's not a comment, then simply skips the current character.
    // Should be called if a '/' is encountered.
    private void skipComment() {
        int length = sourceCode.length();
        if (++index < length) {
            char character = sourceCode.charAt(index);
            if (character == '/') {
                // If it's a line comment (//) then skip to the next line.
                // Find the next line.
                index = sourceCode.indexOf('\n', index);
                // Skip to after it if it's found, or the end of the source code
                // otherwise.
                index = index == -1 ? length : index + 1;
            } else if (character == '*') {
                // If it's a long comment (/* ... */) then skip to after the
                // occurrence of "*/".
                // Find "*/".
                index = sourceCode.indexOf("*/", index);
                // Skip to after it if it's found, or the end of the source code
                // otherwise.
                index = index == -1 ? length : index + 2;
            }
        }
    }

    // Skips characters until all opening brackets/parentheses (defined by
    // openingChar) have been matched by closing brackets/parentheses (defined
    // by closingChar).
    private void skipBrackets(char openingChar, char closingChar) {
        int nestedLevel = 0;
        int length = sourceCode.length();
        char stringType = 0;
        do {
            char character = sourceCode.charAt(index);
            if (stringType != 0) {
                // Currently in string skipping mode.
                if (character == stringType) {
                    // Exit string skipping mode.
                    stringType = 0;
                } else if (character == '\\') {
                    // Skip escaped character.
                    ++index;
                }
            } else if (character == '/') {
                // Skip the comment if there is any.
                skipComment();
                // skipComment skips to the first character after the comment but
                // the index is already incremented by this loop, so decrement it
                // to compensate.
                --index;
            } else if (character == openingChar) {
                // Entering brackets.
                ++nestedLevel;
            } else if (character == closingChar) {
                // Exiting brackets.
                --nestedLevel;
            } else if (character == '\'' || character == '"') {
                // Enter string skipping mode.
                stringType = character;
            }
            // Keep going until the end of the string or if all brackets are matched.
        } while (++index < length && nestedLevel > 0);
    }

    // Tokenize a single statement of code, until a ';', '{', '}', or '=' is
    // found.
    private ArrayList<String> tokenizeStatement(String sourceCode) {
        // Create list of tokens.
        ArrayList<String> tokens = new ArrayList<>();
        // Create string builder for the current token.
        StringBuilder tokenBuilder = new StringBuilder();
        // Keep track of how deep the tokenizer is currently inside parentheses
        // (in which case it is looking at the parameters of a method).
        int parenthesesLevel = 0;
        // Keep track of how many nested '<' or '>' there are (for template arguments).
        int angleBracketsLevel = 0;
        // Whether or not a string is currently being parsed, and whether it started
        // with a ' or ".
        char stringType = 0;
        for (int length = sourceCode.length(); index < length; ) {
            char c = sourceCode.charAt(index);
            if (stringType != 0) {
                // Currently skipping a string.
                ++index;
                if (c == stringType) {
                    // End of string.
                    stringType = 0;
                } else if (c == '\\') {
                    // Skip the next character after \.
                    ++index;
                }
            } else {
                boolean isWhitespace = Character.isWhitespace(c);
                boolean newWord = isWhitespace || c == '/' || (tokenBuilder.length() > 0 &&
                        (c == '(' || c == ')' || c == '.' || c == ',' || c == '<' || c == '>' || c == '[' || c == ']' || c == '=' || c == '{' || c == '?'));
                if (newWord) {
                    // If the current character is whitespace or a special symbol, then consider starting a new token.
                    if (isWhitespace) {
                        // Skip all extra whitespace.
                        while (++index < length && Character.isWhitespace(c = sourceCode.charAt(index))) {}
                    }
                    switch (c) {
                        case '/':
                            skipComment();
                            break;
                        case '.':
                        case ',':
                        case '<':
                        case '>':
                        case '[':
                        case ']':
                        case '?':
                            // If the next non-whitespace character is '.', ',', '<', '>', '[', ']', or '?',
                            // then keep building the token, unless it is ',' and the parser is within
                            // parentheses and not within angle brackets.
                            // This allows things like java.util.ArrayList<String> to be a single token.
                            if (c == '<') {
                                ++angleBracketsLevel;
                            } else if (c == '>') {
                                --angleBracketsLevel;
                            }
                            // Also look for the next non-whitespace character after this one.
                            int nonWhitespaceIndex = index;
                            char nonWhitespaceCharacter = 0;
                            while (++nonWhitespaceIndex < length && Character.isWhitespace(nonWhitespaceCharacter = sourceCode.charAt(nonWhitespaceIndex))) {}
                            // If the next non-whitespace character is a '[', then continue regardless of anything else.
                            if (((c != ',' || parenthesesLevel == 0) && c != '>' && c != ']' && c != '?') || angleBracketsLevel > 0 || nonWhitespaceCharacter == '[') {
                                tokenBuilder.append(c);
                                // Skip whitespace afterwards by jumping to the next non-whitespace
                                // character. This allows things like "var1, var2, var3" to be a single
                                // token.
                                index = nonWhitespaceIndex;
                                break;
                            } else {
                                // Finish the token after all.
                                if (c == '>' || c == ']' || c == '?') {
                                    tokenBuilder.append(c);
                                }
                                ++index;
                            }
                        default:
                            // The token is complete.
                            if (tokenBuilder.length() > 0) {
                                if (tokenBuilder.charAt(0) == '@') {
                                    if (c == '(') {
                                        skipBrackets('(', ')'); // skip parentheses after annotation if there are any
                                    }
                                } else {
                                    tokens.add(tokenBuilder.toString()); // add the token
                                }
                                tokenBuilder.setLength(0); // reset token builder
                            }
                            break;
                    }
                } else {
                    switch (c) {
                        case '\'':
                        case '"':
                            // Enter string skipping mode.
                            stringType = c;
                            ++index;
                            break;
                        case '(':
                        case ')':
                            tokens.add(String.valueOf(c));
                            // If '(' then enter method parameters, otherwise exit it.
                            if (c == '(') {
                                ++parenthesesLevel;
                                ++index;
                            } else {
                                --parenthesesLevel;
                                // Skip whitespace.
                                while (++index < length && Character.isWhitespace(c = sourceCode.charAt(index))) {}
                                if (c != '{' && c != 't') {
                                    // If a ')' is followed by something other than a '{' or a 't' (from "throws"),
                                    // then it is likely not a method.
                                    // It is most likely an enum instantiation instead, which can happen if an enum
                                    // has a constructor. Like:
                                    // enum Example { VALUE(1); Example(int x) {} }
                                    // Make sure that the first token isn't a keyword, as keywords can't be enum
                                    // value identifiers anyway.
                                    if (!IGNORED_KEYWORDS.contains(tokens.get(0))) {
                                        // If there is more than one token before '(', then it's an abstract
                                        // method declaration instead, so check for that.
                                        int indexBeforeParentheses;
                                        for (indexBeforeParentheses = tokens.size() - 1; indexBeforeParentheses >= 0; --indexBeforeParentheses) {
                                            if ("(".equals(tokens.get(indexBeforeParentheses))) {
                                                break;
                                            }
                                        }
                                        if (indexBeforeParentheses < 2) {
                                            // There aren't multiple tokens before '(' so it is an enum after all.
                                            // In that case, keep removing tokens until '(' has been removed, so that instead
                                            // of [VALUE, (, 1, )], only [VALUE] remains, which is the name of the enum value,
                                            // which we are interested in.
                                            while (tokens.size() > 1 && !"(".equals(tokens.remove(tokens.size() - 1))) {}
                                            // Also, continue building the previous token if there is any, since the token
                                            // should be "VALUE1,VALUE2,VALUE3" (for example).
                                            if (tokens.size() > 0) {
                                                tokenBuilder.append(tokens.remove(tokens.size() - 1));
                                            }
                                        }
                                    }
                                }
                            }
                            break;
                        case ':':
                        case ';':
                        case '{':
                        case '}':
                        case '=':
                            int numTokens = tokens.size();
                            String firstToken = numTokens > 0 ? tokens.get(0) : null;
                            // If the character is ':', then only end the statement if it starts with case/default,
                            // or the statement only has one token in it so far (including the builder).
                            // Otherwise, skip to the default character handler below.
                            if (c != ':' || ("case".equals(firstToken) ||
                                             "default".equals(firstToken) ||
                                             "default".equals(tokenBuilder.toString()) ||
                                             (tokenBuilder.length() == 0 ? 0 : 1) + numTokens == 1)) {
                                if (parenthesesLevel > 0) {
                                    // If still in parentheses, then ignore this character.
                                    ++index;
                                    break;
                                }
                                // Otherwise, finish the statement.
                                boolean continueParsing = false;
                                switch (c) {
                                    case '{':
                                        // Skip the brackets.
                                        insideBracketsIndex = index + 1;
                                        skipBrackets('{', '}');
                                        if (tokens.size() == 1 && !IGNORED_KEYWORDS.contains(tokens.get(0))) {
                                            // If there was only one token before '{', it was a enum value
                                            // implementing an abstract method, like for example:
                                            // enum Example { VALUE { void test() {} }; abstract void test(); }
                                            // In that case, it the end of the statement is only reached once ';'
                                            // is found so we should continue parsing the statement despite
                                            // encountering a '{'.
                                            continueParsing = true;
                                            // Continue building the previous token.
                                            tokenBuilder.append(tokens.remove(tokens.size() - 1));
                                            // If the token is a keyword, then it's a block instead, so it shouldn't
                                            // be extended.
                                        } else {
                                            // Otherwise return to the loop in the parse function.
                                            if (inMethod && !(tokens.size() > 0 && "new".equals(tokens.get(0)))) {
                                                // If a method is currently being scanned for variables, then look
                                                // for more variables inside the brackets.
                                                // But if the first token of the current statement is "new", then
                                                // don't include what's in the brackets, as it is an anonymous inner
                                                // class.
                                                index = insideBracketsIndex;
                                                ++curlyBracketsLevel;
                                            }
                                            // The parse loop increments index and skipBrackets places you after
                                            // the closing bracket, so decrement the index here to compensate.
                                            --index;
                                        }
                                        break;
                                    case '=':
                                        int[] bracketLevels = new int[3];
                                        while (++index < length) {
                                            boolean finished = false;
                                            c = sourceCode.charAt(index);
                                            if (stringType != 0) {
                                                // Currently skipping a string.
                                                if (c == stringType) {
                                                    // End of string.
                                                    stringType = 0;
                                                } else if (c == '\\') {
                                                    // Skip the next character after \.
                                                    ++index;
                                                }
                                            } else {
                                                switch (c) {
                                                    // Increase or decrease the bracket levels if opening or
                                                    // closing brackets are found.
                                                    case '[':
                                                        ++bracketLevels[0];
                                                        break;
                                                    case ']':
                                                        --bracketLevels[0];
                                                        break;
                                                    case '{':
                                                        ++bracketLevels[1];
                                                        break;
                                                    case '}':
                                                        --bracketLevels[1];
                                                        break;
                                                    case '(':
                                                        ++bracketLevels[2];
                                                        break;
                                                    case ')':
                                                        --bracketLevels[2];
                                                        break;
                                                    // Go into string skipping mode if a ' or " is found.
                                                    case '\'':
                                                    case '"':
                                                        stringType = c;
                                                        break;
                                                    case ',':
                                                    case ';':
                                                        // Checks if all the bracket levels are 0. If any aren't
                                                        // 0, then the result of binary OR (|) won't be 0, so this
                                                        // way all of them can be checked at once.
                                                        if ((bracketLevels[0] | bracketLevels[1] | bracketLevels[2]) == 0) {
                                                            // Not currently in any kind of brackets.
                                                            if (c == ',') {
                                                                // If a comma was found, then keep parsing the token.
                                                                // This allows for field definitions like "int a = 1, b = 2;".
                                                                continueParsing = true;
                                                                // Continue building previous token.
                                                                tokenBuilder.append(tokens.remove(tokens.size() - 1));
                                                            } else {
                                                                // The semicollon at the end of the token has been reached.
                                                                ++index;
                                                            }
                                                            // Successfully skipped to the first ',' or the end of the
                                                            // statement, whichever came first.
                                                            finished = true;
                                                        }
                                                        break;
                                                    // Ignore any other kinds of characters.
                                                }
                                                // Exit the loop if a , or ; has been found.
                                                if (finished)
                                                    break;
                                            }
                                        }
                                        break;
                                    case '}':
                                        if (inMethod && curlyBracketsLevel > 0) {
                                            // If still within brackets inside a method (looking for
                                            // variables), then just return to the parse loop and
                                            // continue parsing.
                                            --curlyBracketsLevel;
                                        } else {
                                            // Otherwise stop parsing the current class.
                                            index = length;
                                        }
                                        break;
                                    default:
                                        break;
                                }
                                if (!continueParsing) {
                                    // If a token was being built, then finish it.
                                    if (tokenBuilder.length() > 0) {
                                        tokens.add(tokenBuilder.toString());
                                    }
                                    // Return the tokens that were found.
                                    return tokens;
                                }
                                break;
                            }
                        default:
                            // Add the previous character to current token.
                            tokenBuilder.append(c);
                            ++index;
                            break;
                    }
                }
            }
        }
        return null;
    }

    // Parse a statement using a list of tokens.
    private void parseStatementTokens(ArrayList<String> tokens) {
        Accessibility accessibility = Accessibility.PACKAGE;
        boolean isFinal = false, isStatic = false;
        for (int i = 0, numTokens = tokens.size(); i < numTokens; ++i) {
            String token = tokens.get(i);
            switch (token) {
                // Look for accessibility modifiers.
                case "public":
                    accessibility = Accessibility.PUBLIC;
                    break;
                case "private":
                    accessibility = Accessibility.PRIVATE;
                    break;
                case "protected":
                    accessibility = Accessibility.PROTECTED;
                    break;
                // Check if it is static.
                case "static":
                    isStatic = true;
                    break;
                // Check if it is final.
                case "final":
                    isFinal = true;
                    break;
                // Keep track of the package.
                case "package":
                    // Set package if there is a token after 'package'.
                    if (++i < numTokens)
                        currentPackage = tokens.get(i);
                    break;
                // Keep track of imports.
                case "import":
                    // Import type if there is a token after 'import'.
                    if (++i < numTokens) {
                        String fullName = tokens.get(i);
                        int dotIndex = fullName.lastIndexOf('.');
                        if (dotIndex >= 0) {
                            // Let the part after the final dot be the 'nickname'.
                            String shortName = fullName.substring(fullName.lastIndexOf('.') + 1);
                            imports.put(shortName, fullName);
                        }
                    }
                    break;
                // Start parsing a new class, enum or interface.
                case "class":
                case "enum":
                case "interface":
                    if (++i < numTokens) {
                        String className = tokens.get(i);
                        // Back up information to return to after parsing the class (helps
                        // deal with nested classes).
                        int endIndex = index;
                        HashMap<String, String> outerImports = imports;
                        ClassBuilder outerClass = currentClass;
                        String previousPackage = currentPackage;
                        // Make imports for this class separate from the imports for the outer class
                        // (this is convenient because inner class resolution is dealt with by
                        // treating the definition as an "import").
                        imports = new HashMap<>();
                        imports.putAll(outerImports);
                        // Go inside the current class (so a class within an outer class would have
                        // a full name like package.OuterClass.InnerClass).
                        currentPackage += "." + className;
                        // Go to the code inside the brackets (where the class fields and such are).
                        index = insideBracketsIndex;
                        // Start building the new class (at this point 'currentPackage' is the full
                        // name of the class).
                        currentClass = new ClassBuilder(this, currentPackage, token);
                        imports.put(className, currentPackage);
                        currentClass.addImports(imports);
                        // If this is an inner class, then add a relation for that.
                        if (outerClass != null) {
                            currentClass.setOuterClassName(outerClass.getName());
                            outerClass.addImport(className, currentPackage);
                            currentClass.addImports(outerClass);
                        }
                        if (++i < numTokens && tokens.get(i).equals("extends") && ++i < numTokens) {
                            // Check for the extends keyword to add a superclass.
                            currentClass.setSuperClassName(tokens.get(i));
                        } else {
                            --i;
                        }
                        if (++i < numTokens && tokens.get(i).equals("implements") && ++i < numTokens) {
                            // Check for the implements keyword to add an interface.
                            for (String interfaceName : tokens.get(i).split(",")) {
                                currentClass.addInterfaceName(interfaceName);
                            }
                        }
                        // Start parsing the class.
                        parse();
                        // Finish building the class and register it.
                        ClassInfo classInfo = currentClass.toClassInfo();
                        classes.add(classInfo);
                        classMap.put(classInfo.getFullName(), classInfo);
                        // Return to parsing the outer class, if any.
                        index = endIndex;
                        currentPackage = previousPackage;
                        currentClass = outerClass;
                        imports = outerImports;
                    }
                    break;
                default:
                    if (inMethod) {
                        if (i == 0 && IGNORED_KEYWORDS.contains(token)) {
                            // Ignore the entire statement if it's inside a method and starts
                            // with a keyword.
                            return;
                        }
                    } else if (IGNORED_KEYWORDS.contains(token)) {
                        // If not inside a method, then ignore certain keywords regardless of
                        // position, but don't end the statement.
                        break;
                    }
                    // Look for type or member name (identifier).
                    // Members are only relevant inside classes.
                    if (currentClass != null) {
                        if (numTokens == 1) {
                            // Add enum values, which may be separated by a ','.
                            if (inMethod) {
                                // Methods don't have enum values, so ignore the statement if the parser
                                // is currently looking for variables in a method.
                                return;
                            }
                            for (String enumValue : token.split(",")) {
                                // Represent fields as enums with no type.
                                currentClass.addField(Accessibility.PUBLIC, enumValue, null, false, false);
                            }
                        } else {
                            String memberTypeName = token;
                            String memberName;
                            ArrayList<ParameterInfo> parameters = null;
                            if (++i < numTokens) {
                                token = tokens.get(i);
                                if ("(".equals(token)) {
                                    // If the identifier is followed directly by '(', it has no return type
                                    // (it's a constructor).
                                    memberName = memberTypeName;
                                    memberTypeName = null;
                                    --i;
                                } else {
                                    // Otherwise, the previous token was the return type or field type, meaning
                                    // the next one is the name of the method or field.
                                    memberName = token;
                                }
                                if (++i < numTokens && "(".equals(tokens.get(i))) {
                                    // If the next token exists and is '(', then parse the parameters.
                                    if (inMethod) {
                                        // But if it's already inside a method, then ignore the statement (as
                                        // it's a method call instead of a definition).
                                        return;
                                    }
                                    parameters = new ArrayList<>();
                                    // Keep going until end of statement or if ')' is found.
                                    while (++i < numTokens && !")".equals(token = tokens.get(i))) {
                                        boolean isParameterFinal = token.equals("final");
                                        if (isParameterFinal) {
                                            // Go to the next token and stop if it was the final token.
                                            if (++i >= numTokens)
                                                break;
                                            token = tokens.get(i);
                                        }
                                        String parameterName = null, parameterTypeName = null;
                                        int varargIndex = token.indexOf("...");
                                        if (varargIndex != -1) {
                                            // The tokenizer incorrectly turns things like "String... args"
                                            // into a single token ("String...args"). Split them up again if
                                            // that is the case.
                                            parameterTypeName = token.substring(0, varargIndex + 3);
                                            parameterName = token.substring(varargIndex + 3);
                                        } else if (++i < numTokens) {
                                            // Otherwise, the parameter name is the next token.
                                            parameterTypeName = token;
                                            parameterName = tokens.get(i);
                                        }
                                        if (parameterName != null) {
                                            // Add the parameter.
                                            parameters.add(new ParameterInfo(parameterName, parameterTypeName, isParameterFinal));
                                        }
                                    }
                                }
                                if (parameters == null) {
                                    // If no parameter list was found, then add fields instead, which may
                                    // be separated by a ',' if multiple fields are declared at once (Like
                                    // "int var1, var2, var3;"
                                    for (String fieldName : memberName.split(",")) {
                                        if (isJavaIdentifier(fieldName)) {
                                            currentClass.addField(accessibility, fieldName, memberTypeName, isFinal, isStatic);
                                        }
                                    }
                                } else {
                                    // Otherwise, add a method with the parameters that were found.
                                    int endIndex = index;
                                    index = insideBracketsIndex;
                                    inMethod = true;
                                    ClassBuilder outerClass = currentClass;
                                    currentClass = new ClassBuilder(this, "<variable search>", null);
                                    currentClass.addImports(outerClass);
                                    curlyBracketsLevel = 0;
                                    parse();
                                    ValueInfo[] variables = currentClass.getFieldsAsVariables();
                                    currentClass = outerClass;
                                    currentClass.addMethod(accessibility, memberName, memberTypeName, parameters, variables, isFinal, isStatic);
                                    inMethod = false;
                                    index = endIndex;
                                }
                                return;
                            }
                        }
                    }
                    break;
            }
        }
    }

    // Returns whether or not the String is a valid Java identifier.
    private static boolean isJavaIdentifier(String identifier) {
        for (int i = 0, length = identifier.length(); i < length; ++i) {
            char character = identifier.charAt(i);
            if ((i == 0 && !Character.isJavaIdentifierStart(character)) ||
                (i != 0 && !Character.isJavaIdentifierPart(character))) {
                return false;
            }
        }
        return true;
    }
}
