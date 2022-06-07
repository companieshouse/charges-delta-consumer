package uk.gov.companieshouse.charges.delta.mapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextFormatterTest {

    @ParameterizedTest(name = "Map {0} to {1}")
    @MethodSource("entityNameFormatting")
    void testFormatAsEntityName(String input, String expected) {
        // when
        String actual = TextFormatter.formatAsEntityName(input);

        // then
        assertEquals(expected, actual);
    }

    private static Stream<Arguments> entityNameFormatting() {
        return Stream.of(
                Arguments.of(null, null),
                Arguments.of("", ""),
                Arguments.arguments("a", "A"),
                Arguments.arguments("à", "À"),
                Arguments.arguments("ab", "Ab"),
                Arguments.arguments("aB", "Ab"),
                Arguments.arguments("bread butter", "Bread Butter"),
                Arguments.arguments("bReAD BuTteR", "Bread Butter"),
                Arguments.arguments("bread and butter", "Bread and Butter"),
                Arguments.arguments("and or the", "And or The"),
                Arguments.arguments("King (of in the) Hill", "King (Of in The) Hill")
        );
    }
}
