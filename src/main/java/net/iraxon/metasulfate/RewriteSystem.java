package net.iraxon.metasulfate;

import java.util.ArrayList;
import java.util.List;

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

    public RewriteSystemTraverser traverser() {
        return new RewriteSystemTraverser(this);
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

    public record RewriteSystemTraverser(RewriteSystem target) {
        public RewriteSystemTraverser next() {
            var next = new RewriteSystemTraverser(target.outer);
            if (next.target == null)
                return null;
            return next;
        }

        public RewriteRule rule() {
           return target.rule();
        }
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
