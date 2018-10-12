package com.loopperfect.buckaroo.versioning;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.loopperfect.buckaroo.*;
import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;
import org.jparsec.pattern.CharPredicates;

public final class VersioningParsers {

    private VersioningParsers() {

    }

    static final Parser<?> ignoreParser = Scanners.WHITESPACES.skipMany();

    static final Parser<Integer> integerParser = Scanners.INTEGER
        .map(Integer::parseUnsignedInt);

    static final Parser<SemanticVersion> semanticVersionParser1 =
        integerParser.map(SemanticVersion::of);

    static final Parser<Void> semanticVersionSep = Scanners.among("._");

    static final Parser<SemanticVersion> semanticVersionParser2 =
        Parsers.sequence(
            integerParser.followedBy(semanticVersionSep),
            integerParser,
            SemanticVersion::of);

    static final Parser<SemanticVersion> semanticVersionParser3 =
        Parsers.sequence(
            integerParser.followedBy(semanticVersionSep),
            integerParser.followedBy(semanticVersionSep),
            integerParser,
            SemanticVersion::of);

    static final Parser<SemanticVersion> semanticVersionParser4 =
        Parsers.sequence(
            integerParser.followedBy(semanticVersionSep),
            integerParser.followedBy(semanticVersionSep),
            integerParser.followedBy(semanticVersionSep),
            integerParser,
            SemanticVersion::of);

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

    static final Parser<SemanticVersion> semanticVersionParserClean = Parsers.or(
        semanticVersionParser4,
        semanticVersionParser3,
        semanticVersionParser2,
        semanticVersionParser1);

    public static final Parser<SemanticVersion> semanticVersionParser = Scanners.many(CharPredicates.IS_ALPHA_)
        .next(semanticVersionParserClean)
        .between(Scanners.WHITESPACES.skipMany(), Scanners.WHITESPACES.skipMany());

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
            .map(ImmutableList::copyOf);

    static final Parser<AnySemanticVersion> anySemanticVersionParser =
        wildCardTokenParser.between(ignoreParser, ignoreParser).map(x -> AnySemanticVersion.of());

    public static final Parser<ImmutableList<SemanticVersion>> semanticVersionListParser =
        openListTokenParser.between(ignoreParser, ignoreParser).next(
            semanticVersionParser
                .between(ignoreParser, ignoreParser)
                .sepBy(commaTokenParser.between(ignoreParser, ignoreParser)))
            .followedBy(closeListTokenParser.between(ignoreParser, ignoreParser))
            .between(ignoreParser, ignoreParser)
            .map(ImmutableList::copyOf);

    static final Parser<ExactSemanticVersion> exactSemanticVersionParser =
        Parsers.or(
            semanticVersionParser.between(ignoreParser, ignoreParser).map(ExactSemanticVersion::of),
            Parsers.sequence(
                equalsTokenParser.between(ignoreParser, ignoreParser),
                semanticVersionParser.between(ignoreParser, ignoreParser).map(ExactSemanticVersion::of)),
            semanticVersionListParser.map(x -> ExactSemanticVersion.of(ImmutableSet.copyOf(x))));

    static final Parser<BoundedSemanticVersion> atLeastSemanticVersionParser =
        atLeastTokenParser.between(ignoreParser, ignoreParser)
            .next(semanticVersionParser.between(ignoreParser, ignoreParser))
            .map(x -> BoundedSemanticVersion.of(x, AboveOrBelow.ABOVE));

    static final Parser<BoundedSemanticVersion> atMostSemanticVersionParser =
        atMostTokenParser.between(ignoreParser, ignoreParser)
            .next(semanticVersionParser.between(ignoreParser, ignoreParser))
            .map(x -> BoundedSemanticVersion.of(x, AboveOrBelow.BELOW));

    static final Parser<SemanticVersionRange> semanticVersionRangeParser =
        Parsers.sequence(
            semanticVersionParser.followedBy(dashTokenParser.between(ignoreParser, ignoreParser)),
            semanticVersionParser,
            SemanticVersionRange::of)
            .between(ignoreParser, ignoreParser);

    public static final Parser<SemanticVersionRequirement> semanticVersionRequirementParser =
        Parsers.longest(
            anySemanticVersionParser,
            exactSemanticVersionParser,
            atLeastSemanticVersionParser,
            atMostSemanticVersionParser,
            semanticVersionRangeParser);
}
