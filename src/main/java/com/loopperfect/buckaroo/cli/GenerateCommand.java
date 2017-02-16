package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;
import com.loopperfect.buckaroo.routines.Routines;

public final class GenerateCommand implements CLICommand {

    private GenerateCommand() {

    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || (obj != null && obj instanceof GenerateCommand);
    }

    @Override
    public IO<Unit> routine() {
        return Routines.generateBuckFile;
    }

    public static GenerateCommand of() {
        return new GenerateCommand();
    }
}
