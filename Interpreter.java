import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Map;
import java.util.Set;

record Token (String type, Object value) {}

public class Interpreter {

    public static void run(String src)
    /**
     * Interprets raw Metasulfate source
     */
    {
        List<Token> tokens = lex(src);
        System.out.println(tokens);
    }

    public static void run_from_file(String path) throws FileNotFoundException
    {
        try {

            String src = "";
            Scanner s = new Scanner(new File(path));
            while (s.hasNextLine()) {
                src += s.nextLine();
            }
            s.close();

            run(src);

        } catch (FileNotFoundException e) {
            throw e;
        }
    }

    public static List<Token> lex(String src)
    {

        // We will return this thing's values:
        List<Token> tokens = new ArrayList<Token>();

        // For later:
        Map<String, Token> possible_matches = new HashMap<String, Token>();
        String builtString = "";
        boolean foundWhitespace;

        for (int i = 0; i < src.length(); i++) {
            foundWhitespace = whitespace.contains(src.charAt(i));

            if (foundWhitespace && possible_matches.size() > 0) {
                    // Get the String key with the longest length
                    String max = Collections.max(
                        possible_matches.keySet(),
                        (first, second) -> (first.length() - second.length())
                    );
                    tokens.add(possible_matches.get(max));
                }
            if (foundWhitespace || i + 1 == src.length()) {
                builtString = "";
                possible_matches.clear();
            }
            if (!foundWhitespace) {

                builtString += src.substring(i, i + 1);
                Token next = resolve(builtString);
                if (next != null) {
                    possible_matches.put(builtString, next);
                }
            }
        }

        return tokens;
    }

    final private static Set<Character> whitespace = new HashSet<Character>(
            Arrays.asList(new Character[] {' ', '\n', '\t'})
        );

    final private static Set<String> punctuation = new HashSet<String>(
        Arrays.asList(new String[] {
            "[", "]",
            "(", ")",
            "{", "}",
            ";"
        })
    );

    final private static Set<Character> opSymbols = new HashSet<Character>(
        Arrays.asList(new Character[] {
            '_', '.',
            '+', '-',
            '*', '/', '^',
            '=',
            '|', '&',
            '<', '>'
        })
    );

    private static Token resolveIdentifier(String s)
    /**
     * PRECONDITION: The caller must have already determined
     * that s does not map to a non-identifier Token
     */
    {
        // We refine these guesses as we move on:
        String type = "name";
        String value = s;

        // A normal identifier must start with a letter,
        // so we're looking at an operator identifier if
        // this doesn't
        if (!Character.isAlphabetic(s.charAt(0))) type = "op_name";

        // Now to check all the other characters
        int i = 1;
        while (i < s.length()) {
            char current = s.charAt(i);

            // Normal IDs can only contain alphanumerics
            // and underscores. Anything else makes an op id
            if (
                !(
                    Character.isAlphabetic(current)
                    || Character.isDigit(current)
                    || current == '_'
                )
            ) {
                type = "op_name";
            }
            // But if it isn't even one of the
            // allowed operator characters then
            // it's not a Token at all
            else if (
                !(
                    opSymbols.contains(current)
                )
            ) {
                return null;
            }
            i++;
        }
        return new Token(type, value);
    }

    private static Token resolve(String s)
    {
        if (punctuation.contains(s)) {
            return new Token("punc", s);
        }
        if (Character.isDigit(s.charAt(0))) {
            return new Token("int_lit", Integer.valueOf(s));
        }
        return resolveIdentifier(s);
    }
}
