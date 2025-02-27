
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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongBinaryOperator;

public class Metasulfate {
    public static void main(final String[] args) {
        System.out.println("\n\n\n---\n" +
                evalFile("!standard_library.meso") + "\n---\n\n\n");
        /*
         * TEST PROGRAMS:
         * LAMBDA_DOT [PRODUCT DOT 2] LET 'x 3 LET 'y 2 SUM x y
         * == 10
         * LET 'successor [SUM DOT 1] LET 'x 3 LET 'y 2 successor PROD 2 SUM x y
         * == 11
         * "LAMBDA_DOT [LET 'x DOT LAMBDA_DOT [SUM x DOT] .] 1 .
         */
    }

    public static MesoValue eval(final String src) {
        return parse(Lexer.lex(src));
    }

    public static MesoValue evalFile(final String path) {
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

    private static MesoValue parse(final List<String> src) {
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
                throw new IllegalStateException("Comment nesting depth below zero");
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

    public MesoValue parse(final Env scope) {
        final MesoValue rVal = _parse(scope);
        if (hasNext()) {
            throw new IllegalArgumentException("Trailing data in program");
        }
        return rVal;
    }

    private MesoValue _parse(final Env scope) {
        final String t = grab();
        try {
            return new MesoInt(Long.valueOf(t));
        } catch (final NumberFormatException e) {
        }
        switch (t) {
            case "[":
                return new MesoClosure(grabDelimitedRange(t, "]"), scope);
            case "LET":
                final MesoValue name = _parse(scope);
                final MesoValue value = _parse(scope);
                return _parse(scope.extend(name, value));
            case "\'":
                return new MesoName(grab());
            case "ENV":
                return scope;
            case "EXPORT":
                return _parse(scope);
            default:
                MesoValue rVal;
                MesoValue arg;

                try {
                    rVal = new Operator(t);
                } catch (IllegalArgumentException e) {
                    rVal = scope.get(t);
                }

                while (rVal instanceof Closure && (arg = _parse(scope)) != NoApply.NO_APPLY) {
                    System.out.println("Applying: " + rVal);
                    rVal = ((Closure) rVal).apply(arg);
                    System.out.println("rVal is now: " + rVal);
                }
                return rVal;
        }
    }

    private boolean hasNext() {
        return this.cursor < src.size();
    }

    private String grab() {
        final String t = src.get(cursor++);
        System.out.println("Grabbing token: " + t);
        return t;
    }

    /*
     * Continuously grabs input items until reaching a terminator;
     * the terminator is consumed but not returned;
     * if the initiator is not null, then
     * count will be kept to allow for nesting
     */
    private List<String> grabDelimitedRange(final String initiator, final String terminator) {
        int nestingLevel = 1; // There was a starting opening brace
        final List<String> rval = new ArrayList<>();
        String current;
        while (true) {
            if (true) {
                System.out.print("While looking for " + terminator + ", ");
            }
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

interface MesoValue {
}

enum MesoBool implements MesoValue {
    T,
    F
}

enum NoApply implements MesoValue {
    NO_APPLY
}

record MesoInt(long v) implements MesoValue {

    /**
     * Applies a LongBinaryOperator to the values of two
     * MesoInts and makes a new MesoInt from the result
     *
     * @param op LongBinaryOperator to apply
     * @param x  First arg
     * @param y  Second arg
     * @return The new MesoInt representing the result
     */
    public static MesoInt op(final LongBinaryOperator op, final MesoValue x, final MesoValue y) {
        System.out.println("Performing int operation " + op + " on:\n" + x + "\nand:\n" + y);
        return new MesoInt(op.applyAsLong(((MesoInt) x).v, ((MesoInt) y).v));
    }

    public String toString() {
        return Long.toString(v) + " (int)";
    }
}

record MesoName(String n) implements MesoValue {
    public String toString() {
        return "\'" + n;
    }
}

interface Closure extends MesoValue {
    public MesoValue apply(MesoValue arg);
}

record MesoClosure(List<String> def, Env env) implements Closure {

    public MesoValue apply(final MesoValue arg) {
        return new Parser(def).parse(env.extend("DOT", arg));
    }

    public String toString() {
        return defString() + " " + env.toString();
    }

    private String defString() {
        String out = "[";
        for (String word : def) {
            out += word + (!word.equals("\'") ? " " : "");
        }
        return out;
    }
}

/**
 * Provides the built-in operations that Metasulfate supports
 */
enum Operation {
    SUM((x, y) -> x + y),
    DIFF((x, y) -> x - y),

    PROD((x, y) -> x * y),
    QUO((x, y) -> x / y),
    MOD((x, y) -> x % y),

    POW((x, y) -> (long) (Math.pow(x, y)));

    public final LongBinaryOperator longFunc;

    private Operation(LongBinaryOperator longFunc) {
        this.longFunc = longFunc;
    }

    /**
     * Set of String reps of the operations, for use in checking
     * if a String represents one of the operations
     */
    public static final Set<String> Operations = new HashSet<String>(
            Arrays.stream(values()).map((n) -> (n.name())).toList());
}

record Operator(Operation op) implements Closure {

    /**
     * Construct an operator closure from a String naming the operator
     *
     * @param s the String
     * @throws IllegalArgumentException if the String does not name an operator
     */
    public Operator(String s) throws IllegalArgumentException {
        this(Operation.valueOf(s));
    }

    public StageTwoOperator apply(final MesoValue arg1) {
        return new StageTwoOperator(op, arg1);
    }

    /**
     * Due to the curried nature of Metasulfate functions,
     * binary operators are actually two closures; this is the
     * closure for the inner function
     */
    record StageTwoOperator(Operation op, MesoValue arg1) implements Closure {
        public MesoValue apply(final MesoValue arg2) {
            if (arg1 instanceof MesoInt) {
                return MesoInt.op(op.longFunc, arg1, arg2);
            }
            throw new IllegalArgumentException(
                    "Invalid argument set for operation:\n" + op + "\narg1:\n" + arg1 + "\narg2:\n" + arg2);
        }
    }
}

// UNTESTED; possible won't be used
record JavaClosure(Function<MesoValue, MesoValue> func, Env env) implements Closure {
    public JavaClosure(BiFunction<MesoValue, MesoValue, MesoValue> biFunc, Env env) {
        this(
                v1 -> new JavaClosure(v2 -> biFunc.apply(v1, v2), env),
                env);
    }

    public MesoValue apply(final MesoValue arg) {
        return func.apply(arg);
    }
}

/**
 * A record describing the name bindings that are in
 * scope from a given location in the source code
 */
record Env(Env outer, MesoName key, MesoValue value) implements MesoValue {

    public static final Env defaultScope = new Env(
            null, "T", MesoBool.T)
            .extend("F", MesoBool.F)
            .extend(".", NoApply.NO_APPLY);
    public Env(final Env outer, final String k, final MesoValue value) {
        this(outer, new MesoName(k), value);
    }

    public Env extend(final MesoValue key, final MesoValue value) {
        return new Env(this, ((MesoName) key), value);
    }

    public Env extend(final String key, final MesoValue value) {
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

    public MesoValue get(final MesoValue k) {
        if (k.equals(key)) {
            return value;
        }
        if (outer != null) {
            return outer.get(k);
        }
        throw new NoSuchElementException("Undefined name:\n" + k);
    }

    public MesoValue get(final String s) {
        return get(new MesoName(s));
    }

    public String toString() {
        String out = "(function, env: {";
        Env current = this;
        while (current.outer != null && !current.equals(defaultScope)) {
            out += current.key + " == " + current.value + "; ";
            current = current.outer;
        }
        return out + "})";
    }
}
