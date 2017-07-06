package com.loopperfect.buckaroo.views;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.loopperfect.buckaroo.DependencyInstallationEvent;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.Notification;
import com.loopperfect.buckaroo.RecipeIdentifier;
import com.loopperfect.buckaroo.events.*;
import com.loopperfect.buckaroo.resolver.ResolvedDependenciesEvent;
import com.loopperfect.buckaroo.tasks.DependencyInstalledEvent;
import com.loopperfect.buckaroo.tasks.DownloadProgress;
import com.loopperfect.buckaroo.tasks.UpdateProgressEvent;
import com.loopperfect.buckaroo.virtualterminal.Color;
import com.loopperfect.buckaroo.virtualterminal.TerminalPixel;
import com.loopperfect.buckaroo.virtualterminal.UnicodeChar;
import com.loopperfect.buckaroo.virtualterminal.components.*;

import java.util.List;

import static com.google.common.collect.ImmutableList.toImmutableList;

public final class GenericEventRenderer {

    private GenericEventRenderer() {

    }

    public static Component render(final Notification event) {
        Preconditions.checkNotNull(event);
        return Text.of(event.message, Color.GREEN);
    }

    public static Component render(final TouchFileEvent event) {
        Preconditions.checkNotNull(event);
        return FlowLayout.of(Text.of("Touched "), Text.of(event.path.toString(), Color.YELLOW));
    }

    public static Component render(final WriteFileEvent event) {
        Preconditions.checkNotNull(event);
        return FlowLayout.of(Text.of("Wrote "), Text.of(event.path.toString(), Color.YELLOW));
    }

    public static Component render(final DeleteFileEvent event) {
        Preconditions.checkNotNull(event);
        return FlowLayout.of(Text.of("Deleted "), Text.of(event.path.toString(), Color.YELLOW));
    }

    public static Component render(final FileDownloadedEvent event) {
        Preconditions.checkNotNull(event);
        return FlowLayout.of(
            Text.of("Downloaded "),
            Text.of(event.source.toString(), Color.YELLOW),
            Text.of(" to "),
            Text.of(event.destination.toString(), Color.YELLOW));
    }

    private static final TerminalPixel downloadProgressFill = TerminalPixel.of(
        UnicodeChar.of('\u2588'), Color.GREEN, Color.DEFAULT);

    private static final TerminalPixel downloadProgressBackground = TerminalPixel.of(
        UnicodeChar.of('\u2591'), Color.GRAY, Color.DEFAULT);

    private static final ImmutableList<String> spinner = ImmutableList.of(
        "\uD83C\uDF0D", "\uD83C\uDF0E", "\uD83C\uDF0F");

    public static Component render(final DownloadProgress event) {
        Preconditions.checkNotNull(event);
        return event.hasKnownContentLength() ?
            ProgressBar.of(event.progress(), downloadProgressFill, downloadProgressBackground) :
            FlowLayout.of(
                Text.of("Downloaded "),
                Text.of((event.downloaded / 1024L) + "kb", Color.GREEN),
                Text.of(" " + spinner.get(((int)(event.downloaded / (1024L))) % 1024)));
    }

    public static Component render(final ReadLockFileEvent event) {
        Preconditions.checkNotNull(event);
        return Text.of("Read the lock file. ", Color.BLUE);
    }

    public static Component render(final InitProjectEvent event) {
        Preconditions.checkNotNull(event);
        return Text.of("Read the project file. ", Color.BLUE);
    }

    public static Component render(final ReadProjectFileEvent event) {
        Preconditions.checkNotNull(event);
        return Text.of("Read the project file. ", Color.BLUE);
    }

    public static Component render(final FileUnzipEvent event) {
        Preconditions.checkNotNull(event);
        return FlowLayout.of(
            Text.of("Unzipping "),
            Text.of(event.source.toString(), Color.YELLOW),
            Text.of(" to "),
            Text.of(event.target.toString(), Color.YELLOW));
    }

    public static Component render(final RecipeIdentifier identifier) {
        Preconditions.checkNotNull(identifier);
        final List<Component> components = Lists.newArrayList();
        if (identifier.source.isPresent()) {
            components.add(Text.of(identifier.source.get().name, Color.CYAN));
            components.add(Text.of("+", Color.GRAY));
        }
        components.add(Text.of(identifier.organization.name, Color.BLUE));
        components.add(Text.of("/"));
        components.add(Text.of(identifier.recipe.name, Color.MAGENTA));
        return FlowLayout.of(components);
    }

