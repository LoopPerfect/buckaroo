package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;
import com.loopperfect.buckaroo.tasks.ResolveTasks;
import io.reactivex.schedulers.Schedulers;

public final class ResolveCommand implements CLICommand {

    private ResolveCommand() {

    }

    @Override
    public IO<Unit> routine() {
        return context -> {
            System.out.println("Resolve. ");
            ResolveTasks.resolveDependenciesInWorkingDirectory(context.fs().getFS())
                .subscribeOn(Schedulers.from(context.executor()))
                .subscribe(
                    next -> {
                        System.out.println(next);
                    },
                    error -> {
                        error.printStackTrace();
                    },
                    () -> {
                        System.out.println("Done. ");
                    });

            return Unit.of();
        };
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof ResolveCommand;
    }

    public static ResolveCommand of() {
        return new ResolveCommand();
    }
}
