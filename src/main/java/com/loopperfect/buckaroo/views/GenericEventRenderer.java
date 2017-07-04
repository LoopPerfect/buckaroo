package com.loopperfect.buckaroo.views;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.DependencyInstallationEvent;
import com.loopperfect.buckaroo.Event;
import com.loopperfect.buckaroo.Notification;
import com.loopperfect.buckaroo.events.*;
import com.loopperfect.buckaroo.resolver.ResolvedDependenciesEvent;
import com.loopperfect.buckaroo.tasks.DependencyInstalledEvent;
import com.loopperfect.buckaroo.tasks.DownloadProgress;
import com.loopperfect.buckaroo.virtualterminal.Color;
import com.loopperfect.buckaroo.virtualterminal.TerminalPixel;
import com.loopperfect.buckaroo.virtualterminal.UnicodeChar;
import com.loopperfect.buckaroo.virtualterminal.components.Component;
import com.loopperfect.buckaroo.virtualterminal.components.ProgressBar;
import com.loopperfect.buckaroo.virtualterminal.components.StackLayout;
import com.loopperfect.buckaroo.virtualterminal.components.Text;

import java.util.stream.Collectors;

public final class GenericEventRenderer {

    private GenericEventRenderer() {

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

    public static Component render(final DeleteFileEvent event) {
        Preconditions.checkNotNull(event);
        return Text.of("Deleted " + event.path, Color.YELLOW);
    }

    public static Component render(final FileDownloadedEvent event) {
        Preconditions.checkNotNull(event);
        return Text.of("Downloaded " + event.source + " to " + event.destination, Color.YELLOW);
    }

    private static final TerminalPixel downloadProgressFill = TerminalPixel.of(
        UnicodeChar.of('\u2588'), Color.GREEN, Color.DEFAULT);

    private static final TerminalPixel downloadProgressBackground = TerminalPixel.of(
        UnicodeChar.of('\u2591'), Color.DEFAULT, Color.DEFAULT);

    public static Component render(final DownloadProgress event) {
        Preconditions.checkNotNull(event);
        return event.hasKnownContentLength() ?
            ProgressBar.of(event.progress(), downloadProgressFill, downloadProgressBackground) :
            Text.of("Downloaded " + (event.downloaded / 1024L) + "kb");
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
        return Text.of("Unzipping " + event.source + " to " + event.target, Color.YELLOW);
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

    public static Component render(final DependencyInstallationEvent event) {
        Preconditions.checkNotNull(event);
        return StackLayout.of(
            Text.of("downloading: "+ event.progress.getValue0().identifier.toString()),
            render(event.progress.getValue1())
        );
    }

    public static Component render(final FetchGithubProgressEvent event) {
        Preconditions.checkNotNull(event);
        return StackLayout.of(
            Text.of("downloading: "+ event.identifier.toString()),
            render(event.progress)
        );
    }

    public static Component render(final FileHashEvent event) {
        Preconditions.checkNotNull(event);
        return Text.of("Hashed " + event.file + " \n(" + event.sha256 + ")", Color.BLUE);
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
        return Text.of(event.toString());
    }
}
