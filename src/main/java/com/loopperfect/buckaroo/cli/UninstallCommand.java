package com.loopperfect.buckaroo.cli;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.Identifier;
import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;
import com.loopperfect.buckaroo.routines.Routines;

import java.util.Objects;

public final class UninstallCommand implements CLICommand {

    public final Identifier project;

    private UninstallCommand(final Identifier project) {
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
        return null;
    }

    public static UninstallCommand of(final Identifier project) {
        return new UninstallCommand(project);
    }
}
