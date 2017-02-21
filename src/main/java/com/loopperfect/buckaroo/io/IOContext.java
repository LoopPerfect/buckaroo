package com.loopperfect.buckaroo.io;

import java.nio.file.*;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;


public interface IOContext {

    FSContext fs();
    GitContext git();
    ConsoleContext console();

    static IOContext create(final FSContext fs, final GitContext git, final ConsoleContext console) {
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

    public static IOContext actual() {
        return create(
            FSContext.actual(),
            GitContext.actual(),
            ConsoleContext.actual()
        );
    }

    public static IOContext fake() {
        return create(
            FSContext.fake(),
            GitContext.fake(),
            ConsoleContext.fake()
        );
    }
}


