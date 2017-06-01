package com.loopperfect.buckaroo.cli;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.RecipeIdentifier;
import com.loopperfect.buckaroo.SemanticVersionRequirement;
import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;
import com.loopperfect.buckaroo.routines.Install;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class InstallCommand implements CLICommand {

    public final ImmutableMap<RecipeIdentifier, Optional<SemanticVersionRequirement>> targets;

    private InstallCommand(final ImmutableMap<RecipeIdentifier, Optional<SemanticVersionRequirement>> targets) {
        this.targets = Preconditions.checkNotNull(targets);
    }

    @Override
    public IO<Unit> routine() {
        return Install.routine(targets);
    }

    @Override
    public int hashCode() {
        return Objects.hash(targets);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof InstallCommand)) {
            return false;
        }

        final InstallCommand other = (InstallCommand) obj;

        return Objects.equals(targets, other.targets);
    }

    public static InstallCommand of(final RecipeIdentifier project, final Optional<SemanticVersionRequirement> versionRequirement) {
        return new InstallCommand(ImmutableMap.of(project, versionRequirement));
    }

    public static InstallCommand of(final RecipeIdentifier project, final SemanticVersionRequirement versionRequirement) {
        return new InstallCommand(ImmutableMap.of(project, Optional.of(versionRequirement)));
    }

    public static InstallCommand of(final RecipeIdentifier project) {
        return new InstallCommand(ImmutableMap.of(project, Optional.empty()));
    }

    public static InstallCommand of(final List<Map.Entry<RecipeIdentifier,Optional<SemanticVersionRequirement>>> projects) {
        return new InstallCommand(
                projects.stream()
                        .collect(ImmutableMap.toImmutableMap( Map.Entry::getKey, Map.Entry::getValue )));
    }
}
