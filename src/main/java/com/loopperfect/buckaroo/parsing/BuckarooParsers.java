package com.loopperfect.buckaroo.parsing;

import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.SemanticVersion;
import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;

public final class BuckarooParsers {

    private BuckarooParsers() {

    }

    static final Parser<Integer> integerParser = Scanners.INTEGER
            .map(x -> Integer.parseUnsignedInt(x));

    static final Parser<SemanticVersion> semanticVersionParser1 =
            integerParser.map(x -> SemanticVersion.of(x));

    static final Parser<SemanticVersion> semanticVersionParser2 =
            Parsers.sequence(
                    integerParser.followedBy(Scanners.isChar('.')),
                    integerParser,
                    (x, y) -> SemanticVersion.of(x, y));

    static final Parser<SemanticVersion> semanticVersionParser3 =
            Parsers.sequence(
                    integerParser.followedBy(Scanners.isChar('.')),
                    integerParser.followedBy(Scanners.isChar('.')),
                    integerParser,
                    (x, y, z) -> SemanticVersion.of(x, y, z));

    static final Parser<EqualsToken> equalsTokenParser =
            Scanners.string("=").map(x -> EqualsToken.of());

    static final Parser<AtLeastToken> atLeastTokenParser =
            Scanners.string(">=").map(x -> AtLeastToken.of());

    static final Parser<AtMostToken> atMostTokenParser =
            Scanners.string("<=").map(x -> AtMostToken.of());

    static final Parser<OpenListToken> openListTokenParser =
            Scanners.string("[").map(x -> OpenListToken.of());

    static final Parser<CloseListToken> closeListTokenParser =
            Scanners.string("]").map(x -> CloseListToken.of());

    static final Parser<CommaToken> commaTokenParser =
            Scanners.string(",").map(x -> CommaToken.of());

    static final Parser<DashToken> dashTokenParser =
            Scanners.string("-").map(x -> DashToken.of());

    static final Parser<WildCardToken> wildCardTokenParser =
            Scanners.string("*").map(x -> WildCardToken.of());

    public static final Parser<SemanticVersion> semanticVersionParser = Parsers.or(
            semanticVersionParser3,
            semanticVersionParser2,
            semanticVersionParser1); // Order reversed so that we fallback properly

    public static final Parser<ImmutableList<Token>> versionRequirementTokenizer =
            Scanners.WHITESPACES.skipMany().next(
                    Parsers.or(
                            equalsTokenParser,
                            atLeastTokenParser,
                            atMostTokenParser,
                            openListTokenParser,
                            closeListTokenParser,
                            commaTokenParser,
                            dashTokenParser,
                            wildCardTokenParser,
                            semanticVersionParser.map(SemanticVersionToken::of))
                            .sepEndBy(Scanners.WHITESPACES.skipMany()))
                    .map(x -> ImmutableList.copyOf(x));
}
