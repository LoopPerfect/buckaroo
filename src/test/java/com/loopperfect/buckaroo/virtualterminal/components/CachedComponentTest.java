package com.loopperfect.buckaroo.virtualterminal.components;

import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.Mutable;
import com.loopperfect.buckaroo.virtualterminal.Map2D;
import com.loopperfect.buckaroo.virtualterminal.TerminalPixel;
import com.loopperfect.buckaroo.virtualterminal.UnicodeChar;
import org.junit.Test;

import java.util.concurrent.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class CachedComponentTest {

    @Test
    public void of() throws Exception {

        final Text text = Text.of("Hello, world. ");

        final Component a = CachedComponent.of(text);
        final Component b = CachedComponent.of(text);

        // The cache should ensure instances are shared
        assertTrue(a == b);
    }

    @Test
    public void ofAcrossThreads() throws Exception {

        final Text text = Text.of("Hello, world. ");

        final ExecutorService executorService = new ThreadPoolExecutor(
            0, 2, 0, TimeUnit.SECONDS, new SynchronousQueue<>());

        final Future<Component> x = executorService.submit(() -> CachedComponent.of(text));
        final Future<Component> y = executorService.submit(() -> CachedComponent.of(text));

        final Component a = x.get();
        final Component b = y.get();

        // Each thread should create it's own instances
        assertTrue(a != b);

        // But they should be equivalent!
        assertEquals(a, b);
    }

    @Test
    public void render() throws Exception {

        final Mutable<Integer> counter = new Mutable<>(0);

        final Component component = width -> {
            counter.value++;
            return Map2D.of(1, 1, TerminalPixel.class, TerminalPixel.of(UnicodeChar.of('a')));
        };

        final Component cachedComponent = CachedComponent.of(component);

        final ImmutableList<Map2D<TerminalPixel>> renders = ImmutableList.of(
            cachedComponent.render(10),
            cachedComponent.render(10),
            cachedComponent.render(10));

        assertEquals(1, (int)counter.value);
        assertTrue(renders.stream().skip(1).allMatch(x -> x.equals(renders.get(0))));
    }
}