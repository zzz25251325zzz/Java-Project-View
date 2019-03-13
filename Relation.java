package javaprojectview.uml;

import javaprojectview.graphics.SmartText;
import javaprojectview.parser.ClassInfo;

public class Relation {
    
    public enum Type {
        // In order of little to high importance.
        DEPENDENCY, ASSOCIATION, REALIZATION, GENERALIZATION
    }
    
    private final Type type;
    private final ClassInfo from, to;
    private final SmartText description;
    
    public Relation(Type type, ClassInfo from, ClassInfo to) {
        this(type, from, to, null);
    }
    
    public Relation(Type type, ClassInfo from, ClassInfo to, SmartText description) {
        this.type = type;
        this.from = from;
        this.to = to;
        this.description = description;
    }
    
    // Return whether or not another relation connects the same two objects in
    // the same direction as this relation.
    public boolean hasSameConnection(Relation otherRelation) {
        return from == otherRelation.from && to == otherRelation.to;
    }
    
    // Return whether or not another relation should be discarded if there are
    // multiple relations between the same two objects.
    public boolean isMoreImportantThan(Relation otherRelation) {
        return type.ordinal() > otherRelation.type.ordinal();
    }
    
    public Type getType() {
        return type;
    }
    
    public ClassInfo getFrom() {
        return from;
    }
    
    public ClassInfo getTo() {
        return to;
    }
    
    public SmartText getDescription() {
        return description;
    }
}
