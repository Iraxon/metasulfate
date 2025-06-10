package net.iraxon.metasulfate;

/**
 * Copyright Iraxon 2025
 *
 * This file is free software under the GNU GPL v3 or later.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class Metasulfate {
    public static void main(final String[] args) {
        System.out.println("\n\n\n---\n" +
                parse(lex("PROD 1 SUM 1 2")).render("") + "\n---\n\n\n");
        // System.out.println("\n\n\n---\n" +
        // evalFile("!standard_library.meso") + "\n---\n\n\n");
        /*
         * TEST PROGRAMS:
         * LAMBDA_DOT [PRODUCT DOT 2] LET 'x 3 LET 'y 2 SUM x y
         * == 10
         * LET 'successor [SUM DOT 1] LET 'x 3 LET 'y 2 successor PROD 2 SUM x y
         * == 11
         * "LAMBDA_DOT [LET 'x DOT LAMBDA_DOT [SUM x DOT] .] 1 .
         */
    }

    public static MesoExpr eval(final String src) {
        return parse(lex(src));
    }

    public static MesoExpr evalFile(final String path) {
        String src = "";
        final Scanner s;
        try {
            s = new Scanner(new File(path));
        } catch (final FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
        while (s.hasNextLine()) {
            src += s.nextLine() + " ";
        }
        s.close();
        return eval(src);
    }

    private static List<String> lex(final String src) {
        return Lexer.lex(src);
    }

    private static MesoExpr parse(final List<String> src) {
        return new Parser(src).parse(RewriteRules.defaultRules);
    }
}

class Lexer {

    public static List<String> lex(final String rawSrc) {
        final List<String> rVal = new ArrayList<>();
        _lex(rVal, rawSrc);
        System.out.println("Lexer returning:\n" + rVal);
        return rVal;
    }

    private static final Set<Character> punctuation = new HashSet<>(
            Arrays.asList(new Character[] {
                    '[', ']',
                    '(', ')',
                    '{', '}',
                    ':', ';',
                    '\'', '\"',
                    '.'
            }));
    private static final Set<Character> whitespaceCompatible = new HashSet<>(
            Arrays.asList(new Character[] { '(', '\"' }));

    private static void _lex(final List<String> list, final String src) {
        final Consumer<String> add = (s) -> {
            if (s.length() > 0) {
                list.add(s);
            }
        };
        String acc = "";
        int cursor = 0;

        int commentNestingDepth = 0; // Increments on left paren; decreases on right paren

        char current;
        final int len = src.length();
        for (cursor = 0; cursor < len; cursor++) {
            current = src.charAt(cursor);
            if (current == '(') {
                add.accept(acc);
                acc = "";
                commentNestingDepth++;
            }
            if (commentNestingDepth == 0) {
                if (Character.isWhitespace((int) current)) {
                    if (acc.length() > 0 && whitespaceCompatible.contains(acc.charAt(0))) {
                        acc += current;
                    } else {
                        add.accept(acc);
                        acc = "";
                    }
                } else if (punctuation.contains(current)) {
                    add.accept(acc);
                    acc = "";
                    add.accept("" + current);
                } else {
                    if (cursor + 1 >= src.length()) {
                        add.accept(acc + current);
                    }
                    acc += current;
                }
            } else if (commentNestingDepth < 0) {
                throw new IllegalStateException("Unmatched closing paren on comment");
            }
            if (current == ')') {
                commentNestingDepth--;
            }
        }
    }
}

class Parser {
    private final List<String> src;
    private int cursor;

    public Parser(final List<String> src) {
        this.src = src;
        this.cursor = 0;
    }

    public MesoExpr parse(final RewriteRules scope) {
        final MesoExpr rVal = _parse(scope);
        if (hasNext()) {
            final List<String> trailing = new ArrayList<>();
            while (hasNext()) {
                trailing.add(grab());
            }
            throw new IllegalArgumentException(
                    "Trailing data in program:\n"
                            + trailing.stream().reduce("", (x, y) -> x + " " + y + " ")
                            + "\nconsumed tokens evaluate to:\n" + rVal);
        }
        return rVal;
    }

    public MesoExpr _parse(final RewriteRules scope) {
        final String token = grab();
        if (token.equals(".")) {
            return NoApply.NO_APPLY;
        }
        if (token.equals("'")) {
            return new MesoName(grab());
        }
        try {
            return new MesoInt(token);
        } catch (final NumberFormatException e) {
        }
        MesoExpr rVal = new MesoName(token);
        MesoExpr arg;
        while (rVal.arity(scope) > 0) {
            arg = _parse(scope);
            if (arg == NoApply.NO_APPLY) {
                break;
            }
            rVal = new CallExpression(rVal, arg);
        }
        return rVal;
    }

    private boolean hasNext() {
        return this.cursor < src.size();
    }

    private String grab() {
        final String t = src.get(cursor++);
        System.out.println("Grabbing token: " + t);
        return t;
    }

