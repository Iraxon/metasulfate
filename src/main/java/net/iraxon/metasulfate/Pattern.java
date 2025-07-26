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

    public static LiteralPattern of(String s) {
        return new LiteralPattern(Atom.of(s));
    }

    @Override
    public RewriteRules match(AST expr) {
        if (expr.equals(lit))
            return RewriteRules.EMPTY;
        return null;
    }

    @Override
    public String toString() {
        return lit.toString();
    }
}

record VariablePattern(String name) implements Pattern {

    @Override
    public RewriteRules match(AST expr) {
        final var r = RewriteRules.of(new FunctionRewriteRule(LiteralPattern.of(name), expr, RewriteRules.EMPTY));
        // System.out.println("Variable bindings env: (\n" + r + ")");
        return r;
    }

    @Override
    public String toString() {
        return "'" + name;
    }
}

record SequencePattern(List<Pattern> patterns) implements Pattern {

    public static SequencePattern of(Pattern... patternArray) {
        return new SequencePattern(List.of(patternArray));
    }

    @Override
    public RewriteRules match(AST expr) {
        final List<AST> elements;
        if (expr instanceof NestedNode sequenceExpr
                && (elements = sequenceExpr.children()).size() == patterns.size()) {

            // Make a list from the results of matching pattern[i] to sequence[i]
            List<RewriteRules> matchResults = IntStream.range(0, patterns.size()).mapToObj(
                    (i) -> (patterns.get(i).match(elements.get(i)))).toList();

            // Any failures to match in the list mean the whole sequence fails to match
            if (matchResults.stream().anyMatch(x -> x == null)) {
                return null;
            }
            final RewriteRules r = matchResults.stream().reduce(RewriteRules.EMPTY, SequencePattern::merge);
            // System.out.println("Sequence match result: (\n" + r + ")");
            return r;
        }
        return null;
    }

    private static RewriteRules merge(RewriteRules x, RewriteRules y) {
        final RewriteRules r;
        @SuppressWarnings("unused")
        final String criterion;
        if (x.equals(y)) {
            r = x;
            criterion = "EQUAL";
        } else if (x.equals(RewriteRules.EMPTY)) {
            r = y;
            criterion = "FIRST_EMPTY";
        } else if (y.equals(RewriteRules.EMPTY)) {
            r = x;
            criterion = "SECOND_EMPTY";
        } else if (x == null || y == null || x.conflictsWith(y)) {
            r = null;
            criterion = "NULL_OR_CONFLICT";
        } else {
            r = x.extend(y);
            criterion = "EXTEND";
        }
        // System.out.println("Merging: (\n" + x + ") and (\n" + y + ") yielding: (\n" + r + ") criterion: " + criterion);
        return r;
    }

    public String toString() {
        return NestedNode.renderList(patterns);
    }
}

enum SingletonPatterns implements Pattern {
    WILDCARD,
    EMPTY;

    @Override
    public RewriteRules match(AST expr) {
        return switch (this) {
            case WILDCARD -> RewriteRules.EMPTY;
            case EMPTY -> null;
        };
    }
}
