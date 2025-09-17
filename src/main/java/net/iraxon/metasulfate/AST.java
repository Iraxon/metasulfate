package net.iraxon.metasulfate;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

interface AST {

    public Pattern asPattern();

    record CacheEntry(RewriteSystem rules, AST in) {
    }

    static ConcurrentHashMap<CacheEntry, AST> rewriteCache = new ConcurrentHashMap<>();

    private static <R> R doUntilSame(R input, Function<R, R> function) {
        R previous;
        R next = input;
        do {
            previous = next;
            next = function.apply(previous);
        } while (previous != next);
        return next;
    }

    public default AST rewrite(RewriteSystem rules) {
        var entry = new CacheEntry(rules, this);
        final AST rVal;
        if (rewriteCache.containsKey(entry)) {
            rVal = rewriteCache.get(entry);
        } else {
            var traverser = entry.rules().traverser();
            AST value = entry.in();
            RewriteRule rule;
            do {
                rule = traverser.rule();
                value = value.rewrite(rule);
                traverser = traverser.next();
            } while (traverser != null);
            rewriteCache.put(entry, value);
            rVal = value;
        }
        return rVal;
    }

    public default AST rewrite(RewriteRule rule) {
        AST rVal = doUntilSame(this, (term) -> rule.apply(term));
        if (this.equals(rVal))
            System.out.println("       " + this + " does not match " + rule);
        else
            System.out.println(this + " becomes " + rVal);
        return rVal;
    }

    public default AST exhaustiveRewrite(RewriteSystem r) {
        return doUntilSame(this, term -> term.rewrite(r));
    }
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
    END_OF_LIST,
    END_OF_PATTERN;

    @Override
    public Pattern asPattern() {
        throw new UnsupportedOperationException("Attempted to convert " + this + " to pattern");
    }
}

record NestedNode(List<AST> children) implements AST {

    @Override
    public Pattern asPattern() {
        return SequencePattern.of(children.stream().map(AST::asPattern).toArray(Pattern[]::new));
    }

    public AST rewriteChildren(RewriteRule r) {
        return new NestedNode(children.stream().map((child) -> (child.rewrite(r))).toList());
    }

    public AST rewriteSuper(RewriteRule r) {
        return AST.super.rewrite(r);
    }

    public static String renderList(List<?> list) {
        return renderList(list, false);
    }

    public static String renderList(List<?> list, boolean curlyBrackets) {
        return (curlyBrackets ? "{" : "[")
                + list.stream().map(x -> x.toString()).reduce("", (x, y) -> x.equals("") ? y : x + " " + y)
                + (curlyBrackets ? "}" : "]");
    }

    @Override
    public String toString() {
        return renderList(children);
    }
}