    /**
     * Continuously grabs input items until reaching a terminator;
     * the terminator is consumed but not returned; count will be
     * kept to allow for nesting
     *
     * @param initiator  The left delimeter
     * @param terminator The right delimiter
     */
    private List<String> grabDelimitedRange(final String initiator, final String terminator) {
        int nestingLevel = 1; // There was a starting opening brace
        final List<String> rval = new ArrayList<>();
        String current;
        while (true) {
            System.out.print("While looking for " + terminator + ", ");
            current = grab();
            if (current.equals(initiator)) {
                nestingLevel++;
            } else if (current.equals(terminator)) {
                nestingLevel--;
            }
            if (nestingLevel < 1) {
                return rval;
            }
            rval.add(current);
        }
    }
}

interface MesoExpr {
    default MesoExpr eval(final RewriteRules scope) {
        return (ValueExpression) this;
    }

    /**
     * Returns 0 for an expression that does not evaluate to a function
     * The behavior otherwise is best shown by example:
     * `SUM` has arity two because it should be applied once to get a function,
     * and then that function should be applied to get a non-function.
     * `SUM 1` evaluates to a function that should consume one argument, so it has
     * arity 1.
     * `SUM 1 2` has arity 0 because it evaluates to the non-function constant `3`
     * Bracket literals all have arity 0 because they are not meant to be applied
     * where they are written.
     *
     * @return The expression's arity number
     */
    int arity(RewriteRules scope);

    /**
     * Produces a nice String representation of the expression tree
     *
     * @param pre An amalgamation of spaces and vertical lines that should
     *            go before each line of the output after the first, if any; this
     *            controls nesting
     * @return A human-readable String representation of the tree
     */
    default String render(final String pre) {
        return this.toString();
    }
}

interface ValueExpression extends MesoExpr {
    default ValueExpression eval(final RewriteRules scope) {
        return this;
    }
}

interface NonFunctionValueExpression extends ValueExpression {
    @Override
    default int arity(final RewriteRules scope) {
        return 0;
    }
}

record CallExpression(MesoExpr left, MesoExpr right) implements MesoExpr {

    // public boolean isApplicable() {
    // return arity > 0;
    // }

    // public ValueExpression eval(Env scope) {
    // return MesoBool.T;
    // }

    public ValueExpression apply(final MesoExpr arg) {
        throw new RuntimeException();
    }

    @Override
    public int arity(final RewriteRules scope) {
        return this.left.arity(scope) - 1;
    }

    @Override
    public String render(final String pre) {
        return (pre.equals("") ? "╠" : "══") + this.left.render(pre + "║ ") + "\n"
                + pre + "╚═" + this.right.render(pre + "   ");
    }
}

// record NameExpression(MesoName name) implements MesoExpr {
//     public NameExpression(final String k) {
//         this(new MesoName(k));
//     }

//     @Override
//     public MesoExpr eval(final RewriteRules scope) {
//         try {
//             return scope.get(this.name);
//         } catch (final NoSuchElementException e) {
//             return this;
//         }
//     }

//     public int arity(final RewriteRules scope) {
//         MesoExpr v;
//         try {
//             return scope.rewrite(this.name).arity(scope);
//         } catch (final NoSuchElementException e) {
//             return 0;
//         }
//     }

//     public String toString() {
//         return this.name.n();
//     }
// }

interface MesoType {
}

enum MesoNull implements NonFunctionValueExpression {
    NULL;
}

enum NoApply implements NonFunctionValueExpression {
    NO_APPLY;
}

enum ListStop implements NonFunctionValueExpression {
    LIST_STOP;
}

enum MesoBool implements NonFunctionValueExpression {
    T,
    F;
}

record MesoInt(long val) implements NonFunctionValueExpression {

    /**
     * Construct a MesoInt from a String
     *
     * @param s String representation of the value to box
     * @throws NumberFormatException When passed a String that cannot be parsed into
     *                               a number
     */
    public MesoInt(final String s) throws NumberFormatException {
        this(Long.valueOf(s));
    }

    public String toString() {
        return Long.toString(val);
    }
}

record MesoName(String n) implements NonFunctionValueExpression {
}

record MesoList(List<ValueExpression> list) implements NonFunctionValueExpression {
    public String toString() {
        final String out = "{" + list.stream().map(x -> x.toString() + " ").reduce("", (x, y) -> x + y);
        return out.substring(0, out.length() - 1) + "}";
    }
}

interface Closure extends ValueExpression {
    public MesoExpr apply(ValueExpression arg);
}

record MesoClosure(List<String> def, RewriteRules env, int arity) implements Closure {

    @Override
    public MesoExpr apply(final ValueExpression arg) {
        return new Parser(def).parse(env.extend("DOT", arg).extend("REC", this));
    }

    public int arity(RewriteRules r) {
        return arity;
    }

    public String toString() {
        return defString() + "  (function, env: " + env.shortString(1) + ")";
    }

    private String defString() {
        String out = "[";
        for (final String word : def) {
            out += word + (!word.equals("\'") ? " " : "");
        }
        return out + "]";
    }
}

