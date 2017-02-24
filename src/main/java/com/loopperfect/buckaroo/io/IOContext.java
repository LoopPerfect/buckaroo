package com.loopperfect.buckaroo.io;

import com.google.common.base.Preconditions;

public interface IOContext {

    FSContext fs();
    GitContext git();
    ConsoleContext console();
    HttpContext http();

    static IOContext of(final FSContext fs, final GitContext git, final ConsoleContext console, final HttpContext http) {

        Preconditions.checkNotNull(fs);
        Preconditions.checkNotNull(git);
        Preconditions.checkNotNull(console);
        Preconditions.checkNotNull(http);

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

            @Override
            public HttpContext http() {
                return http;
            }
        };
    }

    static IOContext actual() {
        return of(
                FSContext.actual(),
                GitContext.actual(),
                ConsoleContext.actual(),
                HttpContext.actual());
    }

    static IOContext fake() {
        return of(
                FSContext.fake(),
                GitContext.fake(),
                ConsoleContext.fake(),
                HttpContext.fake());
    }
}


