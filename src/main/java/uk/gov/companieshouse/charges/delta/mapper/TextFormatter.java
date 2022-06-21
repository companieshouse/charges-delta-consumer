package uk.gov.companieshouse.charges.delta.mapper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.springframework.stereotype.Component;

@Component
public class TextFormatter {

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList("A", "AN", "AT",
            "AS", "AND", "ARE", "BUT", "BY", "ERE", "FOR", "FROM", "IN", "INTO", "IS", "OF", "ON",
            "ONTO", "OR", "OVER", "PER", "THE", "TO", "THAT", "THAN", "UNTIL", "UNTO", "UPON",
            "VIA", "WITH", "WHILE", "WHILST", "WITHIN", "WITHOUT"));

    private static final Set<String> ENTITIES = new HashSet<>(Arrays.asList("ARD", "NI", "SE",
            "GB", "SC", "UK", "LTD", "L.T.D", "PLC", "P.L.C", "UNLTD", "CIC", "C.I.C", "LLP",
            "L.P", "LP", "EEIG", "OEIC", "ICVC", "AEIE", "C.B.C", "C.C.C", "CBC", "CBCN", "CBP",
            "CCC", "CYF", "EESV", "EOFG", "EOOS", "GEIE", "GELE", "PAC", "PCCLIMITED", "PCCLTD",
            "PROTECTEDCELLCOMPANY", "CWMNICELLGWARCHODEDIG", "CCGCYFYNGEDIG", "CCGCYF"));

    private static final Pattern STEM_PATTERN = Pattern.compile(
            "(\\p{L}[\\p{L}']*)", Pattern.CASE_INSENSITIVE);
    private static final Pattern FIRST_WORD = Pattern.compile("^(\\p{L}[\\p{L}']*)");
    private static final Pattern MATCHES_ENTITY = Pattern.compile(
            "(\\b(?i:" + String.join("|", ENTITIES) + ")\\b)");
    private static final Pattern LAST_WORD = Pattern.compile("(\\p{L}[\\p{L}']*)$");
    private static final Pattern OPENING_PARENTHESIS = Pattern.compile("[(](\\p{L}[\\p{L}']*)");
    private static final Pattern CLOSING_PARENTHESIS = Pattern.compile("(\\p{L}[\\p{L}']*)[)]");
    private static final Pattern COLON = Pattern.compile("([:;]\\s+)(\\p{L}[\\p{L}']*)");
    private static final Pattern NEWLINE = Pattern.compile("\\n");
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s+");
    private static final Pattern ABBREVIATIONS = Pattern.compile("\\b(\\p{L})[.]");
    private static final Pattern MIXED_ALPHANUMERIC = Pattern.compile("(\\w+\\d+\\w*|\\d+\\w+)");
    private static final Pattern TOKENISATION_PATTERN = Pattern.compile("(\\S+(\\s+|$))");
    private static final Pattern POSSESSIVE_PATTERN = Pattern.compile(
            "^([^a-z]*)I[^a-z]",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern POSSESSIVE_END_OF_SENTENCE_PATTERN = Pattern.compile(
            "([.]|[!?]+)[^.!?a-z]*$"
    );
    private static final Pattern FORWARDSLASH_ABBREVIATION = Pattern.compile("^(.?/)(.*)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern FIRST_LETTER = Pattern.compile("([a-z])",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern GENERAL_ABBREVIATION = Pattern.compile(
            String.join("|",
                    "^(etc[.]|pp[.]|ph[.]?d[.]|",
                    "(?:[A-Z][.])(?:[A-Z][.])+|",
                    "(^[^a-zA-Z]*([a-z][.])+))[^a-z]*\\s"),
            Pattern.CASE_INSENSITIVE);
    private static final Pattern END_OF_SENTENCE_PATTERN = Pattern.compile(
            "[a-z][^a-z.!?]*[.!?]+([^a-z\\s.!?]*)\\s",
            Pattern.CASE_INSENSITIVE);

    /**
     * Format a given string as an entity name in accordance to the following rules
     * ordered by precedence:
     * <br>
     * <ul>
     * <li>format(a) == format(b) where a is case-insensitively equal to b</li>
     * <li>If the provided string is null or empty, return the provided value as given.</li>
     * <li>Any words containing both letters and numbers must be uppercase.</li>
     * <li>Any entity names or country codes must be uppercase.</li>
     * <li>Any words within parentheses must use highlight casing.</li>
     * <li>Any word proceeding a colon must use highlight casing.</li>
     * <li>First and last words must use highlight casing.</li>
     * <li>Any stop words must be lowercase.</li>
     * <li>All other words must use highlight casing.</li>
     * </ul>
     *
     * @param text The text that will be recased.
     * @return Text recased in accordance to the above rules.
     */
    String formatAsEntityName(String text) {
        if (StringUtils.isEmpty(text)) {
            return text;
        }
        text = text.toUpperCase(Locale.UK);
        text = mapToken(STEM_PATTERN, text, (token, matcher) ->
                STOP_WORDS.contains(token) ? token.toLowerCase(Locale.UK) :
                        WordUtils.capitalizeFully(token), true);
        text = mapToken(FIRST_WORD, text, (token, matcher) ->
                        WordUtils.capitalizeFully(token), false);
        text = mapToken(LAST_WORD, text, (token, matcher) ->
                WordUtils.capitalizeFully(token), false);
        text = mapToken(OPENING_PARENTHESIS, text, (token, matcher) ->
                        "(" + WordUtils.capitalizeFully(matcher.group(1)), false);
        text = mapToken(CLOSING_PARENTHESIS, text, (token, matcher) ->
                        WordUtils.capitalizeFully(matcher.group(1)) + ")", false);
        text = mapToken(COLON, text, (token, matcher) ->
                        matcher.group(1) + WordUtils.capitalizeFully(matcher.group(2)), false);
        text = mapToken(NEWLINE, text, (token, matcher) -> " ", true);
        text = mapToken(ABBREVIATIONS, text, (token, matcher) ->
                        matcher.group(1).toUpperCase(Locale.UK) + ".", true);
        text = mapToken(MULTIPLE_SPACES, text, (token, matcher) -> " ", true);
        text = mapToken(MIXED_ALPHANUMERIC, text, (token, matcher) ->
                        matcher.group(1).toUpperCase(Locale.UK), true);
        text = mapToken(MATCHES_ENTITY, text, (token, matcher) ->
                        ENTITIES.contains(token.toUpperCase(Locale.UK))
                                ? token.toUpperCase(Locale.UK)
                                : token, true);
        return text.trim();
    }

    /**
     * Format a given string as a sentence in accordance to the following rules
     * ordered by precedence:
     * <br>
     * <ul>
     * <li>format(a) == format(b) where a is case-insensitively equal to b</li>
     * <li>If the provided string is null or empty, return the provided value as given.</li>
     * <li>Any words containing both letters and numbers must be uppercase.</li>
     * <li>Any entity names or country codes must be uppercase.</li>
     * <li>The first word in a sentence will be converted to title casing.</li>
     * <li>A string beginning with a forwardslash abbreviation will be cased a/cat => A/Cat.</li>
     * <li>All single i's will be converted to uppercase.</li>
     * <li>Words following general abbreviations (e.g. etc.) will be lowercase.</li>
     * <li>Any word following an unpaired bracket will be lowercase.</li>
     * <li>All newlines will be converted into an empty space.</li>
     * <li>Any title abbreviation will be uppercase.</li>
     * </ul>
     *
     * @param text The text that will be recased.
     * @return Text recased in accordance to the above rules.
     */
    String formatAsSentence(String text) {
        if (StringUtils.isEmpty(text)) {
            return text;
        }
        text = text.toUpperCase(Locale.UK);
        Matcher forwardslashAbbreviationMatcher = FORWARDSLASH_ABBREVIATION.matcher(text);
        String start = "";
        if (forwardslashAbbreviationMatcher.find()) {
            start = forwardslashAbbreviationMatcher.group(1);
            text = forwardslashAbbreviationMatcher.group(2);
        }
        SentenceState sentenceState = new SentenceState();
        text = mapToken(TOKENISATION_PATTERN, text,
                (token, matcher) ->
                        TextFormatter.mapWord(token, sentenceState), true);
        if (!start.isEmpty()) {
            text = start + text;
        }
        text = mapToken(NEWLINE, text, (token, matcher) -> " ", true);
        text = mapToken(ABBREVIATIONS, text, (token, matcher) ->
                        matcher.group(1).toUpperCase(Locale.UK) + ".", true);
        text = mapToken(MULTIPLE_SPACES, text, (token, matcher) -> " ", true);
        text = mapToken(MIXED_ALPHANUMERIC, text, (token, matcher) ->
                        matcher.group(1).toUpperCase(Locale.UK), true);
        text = mapToken(MATCHES_ENTITY, text, (token, matcher) ->
                        ENTITIES.contains(token.toUpperCase(Locale.UK))
                                ? token.toUpperCase(Locale.UK)
                                : token, true);
        return text.trim();
    }

    /**
     * Format a given string as a particulars string in accordance to the following rules
     * ordered by precedence:
     * <br>
     * <ul>
     * <li>format(a) == format(b) where a is case-insensitively equal to b</li>
     * <li>The final character will be exactly one full stop.</li>
     * <li>The string will be formatted
     * {@link TextFormatter#formatAsSentence(String) as a sentence}.</li>
     * <li>Escaped newlines will be replaced with full stops.</li>
     * <li>An escaped newline will be removed from the start of the string if present.</li>
     * <li>Multiple escaped newlines will be replaced with a single escaped newline.</li>
     * <li>Spaces before newlines will be removed.</li>
     * <li>Multiple spaces will be replaced with a single space.</li>
     * </ul>
     *
     * @param text The text that will be recased.
     * @return Text recased in accordance to the above rules.
     */
    String formatAsParticulars(String text) {
        if (StringUtils.isEmpty(text)) {
            return text;
        }
        text = text.replaceAll("\\h+", " ")
                .replaceAll("\\h\\\\n", "\\\\n")
                .replaceAll("(?:\\\\n)+", "\\\\n")
                .replaceAll("\\A\\\\n", "")
                .replaceAll("\\\\n+", ". ");
        text = formatAsSentence(text);
        if (!text.endsWith(".")) {
            text += ".";
        }
        return text;
    }

    private static String mapWord(String token, SentenceState sentenceState) {
        Matcher possessivePatternMatcher = POSSESSIVE_PATTERN.matcher(token);
        Matcher possessiveEndOfSentenceMatcher = POSSESSIVE_END_OF_SENTENCE_PATTERN.matcher(token);
        if (possessivePatternMatcher.find()) {
            sentenceState.setEndOfSentence(possessiveEndOfSentenceMatcher.find());
            sentenceState.setMatchingBracket(
                    possessivePatternMatcher.group(1).matches("^[\\[(].*$"));
            return token.toUpperCase(Locale.UK);
        }
        token = token.toLowerCase(Locale.UK);
        if (sentenceState.isEndOfSentence()) {
            token = mapToken(FIRST_LETTER,
                    token, (t, m) -> t.toUpperCase(Locale.UK), false);
            sentenceState.setMatchingBracket(token.matches("^[\\[(].*$"));
        }
        Matcher generalAbbreviationMatcher = GENERAL_ABBREVIATION.matcher(token);
        Matcher endOfSentenceMatcher = END_OF_SENTENCE_PATTERN.matcher(token);
        sentenceState.setEndOfSentence(!generalAbbreviationMatcher.find()
                && endOfSentenceMatcher.find()
                && !(endOfSentenceMatcher.group(1).matches("[])].*$")
                && !sentenceState.isMatchingBracket()));
        return token;
    }

    private static String mapToken(Pattern pattern,
                                   String token,
                                   BiFunction<String, Matcher, String> matchRemappingFunction,
                                   boolean global) {
        Matcher matcher = pattern.matcher(token);
        StringBuilder result = new StringBuilder();
        int start;
        int end;
        int prevEnd = 0;
        while (matcher.find()) {
            start = matcher.start();
            end = matcher.end();
            if (start > 0) {
                result.append(token.substring(prevEnd, start));
            }
            result.append(matchRemappingFunction.apply(
                    token.substring(start, end), matcher));
            prevEnd = end;
            if (!global) {
                break;
            }
        }
        result.append(token.substring(prevEnd));
        return result.toString();
    }

    private static class SentenceState {
        private boolean endOfSentence = true;
        private boolean matchingBracket = false;

        private boolean isEndOfSentence() {
            return endOfSentence;
        }

        private void setEndOfSentence(boolean endOfSentence) {
            this.endOfSentence = endOfSentence;
        }

        private boolean isMatchingBracket() {
            return matchingBracket;
        }

        private void setMatchingBracket(boolean matchingBracket) {
            this.matchingBracket = matchingBracket;
        }
    }
}