    public static Component render(final ResolvedDependenciesEvent event) {
        Preconditions.checkNotNull(event);
        if (event.dependencies.dependencies.isEmpty()) {
            return Text.of("There were no dependencies to resolve. ");
        }
        return StackLayout.of(
            Text.of("Resolved: "),
            ListLayout.of(
                event.dependencies.dependencies.entrySet()
                    .stream()
                    .map(x -> FlowLayout.of(
                        render(x.getKey()),
                        Text.of(" = "),
                        Text.of(x.getValue().getValue0().encode(), Color.GREEN)))
                    .collect(toImmutableList())));
    }

    public static Component render(final DependencyInstallationEvent event) {
        Preconditions.checkNotNull(event);
        return StackLayout.of(
            FlowLayout.of(
                Text.of("Downloading: ", Color.GRAY),
                render(event.progress.getValue0().identifier)),
            ListLayout.of(render(event.progress.getValue1())));
    }

    public static Component render(final UpdateProgressEvent event) {
        Preconditions.checkNotNull(event);
        return StackLayout.of(
            Text.of("updating cookbooks:"),
            ListLayout.of(
                event.progress.entrySet().stream().map(
                    e-> FlowLayout.of(
                            Text.of(e.getKey().url, Color.MAGENTA),
                            Text.of(" : "),
                            render(e.getValue()))
                ).collect(toImmutableList())
        ));
    }

    public static Component render(final FetchGithubProgressEvent event) {
        Preconditions.checkNotNull(event);
        return StackLayout.of(
            FlowLayout.of(
                Text.of("Downloading: ", Color.GRAY),
                render(event.identifier)),
            ListLayout.of(render(event.progress)));
    }

    public static Component render(final FileHashEvent event) {
        Preconditions.checkNotNull(event);
        return FlowLayout.of(
            Text.of("Hashed "),
            Text.of(event.file.toString(), Color.YELLOW),
            Text.of(" ("),
            Text.of(event.sha256.toString(), Color.BLUE),
            Text.of(")"));
    }

    public static Component render(final DeleteFileIfExistsEvent event) {
        Preconditions.checkNotNull(event);
        return event.didDelete ?
            render(DeleteFileEvent.of(event.path)) :
            StackLayout.of();
    }

    public static Component render(final DependencyInstalledEvent event) {
        Preconditions.checkNotNull(event);
        return Text.of("Installed " + event.dependency.identifier.encode(), Color.GREEN);
    }

    public static Component render(final Event event) {
        Preconditions.checkNotNull(event);
        if (event instanceof Notification) {
            return render((Notification)event);
        }
        if (event instanceof WriteFileEvent) {
            return render((WriteFileEvent)event);
        }
        if (event instanceof TouchFileEvent) {
            return render((TouchFileEvent)event);
        }
        if (event instanceof FileDownloadedEvent) {
            return render((FileDownloadedEvent)event);
        }
        if (event instanceof DownloadProgress) {
            return render((DownloadProgress)event);
        }
        if (event instanceof DependencyInstallationEvent) {
            return render((DependencyInstallationEvent) event);
        }
        if (event instanceof FetchGithubProgressEvent) {
            return render((FetchGithubProgressEvent) event);
        }
        if (event instanceof ResolvedDependenciesEvent) {
            return render((ResolvedDependenciesEvent) event);
        }
        if (event instanceof ReadProjectFileEvent) {
            return render((ReadProjectFileEvent) event);
        }
        if (event instanceof ReadLockFileEvent) {
            return render((ReadLockFileEvent) event);
        }
        if (event instanceof FileUnzipEvent) {
            return render((FileUnzipEvent) event);
        }
        if (event instanceof FileHashEvent) {
            return render((FileHashEvent) event);
        }
        if (event instanceof DependencyInstalledEvent) {
            return render((DependencyInstalledEvent) event);
        }
        if (event instanceof DeleteFileEvent) {
            return render((DeleteFileEvent) event);
        }
        if (event instanceof DeleteFileIfExistsEvent) {
            return render((DeleteFileIfExistsEvent) event);
        }
        if (event instanceof InitProjectEvent) {
            return render((InitProjectEvent) event);
        }
        if (event instanceof UpdateProgressEvent) {
            return render(((UpdateProgressEvent) event));
        }
        return Text.of(event.toString());
    }
}
