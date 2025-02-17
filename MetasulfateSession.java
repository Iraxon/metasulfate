import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

record Val(String type, Object value) {
    public Val(long x) {
        this("int", x);
    }

    public Val(String s) {
        this("str", s);
    }

    public Val(List<Val> l) {
        this("list", l);
    }

    public String toString() {
        return this.type.toString() + " " + this.value.toString();
    }
}

public class MetasulfateSession {

    private final boolean verbose;
    private final Scope globalScope;

    public MetasulfateSession(boolean verbose) {
        this.verbose = verbose;
        this.globalScope = new Scope();
    }

    public MetasulfateSession() {
        this(false);
    }

    public Val eval(String src)
    /**
     * Interprets raw Metasulfate source
     */
    {
        if (verbose) {
            System.out.println("Source:\n" + src);
        }
        List<Token> tokens = lex(src);
        if (verbose) {
            System.out.println("Parsed tokens:\n" + tokens);
        }
        return evalTokens(tokens);
    }

    public Val evalFile(String path) throws FileNotFoundException {
        try {

            String src = "";
            Scanner s = new Scanner(new File(path));
            while (s.hasNextLine()) {
                src += s.nextLine();
            }
            s.close();

            return eval(src);

        } catch (FileNotFoundException e) {
            throw e; // To be replaced with handling logic (maybe)
        }
    }

    private Val evalTokens(List<Token> src) {
        return evalTokens(src, globalScope);
    }

    private Val evalTokens(List<Token> src, Scope scope) {
        return evalNTokens(src, scope, 0, 1).expr().get(0);
    }

