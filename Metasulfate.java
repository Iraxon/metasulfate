
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
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Consumer;

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

    public static ValueExpression eval(final String src) {
        return parse(lex(src)).eval();
    }

    public static ValueExpression evalFile(final String path) {
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
        return new Parser(src).parse(Env.defaultScope);
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

    public MesoExpr parse(final Env scope) {
        final MesoExpr rVal = _parse(scope);
        if (hasNext()) {
            List<String> trailing = new ArrayList<>();
            while (hasNext()) {
                trailing.add(grab());
            }
            throw new IllegalArgumentException(
                    "Trailing data in program" + trailing.stream().reduce("", (x, y) -> x + " " + y + " "));
        }
        return rVal;
    }

    public MesoExpr _parse(final Env scope) {
        final String token = grab();
        if (token.equals(".")) {
            return NoApply.NO_APPLY;
        }
        try {
            return new MesoInt(token);
        } catch (NumberFormatException e) {
        }
        if (true) {
            return new CallExpression(new CallExpression(new NameExpression(token), _parse(scope)), _parse(scope));
        }
        // MesoExpr rVal = new MesoName(token);
        // return rVal;
        return null;
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
    default ValueExpression eval() {
        return (ValueExpression) this;
    }

    default boolean isFunction() {
        return false;
    }

    /**
     * Produces a nice String representation of the expression tree
     *
     * @param pre An amalgamation of spaces and vertical lines that should
     *            go before each line of the output after the first, if any; this
     *            controls nesting
     * @return A human-readable String representation of the tree
     */
    default String render(String pre) {
        return this.toString();
    }
}

interface ValueExpression extends MesoExpr {
    default ValueExpression eval(Env scope) {
        return this;
    }

    default int closureNumber() {
        return 0;
    }
}

record CallExpression(MesoExpr left, MesoExpr right) implements MesoExpr {

    // public CallExpression(MesoExpr left, MesoExpr right) {
    // this(left, right, left.closureNumber());
    // }

    // public boolean isApplicable() {
    // return closureNumber > 0;
    // }

    public ValueExpression eval(Env scope) {
        return MesoBool.T;
    }

    public ValueExpression apply(MesoExpr arg) {
        throw new RuntimeException();
    }

    public String render(String pre) {
        // Python to translate from:
        // LEFT_ELEMENT = (
        // self.left.render(pipes + "║ ")
        // if isinstance(self.left, TournTree)
        // else "═ " + str(self.left)
        // )
        // RIGHT_ELEMENT = (
        // self.right.render(pipes + " ")
        // if isinstance(self.right, TournTree)
        // else "═ " + str(self.right)
        // )
        // return f"{'═╗' if pipes != '' else
        // ''}\n{pipes}╠{LEFT_ELEMENT}\n{pipes}╚{RIGHT_ELEMENT}"
        // final String header = (!pre.equals("")) ? "═╗" : "";
        // final String l = this.left.render(pre + "║ ");
        // final String r = this.left.render(pre + " ");
        // return header + "@\n"
        // + pre + "╠" + l + "\n"
        // + pre + "╚" + r + "\n";
        return (pre.equals("")? "" : "═╗") + "\n"
                + pre + "╠" + this.left.render(pre + "║ ") + "\n"
                + pre + "╚" + this.right.render(pre + "  ");
    }
}

record NameExpression(MesoName name) implements MesoExpr {
    public NameExpression(String k) {
        this(new MesoName(k));
    }

    public ValueExpression eval(Env scope) {
        return scope.get(this.name);
    }

    public int closureNumber() {
        return this.eval(null).closureNumber();
    }

    public String toString() {
        return this.name.n();
    }
}

interface MesoType {
}

enum MesoNull implements ValueExpression {
    NULL;
}

enum NoApply implements ValueExpression {
    NO_APPLY;
}

enum ListStop implements ValueExpression {
    LIST_STOP;
}

enum MesoBool implements ValueExpression {
    T,
    F;
}

record MesoInt(long val) implements ValueExpression {

    /**
     * Construct a MesoInt from a String
     *
     * @param s String representation of the value to box
     * @throws NumberFormatException When passed a String that cannot be parsed into
     *                               a number
     */
    public MesoInt(String s) throws NumberFormatException {
        this(Long.valueOf(s));
    }

    public String toString() {
        return Long.toString(val);
    }
}

record MesoName(String n) implements ValueExpression {
}

record MesoList(List<ValueExpression> list) implements ValueExpression {
    public String toString() {
        String out = "{" + list.stream().map(x -> x.toString() + " ").reduce("", (x, y) -> x + y);
        return out.substring(0, out.length() - 1) + "}";
    }
}

/**
 * A record describing the name bindings that are in
 * scope from a given location in the source code
 */
record Env(Env outer, MesoName key, ValueExpression value) {

    public static final Env defaultScope = new Env(
            null, "T", MesoBool.T)
            .extend("F", MesoBool.F)
            .extend(".", NoApply.NO_APPLY);
    public Env(final Env outer, final String k, final ValueExpression value) {
        this(outer, new MesoName(k), value);
    }

    public Env extend(final MesoName key, final ValueExpression value) {
        return new Env(this, ((MesoName) key), value);
    }

    public Env extend(final String key, final ValueExpression value) {
        return new Env(this, new MesoName(key), value);
    }

    // UNTESTED
    public Env extend(final Env other) {
        Env rVal = this;
        Env current = other;
        while (current.outer != null) {
            rVal = rVal.extend(current.key, current.value);
            current = current.outer;
        }
        return rVal;
    }

    /**
     * Checks a name in the current Env and all
     * outer Envs and provides the matching value
     *
     * @param n a name, which must be a ValueExpression and should be a
     *          MesoName
     * @return the value bound to the provided name in this Env
     * @throws NoSuchElementException if the name is not bound
     */
    public ValueExpression get(final MesoName n) throws NoSuchElementException {
        Env current = this;
        while (true) {
            if (n.equals(current.key)) {
                return current.value;
            } else if (current.outer != null) {
                current = current.outer;
            } else {
                break;
            }
        }
        throw new NoSuchElementException("Undefined name:\n" + n + "\nin:\n" + this);
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
        Env current = this;
        while (current.outer != null && count != 0) {
            out += current.key + " : " + current.value + "; ";
            current = current.outer;
            count--;
        }
        return out + "}";
    }
}
