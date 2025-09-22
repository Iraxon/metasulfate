package net.iraxon.metasulfate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UncheckedIOException;

/**
 * Copyright Iraxon 2025
 *
 * This file is free software under the GNU GPL v3 or later.
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import net.iraxon.metasulfate.Metasulfate.Term.Nested.RewriteOrder;
import net.iraxon.metasulfate.Metasulfate.Term.Nested.Simple;

public class Metasulfate {

    private static final Function<Term, Term> GLOBAL = Function.identity();

    public static void main(final String[] args) {
        System.out.println("\n\n\n---\n" +
                evalFile("src/main/resources/metasulfate/!standard_library.meso") + "\n---\n\n\n");
    }

    public static Term evalFile(final String path) {
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

    public static Term parseLex(final String src) {
        return parse(lex(src));
    }

    public static Term eval(final String src) {
        final var parsedLexed = parseLex(src);
        System.out.println(parsedLexed);
        final var rVal = parsedLexed.rewrite(GLOBAL);
        System.out.println(rVal);
        return rVal;
    }

    private static List<String> lex(final String src) {
        return Lexer.lex(src);
    }

    private static Term parse(final List<String> src) {
        return new Parser(src).parse();
    }

    private static class Lexer {

        public static List<String> lex(final String rawSrc) {
            final List<String> rVal = new ArrayList<>();
            _lex(rVal, rawSrc);
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
            String accumulatedToken = "";
            int cursor = 0;

            int commentNestingDepth = 0; // Increments on left paren; decreases on right paren

            char current;
            final int len = src.length();
            for (cursor = 0; cursor < len; cursor++) {
                current = src.charAt(cursor);
                if (current == '(') {
                    add.accept(accumulatedToken);
                    accumulatedToken = "";
                    commentNestingDepth++;
                }
                if (commentNestingDepth == 0) {
                    if (Character.isWhitespace((int) current)) {
                        if (accumulatedToken.length() > 0 && whitespaceCompatible.contains(accumulatedToken.charAt(0))) {
                            accumulatedToken += current;
                        } else {
                            add.accept(accumulatedToken);
                            accumulatedToken = "";
                        }
                    } else if (punctuation.contains(current)) {
                        add.accept(accumulatedToken);
                        accumulatedToken = "";
                        add.accept("" + current);
                    } else {
                        if (cursor + 1 >= src.length()) {
                            add.accept(accumulatedToken + current);
                        }
                        accumulatedToken += current;
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

    public static class Lazy<T> implements Supplier<T> {
        private final Supplier<T> supplier;
        private boolean cacheFilled = false;
        private T cache = null;

        public static <R> Lazy<R> of(Supplier<R> supplier) {
            return new Lazy<>(supplier);
        }

        public Lazy(Supplier<T> supplier) {
            this.supplier = supplier;
        }

        @Override
        public T get() {
            if (!cacheFilled) {
                cache = supplier.get();
                cacheFilled = true;
            }
            return cache;
        }
    }

    public static sealed interface Term permits Term.Atomic, Term.Nested, Term.Singleton {

        static abstract class Statics {
            private static final boolean debug = true;
            private static final Map<Term, Map<Function<Term, Term>, Term>> cache = new ConcurrentHashMap<>();
        }

        private static Term rewrite(final Term term, final Function<Term, Term> rule) {
            final Term thingToPrint = term;

            Statics.cache.putIfAbsent(term, new ConcurrentHashMap<Function<Term, Term>, Term>());
            final var rVal = Statics.cache.get(term).containsKey(rule) ? Statics.cache.get(term).get(rule) : switch (term) {
                case final Nested n -> switch (n) {
                    case final RewriteOrder r -> r.subject().rewrite(rule.compose(
                            termFromSubject -> {
                                final var match = r.pattern().match(termFromSubject);
                                if (match == null) {
                                    return termFromSubject;
                                }
                                return r.expr().rewrite(
                                        termFromExpr -> match == null ? termFromExpr
                                                : match.getOrDefault(termFromExpr, termFromExpr));
                            }));
                    case final Nested nested -> untilSame(
                            Nested.of(nested.children().stream().map(child -> child.rewrite(rule)).toList()),
                            rule);
                };
                case final Atomic a -> untilSame(a, rule);
                case final Singleton s -> s.err();
            };
            if (Statics.debug) {
                System.out.println(thingToPrint.toString() + " ::- " + rVal);
            }
            Statics.cache.get(term).putIfAbsent(rule, rVal);
            return rVal;
        }

        /**
         * Rewrites all subterms (including possibly the term itself)
         * using the given rewrite rule
         *
         * @param rule A rewrite rule, which defaults to returning the input unchanged
         *             if there is no match
         * @return The rewritten term
         */
        public default Term rewrite(final Function<Term, Term> rule) {
            return Term.rewrite(this, rule);
        }

        public static Map<Term, Term> match(final Term pattern, final Term other) {
            return switch (pattern) {
                case final Atomic a -> a.name().length() >= 1 && a.name().charAt(0) == '\''
                        ? Map.of(Atomic.of(a.name().substring(1)), other)
                        : (a.equals(other) ? Map.of() : null);
                case final Nested n -> switch (n) {
                    case final RewriteOrder r -> null;
                    case final Nested nested -> matchNested(nested, other);
                };
                case final Singleton s -> Map.of(s.err(), s);
            };
        }

        private static Map<Term, Term> matchNested(final Nested self, final Term other) {
            final int size = self.children().size();
            if (other instanceof final Nested nestedOther && nestedOther.children().size() == size) {
                return IntStream.range(0, size).mapToObj(
                        i -> self.children().get(i).match(nestedOther.children().get(i)))
                        .reduce(Map.of(), Simple::merge);
            }
            return null;
        }

        /**
         * Treating this term as a pattern, returns the variable bindings
         * made by pattern matching
         *
         * @param other Another Term
         * @return A Map or null if there is no match
         */
        public default Map<Term, Term> match(final Term other) {
            return Term.match(this, other);
        }

        private static <R> R untilSame(final R input, final Function<R, R> function) {
            R previous;
            R next = input;
            do {
                previous = next;
                next = function.apply(previous);
            } while (!previous.equals(next));
            return next;
        }

        private static String renderList(final List<?> list, final boolean curlyBrackets) {
            final char left = curlyBrackets ? '{' : '[';
            final char right = curlyBrackets ? '}' : ']';
            return left + list.stream().map(t -> t.toString()).reduce("",
                    (s1, s2) -> (s1.equals("") ? "" : s1 + " ") + s2) + right;
        }

        public static non-sealed interface Atomic extends Term {
            static ConcurrentHashMap<String, Name> cache = new ConcurrentHashMap<>();

            public static Name of(final String name) {
                return cache.computeIfAbsent(name, Name::new);
            }

            public String name();

            public static record Name(String name) implements Atomic {
                public Name {
                    Objects.requireNonNull(name);
                }

                @Override
                public final String toString() {
                    return name;
                }
            }
        }

        public static sealed interface Nested extends Term permits Nested.Simple, Nested.RewriteOrder {

            public static Term of(final List<Term> children) {
                return children.size() > 1
                        ? children.size() == 4 && children.get(RewriteOrder.MARKER_INDEX).equals(RewriteOrder.MARKER)
                                ? new RewriteOrder(children.get(3), children.get(0), children.get(2))
                                : new Simple(children)
                        : children.get(0);
            }

            public List<Term> children();

            public static record Simple(List<Term> children) implements Nested {
                public Simple {
                    Objects.requireNonNull(children);
                }

                private static Map<Term, Term> merge(final Map<Term, Term> first, final Map<Term, Term> second) {
                    if (first == null || second == null) {
                        return null;
                    }
                    if (second.keySet().stream()
                            .anyMatch(x -> first.containsKey(x) && !second.get(x).equals(first.get(x)))) {
                        return null;
                    }
                    final HashMap<Term, Term> rVal = new HashMap<>(first);
                    rVal.putAll(second);
                    return Map.copyOf(rVal);
                }

                @Override
                public String toString() {
                    return renderList(children, false);
                }
            }

            public static record RewriteOrder(Term subject, Term pattern, Term expr) implements Nested {
                public static final Atomic.Name MARKER = Atomic.of("->");
                public static final int MARKER_INDEX = 1;

                public RewriteOrder {
                    Objects.requireNonNull(subject);
                    Objects.requireNonNull(pattern);
                    Objects.requireNonNull(expr);
                }

                @Override
                public List<Term> children() {
                    return List.of(MARKER, subject, pattern, expr);
                }

                @Override
                public final String toString() {
                    return "[ (REWRITE) " + pattern.toString() + " -> " + expr.toString() + " " + subject.toString()
                            + "]";
                }

            }
        }

        public static enum Singleton implements Term {
            END_OF_LIST,
            END_OF_SEQUENCE_PATTERN;

            public Singleton err() {
                throw new UnsupportedOperationException("Parser-exclusive pseudoterm " + this + " in final output.");
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

        public Term parse() {
            final Term rVal = parseList();
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

        private Term parseValue() {
            return switch (grab()) {
                case "[" -> parseList();
                case "]" -> Term.Singleton.END_OF_LIST;
                case "'" -> Term.Atomic.of('\'' + grab());
                case final String s -> Term.Atomic.of(s);
            };
        }

        private Term parseList() {
            final ArrayList<Term> nodes = new ArrayList<>();
            // System.out.println("Parsing list");
            Term current;
            while (hasNext() && ((current = parseValue()) != Term.Singleton.END_OF_LIST)) {
                if (current != null) {
                    nodes.add(current);
                }
            }
            return switch (nodes.size()) {
                // case 1 -> new ValueNode(nodes.get(0));
                case 0 -> null;
                default -> Term.Nested.of(List.copyOf(nodes));
            };
        }

        private boolean hasNext() {
            return this.cursor < src.size();
        }

        private String grab() {
            final String t = src.get(cursor++);
            // System.out.println("Grabbing token: " + t);
            return t;
        }

        @SuppressWarnings("unused")
        private String peek(final int n) {
            try {
                return src.get(cursor + n);
            } catch (final IndexOutOfBoundsException e) {
                return null;
            }
        }
    }
}
