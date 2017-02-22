package com.loopperfect.buckaroo.io;

import com.google.common.base.Preconditions;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.Either;
import com.loopperfect.buckaroo.Unit;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

@FunctionalInterface
public interface IO<T> {

    T run(final IOContext context);

    default <U> IO<U> flatMap(final Function<T, IO<U>> f) {
        Preconditions.checkNotNull(f);
        return context -> {
            Preconditions.checkNotNull(context);
            final T t = run(context);
            return f.apply(t).run(context);
        };
    }

    default <U> IO<U> map(final Function<T, U> f) {
        Preconditions.checkNotNull(f);
        return flatMap(x -> IO.value(f.apply(x)));
    }

    default <U> IO<U> then(final IO<U> io) {
        Preconditions.checkNotNull(io);
        return flatMap(ignored -> io);
    }

    default IO<Unit> ignore() {
        return then(noop());
    }

    default <U> IO<U> ifThenElse(final Predicate<T> condition, final Function<T, IO<U>> then, final Function<T, IO<U>> otherwise) {
        Preconditions.checkNotNull(condition);
        Preconditions.checkNotNull(then);
        Preconditions.checkNotNull(otherwise);
        return context -> {
            Preconditions.checkNotNull(context);
            final T t = run(context);
            if (condition.test(t)) {
                return then.apply(t).run(context);
            } else {
                return otherwise.apply(t).run(context);
            }
        };
    }

    default IO<T> until(final Predicate<T> condition) {
        Preconditions.checkNotNull(condition);
        return context -> {
            Preconditions.checkNotNull(context);
            T t = run(context);
            while (!condition.test(t)) {
                t = run(context);
            }
            return t;
        };
    }

    default IO<T> fallback(final Predicate<T> condition, final Function<T, IO<T>> retry) {
        Preconditions.checkNotNull(condition);
        Preconditions.checkNotNull(retry);
        return context -> {
            final T t = run(context);
            if (condition.test(t)) {
                return retry.apply(t).run(context);
            }
            return t;
        };
    }

    static <T> IO<T> of(IO<T> io) {
        return io;
    }

    static <T> IO<T> value(final T value) {
        Preconditions.checkNotNull(value);
        return context -> value;
    }

    static IO<Unit> println(final Object x) {
        Preconditions.checkNotNull(x);
        return context -> {
            Preconditions.checkNotNull(context);
            context.println(x.toString());
            return Unit.of();
        };
    }

    static IO<Optional<String>> read() {
        return context -> {
            Preconditions.checkNotNull(context);
            return context.readln();
        };
    }

    static IO<Optional<IOException>> createDirectory(final Path path) {
        Preconditions.checkNotNull(path);
        return context -> {
            Preconditions.checkNotNull(context);
            return context.createDirectory(path);
        };
    }

    static IO<Optional<IOException>> writeFile(final String path, final String content, final boolean overwrite) {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(content);
        return context -> {
            Preconditions.checkNotNull(context);
            return context.writeFile(context.getPath(path), content, overwrite);
        };
    }

    static IO<Optional<IOException>> writeFile(final String path, final String content) {
        return writeFile(path, content, false);
    }

    static IO<Either<IOException, String>> readFile(final Path path) {
        Preconditions.checkNotNull(path);
        return context -> {
            Preconditions.checkNotNull(context);
            return context.readFile(path);
        };
    }

    static IO<Either<IOException, ImmutableList<Path>>> listFiles(final Path path) {
        Preconditions.checkNotNull(path);
        return context -> {
            Preconditions.checkNotNull(context);
            return context.listFiles(path);
        };
    }

    static IO<Unit> noop() {
        return context -> Unit.of();
    }

    static <T> IO<ImmutableList<T>> sequence(final ImmutableList<IO<T>> ts) {
        Preconditions.checkNotNull(ts);
        return context -> {
            Preconditions.checkNotNull(context);
            final ImmutableList.Builder builder = ImmutableList.builder();
            for (final IO<T> t : ts) {
                final T r = t.run(context);
                builder.add(r);
            }
            return builder.build();
        };
    }

    static <T, U> Function<IO<T>, IO<U>> lift(final Function<T, U> f) {
        Preconditions.checkNotNull(f);
        return x -> {
            Preconditions.checkNotNull(x);
            return x.map(f);
        };
    }
}
