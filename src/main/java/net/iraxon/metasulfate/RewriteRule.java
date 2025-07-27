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
    public AST apply(AST input, RewriteSystem env);

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
    public AST apply(AST input, RewriteSystem env) {
        return switch (this) {
            case EMPTY -> input;
            case REWRITE_RULE_DECLARATION -> applyRuleDeclaration(input, env);
        };
    }

    @Override
    public Pattern pattern() {
        return switch (this) {
            case EMPTY -> SingletonPatterns.EMPTY;
            case REWRITE_RULE_DECLARATION -> DECLARATION;
        };
    }

    private AST applyRuleDeclaration(AST in, RewriteSystem env) {
        final List<AST> children;
        if (in instanceof NestedNode nested && (children = nested.children()).size() == 4
                && children.get(1).equals(Atom.of("->"))) {

            final AST left = children.get(0);
            final AST right = children.get(2);

            final RewriteSystem rewriteEnv = env.extend(new FunctionRewriteRule(left.asPattern(), right, env));

            return env.rewrite(rewriteEnv.rewrite(children.get(3)));
        }
        return in;
    }
}

record FunctionRewriteRule(Pattern pattern, AST expr, RewriteSystem closureEnv) implements RewriteRule {

    @Override
    public AST apply(AST input, RewriteSystem env) {

        AST r = input;

        final RewriteSystem matchRewriteRules = pattern.match(r);
        // System.out.println(switch (matchRewriteRules) {
        //     case null -> "\t\tTerm " + r + " did not match " + pattern;
        //     default ->
        //         "Term " + r + " matched " + pattern + " yielding env: (\n" + matchRewriteRules + ")";
        // });
        if (matchRewriteRules != null) {
            return closureEnv.rewrite(matchRewriteRules.rewrite(expr));
        }
        return input;
    }

    @Override
    public String toString() {
        return pattern.toString() + " -> " + expr.toString()
                /*+ (closureEnv.equals(RewriteRules.EMPTY) ? "" : " (\n" + closureEnv.toString() + ")")*/;
    }
}
