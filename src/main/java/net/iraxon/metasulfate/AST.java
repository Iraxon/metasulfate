package net.iraxon.metasulfate;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

interface AST {
    public Pattern asPattern();
}

record Atom(String name) implements AST {

    private static ConcurrentHashMap<String, Atom> atomCache = new ConcurrentHashMap<>();

    /**
     * @deprecated Please use the factory method.
     */
    @Deprecated
    public Atom {
        Objects.requireNonNull(name);
    }

    public static Atom of(String n) {
        return atomCache.computeIfAbsent(n, Atom::new);
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

enum ASTSingletons implements AST {
    END_OF_LIST;

    @Override
    public Pattern asPattern() {
        throw new UnsupportedOperationException("Attempted to convert END_OF_LIST to pattern");
    }
}

record NestedNode(List<AST> children) implements AST {

    @Override
    public Pattern asPattern() {
        return SequencePattern.of(children.stream().map(AST::asPattern).toArray(Pattern[]::new));
    }

    public static String renderList(List<?> list) {
        return "[" + list.stream().map(x -> x.toString()).reduce("", (x, y) -> x.equals("") ? y : x + " " + y)
                + "]";
    }

    @Override
    public String toString() {
        return renderList(children);
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
