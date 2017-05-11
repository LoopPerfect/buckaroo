package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.Identifier;
import com.loopperfect.buckaroo.RecipeIdentifier;
import com.loopperfect.buckaroo.versioning.VersioningParsers;
import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;
import org.jparsec.pattern.CharPredicates;

public final class CLIParsers {

    private CLIParsers() {

    }

    public static final Parser<Identifier> identifierParser =
            Scanners.isChar(CharPredicates.IS_ALPHA_NUMERIC).times(1).followedBy(
                    Scanners.isChar(CharPredicates.or(
                            CharPredicates.IS_ALPHA_NUMERIC,
                            CharPredicates.among("-_+"))).times(1, 29))
                    .source()
                    .map(Identifier::of);

    public static final Parser<RecipeIdentifier> recipeIdentifierParser =
        Parsers.sequence(
            identifierParser.followedBy(Scanners.isChar('/')),
            identifierParser,
            RecipeIdentifier::of);

    static final Parser<Void> initTokenParser =
            Scanners.stringCaseInsensitive("init");

    static final Parser<Void> upgradeTokenParser =
            Scanners.stringCaseInsensitive("upgrade");

    static final Parser<Void> installTokenParser =
            Scanners.stringCaseInsensitive("install");

    static final Parser<Void> uninstallTokenParser =
            Scanners.stringCaseInsensitive("uninstall");

    static final Parser<Void> updateTokenParser =
            Scanners.stringCaseInsensitive("update");

    static final Parser<Void> libraryTokenParser =
            Scanners.stringCaseInsensitive("library");

    static final Parser<Void> recipesTokenParser =
            Scanners.stringCaseInsensitive("organizations");

    static final Parser<Void> generateTokenParser =
            Scanners.stringCaseInsensitive("generate");

    static final Parser<Void> cookbooksTokenParser =
            Scanners.stringCaseInsensitive("cookbooks");

    static final Parser<Void> dependenciesTokenParser =
            Scanners.stringCaseInsensitive("dependencies");

    static final Parser<Void> versionTokenParser =
            Scanners.stringCaseInsensitive("version");

    static final Parser<Void> quickstartTokenParser =
            Scanners.stringCaseInsensitive("quickstart");

    static final Parser<Void> helpTokenParser =
        Scanners.stringCaseInsensitive("help");

    static final Parser<Void> ignoreParser =
            Scanners.WHITESPACES.skipMany();

    static final Parser<RecipesCommand> recipesCommandParser =
            recipesTokenParser.between(ignoreParser, ignoreParser)
                    .map(x -> RecipesCommand.of());

    static final Parser<InstallExistingCommand> installExistingCommandParser =
            installTokenParser
                    .between(ignoreParser, ignoreParser)
                    .map(x -> InstallExistingCommand.of());

    static final Parser<InstallCommand> installCommandParser =
            Parsers.longest(
                Parsers.sequence(
                        installTokenParser
                                .followedBy(Scanners.WHITESPACES.atLeast(1)),
                    recipeIdentifierParser,
                        (x, y) -> InstallCommand.of(y))
                        .between(ignoreParser, ignoreParser),
                Parsers.sequence(
                        installTokenParser
                                .followedBy(Scanners.WHITESPACES.atLeast(1)),
                    recipeIdentifierParser,
                        VersioningParsers.semanticVersionRequirementParser,
                        (x, y, z) -> InstallCommand.of(y, z))
                        .between(ignoreParser, ignoreParser));

    static final Parser<UninstallCommand> uninstallCommandParser =
        Parsers.longest(
            Parsers.sequence(uninstallTokenParser
                    .followedBy(Scanners.WHITESPACES.atLeast(1)),
                identifierParser,
                (x, y) -> UninstallCommand.of(y)).between(ignoreParser, ignoreParser),
            Parsers.sequence(uninstallTokenParser
                    .followedBy(Scanners.WHITESPACES.atLeast(1)),
                recipeIdentifierParser,
                (x, y) -> UninstallCommand.of(y)).between(ignoreParser, ignoreParser));

    static final Parser<UpdateCommand> updateCommandParser =
            Parsers.longest(
                updateTokenParser.followedBy(Scanners.WHITESPACES.atLeast(1))
                        .next(identifierParser)
                        .between(ignoreParser, ignoreParser)
                        .map(x -> UpdateCommand.of(x)),
                updateTokenParser
                        .between(ignoreParser, ignoreParser)
                        .map(x -> UpdateCommand.of()));

    static final Parser<InitCommand> initCommandParser =
            initTokenParser.between(ignoreParser, ignoreParser)
                    .map(x -> InitCommand.of());

    static final Parser<UpgradeCommand> upgradeCommandParser =
            upgradeTokenParser.between(ignoreParser, ignoreParser)
                    .map(x -> UpgradeCommand.of());

    static final Parser<GenerateCommand> generateCommandParser =
            generateTokenParser.between(ignoreParser, ignoreParser)
                    .map(x -> GenerateCommand.of());

    static final Parser<CookbooksCommand> cookbooksCommandParser =
            cookbooksTokenParser.between(ignoreParser, ignoreParser)
                    .map(x -> CookbooksCommand.of());

    static final Parser<DependenciesCommand> dependenciesCommandParser =
            dependenciesTokenParser.between(ignoreParser, ignoreParser)
                    .map(x -> DependenciesCommand.of());

    static final Parser<VersionCommand> versionCommandParser =
            versionTokenParser.between(ignoreParser, ignoreParser)
                    .map(x -> VersionCommand.of());

    static final Parser<QuickstartCommand> quickstartCommandParser =
            quickstartTokenParser.between(ignoreParser, ignoreParser)
                    .map(x -> QuickstartCommand.of());

    static final Parser<HelpCommand> helpCommandParser =
        helpTokenParser.between(ignoreParser, ignoreParser)
            .map(x -> HelpCommand.of());

    public static final Parser<CLICommand> commandParser =
        Parsers.longest(
            initCommandParser,
            upgradeCommandParser,
            installExistingCommandParser,
            installCommandParser,
            uninstallCommandParser,
            updateCommandParser,
            recipesCommandParser,
            generateCommandParser,
            cookbooksCommandParser,
            dependenciesCommandParser,
            versionCommandParser,
            quickstartCommandParser,
            helpCommandParser);
}
