import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {
    public static void testSortLambda() {
        List<String> testStrings = Arrays.asList(
                new String[] { "abcde", "a", "abc", "ab", "abcd" });
        System.out.println("List:" + testStrings);
        testStrings.sort(
                // Sort by String length, descending
                (first, second) -> (second.length() - first.length()));
        System.out.println("Sorted list:" + testStrings);
    }

    public static void testMaxLambda() {
        Map<String, Integer> possible_matches = new HashMap<String, Integer>();
        possible_matches.put("ab", 2);
        possible_matches.put("abc", 42);
        possible_matches.put("abcde", 4);
        possible_matches.put("a", 102);
        String max = Collections.max(
                possible_matches.keySet(),
                (first, second) -> (first.length() - second.length()));
        System.out.println(max + ", " + possible_matches.get(max));
    }

    public static void main(String[] args) {
        testMaxLambda();

        MetasulfateSession interpreter = new MetasulfateSession();

        System.out.println(interpreter.lex(
                "1 + 1 == myVariable_thing ** 4 + 5 (6 - 2) {34 + x}"));
    }
}
