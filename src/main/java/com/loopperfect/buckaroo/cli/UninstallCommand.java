package com.loopperfect.buckaroo.cli;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.Either;
import com.loopperfect.buckaroo.Identifier;
import com.loopperfect.buckaroo.RecipeIdentifier;
import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;
import com.loopperfect.buckaroo.routines.Uninstall;

import java.util.Objects;

public final class UninstallCommand implements CLICommand {

    public final Either<Identifier, RecipeIdentifier> project;

    private UninstallCommand(final Either<Identifier, RecipeIdentifier> project) {
        Preconditions.checkNotNull(project);
        this.project = project;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof UninstallCommand)) {
            return false;
        }
        final UninstallCommand other = (UninstallCommand) obj;
        return Objects.equals(project, other.project);
    }

    @Override
    public int hashCode() {
        return Objects.hash(project);
    }

    @Override
    public IO<Unit> routine() {
        return Uninstall.routine(project);
    }

    public static UninstallCommand of(final Either<Identifier, RecipeIdentifier> project) {
        return new UninstallCommand(project);
    }

    public static UninstallCommand of(final Identifier project) {
        return new UninstallCommand(Either.left(project));
    }

    public static UninstallCommand of(final RecipeIdentifier project) {
        return new UninstallCommand(Either.right(project));
    }
}
