import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Interpreter {

    public static List<Token> lex(String src)
    {
        final Set<Character> whitespace = new HashSet<Character>(
            Arrays.asList(new Character[] {' ', '\n', '\t'})
        );

        // We will return this thing's values:
        List<Token> tokens = new ArrayList<Token>();

        // For later:
        Map<String, Token> possible_matches = new HashMap<String, Token>();
        String builtString = "";

        for (int i = 0; i < src.length(); i++) {

            if (whitespace.contains(src.charAt(i)) || i + 1 == src.length()) {

                if (possible_matches.size() > 0) {
                    // Get the String key with the longest length
                    String max = Collections.max(
                        possible_matches.keySet(),
                        (first, second) -> (first.length() - second.length())
                    );
                    tokens.add(possible_matches.get(max));
                }
                builtString = "";
                possible_matches.clear();

            } else {

                builtString += src.substring(i, i + 1);
                Token next = Token.resolve(builtString);
                if (next != null) {
                    possible_matches.put(builtString, next);
                }
            }
        }

        return tokens;
    }
    public static void run(String src)
    /**
     * Interprets raw Metasulfate source
     */
    {
        ;
    }

    public static void run_from_file(String path)
    {
        ;
    }
}
