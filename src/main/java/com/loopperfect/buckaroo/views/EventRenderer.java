package com.loopperfect.buckaroo.views;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.DependencyInstallationProgress;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.Notification;
import com.loopperfect.buckaroo.events.*;
import com.loopperfect.buckaroo.resolver.ResolvedDependenciesEvent;
import com.loopperfect.buckaroo.tasks.DownloadProgress;
import com.loopperfect.buckaroo.virtualterminal.Color;
import com.loopperfect.buckaroo.virtualterminal.TerminalPixel;
import com.loopperfect.buckaroo.virtualterminal.components.Component;
import com.loopperfect.buckaroo.virtualterminal.components.ProgressBar;
import com.loopperfect.buckaroo.virtualterminal.components.StackLayout;
import com.loopperfect.buckaroo.virtualterminal.components.Text;

import java.util.Comparator;
import java.util.stream.Collectors;

public final class EventRenderer {

    private EventRenderer() {

    }

    public static Component render(final Notification event) {
        Preconditions.checkNotNull(event);
        return Text.of(event.message, Color.GREEN);
    }

    public static Component render(final TouchFileEvent event) {
        Preconditions.checkNotNull(event);
        return Text.of("Touched " + event.path, Color.YELLOW);
    }

    public static Component render(final WriteFileEvent event) {
        Preconditions.checkNotNull(event);
        return Text.of("Wrote " + event.path, Color.YELLOW);
    }

    public static Component render(final FileDownloadedEvent event) {
        Preconditions.checkNotNull(event);
        return Text.of("Downloaded " + event.source + " to " + event.destination, Color.YELLOW);
    }

    private static final TerminalPixel downloadProgressFill = TerminalPixel.fill(Color.WHITE);
    private static final TerminalPixel downloadProgressBackground = TerminalPixel.fill(Color.BLACK);

    public static Component render(final DownloadProgress event) {
        Preconditions.checkNotNull(event);
        return event.hasKnownContentLength() ?
            ProgressBar.of(event.progress(), downloadProgressFill, downloadProgressBackground) :
            Text.of("Downloaded " + (event.contentLength / 1024L) + "kb");
    }

    public static Component render(final ReadLockFileEvent event) {
        Preconditions.checkNotNull(event);
        return Text.of("Read the lock file. ", Color.BLUE);
    }

    public static Component render(final ReadProjectFileEvent event) {
        Preconditions.checkNotNull(event);
        return Text.of("Read the project file. ", Color.BLUE);
    }

    public static Component render(final FileUnzipEvent event) {
        Preconditions.checkNotNull(event);
        return Text.of("Unzipping :"+ event.toString(), Color.YELLOW);
    }

    public static Component render(final ResolvedDependenciesEvent event) {
        Preconditions.checkNotNull(event);
        if (event.dependencies.dependencies.isEmpty()) {
            return Text.of("There were no dependencies to resolve. ");
        }
        return Text.of("Resolved: \n" + event.dependencies.dependencies.entrySet()
            .stream()
            .map(x -> "  " + x.getKey().encode() + " = " + x.getValue().getValue0().encode())
            .collect(Collectors.joining("\n")));
    }

    public static Component render(final DependencyInstallationProgress event) {
        Preconditions.checkNotNull(event);
        return StackLayout.of(event.progress.entrySet()
            .stream()
            .sorted(Comparator.comparing(x -> x.getKey().identifier.encode()))
            .map(x -> StackLayout.of(
                Text.of(x.getKey().identifier.encode()),
                render(x.getValue())))
            .collect(ImmutableList.toImmutableList()));
    }

    public static Component render(final FileHashEvent event) {
        Preconditions.checkNotNull(event);
        return Text.of("Hashed " + event.file + " \n(" + event.sha256 + ")", Color.BLUE);
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
        if (event instanceof DependencyInstallationProgress) {
            return render((DependencyInstallationProgress) event);
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
        return Text.of(event.toString());
    }
}
