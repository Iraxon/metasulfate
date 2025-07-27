package net.iraxon.metasulfate;

import java.util.List;

interface RewriteRule {

    public static final RewriteRule EMPTY = SingletonRewriteRules.EMPTY;

    /**
     * @param input A term
     * @param env   The ruleset that is invoking the application of the rule (needed
     *              for propagating rewriting
     *              downward)
     * @return the result of applying this rewrite rule to the input or
     *         null if the input does not match this rewrite rule
     */
    public AST apply(AST input, RewriteRules env);

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
    public AST apply(AST input, RewriteRules env) {
        return switch (this) {
            case EMPTY -> null;
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

    private AST applyRuleDeclaration(AST in, RewriteRules env) {
        final List<AST> children;
        if (in instanceof NestedNode nested && (children = nested.children()).size() == 4
                && children.get(1).equals(Atom.of("->"))) {

            final AST left = env.rewrite(children.get(0));
            final AST right = env.rewrite(children.get(2));

            final RewriteRules rewriteEnv = env.extend(new FunctionRewriteRule(left.asPattern(), right, env));

            return env.rewrite(rewriteEnv.rewrite(children.get(3)));
        }
        return null;
    }
}

record FunctionRewriteRule(Pattern pattern, AST expr, RewriteRules env) implements RewriteRule {

    @Override
    public AST apply(AST input, RewriteRules env) {
        final RewriteRules matchRewriteRules = pattern.match(input);
        System.out.println("Term " + input + " tested against " + pattern + " yielding env: (\n" + matchRewriteRules + ")");
        if (matchRewriteRules == null) {
            return null;
        }
        return env.rewrite(matchRewriteRules.rewrite(expr));
    }

    @Override
    public String toString() {
        return pattern.toString() + " -> " + expr.toString() + (env.equals(RewriteRules.EMPTY) ? "" : " (\n" + env.toString() + ")");
    }
}