/**
 * An association list of expressions to expressions,
 * describing a set of rewrite rules (substitutions)
 */
record RewriteRules(RewriteRules outer, Predicate<MesoExpr> pattern,UnaryOperator<MesoExpr> rule) {

    public static final RewriteRules defaultRules = new RewriteRules(null,
            new MesoName("T"), MesoBool.T)
            .extend("F", MesoBool.F)
            .extend(".", NoApply.NO_APPLY);

    public RewriteRules(final RewriteRules outer, final Predicate<MesoExpr> pattern, final MesoExpr r) {
        this(outer, pattern, ((UnaryOperator<MesoExpr>) v -> r));
    }

    public RewriteRules(final RewriteRules outer, final MesoExpr left, final MesoExpr right) {
        this(outer, ((Predicate<MesoExpr>) v -> v.equals(left)), right);
    }

    public RewriteRules extend(final Predicate<MesoExpr> condition, final UnaryOperator<MesoExpr> r) {
        return new RewriteRules(this, condition, r);
    }

    public RewriteRules extend(final Predicate<MesoExpr> condition, final MesoExpr r) {
        return new RewriteRules(this, condition, r);
    }

    public RewriteRules extend(final MesoExpr l, final MesoExpr r) {
        return new RewriteRules(this, l, r);
    }

    public RewriteRules extend(final String l, final MesoExpr r) {
        return this.extend(new MesoName(l), r);
    }

    public RewriteRules extend(final RewriteRules r) {
        RewriteRules rVal = this;
        RewriteRules current = r;
        while (current.outer != null) {
            rVal = rVal.extend(current.pattern, current.rule);
            current = current.outer;
        }
        return rVal;
    }

    /**
     * Applies one step of rewriting to the input; it is not
     * guaranted that the output will be different than the
     * input
     * @param in A MesoExpr
     * @return The result of one step of rewriting according to the contained rules
     */
    public MesoExpr rewrite(final MesoExpr in) {
        RewriteRules current = this;
        while (true) {
            if (current.pattern.test(in)) {
                return current.rule.apply(in);
            } else if (current.outer != null) {
                current = current.outer;
            } else {
                break;
            }
        }
        return in;
    }

    public String toString() {
        return shortString(1);
    }

    public String shortString() {
        return shortString(10);
    }

    /**
     * Returns a String representation of a fixed number of the most recent
     * entries in this Env
     *
     * @param count Number of entries to include; negative values allow as many
     *              entries as the Env holds
     * @return String representation of this Env including only that many entries
     */
    public String shortString(int count) {
        String out = "{";
        RewriteRules current = this;
        while (current.outer != null && count != 0) {
            out += current.pattern + " : " + current.rule + "; ";
            current = current.outer;
            count--;
        }
        return out + "}";
    }
}

/**
 * A record describing the name bindings that are in
 * scope from a given location in the source code
 */
// record Env(Env outer, MesoName key, ValueExpression value) {

//     public static final Env defaultScope = new Env(
//             null, "T", MesoBool.T)
//             .extend("F", MesoBool.F)
//             .extend(".", NoApply.NO_APPLY);
//     public Env(final Env outer, final String k, final ValueExpression value) {
//         this(outer, new MesoName(k), value);
//     }

//     public Env extend(final MesoName key, final ValueExpression value) {
//         return new Env(this, ((MesoName) key), value);
//     }

//     public Env extend(final String key, final ValueExpression value) {
//         return new Env(this, new MesoName(key), value);
//     }

//     // UNTESTED
//     public Env extend(final Env other) {
//         Env rVal = this;
//         Env current = other;
//         while (current.outer != null) {
//             rVal = rVal.extend(current.key, current.value);
//             current = current.outer;
//         }
//         return rVal;
//     }

//     /**
//      * Checks a name in the current Env and all
//      * outer Envs and provides the matching value
//      *
//      * @param n a name, which must be a ValueExpression and should be a
//      *          MesoName
//      * @return the value bound to the provided name in this Env
//      * @throws NoSuchElementException if the name is not bound
//      */
//     public ValueExpression get(final MesoName n) throws NoSuchElementException {
//         Env current = this;
//         while (true) {
//             if (n.equals(current.key)) {
//                 return current.value;
//             } else if (current.outer != null) {
//                 current = current.outer;
//             } else {
//                 break;
//             }
//         }
//         throw new NoSuchElementException("Undefined name:\n" + n + "\nin:\n" + this);
//     }

//     public String toString() {
//         return shortString(1);
//     }

//     public String shortString() {
//         return shortString(10);
//     }

//     /**
//      * Returns a String representation of a fixed number of the most recent
//      * entries in this Env
//      *
//      * @param count Number of entries to include; negative values allow as many
//      *              entries as the Env holds
//      * @return String representation of this Env including only that many entries
//      */
//     public String shortString(int count) {
//         String out = "{";
//         Env current = this;
//         while (current.outer != null && count != 0) {
//             out += current.key + " : " + current.value + "; ";
//             current = current.outer;
//             count--;
//         }
//         return out + "}";
//     }
// }
