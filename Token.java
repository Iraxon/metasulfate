import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public record Token (
    String type,
    Object value
) {

    final private static Set<String> punctuation = new HashSet<String>(
        Arrays.asList(new String[] {
            "[", "]",
            "(", ")",
            "{", "}"
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

    public static Token resolve(String s)
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
