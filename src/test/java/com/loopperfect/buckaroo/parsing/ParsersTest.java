package com.loopperfect.buckaroo.parsing;

import com.loopperfect.buckaroo.SemanticVersion;
import org.jparsec.Parser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class ParsersTest {

    @Test
    public void testSemanticVersionParser() {

        final Parser<SemanticVersion> parser = Parsers.semanticVersionParser;

        assertEquals(SemanticVersion.of(123), parser.parse("123"));
        assertEquals(SemanticVersion.of(1, 2), parser.parse("1.2"));
        assertEquals(SemanticVersion.of(1, 2, 3), parser.parse("1.2.3"));
    }
}