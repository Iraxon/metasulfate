package net.iraxon.metasulfate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

interface AST {
    public Pattern asPattern();
}

record Atom(String name) implements AST {

    private static Map<String, Atom> atomCache = new HashMap<>();

    /**
     * @deprecated
     * Please use the factory method.
     */
    @Deprecated
    public Atom {
        atomCache.put(name, this);
    }

    public static Atom of(String n) {
        return switch (atomCache.get(n)) {
            case null -> new Atom(n);
            case Atom a -> a;
        };
    }

    @Override
    public Pattern asPattern() {
        return new LiteralPattern(this);
    }

    @Override
    public String toString() {
        return name;
    }
}

/**
 * @deprecated
 */
@Deprecated
record ValueNode(Object value) implements AST {
    @Override
    public Pattern asPattern() {
        return switch (value) {
            case OldName n -> null;
            default -> new LiteralPattern(this);
        };
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

enum ASTSingletons implements AST {
    END_OF_LIST;

    @Override
    public Pattern asPattern() {
        throw new RuntimeException("Attempted to convert END_OF_LIST to pattern");
    }
}

record NestedNode(List<AST> children) implements AST {

    @Override
    public Pattern asPattern() {
        return SequencePattern.of(children.stream().map(AST::asPattern).toArray(Pattern[]::new));
    }

    @Override
    public String toString() {
        return "[" + children.stream().map(x -> x.toString()).reduce("", (x, y) -> x.equals("") ? y : x + " " + y)
                + "]";
    }

    // private static boolean listMatch(List<AST> list, List<Object> pattern) {
    // if (list.size() != pattern.size()) {
    // return false;
    // }
    // for (int i = 0; i < list.size(); i++) {
    // if (!(list.get(i).equals(pattern.get(i))
    // || pattern.get(i).equals("*"))) {
    // return false;
    // }
    // }
    // return true;
    // }
}

record NameNode(String n) implements AST {

    @Override
    public Pattern asPattern() {
        return new VariablePattern(n);
    }

    @Override
    public String toString() {
        return "'" + n;
    }
}

/**
 * @deprecated
 */
@Deprecated
record OldName(String name) implements CharSequence {

    public static OldName of(String s) {
        return new OldName(s);
    }

    @Override
    public char charAt(int index) {
        return name.charAt(index);
    }

    @Override
    public int length() {
        return name.length();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return name.subSequence(start, end);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof OldName otherName && otherName.name.equals(this.name);
    }

    @Override
    public int hashCode() {
        return 31 * (name != null ? name.hashCode() : 0) + getClass().hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
