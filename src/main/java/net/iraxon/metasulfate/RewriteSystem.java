package net.iraxon.metasulfate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An association list of rewrite rules
 */
record RewriteSystem(RewriteSystem outer, RewriteRule rule) {

    public static final RewriteSystem EMPTY = new RewriteSystem(null, RewriteRule.EMPTY);
    public static final RewriteSystem DEFAULT = RewriteSystem.of(SingletonRewriteRules.REWRITE_RULE_DECLARATION);

    // public static final RewriteRules defaultRules = new RewriteRules(null,
    // new MesoName("T"), MesoBool.T)
    // .extend("F", MesoBool.F)
    // .extend(".", NoApply.NO_APPLY);

    /**
     * @deprecated Please do not use the constructor; use {@code RewriteRules::of}
     */
    @Deprecated
    public RewriteSystem {
    }

    public static RewriteSystem of(RewriteRule rule) {
        return EMPTY.extend(rule);
    }

    public List<RewriteRule> asList() {
        ArrayList<RewriteRule> rVal = new ArrayList<>();
        RewriteSystem current = this;
        do {
            rVal.add(current.rule);
            current = current.outer;
        } while (current != null);
        return List.copyOf(rVal);
    }

    /**
     * @return the number of rules in this RewriteRules object; note that
     *         certain optimizations can result in EMPTY rules and EMPTY
     *         RewriteRules objects
     *         being cut out, so this number does not necessarily reflect the number
     *         of extensions
     */
    public int size() {
        return asList().size();
    }

    public RewriteSystem extend(final RewriteRule newRule) {
        if (newRule.equals(RewriteRule.EMPTY)) {
            return this;
        }
        return new RewriteSystem(this.equals(EMPTY) ? null : this, newRule);
    }

    public RewriteSystem extend(final RewriteSystem other) {
        // System.out.println("Extending: (\n" + this + ") with: (\n" + other + ")");
        if (other.equals(EMPTY))
            return this;
        RewriteSystem rVal = this;
        for (RewriteRule rule : other.asList()) {
            // System.out.println("Adding rule: " + rule);
            rVal = rVal.extend(rule);
        }
        return rVal;
    }

    private record CacheEntry(RewriteSystem rules, AST in) {
    }

    private static ConcurrentHashMap<CacheEntry, AST> rewriteCache = new ConcurrentHashMap<>();

    /**
     * Rewrites the input by applying all rules from bottom to top
     *
     * @param input A term
     * @return The result of the aforementioned rewriting or the provided input
     *         if none of the rules apply to this term
     */
    public AST rewrite(final AST input) {

        final CacheEntry cacheEntry = new CacheEntry(this, input);
        if (rewriteCache.containsKey(cacheEntry))
            return rewriteCache.get(cacheEntry);

        final AST before = rewriteChildrenIfNested(input);
        final AST after = rule.apply(before, this);
        if (!rule.equals(SingletonRewriteRules.REWRITE_RULE_DECLARATION)) {
            if (before.equals(after)) {
                System.out.println("\t\tTerm " + before + " did not match " + rule);
            } else {
                System.out.println("Term " + before + " rewritten by " + rule + " yielding : " + after);
            }
        }
        rewriteCache.put(cacheEntry, after);
        return after;

        // AST r = input;
        // if (input instanceof NestedNode n) {
        // r = rewriteChildren(n);
        // }
        // AST thisApply = rule.apply(r, this);
        // if (thisApply != null) {
        // r = thisApply;
        // } else if (outer != null) {
        // System.out.println(outer);
        // r = outer.rewrite(r);
        // }
        // return r;
    }

    /**
     * Rewrites the children of a nested node
     *
     * @param in A NestedNode
     * @return The same, but with each child rewriten
     */
    public NestedNode rewriteChildren(NestedNode in) {
        return new NestedNode(in.children().stream().map(this::rewrite).toList());
    }

    public AST rewriteChildrenIfNested(AST in) {
        return switch (in) {
            case NestedNode n -> rewriteChildren(n);
            default -> in;
        };
    }

    public boolean conflictsWith(RewriteSystem other) {
        if (this.equals(other)) {
            return false;
        }
        final var thisList = this.asList();
        final var otherList = other.asList();

        for (var rule : thisList) {
            for (var otherRule : otherList) {
                if ((rule.pattern().equals(otherRule.pattern())) && !rule.equals(otherRule)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        if (this.equals(RewriteSystem.EMPTY)) {
            return "\tRewriteRules.EMPTY\n";
        }
        return (outer == null ? "" : outer.toString())
                + "\t" + rule.toString() + "\n";
    }

    // public String toString() {
    // return shortString(1);
    // }

    // public String shortString() {
    // return shortString(10);
    // }

    // /**
    // * Returns a String representation of a fixed number of the most recent
    // * entries in this Env
    // *
    // * @param count Number of entries to include; negative values allow as many
    // * entries as the Env holds
    // * @return String representation of this Env including only that many entries
    // */
    // public String shortString(int count) {
    // String out = "{";
    // RewriteRules current = this;
    // while (current.outer != null && count != 0) {
    // out += current.pattern + " : " + current.rule + "; ";
    // current = current.outer;
    // count--;
    // }
    // return out + "}";
    // }
}
