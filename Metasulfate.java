import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;

public class Metasulfate {
    public static void main(String[] args) {
        System.out.println(
                eval("LET 'double LAMBDA_DOT [PRODUCT DOT 2]. double 2"));
    }
    public static MesoValue eval(String src) {
        return new Parser(lex(src)).parse(Scope.defaultScope);
    }
    // System.out.println("\n---\n" +
    // interpreter.evalFile("!standard_library.meso"));
    /*
     * TEST PROGRAMS:
     * LAMBDA_DOT [PRODUCT DOT 2] LET 'x 3 LET 'y 2 SUM x y
     * == 10
     *
     * "LAMBDA_DOT [LET 'x DOT LAMBDA_DOT [SUM x DOT] .] 1 .
     */

    // Lexer stuff:

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

    private static List<String> lex(String rawSrc) {
        final List<String> rVal = new ArrayList<>();
        lexRec(rVal, rawSrc, "", 0);
        System.out.println("Lexer returning:\n" + rVal);
        return rVal;
    }

    private static void lexRec(List<String> list, String src, String acc, int cursor) {
        Consumer<String> add = (s) -> {
            if (s.length() > 0) {
                list.add(s);
            }
        };
        char current;
        int len = src.length();
        for (cursor = 0; cursor < len; cursor++) {
            current = src.charAt(cursor);
            if (Character.isWhitespace(current)) {
                if (acc.length() > 0 && whitespaceCompatible.contains(acc.charAt(0))) {
                    acc += current;
                } else {
                    add.accept(acc);
                    acc = "";
                }
            } else if (punctuation.contains(current)) {
                add.accept(acc);
                add.accept("" + current);
                acc = "";
            } else {
                if (cursor + 1 >= src.length()) {
                    add.accept(acc + current);
                }
                acc += current;
            }
        }
    }

    private static MesoValue parse(List<String> src) {
        Parser parser = new Parser(src);
        return parser.parse(Scope.defaultScope);
    }
}

class Parser {
    private List<String> src;
    private int cursor;

    public Parser(List<String> src) {
        this.src = src;
        this.cursor = 0;
    }

    public MesoValue parse(Scope scope) {
        MesoValue rVal = _parse(scope);
        if (hasNext()) {
            throw new IllegalArgumentException("Trailing data in program");
        }
        return rVal;
    }

    private MesoValue _parse(Scope scope) {
        String t = grab();
        switch (t) {
            case "LAMBDA_DOT":
                return new MesoClosure(grabDelimitedRange(t, "]"), scope);
            case "LET":
                Scope letScope = scope.extend(_parse(scope), _parse(scope));
                return _parse(letScope);
            case "\'":
                return new MesoName(grab());
            default:
                return scope.get(t);
        }
    }

    private boolean hasNext() {
        return this.cursor < src.size();
    }

    private String grab() {
        String t = src.get(cursor++);
        System.out.println("Grabbing token: " + t);
        return t;
    }

    private List<String> grabN(int n) {
        List<String> rval = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            rval.add(grab());
        }
        return rval;
    }

    /*
     * Continuously grabs input items until reaching a terminator;
     * the terminator is consumed but not returned;
     * if the initiator is not null, then
     * count will be kept to allow for nesting
     */
    private List<String> grabDelimitedRange(String initiator, String terminator) {
        int nestingLevel = 1; // There was a starting opening brace
        List<String> rval = new ArrayList<>();
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

enum Quote implements MesoValue {
    QUOTE
}

record MesoName(String n) implements MesoValue {}

record MesoClosure(List<String> def, Scope env) implements MesoValue {

    public MesoValue apply(MesoValue arg) {
        return new Parser(def).parse(env);
    }

    public String toString() {
        return def.toString() + " " + env.toString();
    }
}

record Scope(Scope outer, MesoName key, MesoValue value) {

    public static final Scope defaultScope = new Scope(
            null, "T", MesoBool.T)
            .extend("F", MesoBool.F)
            .extend(".", NoApply.NO_APPLY);
    public Scope(Scope outer, String k, MesoValue value) {
        this(outer, new MesoName(k), value);
    }

    public Scope extend(MesoValue key, MesoValue value) {
        return new Scope(this, ((MesoName)key), value);
    }

    public Scope extend(String key, MesoValue value) {
        return new Scope(this, new MesoName(key), value);
    }

    public MesoValue get(MesoValue k) {
        if (k.equals(key)) {
            return value;
        }
        if (outer != null) {
            return outer.get(k);
        }
        throw new NoSuchElementException("Undefined name:\n" + k);
    }

    public MesoValue get (String s) {
        return get(new MesoName(s));
    }

    public String toString() {
        String out = "(Env: {";
        Scope current = this;
        while (current.outer != null && !current.equals(defaultScope)) {
            out += current.key + " == " + current.value + "; ";
            current = current.outer;
        }
        return out + "})";
    }
}
