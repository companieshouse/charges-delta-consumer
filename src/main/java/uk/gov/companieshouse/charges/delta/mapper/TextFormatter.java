package uk.gov.companieshouse.charges.delta.mapper;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringTokenizer;
import org.apache.commons.text.WordUtils;
import org.apache.commons.text.translate.AggregateTranslator;
import org.apache.commons.text.translate.CharSequenceTranslator;
import org.apache.commons.text.translate.EntityArrays;
import org.apache.commons.text.translate.LookupTranslator;

final class TextFormatter {

    private static final Pattern STEM_PATTERN =
            Pattern.compile("(\\()(\\p{Alnum}+)");
    private static final Pattern COLON_PATTERN =
            Pattern.compile("[;:]$");
    private static final Pattern MIXED_ALNUM_PATTERN =
            Pattern.compile("\\p{L}+\\p{N}+|\\p{N}+\\p{L}+");
    private static final Pattern ABBREVIATION_PATTERN =
            Pattern.compile("(\\P{L})*(\\p{L}[.])+");
    private static final Pattern I_PATTERN =
            Pattern.compile("\\bI\\b");
    private static final Pattern FORWARD_SLASH_ABBREVIATION_PATTERN =
            Pattern.compile("^(.?/)(.*)$");
    private static final Pattern SENTENCE_ENDING_PATTERN =
            Pattern.compile("[.?!]\\P{Alnum}*$");
    private static final Pattern WORD_BEGINNING_PATTERN =
            Pattern.compile("^(\\P{L}*)(\\p{L}+)(.*)$");
    private static final Pattern GENERAL_ABBREV_PATTERN =
            Pattern.compile("ETC[.]|PP[.]|PH[.]?D[.]");
    private static final CharSequenceTranslator UNESCAPE_HTML_ENTITIES = new AggregateTranslator(
            new LookupTranslator(EntityArrays.ISO8859_1_UNESCAPE),
            new LookupTranslator(EntityArrays.HTML40_EXTENDED_UNESCAPE)
    );

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList("A", "AN", "AT",
            "AS", "AND", "ARE", "BUT", "BY", "ERE", "FOR", "FROM", "IN", "INTO", "IS", "OF", "ON",
            "ONTO", "OR", "OVER", "PER", "THE", "TO", "THAT", "THAN", "UNTIL", "UNTO", "UPON",
            "VIA", "WITH", "WHILE", "WHILST", "WITHIN", "WITHOUT"));
    private static final Set<String> ENTITIES = new HashSet<>(Arrays.asList("ARD", "NI", "SE",
            "GB", "SC", "UK", "LTD", "L.T.D", "PLC", "P.L.C", "UNLTD", "CIC", "C.I.C", "LLP",
            "L.P", "LP", "EEIG", "OEIC", "ICVC", "AEIE", "C.B.C", "C.C.C", "CBC", "CBCN", "CBP",
            "CCC", "CYF", "EESV", "EOFG", "EOOS", "GEIE", "GELE", "PAC", "PCCLIMITED", "PCCLTD",
            "PROTECTEDCELLCOMPANY", "CWMNICELLGWARCHODEDIG", "CCGCYFYNGEDIG", "CCGCYF"));

    private TextFormatter() {
    }

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
    static String formatAsEntityName(String text) {
        return format(text, new EntityCaseStateFactory());
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
    static String formatAsSentence(String text) {
        return format(text, new SentenceCaseStateFactory());
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
    static String formatAsParticulars(String text) {
        if (StringUtils.isEmpty(text)) {
            return text;
        }
        String result = text.replaceAll("\\h+", " ")
                .replaceAll("\\h\\\\n", "\\\\n")
                .replaceAll("(?:\\\\n)+", "\\\\n")
                .replaceAll("\\A\\\\n", "")
                .replaceAll("\\\\n+", ". ");
        result = formatAsSentence(result);
        if (!result.endsWith(".")) {
            result += ".";
        }
        return result;
    }

    private static String format(String text, StateFactory stateFactory) {
        if (StringUtils.isEmpty(text)) {
            return text;
        }
        String unescapedText = UNESCAPE_HTML_ENTITIES.translate(text);
        String result = unescapedText.toUpperCase(Locale.UK);
        StringTokenizer tokenizer = new StringTokenizer(result);
        StringBuilder builder = new StringBuilder();
        int index = 0;
        FormatterStateMachine stateMachine = new FormatterStateMachine(stateFactory);
        while (tokenizer.hasNext()) {
            String token = tokenizer.next();
            stateMachine.setToken(token);
            if (isEntity(token)) {
                stateMachine.entityName();
            } else if (isForwardslashAbbr(token, index)) {
                stateMachine.forwardslashAbbr();
            } else if (isOpeningParenthesis(token)) {
                stateMachine.parenthesis();
            } else if (isStopWord(token, index, tokenizer.hasNext())) {
                stateMachine.stopWord();
            } else {
                stateMachine.regularText();
            }
            builder.append(stateMachine.getMappedToken()).append(" ");
            if (endOfSentence(token)) {
                stateMachine.endSentence();
            } else if (endsWithColon(token)) {
                stateMachine.colon();
            }
            index++;
        }
        return builder.toString().trim();
    }

    private static boolean isEntity(String token) {
        Matcher mixedAlnumMatcher = MIXED_ALNUM_PATTERN.matcher(token);
        Matcher abbreviationMatcher = ABBREVIATION_PATTERN.matcher(token);
        Matcher wordBeginningMatcher = WORD_BEGINNING_PATTERN.matcher(token);
        Matcher iletterMatcher = I_PATTERN.matcher(token);
        return (wordBeginningMatcher.matches() && ENTITIES.contains(wordBeginningMatcher.group(2)))
                || mixedAlnumMatcher.find()
                || abbreviationMatcher.matches()
                || iletterMatcher.find();
    }

    private static boolean isForwardslashAbbr(String token, int index) {
        Matcher forwardSlashAbbrevMatcher = FORWARD_SLASH_ABBREVIATION_PATTERN.matcher(token);
        return index == 0 && forwardSlashAbbrevMatcher.matches();
    }

    private static boolean isOpeningParenthesis(String token) {
        Matcher tokenMatcher = STEM_PATTERN.matcher(token);
        return tokenMatcher.find();
    }

    private static boolean isStopWord(String token, int index, boolean hasNext) {
        return STOP_WORDS.contains(token) && index > 0 && hasNext;
    }

    private static boolean endOfSentence(String token) {
        Matcher sentenceEndingMatcher = SENTENCE_ENDING_PATTERN.matcher(token);
        Matcher generalAbbrevPattern = GENERAL_ABBREV_PATTERN.matcher(token);
        Matcher titleAbbrevPattern = ABBREVIATION_PATTERN.matcher(token);
        return sentenceEndingMatcher.find()
                && !generalAbbrevPattern.matches()
                && !titleAbbrevPattern.matches();
    }

    private static boolean endsWithColon(String token) {
        Matcher colonMatcher = COLON_PATTERN.matcher(token);
        return colonMatcher.find();
    }

    private interface StateFactory {
        FormatterState newRegularTextState(FormatterStateMachine stateMachine);

        FormatterState newAfterColonState(FormatterStateMachine stateMachine);

        FormatterState newEntityNameState(FormatterStateMachine stateMachine);

        FormatterState newParenthesesState(FormatterStateMachine stateMachine);

        FormatterState newStopWordState(FormatterStateMachine stateMachine);

        FormatterState newForwardslashAbbrState(FormatterStateMachine stateMachine);

        FormatterState newEndSentenceState(FormatterStateMachine stateMachine);

        FormatterState newStartSentenceState(FormatterStateMachine stateMachine);
    }

    private static class EntityCaseStateFactory implements StateFactory {
        @Override
        public FormatterState newRegularTextState(FormatterStateMachine stateMachine) {
            return new RegularEntityText(stateMachine);
        }

        @Override
        public FormatterState newAfterColonState(FormatterStateMachine stateMachine) {
            return new AfterColon(stateMachine);
        }

        @Override
        public FormatterState newEntityNameState(FormatterStateMachine stateMachine) {
            return new EntityName(stateMachine);
        }

        @Override
        public FormatterState newParenthesesState(FormatterStateMachine stateMachine) {
            return new Parenthesis(stateMachine);
        }

        @Override
        public FormatterState newStopWordState(FormatterStateMachine stateMachine) {
            return new StopWord(stateMachine);
        }

        @Override
        public FormatterState newForwardslashAbbrState(FormatterStateMachine stateMachine) {
            return new RegularEntityText(stateMachine);
        }

        @Override
        public FormatterState newEndSentenceState(FormatterStateMachine stateMachine) {
            return new RegularEntityText(stateMachine);
        }

        @Override
        public FormatterState newStartSentenceState(FormatterStateMachine stateMachine) {
            return new RegularEntityText(stateMachine);
        }
    }

    private static class SentenceCaseStateFactory implements StateFactory {
        @Override
        public FormatterState newRegularTextState(FormatterStateMachine stateMachine) {
            return new RegularSentenceText(stateMachine);
        }

        @Override
        public FormatterState newAfterColonState(FormatterStateMachine stateMachine) {
            return new RegularSentenceText(stateMachine);
        }

        @Override
        public FormatterState newEntityNameState(FormatterStateMachine stateMachine) {
            return new EntityName(stateMachine);
        }

        @Override
        public FormatterState newParenthesesState(FormatterStateMachine stateMachine) {
            return new RegularSentenceText(stateMachine);
        }

        @Override
        public FormatterState newStopWordState(FormatterStateMachine stateMachine) {
            return new RegularSentenceText(stateMachine);
        }

        @Override
        public FormatterState newForwardslashAbbrState(FormatterStateMachine stateMachine) {
            return new ForwardslashAbbreviation(stateMachine);
        }

        @Override
        public FormatterState newEndSentenceState(FormatterStateMachine stateMachine) {
            return new EndSentence(stateMachine);
        }

        @Override
        public FormatterState newStartSentenceState(FormatterStateMachine stateMachine) {
            return new StartSentence(stateMachine);
        }
    }

    private static class FormatterStateMachine {
        private final FormatterState textState;
        private final FormatterState afterColonState;
        private final FormatterState entityNameState;
        private final FormatterState parenthesisState;
        private final FormatterState stopWordState;
        private final FormatterState forwardslashAbbrState;
        private final FormatterState endSentenceState;
        private final FormatterState startSentenceState;

        private FormatterState currentState;
        private String token;

        FormatterStateMachine(StateFactory stateFactory) {
            this.textState = stateFactory.newRegularTextState(this);
            this.afterColonState = stateFactory.newAfterColonState(this);
            this.entityNameState = stateFactory.newEntityNameState(this);
            this.parenthesisState = stateFactory.newParenthesesState(this);
            this.stopWordState = stateFactory.newStopWordState(this);
            this.forwardslashAbbrState = stateFactory.newForwardslashAbbrState(this);
            this.endSentenceState = stateFactory.newEndSentenceState(this);
            this.startSentenceState = stateFactory.newStartSentenceState(this);
            this.currentState = this.endSentenceState;
        }

        void regularText() {
            this.currentState.regularText();
        }

        void colon() {
            this.currentState.colon();
        }

        void entityName() {
            this.currentState.entityName();
        }

        void parenthesis() {
            this.currentState.parenthesis();
        }

        void stopWord() {
            this.currentState.stopWord();
        }

        void forwardslashAbbr() {
            this.currentState.forwardslashAbbreviation();
        }

        void endSentence() {
            this.currentState.endSentence();
        }

        void setToken(String token) {
            this.token = token;
        }

        String getMappedToken() {
            return this.currentState.mapToken(this.token);
        }
    }

    private interface FormatterState {
        void regularText();

        void colon();

        void entityName();

        void parenthesis();

        void stopWord();

        void forwardslashAbbreviation();

        void endSentence();

        String mapToken(String token);
    }

    private abstract static class AbstractState implements FormatterState {

        private final FormatterStateMachine stateMachine;

        public AbstractState(FormatterStateMachine stateMachine) {
            this.stateMachine = stateMachine;
        }

        @Override
        public void regularText() {
            this.stateMachine.currentState = this.stateMachine.textState;
        }

        @Override
        public void colon() {
            this.stateMachine.currentState = this.stateMachine.afterColonState;
        }

        @Override
        public void entityName() {
            this.stateMachine.currentState = this.stateMachine.entityNameState;
        }

        @Override
        public void parenthesis() {
            this.stateMachine.currentState = this.stateMachine.parenthesisState;
        }

        @Override
        public void stopWord() {
            this.stateMachine.currentState = this.stateMachine.stopWordState;
        }

        @Override
        public void forwardslashAbbreviation() {
            this.stateMachine.currentState = this.stateMachine.forwardslashAbbrState;
        }

        @Override
        public void endSentence() {
            this.stateMachine.currentState = this.stateMachine.endSentenceState;
        }

        @Override
        public String mapToken(String token) {
            throw new IllegalStateException("Attempted to fetch a token from a nonexistent word");
        }

    }

    private static class RegularEntityText extends AbstractState {

        public RegularEntityText(FormatterStateMachine stateMachine) {
            super(stateMachine);
        }

        @Override
        public String mapToken(String token) {
            return WordUtils.capitalizeFully(token);
        }
    }

    private static class RegularSentenceText extends AbstractState {

        public RegularSentenceText(FormatterStateMachine stateMachine) {
            super(stateMachine);
        }

        @Override
        public String mapToken(String token) {
            return token.toLowerCase(Locale.UK);
        }
    }

    private static class AfterColon extends AbstractState {

        private final FormatterStateMachine stateMachine;

        public AfterColon(FormatterStateMachine stateMachine) {
            super(stateMachine);
            this.stateMachine = stateMachine;
        }

        @Override
        public void stopWord() {
            this.stateMachine.currentState = this.stateMachine.textState;
        }

        @Override
        public String mapToken(String token) {
            return WordUtils.capitalizeFully(token);
        }
    }

    private static class EntityName extends AbstractState {

        public EntityName(FormatterStateMachine stateMachine) {
            super(stateMachine);
        }

        @Override
        public String mapToken(String token) {
            return token.toUpperCase(Locale.UK);
        }
    }

    private static class Parenthesis extends AbstractState {

        public Parenthesis(FormatterStateMachine stateMachine) {
            super(stateMachine);
        }

        @Override
        public String mapToken(String token) {
            Matcher tokenMatcher = STEM_PATTERN.matcher(token);
            if (tokenMatcher.matches()) {
                String punctuation = tokenMatcher.group(1);
                token = tokenMatcher.group(2);
                return punctuation + WordUtils.capitalizeFully(token);
            } else {
                throw new IllegalArgumentException("Tried to map a non-matching token");
            }
        }
    }

    private static class StopWord extends AbstractState {

        public StopWord(FormatterStateMachine stateMachine) {
            super(stateMachine);
        }

        @Override
        public String mapToken(String token) {
            return token.toLowerCase(Locale.UK);
        }
    }

    private static class ForwardslashAbbreviation extends AbstractState {
        public ForwardslashAbbreviation(FormatterStateMachine stateMachine) {
            super(stateMachine);
        }

        @Override
        public String mapToken(String token) {
            Matcher forwardSlashAbbrevMatcher = FORWARD_SLASH_ABBREVIATION_PATTERN.matcher(token);
            if (forwardSlashAbbrevMatcher.matches()) {
                return forwardSlashAbbrevMatcher.group(1)
                        .toUpperCase(Locale.UK)
                        + WordUtils.capitalizeFully(forwardSlashAbbrevMatcher.group(2));
            } else {
                throw new IllegalArgumentException("Tried to map a non-forwardslash abbreviation "
                        + "as a forwardslash abbreviation");
            }
        }
    }

    private static class EndSentence extends AbstractState {

        private final FormatterStateMachine stateMachine;

        public EndSentence(FormatterStateMachine stateMachine) {
            super(stateMachine);
            this.stateMachine = stateMachine;
        }

        @Override
        public void regularText() {
            this.stateMachine.currentState = stateMachine.startSentenceState;
        }

        @Override
        public void stopWord() {
            this.stateMachine.currentState = stateMachine.startSentenceState;
        }
    }

    private static class StartSentence extends AbstractState {

        public StartSentence(FormatterStateMachine stateMachine) {
            super(stateMachine);
        }

        @Override
        public String mapToken(String token) {
            Matcher wordBeginningPattern = WORD_BEGINNING_PATTERN.matcher(token);
            if (wordBeginningPattern.matches()) {
                String punct = wordBeginningPattern.group(1);
                String word = wordBeginningPattern.group(2);
                String trailing = wordBeginningPattern.group(3);
                return punct + WordUtils.capitalizeFully(word) + trailing;
            } else {
                return token;
            }
        }
    }
}
