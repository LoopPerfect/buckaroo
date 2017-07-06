package com.loopperfect.buckaroo.virtualterminal.components;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.virtualterminal.Map2D;
import com.loopperfect.buckaroo.virtualterminal.TerminalPixel;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 *  A decorator for components that caches the render function.
 *
 *  The static constructor also caches the CachedComponent instances themselves,
 *  which encourages cache hits.
 *
 *  Each thread has its own cache for performance.
 */
public final class CachedComponent implements Component {

    public final Component component;

    private final transient Map<Integer, Map2D<TerminalPixel>> cachedRender = new HashMap<>();

    private CachedComponent(final Component component) {
        Preconditions.checkNotNull(component);
        this.component = component;
    }

    public boolean equals(final CachedComponent other) {
        Preconditions.checkNotNull(other);
        return this == other || Objects.equals(component, other.component);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || obj != null &&
            obj instanceof CachedComponent &&
            equals((CachedComponent)obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(component);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .addValue(component)
            .toString();
    }

    @Override
    public Map2D<TerminalPixel> render(final int width) {
        Preconditions.checkArgument(width >= 0);
        if (cachedRender.containsKey(width)) {
            return cachedRender.get(width);
        }
        final Map2D<TerminalPixel> render = component.render(width);
        cachedRender.putIfAbsent(width, render);
        return render;
    }

    private static final ThreadLocal<Map<Component, CachedComponent>> instanceCache = ThreadLocal.withInitial(HashMap::new);

    public static Component of(final Component component) {
        final Map<Component, CachedComponent> cache = instanceCache.get();
        if (cache.containsKey(component)) {
            return cache.get(component);
        }
        final CachedComponent cachedComponent = new CachedComponent(component);
        cache.put(component, cachedComponent);
        return cachedComponent;
    }
}
