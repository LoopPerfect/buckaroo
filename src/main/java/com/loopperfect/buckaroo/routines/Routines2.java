package com.loopperfect.buckaroo.routines;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.io.IO;
import com.loopperfect.buckaroo.io.IOContext;
import com.loopperfect.buckaroo.serialization.Serializers;

import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.Path;

public final class Routines2 {

    private Routines2() {

    }

    private static final IO<Path> getBuckarooPath = context ->
            Paths.get(context.getUserHomeDirectory().toString(), "/.buckaroo/");

    private static final IO<Path> getGlobalConfigPath = getBuckarooPath
            .map(path -> Paths.get(path.toString(), "config.json"));

    private static final IO<Path> getProjectFilePath = context ->
            Paths.get(context.getWorkingDirectory().toString(), "buckaroo.json");

    private static final IO<Either<IOException, Project>> loadProject = getProjectFilePath
            .flatMap(IO::readFile)
            .map(fileRead -> fileRead.rightProjection(x -> Serializers.gson().fromJson(x, Project.class)));

    private static final IO<Either<IOException, BuckarooConfig>> loadConfig = getGlobalConfigPath
            .flatMap(IO::readFile)
            .map(fileRead -> fileRead.rightProjection(x -> Serializers.gson().fromJson(x, BuckarooConfig.class)));

    private static final IO<ImmutableList<Either<IOException, Recipe>>> readCookBookFromCache(final RemoteCookBook cookBook) {
        Preconditions.checkNotNull(cookBook);
        return getBuckarooPath
                .map(path -> Paths.get(path.toString(), cookBook.name.name))
                .flatMap(path -> context -> context.listFiles(path))
                .
    }

//    private static final IO<ImmutableList<Either<IOException, CookBook>>> loadCookBooks(final BuckarooConfig config) {
//        Preconditions.checkNotNull(config);
//        return context -> {
//            config.cookBooks.
//        };
//    }
}
