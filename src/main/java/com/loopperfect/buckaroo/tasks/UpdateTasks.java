package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableTable;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.events.ReadConfigFileEvent;
import io.reactivex.Observable;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

public final class UpdateTasks {

    private UpdateTasks() {

    }

    private static Observable<Event> updateCookbook(final Path folder, final RemoteCookbook cookbook) {

        Preconditions.checkNotNull(folder);
        Preconditions.checkNotNull(cookbook);

        return Observable.create(emitter -> {

            try {
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

                    emitter.onNext(Notification.of(cookbookFolder + " already exists. "));

                    // Ensure it is a folder
                    if (!Files.isDirectory(cookbookFolder)) {
                        throw new CookbookUpdateException(cookbookFolder + " is not a directory. ");
                    }

                    // TODO: Find a work-around for the File API

                    // Ensure that the remote is correct
                    emitter.onNext(Notification.of("Verifying the remote URL... "));

                    final Repository repository = new FileRepositoryBuilder()
                        .setGitDir(fs.getPath(cookbookFolder.toString(), ".git").toFile())
                        .build();

                    final String url = repository.getConfig().getString("remote", "origin", "url");
                    if (!url.equalsIgnoreCase(cookbook.url)) {
                        throw new CookbookUpdateException("The remote of the cookbook at " + cookbookFolder + " does not match what was expected. " +
                            "Expected " + cookbook.url + " but found " + url);
                    }

                    // Check the status
                    emitter.onNext(Notification.of("Verifying the Git status... "));
                    final Status status = Git.open(cookbookFolder.toFile()).status().call();

                    if (!status.isClean()) {
                        throw new CookbookUpdateException(cookbookFolder
                            + " is not clean! This means you made changes to your cookbook folder."
                            + " Commit your changes or discard them manually");
                    }

                    // Do a pull!
                    emitter.onNext(Notification.of("Pulling the latest changes... "));

                    final PullResult pullResult = Git.open(cookbookFolder.toFile()).pull().call();
                    emitter.onNext(Notification.of(pullResult.getMergeResult().toString()+" "));
                } else {

                    emitter.onNext(Notification.of(cookbookFolder + " does not exist... "));

                    // TODO: Find a work-around for the File API

                    // Clone the cookbook
                    emitter.onNext(Notification.of("Cloning " + cookbook.url + "... "));

                    Git.cloneRepository()
                        .setDirectory(cookbookFolder.toFile())
                        .setURI(cookbook.url)
                        .call();
                }

                emitter.onComplete();
            } catch (final Throwable e) {
                System.out.println(e.toString());
                emitter.onError(e);
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
            .flatMap(event -> {

                final ImmutableMap<RemoteCookbook, Observable<Event>> updateCookbookTasks = event.config.cookbooks.stream()
                    .collect(toImmutableMap(
                        i -> i,
                        i -> updateCookbook(buckarooFolder, i)
                             .startWith(Notification.of("updating "+ i.name.toString()))));

                return MoreObservables.mergeMaps(updateCookbookTasks)
                    .map(UpdateProgressEvent::of);
            })
            .cast(Event.class)
            .concatWith(Observable.just((Event)Notification.of("all cookbooks updated")));
    }
}
