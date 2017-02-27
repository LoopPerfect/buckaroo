package com.loopperfect.buckaroo.versioning;

import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.*;
import org.jparsec.Parser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public final class VersioningParsersTest {

    @Test
    public void testSemanticVersionParser() {

        final Parser<SemanticVersion> parser = VersioningParsers.semanticVersionParser;

        assertEquals(SemanticVersion.of(123), parser.parse("123"));
        assertEquals(SemanticVersion.of(1, 2), parser.parse("1.2"));
        assertEquals(SemanticVersion.of(1, 2, 3), parser.parse("1.2.3"));
    }

    @Test
    public void testVersionRequirementTokenizer1() {

        final Parser<ImmutableList<Token>> parser = VersioningParsers.versionRequirementTokenizer;

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

        final Parser<ImmutableList<Token>> parser = VersioningParsers.versionRequirementTokenizer;

        final ImmutableList<Token> expected = ImmutableList.of(
            AtLeastToken.of(),
            SemanticVersionToken.of(SemanticVersion.of(7, 2)));

        final ImmutableList<Token> actual = parser.parse(" >= 7.2");

        assertEquals(expected, actual);
    }

    @Test
    public void testVersionRequirementTokenizer3() {

        final Parser<ImmutableList<Token>> parser = VersioningParsers.versionRequirementTokenizer;

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

    @Test
    public void testSemanticVersionRequirementParser() {

        final Parser<SemanticVersionRequirement> parser = VersioningParsers.semanticVersionRequirementParser;

        assertEquals(AnySemanticVersion.of(), parser.parse("*"));
        assertEquals(AnySemanticVersion.of(), parser.parse("  *  "));

        assertEquals(ExactSemanticVersion.of(SemanticVersion.of(1, 2, 3)), parser.parse("1.2.3"));
        assertEquals(ExactSemanticVersion.of(SemanticVersion.of(2, 4)), parser.parse("  2.4"));

        assertEquals(ExactSemanticVersion.of(SemanticVersion.of(1, 1)), parser.parse("=1.1"));
        assertEquals(ExactSemanticVersion.of(SemanticVersion.of(7)), parser.parse("  =  7 "));

        assertEquals(
            ExactSemanticVersion.of(SemanticVersion.of(1, 1), SemanticVersion.of(1, 2)),
            parser.parse(" [ 1.1, 1.2  ] "));

        assertEquals(
            BoundedSemanticVersion.of(SemanticVersion.of(4, 7), AboveOrBelow.ABOVE),
            parser.parse(">=4.7"));

        assertEquals(
            BoundedSemanticVersion.of(SemanticVersion.of(6, 5, 1), AboveOrBelow.BELOW),
            parser.parse("<=6.5.1"));

        assertEquals(
            SemanticVersionRange.of(SemanticVersion.of(7, 2), SemanticVersion.of(9)),
            parser.parse("  7.2 - 9  "));

        assertEquals(
            SemanticVersionRange.of(SemanticVersion.of(1, 0, 1), SemanticVersion.of(4, 3)),
            parser.parse("1.0.1-4.3"));
    }
}
