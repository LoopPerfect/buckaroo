package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.loopperfect.buckaroo.*;
import com.loopperfect.buckaroo.buck.BuckConfig;
import com.loopperfect.buckaroo.events.ReadLockFileEvent;
import com.loopperfect.buckaroo.events.WriteFileEvent;
import io.reactivex.Observable;
import io.reactivex.Single;
import org.javatuples.Pair;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Collectors;

import static com.google.common.collect.ImmutableList.toImmutableList;

public final class InstallExistingTasks {

    private InstallExistingTasks() {

    }

    private static Path buckarooDirectory(final Path projectDirectory) {
        return projectDirectory.resolve("buckaroo");
    }

    private static Path dependencyFolder(final Path buckarooDirectory, final RecipeIdentifier identifier) {
        return buckarooDirectory.resolve(identifier.source.map(x -> x.name).orElse("official"))
            .resolve(identifier.organization.name)
            .resolve(identifier.recipe.name);
    }

    private static ImmutableMap<String, ImmutableMap<String, String>> generateBuckConfig(final Path projectDirectory, final ImmutableList<RecipeIdentifier> dependencies) {
        Preconditions.checkNotNull(projectDirectory);
        Preconditions.checkNotNull(dependencies);

        final ImmutableMap<String, String> repositories = dependencies.stream()
            .collect(ImmutableMap.toImmutableMap(CommonTasks::toFolderName, x -> {
                    final Path dependencyFolder = dependencyFolder(buckarooDirectory(projectDirectory), x);

                    return projectDirectory.toAbsolutePath().relativize(dependencyFolder.toAbsolutePath()).toString();
                }));

        return ImmutableMap.of("repositories", repositories);
    }

    public static Single<WriteFileEvent> touchAndPatchBuckConfig(final Path projectDirectory, final ImmutableList<RecipeIdentifier> dependencies) {
        final Path buckConfigPath = projectDirectory.resolve(".buckconfig").toAbsolutePath();

        return CommonTasks.touchFile(buckConfigPath).flatMap(touch -> CommonTasks.readFile(buckConfigPath).flatMap(content -> {
            try {
                final ImmutableMap<String, ImmutableMap<String, String>> config = BuckConfig.parse(content);
                final ImmutableMap<String, ImmutableMap<String, String>> generatedConfig =
                        generateBuckConfig(projectDirectory, dependencies);

                final ImmutableMap<String, ImmutableMap<String, String>> mergedConfig =
                        BuckConfig.override(
                            BuckConfig.removeBuckarooConfig(config), generatedConfig);

                return CommonTasks.writeFile(BuckConfig.serialize(mergedConfig), buckConfigPath, true);
            } catch (final Exception error) {
                return Single.error(error);
            }
        }));
    }

    private static Observable<Event> downloadResolvedDependency(final FileSystem fs, final ResolvedDependency resolvedDependency, final Path target) {

        Preconditions.checkNotNull(fs);
        Preconditions.checkNotNull(resolvedDependency);
        Preconditions.checkNotNull(target);

        final Observable<Event> downloadSourceCode = Single.fromCallable(() -> Files.exists(target))
            .flatMapObservable(exists -> {
                if (exists) {
                    return Observable.empty();
                }
                return resolvedDependency.source.join(
                    gitCommit -> CacheTasks.cloneAndCheckoutUsingCache(gitCommit, target),
                    remoteArchive -> CacheTasks.downloadUsingCache(remoteArchive, target, StandardCopyOption.REPLACE_EXISTING));
            });

        final Path buckFilePath = fs.getPath(target.toString(), "BUCK");
        final Observable<Event> downloadBuckFile = Files.exists(buckFilePath) ?
            Observable.empty() :
            resolvedDependency.buckResource
                .map(x -> CommonTasks.downloadRemoteFile(fs, x, buckFilePath))
                .orElse(Observable.empty());

        final Path buckarooDepsFilePath = fs.getPath(target.toString(), "BUCKAROO_DEPS");
        final Observable<Event> writeBuckarooDeps = Single.fromCallable(() ->
            CommonTasks.generateBuckarooDeps(resolvedDependency.dependencies))
            .flatMap(content -> CommonTasks.writeFile(
                content,
                buckarooDepsFilePath,
                true))
            .cast(Event.class)
            .toObservable();

        return Observable.concat(
            downloadSourceCode,
            downloadBuckFile,
            writeBuckarooDeps.cast(Event.class));
    }

