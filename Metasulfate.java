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
                evalFile ("!standard_library.meso") + "\n---\n\n\n");
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
            src += s.nextLine();
        }
        s.close();
        return eval(src);
    }

    private static MesoValue parse(final List<String> src) {
        return new Parser(src).parse(Scope.defaultScope);
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
                if (Character.isWhitespace(current)) {
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

    public MesoValue parse(final Scope scope) {
        final MesoValue rVal = _parse(scope);
        if (hasNext()) {
            throw new IllegalArgumentException("Trailing data in program");
        }
        return rVal;
    }

    private MesoValue _parse(final Scope scope) {
        final String t = grab();
        try {
            return new MesoInt(Long.valueOf(t));
        } catch (final NumberFormatException e) {
        }
        if (MesoInt.opMap.containsKey(t)) {
            return MesoInt.op(MesoInt.opMap.get(t), _parse(scope), _parse(scope));
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
            default:
                MesoValue rVal = scope.get(t);
                MesoValue arg;
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
    public static final Map<String, LongBinaryOperator> opMap = Map.ofEntries(
            Map.entry("SUM", (x, y) -> (x + y)),
            Map.entry("DIFF", (x, y) -> (x - y)),
            Map.entry("PROD", (x, y) -> (x * y)),
            Map.entry("QUO", (x, y) -> (x / y)),
            Map.entry("POW", (x, y) -> ((long) Math.pow(x, y))));

    public static MesoInt op(final LongBinaryOperator op, final MesoValue x, final MesoValue y) {
        System.out.println("Performing int operation " + op + " on:\n" + x + "\nand:\n" + y);
        return new MesoInt(op.applyAsLong(((MesoInt) x).v, ((MesoInt) y).v));
    }

    public String toString() {
        return Long.toString(v) + " (int)";
    }
}

record MesoName(String n) implements MesoValue {
}

interface Closure extends MesoValue {
    public MesoValue apply(MesoValue arg);
}

record MesoClosure(List<String> def, Scope env) implements Closure {

    public MesoValue apply(final MesoValue arg) {
        return new Parser(def).parse(env.extend("DOT", arg));
    }

    public String toString() {
        return def.toString() + " " + env.toString();
    }
}

// UNTESTED
record JavaClosure(Function<MesoValue, MesoValue> func, Scope env) implements Closure {
    public JavaClosure(BiFunction<MesoValue, MesoValue, MesoValue> biFunc, Scope env) {
        this(
                v1 -> new JavaClosure(v2 -> biFunc.apply(v1, v2), env),
                env);
    }

    public MesoValue apply(final MesoValue arg) {
        return func.apply(arg);
    }
}

record Scope(Scope outer, MesoName key, MesoValue value) {

    public static final Scope defaultScope = new Scope(
            null, "T", MesoBool.T)
            .extend("F", MesoBool.F)
            .extend(".", NoApply.NO_APPLY);
    public Scope(final Scope outer, final String k, final MesoValue value) {
        this(outer, new MesoName(k), value);
    }

    public Scope extend(final MesoValue key, final MesoValue value) {
        return new Scope(this, ((MesoName) key), value);
    }

    public Scope extend(final String key, final MesoValue value) {
        return new Scope(this, new MesoName(key), value);
    }

    // UNTESTED
    public Scope extend(final Scope other) {
        Scope rVal = this;
        Scope current = other;
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
        Scope current = this;
        while (current.outer != null && !current.equals(defaultScope)) {
            out += current.key + " == " + current.value + "; ";
            current = current.outer;
        }
        return out + "})";
    }
}