    private EvalTokensOutput evalNTokens(List<Token> src, Scope scope, int arg_cursor, int n)
    /*
     * Returns a list of N
     */
    {
        if (src.size() == 0) {
            throw new IllegalArgumentException("Token list is empty");
        }
        int cursor = arg_cursor;
        List<Val> solution = new ArrayList<Val>();
        Val preliminarySolution = null;
        Token t;
        Token lookahead;
        EvalTokensOutput intermediary_output;
        try {
            while (solution.size() < n) {
                t = src.get(cursor);

                switch (t.type()) {

                    case "id":

                        switch ((String) t.value()) {
                            /*
                             * case "ASSIGN":
                             * intermediary_output = evalNTokens(src, scope, cursor + 1, 2);
                             * cursor = intermediary_output.cursor();
                             * scope.put(
                             * intermediary_output.expr().get(0),
                             * intermediary_output.expr().get(1));
                             * break;
                             */
                            case "'":
                                if (cursor + 1 >= src.size()) {
                                    throw new IllegalArgumentException(
                                            "Unexpected end of input after quote operator (')");
                                }
                                lookahead = src.get(cursor + 1);
                                preliminarySolution = new Val("name", lookahead);
                                System.out.println("PS=" + preliminarySolution);
                                break;
                            case "LAMBDA":
                                intermediary_output = evalNTokens(src, scope, ++cursor, 2);
                                cursor = intermediary_output.cursor();

                                Scope newScope = new Scope(scope);

                                preliminarySolution = (new Val(
                                        "func",
                                        new Closure(
                                                intermediary_output.expr().get(0),
                                                intermediary_output.expr().get(1),
                                                newScope)));
                                break;
                            case "LET":
                                intermediary_output = evalNTokens(src, scope, cursor, 1);
                            case "SUM":
                                intermediary_output = evalNTokens(src, scope, cursor + 1, 2);
                                cursor = intermediary_output.cursor();
                                preliminarySolution = (new Val(
                                        (long) intermediary_output.expr().get(0).value()
                                                + (long) intermediary_output.expr().get(1).value()));
                                break;
                            default:
                                // This case intentionally does not raise an error
                                break;
                        }
                        break;
                    case "lit":
                        preliminarySolution = (new Val(
                                "lit",
                                t.value()));
                        break;
                    case "punc":
                        switch ((String) t.value()) {
                            case "[":
                                break;
                            default:
                                throw new IllegalArgumentException("Unknown punctuation token:\n" + t);
                        }
                        break;
                    default:
                        throw new IllegalArgumentException("Token has unknown type:\n" + t);
                }
                Val scopeRead = scope.get(preliminarySolution);
                if (scopeRead != null) {
                    preliminarySolution = scopeRead;
                }
                if (preliminarySolution == null) {
                    throw new IllegalArgumentException(
                            "Undefined token (null parse):\n" + t + "\nwith expr stack:\n" + solution);
                }
                solution.add(preliminarySolution);
                cursor++;
            }
        } catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Parser compelled to check out of bounds:\n" + e);
        }
        if (verbose) {
            System.out.println("Read expr(s) " + solution);
        }
        return new EvalTokensOutput(solution, cursor);
    }

    private List<Token> lex(String src) {
        List<Token> tokens = new ArrayList<Token>();

        Map<String, Token> possibleMatches = new HashMap<String, Token>();
        String builtString;
        Token next;
        String max;

        int i = 0;
        int j;
        while (i < src.length()) {

            // System.out.println("Running outer loop on " + src.charAt(i));

            // Assemble possible tokens that begin with src.charAt(i):
            builtString = "";
            possibleMatches.clear();
            j = i;
            while (j < src.length() && !whitespace.contains(src.charAt(j))) {
                // System.out.println("Inner loop on " + src.charAt(j));
                builtString += src.charAt(j);
                next = resolve(builtString);
                if (next != null)
                    possibleMatches.put(builtString, next);
                j++;
            }

            max = null;
            if (possibleMatches.size() > 0) {
                // Add the longest possible token to the output list:
                max = Collections.max(
                        possibleMatches.keySet(),
                        (first, second) -> (first.length() - second.length()));
                tokens.add(possibleMatches.get(max));
                i += max.length();
            } else {
                i++;
            }
        }
        return tokens;
    }

    private final static Set<Character> whitespace = new HashSet<Character>(
            Arrays.asList(new Character[] { ' ', '\n', '\t' }));

    private final static Set<String> punctuation = new HashSet<String>(
            Arrays.asList(new String[] {
                    "[", "]",
                    "(", ")",
                    "{", "}",
                    ";"
            }));

    private final static Set<Character> opSymbols = new HashSet<Character>(
            Arrays.asList(new Character[] {
                    '_', '.',
                    '+', '-',
                    '*', '/', '^',
                    '=',
                    '|', '&',
                    '<', '>',
                    '\''
            }));

    private static Token resolve(String s) {
        if (punctuation.contains(s)) {
            return new Token("punc", s);
        }
        try {
            return new Token("lit", Long.valueOf(s));
        } catch (NumberFormatException e) {
        }
        if (s.equals("\'")) {
            return new Token("id", "\'");
        }
        {
            boolean isIdentifier = true;

            final char first = s.charAt(0);

            if (!Character.isLetter(first)) {
                isIdentifier = false;
            }

            int i = 1;
            char c;
            while (i < s.length() && isIdentifier) {
                // System.out.println("Running resolve loop on '" + s + "'");
                c = s.charAt(i);
                if (!(Character.isLetterOrDigit(c)
                        || opSymbols.contains(c))) {
                    isIdentifier = false;
                }
                i++;
            }
            if (isIdentifier) {
                return new Token("id", s);
            }
        }
        return null;
    }

    private static BuildListOutput buildList(List<Token> src, int arg_cursor, Token terminator) {
        List<Token> rval = new ArrayList<Token>();
        int cursor = arg_cursor;
        Token current;
        while (cursor < src.size()
                && !((current = src.get(cursor)).equals(terminator))) {
            rval.add(current);
            cursor++;
        }
        return new BuildListOutput(rval, cursor);
    }
}

record Token(String type, Object value) {
}

record EvalTokensOutput(List<Val> expr, int cursor) {
}

record BuildListOutput(List<Token> tokens, int cursor) {
}

record Closure(Val argName, Val def, Scope env) {

    @SuppressWarnings("unchecked")
    public List<Token> getDef() {
        if (def.value() instanceof List) {
            List<?> list = (List<?>) def.value();
            if (list.stream().allMatch(v -> v instanceof Token)) {
                return (List<Token>) (list);
            }
        }
        throw new IllegalArgumentException("Function definition Value does not hold List<Token>:\n" + def);
    }
}

class Scope {
    private final Map<Val, Val> map;
    private final Scope outer;

    public Scope(Scope outer) {
        this.map = new HashMap<Val, Val>();
        this.outer = outer;
    }

    public Scope() {
        this(null);
    }

    public void put(Val k, Val v) {
        map.put(k, v);
    }

    public Val get(Val item) {
        Val rVal = this.map.get(item);
        if (rVal == null) {
            if (this.outer == null) {
                return null;
            }
            return this.outer.get(item);
        }
        return rVal;
    }
}
