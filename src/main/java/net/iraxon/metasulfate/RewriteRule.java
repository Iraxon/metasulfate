package net.iraxon.metasulfate;

import java.util.List;

interface RewriteRule {

    public static final RewriteRule EMPTY = SingletonRewriteRules.EMPTY;

    /**
     * @param input A term
     * @return the result of applying this rewrite rule to the input or
     *         null if the input does not match this rewrite rule
     */
    public AST apply(AST input);
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

    @Override
    public AST apply(AST input) {
        return switch (this) {
            case EMPTY -> null;
            case REWRITE_RULE_DECLARATION -> applyRuleDeclaration(input);
        };
    }

    private AST applyRuleDeclaration(AST in) {
        final List<AST> children;
        if (in instanceof NestedNode nested && (children = nested.children()).size() == 4
                && children.get(1).equals(Atom.of("->"))) {
            final AST left = children.get(0);
            final AST right = children.get(2);
            final AST rest = children.get(3);

            return RewriteRules.of(new FunctionRewriteRule(left.asPattern(), right, RewriteRules.EMPTY)).rewrite(rest);
        }
        return null;
    }
}

record VariableRewriteRule(String name, AST value) implements RewriteRule {

    @Override
    public AST apply(AST input) {
        if (input instanceof Atom aInput
                && aInput.name().equals(this.name)) {
            return value;
        }
        return null;
    }
}

record FunctionRewriteRule(Pattern pattern, AST expr, RewriteRules env) implements RewriteRule {

    @Override
    public AST apply(AST input) {
        RewriteRules matchRewriteRules = pattern.match(input);
        if (matchRewriteRules == null) {
            return null;
        }
        return env.extend(matchRewriteRules).rewrite(expr);
    }
}
