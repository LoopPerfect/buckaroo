package com.loopperfect.buckaroo.parsing;

import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.SemanticVersion;
import org.jparsec.Parser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class BuckarooParsersTest {

    @Test
    public void testSemanticVersionParser() {

        final Parser<SemanticVersion> parser = BuckarooParsers.semanticVersionParser;

        assertEquals(SemanticVersion.of(123), parser.parse("123"));
        assertEquals(SemanticVersion.of(1, 2), parser.parse("1.2"));
        assertEquals(SemanticVersion.of(1, 2, 3), parser.parse("1.2.3"));
    }

    @Test
    public void testVersionRequirementTokenizer1() {

        final Parser<ImmutableList<Token>> parser = BuckarooParsers.versionRequirementTokenizer;

        assertEquals(ImmutableList.of(SemanticVersionToken.of(SemanticVersion.of(1))), parser.parse("1"));
        assertEquals(ImmutableList.of(AtLeastToken.of()), parser.parse(">= "));
        assertEquals(ImmutableList.of(EqualsToken.of()), parser.parse("  =  "));
        assertEquals(ImmutableList.of(AtMostToken.of()), parser.parse("  <=  "));
        assertEquals(ImmutableList.of(DashToken.of()), parser.parse("  -  "));
        assertEquals(ImmutableList.of(OpenListToken.of()), parser.parse(" [   "));
        assertEquals(ImmutableList.of(CloseListToken.of()), parser.parse("]  "));
        assertEquals(ImmutableList.of(CommaToken.of()), parser.parse(","));
        assertEquals(ImmutableList.of(WildCardToken.of()), parser.parse("  *"));
        assertEquals(ImmutableList.of(OpenListToken.of(), CloseListToken.of()), parser.parse("[]"));
    }

    @Test
    public void testVersionRequirementTokenizer2() {

        final Parser<ImmutableList<Token>> parser = BuckarooParsers.versionRequirementTokenizer;

        final ImmutableList<Token> expected = ImmutableList.of(
                AtLeastToken.of(),
                SemanticVersionToken.of(SemanticVersion.of(7, 2)));

        final ImmutableList<Token> actual = parser.parse(" >= 7.2");

        assertEquals(expected, actual);
    }

    @Test
    public void testVersionRequirementTokenizer3() {

        final Parser<ImmutableList<Token>> parser = BuckarooParsers.versionRequirementTokenizer;

        final ImmutableList<Token> expected = ImmutableList.of(
                OpenListToken.of(),
                SemanticVersionToken.of(SemanticVersion.of(7, 2)),
                CommaToken.of(),
                SemanticVersionToken.of(SemanticVersion.of(7, 3)),
                CommaToken.of(),
                SemanticVersionToken.of(SemanticVersion.of(8)),
                CloseListToken.of());

        final ImmutableList<Token> actual = parser.parse(" [  7.2  , 7.3, 8]");

        assertEquals(expected, actual);
    }
}