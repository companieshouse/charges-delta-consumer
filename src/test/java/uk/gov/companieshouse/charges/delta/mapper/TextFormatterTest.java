package uk.gov.companieshouse.charges.delta.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextFormatterTest {

    private TextFormatter textFormatter;

    @BeforeEach
    void beforeEach() {
        textFormatter = new TextFormatter();
    }

    @ParameterizedTest(name = "Map [{0}] to [{1}]")
    @MethodSource("entityNameFormatting")
    @DisplayName("Format text as an entity name")
    void testFormatAsEntityName(String input, String expected) {
        // when
        String actual = textFormatter.formatAsEntityName(input);

        // then
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "Map [{0}] to [{1}]")
    @MethodSource("sentenceFormatting")
    @DisplayName("Format text as a sentence")
    void testFormatAsSentence(String input, String expected) {
        // when
        String actual = textFormatter.formatAsSentence(input);

        // then
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "Map [{0}] to [{1}]")
    @MethodSource("particularsFormatting")
    @DisplayName("Format text as particulars")
    void testFormatAsParticulars(String input, String expected) {
        // when
        String actual = textFormatter.formatAsParticulars(input);

        // then
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "End of sentence should be [{1}] for token [{0}]")
    @MethodSource("endOfSentence")
    @DisplayName("Determine if provided token is the end of a sentence")
    void testEndOfSentence(String token, TextFormatter.SentenceTerminationState expected) {
        // when
        TextFormatter.SentenceTerminationState actual = TextFormatter.isEndOfSentence(token);

        // then
        assertEquals(expected, actual);
    }

    @ParameterizedTest(name = "Possessiveness should be [{1}] for token [{0}]")
    @MethodSource("possessiveState")
    @DisplayName("Determine if provided token is possessive")
    void testPossessiveness(String token, TextFormatter.Possessiveness expected) {
        // when
        TextFormatter.Possessiveness actual = TextFormatter.isPossessive(token);

        // then
        assertEquals(expected, actual);
    }

    private static Stream<Arguments> entityNameFormatting() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("", ""),
                Arguments.of("a", "A"),
                Arguments.of("æ", "Æ"),
                Arguments.of("ab", "Ab"),
                Arguments.of("aB", "Ab"),
                Arguments.of("bread butter", "Bread Butter"),
                Arguments.of("bReAD BuTteR", "Bread Butter"),
                Arguments.of("bread and butter", "Bread and Butter"),
                Arguments.of("and or the", "And or The"),
                Arguments.of("King (of in the) Hill", "King (Of in The) Hill"),
                Arguments.of("King (.of in the) Hill", "King (.of in The) Hill"),
                Arguments.of("King .(of in the) Hill", "King .(Of in The) Hill"),
                Arguments.of("King .(.of in the.). Hill", "King .(.of in the.). Hill"),
                Arguments.of("King (is king of the) Hill", "King (Is King of The) Hill"),
                Arguments.of("King (of. in the) Hill", "King (Of. in The) Hill"),
                Arguments.of("An apple; an orange", "An Apple; An Orange"),
                Arguments.of("An apple; \"an orange", "An Apple; \"an Orange"),
                Arguments.of("An apple; and; an orange", "An Apple; And; an Orange"),
                Arguments.of("java coffee 4l1f3", "Java Coffee 4L1F3"),
                Arguments.of("java coffee \"4l1f3\"", "Java Coffee \"4L1F3\""),
                Arguments.of("llp", "LLP"),
                Arguments.of("Director is from the uk", "Director is from the UK"),
                Arguments.of("a\nb", "A B"),
                Arguments.of("d.r.", "D.R."),
                Arguments.of("a  \t b", "A B"),
                Arguments.of("b.sc", "B.SC"),
                Arguments.of("d.r john smith b.sc of london", "D.R John Smith B.SC of London"),
                Arguments.of("b.sc.", "B.SC."),
                Arguments.of("b.sci.", "B.Sci."),
                Arguments.of("a.b.c.d.sci.", "A.B.C.D.Sci."),
                Arguments.of("sci.d.c.b.a.", "Sci.D.C.B.A."),
                Arguments.of("the word is sci.d.c.b.a.", "The Word is Sci.D.C.B.A."),
                Arguments.of("the word is; sci.d.c.b.a.", "The Word is; Sci.D.C.B.A."), // stop words surrounded with punctuation must not be capitalised
                Arguments.of("the word is s.ci.d.c.b.a.", "The Word is S.Ci.D.C.B.A."),
                Arguments.of("b.a.!b.\"sc.m?.a.?m.sc.", "B.A.!B.\"SC.M?.A.?M.SC."),
                Arguments.of("harrow-on-the-hill", "Harrow-on-the-Hill"), // delimited stop words must not be capitalised
                Arguments.of("the presenter is from harrow-on-the-hill", "The Presenter is from Harrow-on-the-Hill"),
                Arguments.of("the presenter is from \"harrow\"-\"on-the\"-\"hill!!!", "The Presenter is from \"Harrow\"-\"on-the\"-\"Hill!!!"),
                Arguments.of("don't tell me.", "Don't Tell Me."),
                Arguments.of("c,om,mas", "C,Om,Mas"),
                Arguments.of("the trees, the branches and the leaves", "The Trees, the Branches and the Leaves"),
                Arguments.of("2 + 2 = 4", "2 + 2 = 4"),
                Arguments.of("2+2=4", "2+2=4"),
                Arguments.of("d.r. jOhN f SmItH of lC123456    pLc (IS anybody ERE); aND\nIS  From the uk in lONdON", "D.R. John F Smith of LC123456 PLC (Is Anybody Ere); And is from the UK in London")
        );
    }

    private static Stream<Arguments> sentenceFormatting() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("", ""),
                Arguments.of(".", "."),
                Arguments.of("a", "A"),
                Arguments.of("æ", "æ"),
                Arguments.of("ab", "Ab"),
                Arguments.of("aB", "Ab"),
                Arguments.of("bread butter", "Bread butter"),
                Arguments.of("bReAD BuTteR", "Bread butter"),
                Arguments.of("i think therefore i am", "I think therefore I am"),
                Arguments.of("\"i am?\"", "\"I am?\""),
                Arguments.of("p/office p/office", "P/Office p/office"),
                Arguments.of("p/!office p/office", "P/!Office p/office"),
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
                Arguments.of("d.r.", "D.R."),
                Arguments.of("b.sc", "B.SC"),
                Arguments.of("d.r john smith b.sc of london", "D.r john smith B.SC of london"),
                Arguments.of("d.r john smith b.sci of london", "D.r john smith B.sci of london"),
                Arguments.of("d.r john smith b.sci.b.sci.b.sci. of london", "D.r john smith B.sci.B.sci.B.sci. Of london"),
                Arguments.of("b.sc.", "B.SC."),
                Arguments.of("b.sci.", "B.sci."),
                Arguments.of("a.b.c.d.sci.", "A.B.C.D.sci."),
                Arguments.of("sci.d.c.b.a.", "Sci.D.C.B.A."),
                Arguments.of("the word is sci.d.c.b.a.", "The word is sci.D.C.B.A."),
                Arguments.of("the word is s.ci.d.c.b.a.", "The word is S.ci.D.C.B.A."),
                Arguments.of("the word is; sci.d.c.b.a.", "The word is; sci.D.C.B.A."),
                Arguments.of("b.a.!b.\"sc.m?.a.?m.sc.", "B.A.!B.\"SC.m?.A.?M.SC."),
                Arguments.of("to be. or not to be.", "To be. Or not to be."),
                Arguments.of("£220,000.00                              AND ALL OTHER MONIES DUE OR TO BECOME DUE", "£220,000.00 and all other monies due or to become due"),
                Arguments.of("john smith b.sc. is here", "John smith B.SC. Is here"),
                Arguments.of("i am mr. john smith ba.sci of london", "I am mr. John smith ba.sci of london"),
                Arguments.of("i am mr. john smith b.sci of london", "I am mr. John smith B.sci of london"),
                Arguments.of("harrow-on-the-hill", "Harrow-on-the-hill"),
                Arguments.of("the presenter is from harrow-on-the-hill", "The presenter is from harrow-on-the-hill"),
                Arguments.of("the presenter is from \"harrow\"-\"on-the\"-\"hill!!!", "The presenter is from \"harrow\"-\"on-the\"-\"hill!!!"),
                Arguments.of("don't tell me.", "Don't tell me."),
                Arguments.of("c,om,mas", "C,om,mas"),
                Arguments.of("the trees, the branches and the leaves", "The trees, the branches and the leaves"),
                Arguments.of("2 + 2 = 4", "2 + 2 = 4"),
                Arguments.of("2+2=4", "2+2=4"),
                Arguments.of("this is a word with two full stops.. so is this..", "This is a word with two full stops.. So is this.."),
                Arguments.of("i.. don't know if this will work", "I.. Don't know if this will work"),
                Arguments.of("p/office the d.r. of an lLp saYs a cAT is ) for ChrIstmAS etc. \n\t but i\tthink (a cat) is 4life! æthelred is ready.", "P/Office the D.R. of an LLP says a cat is ) "
                        + "for christmas etc. but I think (a cat) is 4LIFE! æThelred is ready."),
                Arguments.of("This sentence contains sequence AB.1234. sentence casing should apply after the full stop", "This sentence contains sequence ab.1234. Sentence casing should apply after the full stop"),
                Arguments.of("This sentence contains brackets and sequence AB.1234. (sentence casing) applies inside the brackets and after the full stop.", "This sentence contains brackets and sequence ab.1234. (Sentence casing) applies inside the brackets and after the full stop."),
                Arguments.of("(this sentence has closing brackets after a full stop.) this one does not.", "(This sentence has closing brackets after a full stop.) This one does not."),
                Arguments.of("this sentence has an unmatched closing bracket after a full stop.) this one does not.", "This sentence has an unmatched closing bracket after a full stop.) this one does not."),
                Arguments.of("THIS SENTENCE CONTAINS AN ACRONYM 2.2I WITH AN I", "This sentence contains an acronym 2.2I with an I"),
                Arguments.of("this sentence contains approximately (i)-(iii) roman numerals", "This sentence contains approximately (i)-(iii) roman numerals")
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
                Arguments.of("this is a word with two full stops.. so is this..\\n", "This is a word with two full stops.. So is this.."),
                Arguments.of("Hello\\n\\nWorld", "Hello. World."),
                Arguments.of("Hello\\n \\n \\nWorld", "Hello. World."),
                Arguments.of("\\np/office the d.r. of an lLp saYs a cAT is ) for ChrIstmAS etc.  \\n\\n\t but i\tthink (a cat) is 4life! æthelred is ready", "P/Office the D.R. of an LLP says a cat is ) for christmas etc.. but I think (a cat) is 4LIFE! æThelred is ready.")
        );
    }

    private static Stream<Arguments> endOfSentence() {
        return Stream.of(
                Arguments.of(null, TextFormatter.SentenceTerminationState.NOT_TERMINATED),
                Arguments.of("", TextFormatter.SentenceTerminationState.NOT_TERMINATED),
                Arguments.of(".", TextFormatter.SentenceTerminationState.NOT_TERMINATED),
                Arguments.of("a.", TextFormatter.SentenceTerminationState.NOT_TERMINATED),
                Arguments.of("a. ", TextFormatter.SentenceTerminationState.TERMINATED),
                Arguments.of("a'. ", TextFormatter.SentenceTerminationState.TERMINATED),
                Arguments.of("'a. ", TextFormatter.SentenceTerminationState.TERMINATED),
                Arguments.of("a.' ", TextFormatter.SentenceTerminationState.TERMINATED),
                Arguments.of("a.b ", TextFormatter.SentenceTerminationState.NOT_TERMINATED),
                Arguments.of("b.sc ", TextFormatter.SentenceTerminationState.NOT_TERMINATED),
                Arguments.of("a.b! ", TextFormatter.SentenceTerminationState.TERMINATED),
                Arguments.of("a.b? ", TextFormatter.SentenceTerminationState.TERMINATED),
                Arguments.of("a.) ", TextFormatter.SentenceTerminationState.TERMINATED_WITH_BRACKET),
                Arguments.of("a.) ", TextFormatter.SentenceTerminationState.TERMINATED_WITH_BRACKET),
                Arguments.of("a). ", TextFormatter.SentenceTerminationState.TERMINATED)
        );
    }

    private static Stream<Arguments> possessiveState() {
        return Stream.of(
                Arguments.of(null, TextFormatter.NON_POSSESSIVE),
                Arguments.of("", TextFormatter.NON_POSSESSIVE),
                Arguments.of("I", new TextFormatter.Possessiveness(true, false, false)),
                Arguments.of("I.", new TextFormatter.Possessiveness(true, false, true)),
                Arguments.of("I?", new TextFormatter.Possessiveness(true, false, true)),
                Arguments.of("I!", new TextFormatter.Possessiveness(true, false, true)),
                Arguments.of("-I", new TextFormatter.Possessiveness(true, false, false)),
                Arguments.of("you", TextFormatter.NON_POSSESSIVE),
                Arguments.of("(I", new TextFormatter.Possessiveness(true, true, false)),
                Arguments.of("[I", new TextFormatter.Possessiveness(true, true, false)),
                Arguments.of("[I.", new TextFormatter.Possessiveness(true, true, true)),
                Arguments.of("I.(", new TextFormatter.Possessiveness(true, false, true)),
                Arguments.of("I(.", new TextFormatter.Possessiveness(true, false, true)),
                Arguments.of("I(.", new TextFormatter.Possessiveness(true, false, true)),
                Arguments.of(".I", new TextFormatter.Possessiveness(true, false, false)),
                Arguments.of("sublime", TextFormatter.NON_POSSESSIVE),
                Arguments.of("I..", new TextFormatter.Possessiveness(true, false, true)),
                Arguments.of("II", TextFormatter.NON_POSSESSIVE)
        );
    }
}
