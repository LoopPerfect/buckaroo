package com.loopperfect.buckaroo.crypto;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.io.Files;
import com.loopperfect.buckaroo.Either;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.charset.Charset;

public final class Hash {

    private Hash() {
        super();
    }

    public static HashCode sha256(final String x) {
        Preconditions.checkNotNull(x);
        final Charset charset = Charsets.UTF_8;
        final HashFunction hashFunction = Hashing.sha256();
        return hashFunction.newHasher()
            .putString(x, charset)
            .hash();
    }

    public static Either<IOException, HashCode> sha256(final Path path) {
        Preconditions.checkNotNull(path);
        final HashFunction hashFunction = Hashing.sha256();
        try {
            final HashCode hashCode = hashFunction.newHasher()
                .putBytes(Files.asByteSource(path.toFile()).read())
                .hash();
            return Either.right(hashCode);
        } catch (final IOException e) {
            return Either.left(e);
        }
    }

    public static Either<Exception, HashCode> read(final String x) {
        Preconditions.checkNotNull(x);
        try {
            return Either.right(HashCode.fromString(x));
        } catch (final Exception e) {
            return Either.left(e);
        }
    }
}
