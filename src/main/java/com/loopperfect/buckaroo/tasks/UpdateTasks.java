package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.events.ReadConfigFileEvent;
import io.reactivex.Observable;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

public final class UpdateTasks {

    private UpdateTasks() {

    }

    private static Observable<Event> updateCookbook(final Path folder, final RemoteCookbook cookbook) {

        Preconditions.checkNotNull(folder);
        Preconditions.checkNotNull(cookbook);

        return MoreObservables.fromProcess(observer -> {

            final FileSystem fs = folder.getFileSystem();

            // First, ensure that the target folder exists and is not a file.
            if (Files.exists(folder)) {
                if (!Files.isDirectory(folder)) {
                    throw new IOException(folder + " is not a directory. ");
                }
            } else {
                Files.createDirectories(folder);
            }

            final Path cookbookFolder = fs.getPath(
                folder.toString(),
                cookbook.name.name);

            // If the cookbook folder already exists, then we should do a pull.
            if (Files.exists(cookbookFolder)) {

                observer.onNext(Notification.of(cookbookFolder + " already exists. "));

                // Ensure it is a folder
                if (!Files.isDirectory(cookbookFolder)) {
                    throw new IOException(cookbookFolder + " is not a directory. ");
                }

                // TODO: Find a work-around for the File API

                // Ensure that the remote is correct
                observer.onNext(Notification.of("Verifying the remote URL... "));

                final Repository repository = new FileRepositoryBuilder()
                    .setGitDir(fs.getPath(cookbookFolder.toString(), ".git").toFile())
                    .build();

                final String url = repository.getConfig().getString("remote", "origin", "url");

                if (!url.equalsIgnoreCase(cookbook.url)) {
                    throw new IOException("The remote of the cookbook at " + cookbookFolder + " does not match what was expected. " +
                        "Expected " + cookbook.url + " but found " + url);
                }

                // Check the status
                observer.onNext(Notification.of("Verifying the Git status... "));

                final Status status = Git.open(cookbookFolder.toFile()).status().call();

                if (!status.isClean()) {
                    throw new IOException(cookbookFolder + " is not clean. ");
                }

                // Do a pull!
                observer.onNext(Notification.of("Pulling the latest changes... "));

                Git.open(cookbookFolder.toFile()).pull().call();
            } else {

                observer.onNext(Notification.of(cookbookFolder + " does not already exist. "));

                // TODO: Find a work-around for the File API

                // Clone the cookbook
                observer.onNext(Notification.of("Cloning " + cookbook.url + "... "));

                Git.cloneRepository()
                    .setDirectory(cookbookFolder.toFile())
                    .setURI(cookbook.url)
                    .call();
            }
        });
    }

    public static Observable<Event> updateCookbooks(final FileSystem fs) {

        Preconditions.checkNotNull(fs);

        final Path buckarooFolder = fs.getPath(
            System.getProperty("user.home"),
            ".buckaroo");

        final Path configFilePath = fs.getPath(
            buckarooFolder.toString(),
            "buckaroo.json");

        return CommonTasks.readConfigFile(configFilePath)
            .toObservable()
            .compose(new PublishAndMergeTransformer<ReadConfigFileEvent, Event, Event>(event -> {

                final ImmutableMap<RemoteCookbook, Observable<Event>> updateCookbookTasks = event.config.cookbooks.stream()
                    .collect(ImmutableMap.toImmutableMap(i -> i, i -> updateCookbook(buckarooFolder, i)));

                return MoreObservables.zipMaps(updateCookbookTasks)
                    .map(x -> Notification.of(x.toString()));
            }));
    }
}
