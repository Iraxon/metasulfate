package net.iraxon.metasulfate;

import java.util.List;

interface RewriteRule {

    public static final RewriteRule EMPTY = SingletonRewriteRules.EMPTY;

    /**
     * @param input A term
     * @param env   The ruleset that is invoking the application of the rule (needed
     *              for propagating rewriting
     *              downward)
     * @return the result of applying this rewrite rule to the input (which may just be the input)
     */
    public AST apply(AST input);

    /**
     * @return A pattern describing what terms match the rewrite rule
     */
    public Pattern pattern();
}

enum SingletonRewriteRules implements RewriteRule {
    EMPTY,
    REWRITE_RULE_DECLARATION;

    // public static final FunctionRewriteRule REWRITE_RULE_DECLARATION = new
    // FunctionRewriteRule(
    // SequencePattern.of(
    // VariablePattern.of("left"),
    // LiteralPattern.of("->"),
    // VariablePattern.of("right"),
    // VariablePattern.of("rest")),
    // new ValueNode(Name.of("rest")),
    // RewriteRules.EMPTY);

    public static final SequencePattern DECLARATION = SequencePattern.of(
            new VariablePattern("left"),
            LiteralPattern.of("->"),
            new VariablePattern("right"),
            new VariablePattern("rest"));

    @Override
    public AST apply(AST input) {
        return switch (this) {
            case EMPTY -> input;
            case REWRITE_RULE_DECLARATION -> applyRuleDeclaration(input);
        };
    }

    @Override
    public Pattern pattern() {
        return switch (this) {
            case EMPTY -> SingletonPatterns.EMPTY;
            case REWRITE_RULE_DECLARATION -> DECLARATION;
        };
    }

    private AST applyRuleDeclaration(AST in) {
        final List<AST> children;
        if (in instanceof NestedNode nested && (DECLARATION.match(nested) != null)) {

            children = nested.children();

            final AST left = children.get(0);
            final AST right = children.get(2);
            final AST rest = children.get(3);

            assert left instanceof Pattern;

            return rest.rewrite(new FunctionRewriteRule((Pattern) left, right));
        }
        return in;
    }
}

record FunctionRewriteRule(Pattern pattern, AST expr) implements RewriteRule {

    @Override
    public AST apply(AST input) {

        AST r = input;

        final RewriteSystem matchRewriteRules = pattern.match(r);
        // System.out.println(switch (matchRewriteRules) {
        //     case null -> "\t\tTerm " + r + " did not match " + pattern;
        //     default ->
        //         "Term " + r + " matched " + pattern + " yielding env: (\n" + matchRewriteRules + ")";
        // });
        if (matchRewriteRules != null) {
            return expr.rewrite(matchRewriteRules);
        }
        return input;
    }

    @Override
    public String toString() {
        return pattern.toString() + " -> " + expr.toString()
                /*+ (closureEnv.equals(RewriteRules.EMPTY) ? "" : " (\n" + closureEnv.toString() + ")")*/;
    }
}