    private static Observable<Event> installDependencyLock(final Path projectDirectory, final DependencyLock lock) {

        Preconditions.checkNotNull(projectDirectory);
        Preconditions.checkNotNull(lock);

        final Path dependencyDirectory = dependencyFolder(buckarooDirectory(projectDirectory), lock.identifier)
            .toAbsolutePath();

        final ImmutableList<RecipeIdentifier> dependencies = lock.origin.dependencies.stream()
                .map(i -> i.identifier)
                .collect(ImmutableList.toImmutableList());

        return Observable.concat(

            // Download the code and BUCK file
            downloadResolvedDependency(projectDirectory.getFileSystem(), lock.origin, dependencyDirectory),

            // Patch the .buckconfig
            touchAndPatchBuckConfig(projectDirectory, dependencies).toObservable(),

            // Mark the installation as complete
            Observable.just(DependencyInstalledEvent.of(lock))
        );
    }


    public static Observable<Event> installExistingDependencies(final Path projectDirectory) {

        Preconditions.checkNotNull(projectDirectory);

        final Path lockFilePath = projectDirectory.resolve("buckaroo.lock.json").toAbsolutePath();

        return Observable.concat(

            Observable.just(Notification.of("Installing existing dependencies... ")),

            // Do we have a lock file?
            Single.fromCallable(() -> Files.exists(lockFilePath)).flatMapObservable(hasBuckarooLockFile -> {
                if (hasBuckarooLockFile) {
                    // No need to generate one
                    return Observable.empty();
                }
                // Generate a lock file
                return ResolveTasks.resolveDependencies(projectDirectory);
            }),

            // Read the lock file
            CommonTasks.readLockFile(projectDirectory.resolve("buckaroo.lock.json").toAbsolutePath())
                    .map(ReadLockFileEvent::of).flatMapObservable(

                (ReadLockFileEvent event) -> {
                    final Observable<DependencyInstallationEvent> installs = Observable.merge(
                        event.locks
                            .entries()
                            .stream()
                            .map(i-> installDependencyLock(projectDirectory, i)
                                .map(x->Pair.with(i, x))
                                .map(DependencyInstallationEvent::of))
                            .collect(toImmutableList())
                    );

                    final ImmutableList<RecipeIdentifier> dependencies = event.locks.entries()
                            .stream()
                            .map(i -> i.identifier)
                            .collect(ImmutableList.toImmutableList());

                    return Observable.concat(
                        Observable.just((Event)event),
                        Observable.concat(

                        // Install the locked dependencies
                        installs,

                        // Patch the .buckconfig
                        touchAndPatchBuckConfig(projectDirectory, dependencies).toObservable(),

                        CommonTasks.readProjectFile(projectDirectory.resolve("buckaroo.json"))
                            .result()
                            .flatMapObservable(

                            // Generate the BUCKAROO_DEPS file
                            (Project project) -> Single.fromCallable(() -> CommonTasks.generateBuckarooDeps(event.locks.entries()
                                .stream()
                                .map(i -> ResolvedDependencyReference.of(i.identifier, i.origin.target))
                                // The top-level BUCKAROO_DEPS should only contain immediate dependencies
                                .filter(x -> project.dependencies.requires(x.identifier))
                                .collect(ImmutableList.toImmutableList())))
                                .flatMap(content -> CommonTasks.writeFile(
                                    content, projectDirectory.resolve("BUCKAROO_DEPS"), true))
                                .toObservable()
                                .cast(Event.class)
                        )
                    ));
                }
            ),

            Observable.just((Event) Notification.of("Finished installing dependencies. ")));
    }

    public static Observable<Event> installExistingDependenciesInWorkingDirectory(final FileSystem fs) {
        Preconditions.checkNotNull(fs);
        return installExistingDependencies(fs.getPath(""));
    }
}
