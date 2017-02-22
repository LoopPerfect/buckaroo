package com.loopperfect.buckaroo.io;

import java.nio.file.*;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;


public interface IOContext
    extends GitContext, ConsoleContext, FSContext {

    static IOContext actual() {
        return new IOContext(){
            private final FileSystem fs = FileSystems.getDefault();

            @Override
            public FileSystem getFS() {
                return fs;
            }
        };
    }

    static IOContext fake() {
        return new IOContext() {
            private final FileSystem fs = Jimfs
                .newFileSystem(Configuration.unix());

            @Override
            public FileSystem getFS() {
                return fs;
            }
        };
    }
}


