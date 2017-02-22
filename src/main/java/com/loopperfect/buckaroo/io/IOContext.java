package com.loopperfect.buckaroo.io;

import com.google.common.base.Preconditions;

public interface IOContext {

    FSContext fs();
    GitContext git();
    ConsoleContext console();

    static IOContext of(final FSContext fs, final GitContext git, final ConsoleContext console) {
        Preconditions.checkNotNull(fs);
        Preconditions.checkNotNull(git);
        Preconditions.checkNotNull(console);
        return new IOContext() {
            @Override
            public FSContext fs() {
                return fs;
            }

            @Override
            public GitContext git() {
                return git;
            }

            @Override
            public ConsoleContext console() {
                return console;
            }
        };
    }

    static IOContext actual() {
        return of(
                FSContext.actual(),
                GitContext.actual(),
                ConsoleContext.actual());
    }

    static IOContext fake() {
        return of(
                FSContext.fake(),
                GitContext.fake(),
                ConsoleContext.fake());
    }
}


