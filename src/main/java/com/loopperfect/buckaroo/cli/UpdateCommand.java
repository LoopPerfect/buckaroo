package com.loopperfect.buckaroo.cli;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.BuckarooException;
import com.loopperfect.buckaroo.Identifier;
import com.loopperfect.buckaroo.Routine;

import java.util.Objects;
import java.util.Optional;

public final class UpdateCommand implements CLICommand {

    public final Optional<Identifier> project;

    private UpdateCommand(final Optional<Identifier>  project) {
        this.project = Preconditions.checkNotNull(project);
    }

    @Override
    public Routine routine() {
        return () -> {
            throw new BuckarooException("Not implemented yet! ");
        };
    }

    @Override
    public int hashCode() {
        return Objects.hash(project);
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) {
            return true;
        }

        if (obj == null || !(obj instanceof UpdateCommand)) {
            return false;
        }

        final UpdateCommand other = (UpdateCommand) obj;

        return Objects.equals(project, other.project);
    }

    public static UpdateCommand of(final Optional<Identifier> project) {
        return new UpdateCommand(project);
    }

    public static UpdateCommand of(final Identifier project) {
        return new UpdateCommand(Optional.of(project));
    }

    public static UpdateCommand of() {
        return new UpdateCommand(Optional.empty());
    }
}
