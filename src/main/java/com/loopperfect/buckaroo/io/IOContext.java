package com.loopperfect.buckaroo.io;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.nio.file.*;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.loopperfect.buckaroo.Either;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface IOContext
    extends GitContext, SystemContext, FSContext {

    public static IOContext actual() {
        return new IOContext(){
            private final FileSystem fs = FileSystems.getDefault();

            @Override
            public FileSystem getFS() {
                return fs;
            }
        };
    }

    public static IOContext fake() {
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


