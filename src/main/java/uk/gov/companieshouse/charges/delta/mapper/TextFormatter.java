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
    private static final Pattern COLON_PATTERN = Pattern.compile("[;:]$");
    private static final Pattern MIXED_ALNUM_PATTERN = Pattern.compile("\\p{L}+\\p{N}+|\\p{N}+\\p{L}+");
    private static final Pattern ABBREVIATION_PATTERN = Pattern.compile("(\\p{L}[.])+");

    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList("A","AN","AT","AS","AND","ARE","BUT","BY","ERE","FOR","FROM","IN","INTO","IS","OF","ON","ONTO","OR","OVER","PER","THE","TO","THAT","THAN","UNTIL","UNTO","UPON","VIA","WITH","WHILE","WHILST","WITHIN","WITHOUT"));
    private static final Set<String> ENTITIES = new HashSet<>(Arrays.asList("ARD","NI","SE","GB","SC","UK","LTD","L.T.D","PLC","P.L.C","UNLTD","CIC","C.I.C","LLP","L.P","LP","EEIG","OEIC","ICVC","AEIE","C.B.C","C.C.C","CBC","CBCN","CBP","CCC","CYF","EESV","EOFG","EOOS","GEIE","GELE","PAC","PCCLIMITED","PCCLTD","PROTECTEDCELLCOMPANY","CWMNICELLGWARCHODEDIG","CCGCYFYNGEDIG","CCGCYF"));

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
        boolean afterColon = false;
        while(tokenizer.hasNext()) {
            String token = tokenizer.next();
            Matcher tokenMatcher = STEM_PATTERN.matcher(token);
            Matcher colonMatcher = COLON_PATTERN.matcher(token);
            Matcher mixedAlnumMatcher = MIXED_ALNUM_PATTERN.matcher(token);
            Matcher abbreviationMatcher = ABBREVIATION_PATTERN.matcher(token);
            if (ENTITIES.contains(token) || mixedAlnumMatcher.find() || abbreviationMatcher.matches()) {
                afterColon = false;
                builder.append(token.toUpperCase(Locale.UK) + " ");
            } else if (tokenMatcher.matches()) {
                afterColon = false;
                String punctuation = tokenMatcher.group(1);
                token = tokenMatcher.group(2);
                builder.append(punctuation);
                builder.append(WordUtils.capitalizeFully(token) + " ");
            } else if (STOP_WORDS.contains(token) && index > 0 && tokenizer.hasNext() && !afterColon) {
                builder.append(token.toLowerCase(Locale.UK) + " ");
            } else {
                afterColon = false;
                builder.append(WordUtils.capitalizeFully(token) + " ");
            }
            if (colonMatcher.find()) {
                afterColon = true;
            }
            index++;
        }

        return builder.toString().trim();
    }
}
