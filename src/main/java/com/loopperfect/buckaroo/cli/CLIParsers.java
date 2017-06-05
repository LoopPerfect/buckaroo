package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.Identifier;
import com.loopperfect.buckaroo.PartialDependency;
import com.loopperfect.buckaroo.PartialRecipeIdentifier;
import com.loopperfect.buckaroo.RecipeIdentifier;
import com.loopperfect.buckaroo.versioning.AnySemanticVersion;
import com.loopperfect.buckaroo.versioning.VersioningParsers;
import org.jparsec.Parser;
import org.jparsec.Parsers;
import org.jparsec.Scanners;
import org.jparsec.pattern.CharPredicates;

public final class CLIParsers {

    private CLIParsers() {

    }

    static final Parser<Void> ignoreParser =
        Scanners.WHITESPACES.skipMany();

    public static final Parser<Identifier> identifierParser =
            Scanners.isChar(CharPredicates.IS_ALPHA_NUMERIC).times(1).followedBy(
                    Scanners.isChar(CharPredicates.or(
                            CharPredicates.IS_ALPHA_NUMERIC,
                            CharPredicates.among("-_"))).times(1, 29))
                    .source()
                    .map(Identifier::of);

    public static final Parser<RecipeIdentifier> recipeIdentifierParser =
        Parsers.sequence(
            identifierParser.followedBy(Scanners.isChar('/')),
            identifierParser,
            RecipeIdentifier::of);

    public static final Parser<PartialDependency> partialDependencyParser =
        Parsers.sequence(
            identifierParser.between(ignoreParser, ignoreParser).followedBy(Scanners.isChar(CharPredicates.among("+")))
                .between(ignoreParser, ignoreParser).asOptional(),
            identifierParser.between(ignoreParser, ignoreParser).followedBy(Scanners.isChar(CharPredicates.among("/")))
                .between(ignoreParser, ignoreParser),
            identifierParser.between(ignoreParser, ignoreParser),
            VersioningParsers.semanticVersionRequirementParser
                .between(ignoreParser, ignoreParser).asOptional(),
            PartialDependency::of);

    public static final Parser<PartialRecipeIdentifier> partialRecipeIdentifierParser =
        Parsers.sequence(
            identifierParser.between(ignoreParser, ignoreParser).followedBy(Scanners.isChar(CharPredicates.among("+")))
                .between(ignoreParser, ignoreParser).asOptional(),
            identifierParser.between(ignoreParser, ignoreParser).followedBy(Scanners.isChar(CharPredicates.among("/")))
                .between(ignoreParser, ignoreParser).asOptional(),
            identifierParser.between(ignoreParser, ignoreParser),
            PartialRecipeIdentifier::of);

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

    static final Parser<Void> resolveTokenParser =
        Scanners.stringCaseInsensitive("resolve");

    static final Parser<RecipesCommand> recipesCommandParser =
            recipesTokenParser.between(ignoreParser, ignoreParser)
                    .map(x -> RecipesCommand.of());

    static final Parser<InstallExistingCommand> installExistingCommandParser =
        installTokenParser
            .between(ignoreParser, ignoreParser)
            .map(x -> InstallExistingCommand.of());

    static final Parser<InstallCommand> installCommandParser =
        installTokenParser.between(ignoreParser, ignoreParser)
        .next(partialDependencyParser.atLeast(1))
        .map(InstallCommand::of);

    static final Parser<UninstallCommand> uninstallCommandParser =
        uninstallTokenParser.followedBy(Scanners.WHITESPACES.atLeast(1))
            .next(partialRecipeIdentifierParser)
            .between(ignoreParser, ignoreParser)
            .map(UninstallCommand::of);

    static final Parser<UpdateCommand> updateCommandParser =
        updateTokenParser.between(ignoreParser, ignoreParser).map(ignored -> UpdateCommand.of());

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

    static final Parser<ResolveCommand> resolveCommandParser =
        resolveTokenParser.between(ignoreParser, ignoreParser)
            .map(x -> ResolveCommand.of());

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
            helpCommandParser,
            resolveCommandParser);
}
