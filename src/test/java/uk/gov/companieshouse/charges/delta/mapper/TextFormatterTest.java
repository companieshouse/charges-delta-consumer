package uk.gov.companieshouse.charges.delta.mapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextFormatterTest {

    @ParameterizedTest(name = "Map {0} to {1}")
    @MethodSource("entityNameFormatting")
    @DisplayName("Format text as an entity name")
    void testFormatAsEntityName(String input, String expected) {
        // when
        String actual = TextFormatter.formatAsEntityName(input);

        // then
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "Map {0} to {1}")
    @MethodSource("sentenceFormatting")
    @DisplayName("Format text as a sentence")
    void testFormatAsSentence(String input, String expected) {
        // when
        String actual = TextFormatter.formatAsSentence(input);

        // then
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "Map {0} to {1}")
    @MethodSource("particularsFormatting")
    @DisplayName("Format text as particulars")
    void testFormatAsParticulars(String input, String expected) {
        // when
        String actual = TextFormatter.formatAsParticulars(input);

        // then
        assertEquals(expected, actual);
    }

    private static Stream<Arguments> entityNameFormatting() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("", ""),
                Arguments.of("a", "A"),
                Arguments.of("&aelig;", "Æ"),
                Arguments.of("æ", "Æ"),
                Arguments.of("ab", "Ab"),
                Arguments.of("aB", "Ab"),
                Arguments.of("bread butter", "Bread Butter"),
                Arguments.of("bReAD BuTteR", "Bread Butter"),
                Arguments.of("bread and butter", "Bread and Butter"),
                Arguments.of("and or the", "And or The"),
                Arguments.of("King (of in the) Hill", "King (Of in The) Hill"),
                Arguments.of("King (is king of the) Hill", "King (Is King of The) Hill"),
                Arguments.of("An apple; an orange","An Apple; An Orange"),
                Arguments.of("An apple; and; an orange","An Apple; And; An Orange"),
                Arguments.of("java coffee 4l1f3", "Java Coffee 4L1F3"),
                Arguments.of("java coffee \"4l1f3\"", "Java Coffee \"4L1F3\""),
                Arguments.of("llp", "LLP"),
                Arguments.of("Director is from the uk", "Director is from the UK"),
                Arguments.of("a\nb", "A B"),
                Arguments.of("d.r.", "D.R."),
                Arguments.of("a  \t b", "A B"),
                Arguments.of("d.r. jOhN f SmItH of lC123456    pLc (IS anybody ERE); aND\nIS  From the uk in lONdON", "D.R. John F Smith of LC123456 PLC (Is Anybody Ere); And is from the UK in London")
        );
    }

    private static Stream<Arguments> sentenceFormatting() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("", ""),
                Arguments.of(".", "."),
                Arguments.of("a", "A"),
                Arguments.of("&aelig;", "Æ"),
                Arguments.of("æ", "Æ"),
                Arguments.of("ab", "Ab"),
                Arguments.of("aB", "Ab"),
                Arguments.of("bread butter", "Bread butter"),
                Arguments.of("bReAD BuTteR", "Bread butter"),
                Arguments.of("i think therefore i am", "I think therefore I am"),
                Arguments.of("\"i am?\"", "\"I am?\""),
                Arguments.of("p/office p/office", "P/Office p/office"),
                Arguments.of("one. two. three.", "One. Two. Three."),
                Arguments.of("\"i.\" am. error.", "\"I.\" Am. Error."),
                Arguments.of("\"one.\" two. three.", "\"One.\" Two. Three."),
                Arguments.of("hello, goodbye, etc. greetings", "Hello, goodbye, etc. greetings"),
                Arguments.of("one two ) three", "One two ) three"),
                Arguments.of("Once\nupon\na\ntime", "Once upon a time"),
                Arguments.of("d.r. in the house", "D.R. in the house"),
                Arguments.of("\"d.r. in\" the house", "\"D.R. in\" the house"),
                Arguments.of("one  \t two", "One two"),
                Arguments.of("java coffee 4l1f3", "Java coffee 4L1F3"),
                Arguments.of("llp", "LLP"),
                Arguments.of("\"llp\"", "\"LLP\""),
                Arguments.of("to be. or not to be.", "To be. Or not to be."),
                Arguments.of("&pound;220,000.00                              AND ALL OTHER MONIES DUE OR TO BECOME DUE", "£220,000.00 and all other monies due or to become due"),
                Arguments.of("p/office the d.r. of an lLp saYs a cAT is ) for ChrIstmAS etc. \n\t but i\tthink (a cat) is 4life! æthelred is ready.", "P/Office the D.R. of an LLP says a cat is ) for christmas etc. but I think (a cat) is 4LIFE! Æthelred is ready.")
        );
    }

    private static Stream<Arguments> particularsFormatting() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("", ""),
                Arguments.of("\\n", "."),
                Arguments.of("  ", "."),
                Arguments.of("   \\n", "."),
                Arguments.of("\\n\\n", "."),
                Arguments.of("\\nHello", "Hello."),
                Arguments.of("Hello\\n\\nWorld", "Hello. World."),
                Arguments.of("Hello\\n \\n \\nWorld", "Hello. World."),
                Arguments.of("\\np/office the d.r. of an lLp saYs a cAT is ) for ChrIstmAS etc.  \\n\\n\t but i\tthink (a cat) is 4life! æthelred is ready", "P/Office the D.R. of an LLP says a cat is ) for christmas etc.. But I think (a cat) is 4LIFE! Æthelred is ready.")
        );
    }
}
