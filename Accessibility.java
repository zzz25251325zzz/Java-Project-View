package javaprojectview.parser;

// Enum to determine whether a field, method or class is public, private,
// protected, or package-private (default).
public enum Accessibility {
    
    PUBLIC, PRIVATE, PROTECTED, PACKAGE;
    
    // Turn the enum into a symbol for the output.
    public char getSymbol() {
        switch (this) {
            case PUBLIC:
                return '+';
            case PRIVATE:
                return '-';
            case PROTECTED:
                return '#';
            case PACKAGE:
                return '~';
        }
        return '?';
    }
    
    // Add a space to after the symbol for convenience. Can be extended later
    // if needed.
    @Override
    public String toString() {
        return getSymbol() + " ";
    }
}
