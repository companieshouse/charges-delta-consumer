package uk.gov.companieshouse.charges.delta.mapper;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringTokenizer;
import org.apache.commons.text.WordUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextFormatter {

    private static final Pattern STEM_PATTERN = Pattern.compile("(\\()(\\p{Alnum}+)");
    private static final Pattern COLON_PATTERN = Pattern.compile("[;:]$");
    private static final Pattern MIXED_ALNUM_PATTERN = Pattern.compile("\\p{L}+\\p{N}+|\\p{N}+\\p{L}+");
    private static final Pattern ABBREVIATION_PATTERN = Pattern.compile("(\\p{L}[.])+");

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList("A","AN","AT","AS","AND","ARE","BUT","BY","ERE","FOR","FROM","IN","INTO","IS","OF","ON","ONTO","OR","OVER","PER","THE","TO","THAT","THAN","UNTIL","UNTO","UPON","VIA","WITH","WHILE","WHILST","WITHIN","WITHOUT"));
    private static final Set<String> ENTITIES = new HashSet<>(Arrays.asList("ARD","NI","SE","GB","SC","UK","LTD","L.T.D","PLC","P.L.C","UNLTD","CIC","C.I.C","LLP","L.P","LP","EEIG","OEIC","ICVC","AEIE","C.B.C","C.C.C","CBC","CBCN","CBP","CCC","CYF","EESV","EOFG","EOOS","GEIE","GELE","PAC","PCCLIMITED","PCCLTD","PROTECTEDCELLCOMPANY","CWMNICELLGWARCHODEDIG","CCGCYFYNGEDIG","CCGCYF"));

    private TextFormatter(){
    }

    public static String formatAsEntityName(String text) {
        if (StringUtils.isEmpty(text)) {
            return text;
        }
        String result = text.toUpperCase(Locale.UK);
        StringTokenizer tokenizer = new StringTokenizer(result);
        StringBuilder builder = new StringBuilder();
        int index = 0;
        FormatterStateMachine stateMachine = new FormatterStateMachine();
        while(tokenizer.hasNext()) {
            String token = tokenizer.next();
            stateMachine.setToken(token);
            if (isEntity(token)) {
                stateMachine.entityName();
            } else if (isOpeningParentheses(token)) {
                stateMachine.parentheses();
            } else if (isStopWord(token, index, tokenizer.hasNext())) {
                stateMachine.stopWord();
            } else {
                stateMachine.hasText();
            }
            builder.append(stateMachine.getToken()).append(" ");
            if (endsWithColon(token)) {
                stateMachine.hasColon();
            }
            index++;
        }
        return builder.toString().trim();
    }

    private static boolean isEntity(String token) {
        Matcher mixedAlnumMatcher = MIXED_ALNUM_PATTERN.matcher(token);
        Matcher abbreviationMatcher = ABBREVIATION_PATTERN.matcher(token);
        return ENTITIES.contains(token) || mixedAlnumMatcher.find() || abbreviationMatcher.matches();
    }

    private static boolean isOpeningParentheses(String token) {
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
        private final NoText noTextState;
        private final Text textState;
        private final TextAfterColon textAfterColonState;
        private final EntityName entityNameState;
        private final Parentheses parenthesesState;
        private final StopWord stopWordState;

        private FormatterState currentState;
        private String token;

        public FormatterStateMachine() {
            this.noTextState = new NoText(this);
            this.textState = new Text(this);
            this.textAfterColonState = new TextAfterColon(this);
            this.entityNameState = new EntityName(this);
            this.parenthesesState = new Parentheses(this);
            this.stopWordState = new StopWord(this);
            this.currentState = this.noTextState;
        }

        void hasText() {
            this.currentState.hasText();
        }

        void hasColon() {
            this.currentState.hasColon();
        }

        void entityName() {
            this.currentState.entityName();
        }

        void parentheses() {
            this.currentState.parentheses();
        }

        void stopWord() {
            this.currentState.stopWord();
        }

        void setToken(String token) {
            this.token = token;
        }

        String getToken() {
            return this.currentState.mapToken(this.token);
        }
    }

    private interface FormatterState {
        void hasText();
        void hasColon();
        void entityName();
        void parentheses();
        void stopWord();
        String mapToken(String token);
    }

    private static abstract class AbstractState implements FormatterState {

        private final FormatterStateMachine stateMachine;

        public AbstractState(FormatterStateMachine stateMachine) {
            this.stateMachine = stateMachine;
        }

        @Override
        public void hasText() {
            this.stateMachine.currentState = this.stateMachine.textState;
        }

        @Override
        public void hasColon() {
            this.stateMachine.currentState = this.stateMachine.textAfterColonState;
        }

        @Override
        public void entityName() {
            this.stateMachine.currentState = this.stateMachine.entityNameState;
        }

        @Override
        public void parentheses() {
            this.stateMachine.currentState = this.stateMachine.parenthesesState;
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

    private static class Text extends AbstractState {

        public Text(FormatterStateMachine stateMachine) {
            super(stateMachine);
        }

        @Override
        public String mapToken(String token) {
            return WordUtils.capitalizeFully(token);
        }
    }

    private static class TextAfterColon extends AbstractState {

        private final FormatterStateMachine stateMachine;

        public TextAfterColon(FormatterStateMachine stateMachine) {
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

    static class Parentheses extends AbstractState {

        public Parentheses(FormatterStateMachine stateMachine) {
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
                throw new IllegalStateException("Tried to map a non-matching token");
            }
        }
    }

    static class StopWord extends AbstractState {

        public StopWord(FormatterStateMachine stateMachine) {
            super(stateMachine);
        }

        @Override
        public String mapToken(String token) {
            return token.toLowerCase(Locale.UK);
        }
    }
}
