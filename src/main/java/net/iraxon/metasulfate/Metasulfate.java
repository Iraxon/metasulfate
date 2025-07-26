package net.iraxon.metasulfate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.nio.file.Paths;

/**
 * Copyright Iraxon 2025
 *
 * This file is free software under the GNU GPL v3 or later.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Consumer;

public class Metasulfate {

    public static final RewriteRules GLOBALS = RewriteRules.DEFAULT;

    public static void main(final String[] args) {
        System.out.println("\n\n\n---\n" +
                evalFile("src/main/resources/metasulfate/example.meso") + "\n---\n\n\n");
    }

    public static AST evalFile(final String path) {
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

    public static AST preEval(final String src) {
        return parse(lex(src));
    }

    public static AST eval(final String src) {
        return GLOBALS.rewrite(preEval(src));
    }

    private static List<String> lex(final String src) {
        return Lexer.lex(src);
    }

    private static AST parse(final List<String> src) {
        return new Parser(src).parse();
    }

    private static class Lexer {

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

    private static class Parser {
        private final List<String> src;
        private int cursor;

        public Parser(final List<String> src) {
            this.src = src;
            this.cursor = 0;
        }

        public AST parse() {
            final AST rVal = parseValue();
            if (hasNext()) {
                final ArrayList<String> trailing = new ArrayList<>();
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

        private AST parseValue() {
            String token = grab();
            // System.out.println("Parsing value: " + token);
            return switch (token) {
                case "[" -> parseList();
                case "]" -> ASTSingletons.END_OF_LIST;
                case "'" -> new NameNode(grab());
                default -> Atom.of(token);
            };
        }

        private AST parseList() {
            final ArrayList<AST> nodes = new ArrayList<>();
            // System.out.println("Parsing list");
            AST current;
            while (hasNext() && ((current = parseValue()) != ASTSingletons.END_OF_LIST)) {
                if (current != null) {
                    nodes.add(current);
                }
            }
            return switch (nodes.size()) {
                // case 1 -> new ValueNode(nodes.get(0));
                case 0 -> null;
                default -> new NestedNode(List.copyOf(nodes));
            };
        }

        private boolean hasNext() {
            return this.cursor < src.size();
        }

        private String grab() {
            final String t = src.get(cursor++);
            System.out.println("Grabbing token: " + t);
            return t;
        }

        @SuppressWarnings("unused")
        private String peek(int n) {
            try {
                return src.get(cursor + n);
            } catch (IndexOutOfBoundsException e) {
                return null;
            }
        }
    }
}
