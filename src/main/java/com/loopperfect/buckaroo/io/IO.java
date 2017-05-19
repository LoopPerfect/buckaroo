package com.loopperfect.buckaroo.io;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.Either;
import com.loopperfect.buckaroo.F1;
import com.loopperfect.buckaroo.Unit;
import org.jparsec.functors.Map3;
import org.jparsec.functors.Map4;
import org.jparsec.functors.Map5;

import java.io.IOException;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

@FunctionalInterface
public interface IO<T> extends F1<IOContext, T> {

    default <U> IO<U> flatMap(final Function<T, IO<U>> f) {
        Preconditions.checkNotNull(f);
        return context -> {
            Preconditions.checkNotNull(context);
            final T t = apply(context);
            return f.apply(t).apply(context);
        };
    }

    default <U> IO<U> map(final Function<T, U> f) {
        Preconditions.checkNotNull(f);
        return flatMap(x -> IO.value(f.apply(x)));
    }

    default <U> IO<U> next(final IO<U> io) {
        Preconditions.checkNotNull(io);
        return flatMap(ignored -> io);
    }

    default IO<Unit> ignore() {
        return next(noop());
    }

    default <U> IO<U> ifThenElse(final Predicate<T> condition, final Function<T, IO<U>> then, final Function<T, IO<U>> otherwise) {
        Preconditions.checkNotNull(condition);
        Preconditions.checkNotNull(then);
        Preconditions.checkNotNull(otherwise);
        return context -> {
            Preconditions.checkNotNull(context);
            final T t = apply(context);
            if (condition.test(t)) {
                return then.apply(t).apply(context);
            } else {
                return otherwise.apply(t).apply(context);
            }
        };
    }

    default IO<T> until(final Predicate<T> condition) {
        Preconditions.checkNotNull(condition);
        return context -> {
            Preconditions.checkNotNull(context);
            T t = apply(context);
            while (!condition.test(t)) {
                t = apply(context);
            }
            return t;
        };
    }

    default IO<T> fallback(final Predicate<T> condition, final Function<T, IO<T>> retry) {
        Preconditions.checkNotNull(condition);
        Preconditions.checkNotNull(retry);
        return context -> {
            final T t = apply(context);
            if (condition.test(t)) {
                return retry.apply(t).apply(context);
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
            context.console().println(x.toString());
            return Unit.of();
        };
    }

    static IO<Optional<String>> read() {
        return context -> {
            Preconditions.checkNotNull(context);
            return context.console().readln();
        };
    }

    static IO<Optional<IOException>> createDirectory(final String path) {
        Preconditions.checkNotNull(path);
        return context -> {
            Preconditions.checkNotNull(context);
            return context.fs().createDirectory(path);
        };
    }

    static IO<Optional<IOException>> writeFile(final String path, final String content, final boolean overwrite) {
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(content);
        return context -> {
            Preconditions.checkNotNull(context);
            return context.fs().writeFile(path, content, overwrite);
        };
    }

    static IO<Optional<IOException>> writeFile(final String path, final String content) {
        return writeFile(path, content, false);
    }

    static IO<Optional<IOException>> deleteFile(final String path){
        return context -> {
            Preconditions.checkNotNull(context);
            return context.fs().deleteFile(path);
        };
    }

    static IO<Either<IOException, String>> readFile(final String path) {
        Preconditions.checkNotNull(path);
        return context -> {
            Preconditions.checkNotNull(context);
            return context.fs().readFile(path);
        };
    }

    static IO<Either<IOException, ImmutableList<String>>> listFiles(final String path) {
        Preconditions.checkNotNull(path);
        return context -> {
            Preconditions.checkNotNull(context);
            return context.fs().listFiles(path);
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
                final T r = t.apply(context);
                builder.add(r);
            }
            return builder.build();
        };
    }

    static <A, B, T> IO<T> sequence(final IO<A> a, final IO<B> b, final BiFunction<A, B, T> selector) {
        Preconditions.checkNotNull(a);
        Preconditions.checkNotNull(b);
        Preconditions.checkNotNull(selector);
        return context -> {
            Preconditions.checkNotNull(context);
            return selector.apply(a.apply(context), b.apply(context));
        };
    }

    static <A, B, C, T> IO<T> sequence(final IO<A> a, final IO<B> b, final IO<C> c, final Map3<A, B, C, T> selector) {
        Preconditions.checkNotNull(a);
        Preconditions.checkNotNull(b);
        Preconditions.checkNotNull(c);
        Preconditions.checkNotNull(selector);
        return context -> {
            Preconditions.checkNotNull(context);
            return selector.map(a.apply(context), b.apply(context), c.apply(context));
        };
    }

    static <A, B, C, D, T> IO<T> sequence(final IO<A> a, final IO<B> b, final IO<C> c, final IO<D> d, final Map4<A, B, C, D, T> selector) {
        Preconditions.checkNotNull(a);
        Preconditions.checkNotNull(b);
        Preconditions.checkNotNull(c);
        Preconditions.checkNotNull(d);
        Preconditions.checkNotNull(selector);
        return context -> {
            Preconditions.checkNotNull(context);
            return selector.map(a.apply(context), b.apply(context), c.apply(context), d.apply(context));
        };
    }

    static <A, B, C, D, E, T> IO<T> sequence(final IO<A> a, final IO<B> b, final IO<C> c, final IO<D> d, final IO<E> e, final Map5<A, B, C, D, E, T> selector) {
        Preconditions.checkNotNull(a);
        Preconditions.checkNotNull(b);
        Preconditions.checkNotNull(c);
        Preconditions.checkNotNull(d);
        Preconditions.checkNotNull(e);
        Preconditions.checkNotNull(selector);
        return context -> {
            Preconditions.checkNotNull(context);
            return selector.map(a.apply(context), b.apply(context), c.apply(context), d.apply(context), e.apply(context));
        };
    }

    static <T, U> F1<IO<T>, IO<U>> lift(final F1<T, U> f) {
        Preconditions.checkNotNull(f);
        return x -> {
            Preconditions.checkNotNull(x);
            return x.map(f);
        };
    }
}
