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

public final class TextFormatter {

    private static final Pattern STEM_PATTERN =
            Pattern.compile("(\\()(\\p{Alnum}+)");
    private static final Pattern COLON_PATTERN =
            Pattern.compile("[;:]$");
    private static final Pattern MIXED_ALNUM_PATTERN =
            Pattern.compile("\\p{L}+\\p{N}+|\\p{N}+\\p{L}+");
    private static final Pattern ABBREVIATION_PATTERN =
            Pattern.compile("(\\p{L}[.])+");

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
        String result = text.toUpperCase(Locale.UK);
        StringTokenizer tokenizer = new StringTokenizer(result);
        StringBuilder builder = new StringBuilder();
        int index = 0;
        FormatterStateMachine stateMachine = new FormatterStateMachine();
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
        return builder.toString().trim();
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

    private static class FormatterStateMachine {
        private final RegularText textState;
        private final AfterColon afterColonState;
        private final EntityName entityNameState;
        private final Parenthesis parenthesisState;
        private final StopWord stopWordState;

        private FormatterState currentState;
        private String token;

        public FormatterStateMachine() {
            this.textState = new RegularText(this);
            this.afterColonState = new AfterColon(this);
            this.entityNameState = new EntityName(this);
            this.parenthesisState = new Parenthesis(this);
            this.stopWordState = new StopWord(this);
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
}
