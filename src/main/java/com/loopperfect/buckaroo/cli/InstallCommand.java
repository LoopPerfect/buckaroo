package com.loopperfect.buckaroo.cli;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.Identifier;
import com.loopperfect.buckaroo.SemanticVersionRequirement;

import java.util.Objects;
import java.util.Optional;

public final class InstallCommand implements CLICommand {

    public final Identifier project;
    public final Optional<SemanticVersionRequirement> versionRequirement;

    private InstallCommand(final Identifier project, final Optional<SemanticVersionRequirement> versionRequirement) {
        this.project = Preconditions.checkNotNull(project);
        this.versionRequirement = Preconditions.checkNotNull(versionRequirement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(project, versionRequirement);
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

        return Objects.equals(project, other.project) &&
                Objects.equals(versionRequirement, other.versionRequirement);
    }

    public static InstallCommand of(final Identifier project, final Optional<SemanticVersionRequirement> versionRequirement) {
        return new InstallCommand(project, versionRequirement);
    }

    public static InstallCommand of(final Identifier project, final SemanticVersionRequirement versionRequirement) {
        return new InstallCommand(project, Optional.of(versionRequirement));
    }

    public static InstallCommand of(final Identifier project) {
        return new InstallCommand(project, Optional.empty());
    }
}
