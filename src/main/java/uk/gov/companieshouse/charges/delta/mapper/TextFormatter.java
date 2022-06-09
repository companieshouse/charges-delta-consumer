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

public final class TextFormatter {

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
            Pattern.compile("[.?!]\\P{L}*$");
    private static final Pattern WORD_BEGINNING_PATTERN =
            Pattern.compile("^(\\P{L}*)(\\p{L}+)(.*)$");
    private static final Pattern GENERAL_ABBREV_PATTERN =
            Pattern.compile("ETC[.]|PP[.]|PH[.]?D[.]");
    private static final Pattern WORD_CAPTURE_PATTERN =
            Pattern.compile("^\\P{L}*(\\p{L}+)\\P{L}*");
    public static final CharSequenceTranslator ESCAPE_HTML_ENTITIES = new AggregateTranslator(
            new LookupTranslator(EntityArrays.ISO8859_1_ESCAPE),
            new LookupTranslator(EntityArrays.HTML40_EXTENDED_ESCAPE)
    );
    public static final CharSequenceTranslator UNESCAPE_HTML_ENTITIES = new AggregateTranslator(
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
     * <li>Any words within parentheses must use highlight casing.</li>
     * <li>Any word proceeding a colon must use highlight casing.</li>
     * <li>First and last words must use highlight casing.</li>
     * <li>Any stop words in Appendix B must be lowercase.</li>
     * <li>Any words containing both letters and numbers must be uppercase.</li>
     * <li>Any entity names in Appendix C must be uppercase.</li>
     * <li>Any country codes in Appendix D must be uppercase.</li>
     * <li>All other words must use highlight casing.</li>
     * </ul>
     * @param text The text that will be recased.
     * @return Text recased in accordance to the above rules.
     */
    public static String formatAsEntityName(String text) {
        if (StringUtils.isEmpty(text)) {
            return text;
        }
        String unescapedText = UNESCAPE_HTML_ENTITIES.translate(text);
        String result = unescapedText.toUpperCase(Locale.UK);
        StringTokenizer tokenizer = new StringTokenizer(result);
        StringBuilder builder = new StringBuilder();
        int index = 0;
        FormatterStateMachine stateMachine = new FormatterStateMachine(new EntityCaseStateFactory());
        while (tokenizer.hasNext()) {
            String token = tokenizer.next();
            stateMachine.setToken(token);
            if (isEntity(token)) {
                stateMachine.entityName();
            } else if (isOpeningParenthesis(token)) {
                stateMachine.parenthesis();
            } else if (isStopWord(token, index, tokenizer.hasNext())) {
                stateMachine.stopWord();
            } else {
                stateMachine.regularText();
            }
            builder.append(stateMachine.getMappedToken()).append(" ");
            if (endsWithColon(token)) {
                stateMachine.colon();
            }
            index++;
        }
        return ESCAPE_HTML_ENTITIES.translate(builder.toString().trim());
    }

    public static String formatAsSentence(String text) {
        if (StringUtils.isEmpty(text)) {
            return text;
        }
        String unescapedText = UNESCAPE_HTML_ENTITIES.translate(text);
        String lowerCaseText = unescapedText.toUpperCase(Locale.UK);
        StringTokenizer tokenizer = new StringTokenizer(lowerCaseText);
        StringBuilder builder = new StringBuilder();

        int index = 0;
        boolean endOfSentence = false;
        while(tokenizer.hasNext()) {
            String token = tokenizer.next();
            Matcher mixedAlphanumericMatcher = MIXED_ALNUM_PATTERN.matcher(token);
            Matcher iMatcher = I_PATTERN.matcher(token);
            Matcher forwardSlashAbbrevMatcher = FORWARD_SLASH_ABBREVIATION_PATTERN.matcher(token);
            Matcher sentenceEndingMatcher = SENTENCE_ENDING_PATTERN.matcher(token);
            Matcher wordBeginningPattern = WORD_BEGINNING_PATTERN.matcher(token);
            Matcher generalAbbrevPattern = GENERAL_ABBREV_PATTERN.matcher(token);
            Matcher titleAbbrevPattern = ABBREVIATION_PATTERN.matcher(token);
            Matcher wordCapturePattern = WORD_CAPTURE_PATTERN.matcher(token);
            if ((wordCapturePattern.matches() && ENTITIES.contains(wordCapturePattern.group(1).toUpperCase(Locale.UK))) || mixedAlphanumericMatcher.find() || iMatcher.find() || titleAbbrevPattern.matches()) {
                builder.append(token.toUpperCase(Locale.UK));
            } else if (index == 0 && forwardSlashAbbrevMatcher.matches()) {
                builder.append(forwardSlashAbbrevMatcher.group(1).toUpperCase(Locale.UK))
                        .append(WordUtils.capitalizeFully(forwardSlashAbbrevMatcher.group(2)));
            } else if (index == 0 && wordBeginningPattern.matches()) {
                String punct = wordBeginningPattern.group(1);
                String word = wordBeginningPattern.group(2);
                String trailing = wordBeginningPattern.group(3);
                builder.append(punct).append(WordUtils.capitalizeFully(word)).append(trailing);
            } else if (index == 0 || endOfSentence) {
                builder.append(WordUtils.capitalizeFully(token));
            } else {
                builder.append(token.toLowerCase(Locale.UK));
            }
            endOfSentence = false;
            builder.append(" ");
            if (sentenceEndingMatcher.find() && !generalAbbrevPattern.matches() && !titleAbbrevPattern.matches()) {
                endOfSentence = true;
            }
            index++;
        }
        return ESCAPE_HTML_ENTITIES.translate(builder.toString().trim());
    }

    private static boolean isEntity(String token) {
        Matcher mixedAlnumMatcher = MIXED_ALNUM_PATTERN.matcher(token);
        Matcher abbreviationMatcher = ABBREVIATION_PATTERN.matcher(token);
        return ENTITIES.contains(token)
                || mixedAlnumMatcher.find()
                || abbreviationMatcher.matches();
    }

    private static boolean isOpeningParenthesis(String token) {
        Matcher tokenMatcher = STEM_PATTERN.matcher(token);
        return tokenMatcher.find();
    }

    private static boolean isStopWord(String token, int index, boolean hasNext) {
        return STOP_WORDS.contains(token) && index > 0 && hasNext;
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
    }

    private static class EntityCaseStateFactory implements StateFactory {
        @Override
        public FormatterState newRegularTextState(FormatterStateMachine stateMachine) {
            return new RegularText(stateMachine);
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
            return new RegularText(stateMachine);
        }
    }

    private static class FormatterStateMachine {
        private final FormatterState textState;
        private final FormatterState afterColonState;
        private final FormatterState entityNameState;
        private final FormatterState parenthesisState;
        private final FormatterState stopWordState;
        private final FormatterState forwardslashAbbrState;

        private FormatterState currentState;
        private String token;

        public FormatterStateMachine(StateFactory stateFactory) {
            this.textState = stateFactory.newRegularTextState(this);
            this.afterColonState = stateFactory.newAfterColonState(this);
            this.entityNameState = stateFactory.newEntityNameState(this);
            this.parenthesisState = stateFactory.newParenthesesState(this);
            this.stopWordState = stateFactory.newStopWordState(this);
            this.forwardslashAbbrState = stateFactory.newForwardslashAbbrState(this);
            this.currentState = new NoText(this);
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
        public String mapToken(String token) {
            throw new IllegalStateException("Attempted to fetch a token from a nonexistent word");
        }

    }

    private static class NoText extends AbstractState {

        public NoText(FormatterStateMachine stateMachine) {
            super(stateMachine);
        }

    }

    private static class RegularText extends AbstractState {

        public RegularText(FormatterStateMachine stateMachine) {
            super(stateMachine);
        }

        @Override
        public String mapToken(String token) {
            return WordUtils.capitalizeFully(token);
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
                return forwardSlashAbbrevMatcher.group(1).toUpperCase(Locale.UK) + WordUtils.capitalizeFully(forwardSlashAbbrevMatcher.group(2));
            } else {
                throw new IllegalArgumentException("Tried to map a non-forwardslash abbreviation as a forwardslash abbreviation");
            }
        }
    }
}
