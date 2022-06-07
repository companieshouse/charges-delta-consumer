package uk.gov.companieshouse.charges.delta.mapper;

import org.apache.commons.text.StringTokenizer;
import org.apache.commons.text.WordUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextFormatter {

    private static final Pattern STEM_PATTERN = Pattern.compile("(\\P{Alnum})(\\p{Alnum}+)");

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList("A","AN","AT","AS","AND","ARE","BUT","BY","ERE","FOR","FROM","IN","INTO","IS","OF","ON","ONTO","OR","OVER","PER","THE","TO","THAT","THAN","UNTIL","UNTO","UPON","VIA","WITH","WHILE","WHILST","WITHIN","WITHOUT"));

    private TextFormatter(){
    }

    public static String formatAsEntityName(String text) {
        if (text == null || "".equals(text)) {
            return text;
        }
        String result = text.toUpperCase(Locale.UK);
        StringTokenizer tokenizer = new StringTokenizer(result);
        StringBuilder builder = new StringBuilder();
        int index = 0;
        while(tokenizer.hasNext()) {
            String token = tokenizer.next();
            Matcher tokenMatcher = STEM_PATTERN.matcher(token);
            if (tokenMatcher.matches()) {
                String punctuation = tokenMatcher.group(1);
                token = tokenMatcher.group(2);
                builder.append(punctuation);
            }
            if (STOP_WORDS.contains(token) && index > 0 && tokenizer.hasNext()) {
                builder.append(token.toLowerCase(Locale.UK) + " ");
            } else {
                builder.append(WordUtils.capitalizeFully(token) + " ");
            }
            index++;
        }

        return builder.toString().trim();
    }
}
