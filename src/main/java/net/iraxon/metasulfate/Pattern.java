package net.iraxon.metasulfate;

import java.util.List;
import java.util.stream.IntStream;

interface Pattern {
    /**
     * @param expr The expression to attempt to match; this argument will not be
     *             reduced by the pattern matching algorithm
     * @return The RewriteRules describing the variable bindings made by the pattern
     *         matching or null
     *         if there is no match
     */
    public RewriteRules match(AST expr);
}

record LiteralPattern(AST lit) implements Pattern {

    // public static LiteralPattern of(String n) {
    //     return new LiteralPattern(new ValueNode(OldName.of(n)));
    // }

    @Override
    public RewriteRules match(AST expr) {
        if (!expr.equals(lit))
            return null;
        return RewriteRules.EMPTY;
    }
}

record VariablePattern(String name) implements Pattern {

    @Override
    public RewriteRules match(AST expr) {
        return RewriteRules.of(new VariableRewriteRule(name, expr));
    }
}

record SequencePattern(List<Pattern> patterns) implements Pattern {

    public static SequencePattern of(Pattern... patternArray) {
        return new SequencePattern(List.of(patternArray));
    }

    @Override
    public RewriteRules match(AST expr) {
        final List<AST> elements;
        if (expr instanceof NestedNode sequenceExpr && (elements = sequenceExpr.children()).size() == patterns.size()) {

            List<RewriteRules> matchResults = IntStream.range(0, patterns.size()).mapToObj(
                    (x) -> (patterns.get(x).match(elements.get(x)))).toList();
            if (matchResults.stream().anyMatch((x) -> x == null)) {
                return null;
            }
            return matchResults.stream().reduce(RewriteRules.EMPTY, SequencePattern::merge);
        }
        return null;
    }

    private static RewriteRules merge(RewriteRules x, RewriteRules y) {
        if (x == null || y == null || x.hasConflictingVariables(y))
            return null;
        return x.extend(y);
    }
}

enum SingletonPatterns implements Pattern {
    WILDCARD;

    @Override
    public RewriteRules match(AST expr) {
        return switch (this) {
            case WILDCARD -> RewriteRules.EMPTY;
        };
    }
}
