package com.loopperfect.buckaroo.parsing;

import com.loopperfect.buckaroo.SemanticVersion;
import org.jparsec.Parser;
import org.jparsec.Scanners;

public final class Parsers {

    private Parsers() {

    }

    static final Parser<Integer> integerParser = Scanners.INTEGER
            .map(x -> Integer.parseUnsignedInt(x));

    static final Parser<SemanticVersion> semanticVersionParser1 =
            integerParser.map(x -> SemanticVersion.of(x));

    static final Parser<SemanticVersion> semanticVersionParser2 =
            org.jparsec.Parsers.sequence(
                    integerParser.followedBy(Scanners.isChar('.')),
                    integerParser,
                    (x, y) -> SemanticVersion.of(x, y));

    static final Parser<SemanticVersion> semanticVersionParser3 =
            org.jparsec.Parsers.sequence(
                    integerParser.followedBy(Scanners.isChar('.')),
                    integerParser.followedBy(Scanners.isChar('.')),
                    integerParser,
                    (x, y, z) -> SemanticVersion.of(x, y, z));

    public static final Parser<SemanticVersion> semanticVersionParser = org.jparsec.Parsers.or(
            semanticVersionParser3,
            semanticVersionParser2,
            semanticVersionParser1); // Order reversed so that we fallback properly
}
