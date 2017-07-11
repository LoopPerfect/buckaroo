package com.loopperfect.buckaroo.tasks;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.RemoteFile;
import com.loopperfect.buckaroo.RenderableException;
import com.loopperfect.buckaroo.virtualterminal.Color;
import com.loopperfect.buckaroo.virtualterminal.components.*;

import java.nio.file.Path;
import java.util.Objects;

public final class DownloadFileException extends Exception implements RenderableException {

    public final RemoteFile remoteFile;
    public final Path target;
    public final Throwable cause;

    public DownloadFileException(final RemoteFile remoteFile, final Path target, final Throwable cause) {
        super(cause);
        Preconditions.checkNotNull(remoteFile);
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(cause);
        this.remoteFile = remoteFile;
        this.target = target;
        this.cause = cause;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("remoteFile", remoteFile)
            .add("target", target)
            .addValue(cause)
            .toString();
    }

    public static DownloadFileException wrap(final RemoteFile remoteFile, final Path target, final Throwable cause) {

        Preconditions.checkNotNull(remoteFile);
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(cause);

        if (cause instanceof DownloadFileException) {
            final DownloadFileException other = (DownloadFileException) cause;
            if (Objects.equals(remoteFile, other.remoteFile) &&
                Objects.equals(target, other.target)) {
                return other;
            }
        }

        return new DownloadFileException(remoteFile, target, cause);
    }

    @Override
    public Component render() {
        final Component causeComponent = (cause instanceof RenderableException ?
            ((RenderableException) cause).render() :
            Text.of(cause.toString(), Color.RED));
        return StackLayout.of(
            Text.of("Download error! ", Color.RED),
            Text.of("There was an error downloading a file. "),
            ListLayout.of(
                FlowLayout.of(Text.of("URL: "), Text.of(remoteFile.url.toString(), Color.BLUE)),
                FlowLayout.of(Text.of("Target: "), Text.of(target.toString(), Color.BLUE)),
                StackLayout.of(
                    Text.of("Cause: "),
                    causeComponent)));
    }
}
