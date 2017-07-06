package com.loopperfect.buckaroo.tasks;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.EvenMoreFiles;
import io.reactivex.Single;

import java.nio.charset.Charset;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.UUID;

public final class SessionTasks {

    private SessionTasks() {

    }

    public static Single<UUID> readOrGenerateSessionId(final FileSystem fs) {

        Preconditions.checkNotNull(fs);

        return Single.fromCallable(() -> {

            final Path uuidFilePath = fs.getPath(
                System.getProperty("user.home"),
                ".buckaroo",
                "user-uuid.txt");

            try {

                final String content = EvenMoreFiles.read(uuidFilePath);

                return UUID.fromString(content.trim());
            } catch (final Throwable e) {

                try {

                    final UUID identifier = UUID.randomUUID();

                    EvenMoreFiles.writeFile(
                        uuidFilePath,
                        identifier.toString() + "\n",
                        Charset.defaultCharset(),
                        true);

                    return identifier;

                } catch (final Throwable e2) {

                    return UUID.randomUUID();
                }
            }
        });
    }
}
