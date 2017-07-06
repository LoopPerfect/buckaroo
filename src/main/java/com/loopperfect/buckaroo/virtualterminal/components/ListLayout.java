package com.loopperfect.buckaroo.virtualterminal.components;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.virtualterminal.Map2D;
import com.loopperfect.buckaroo.virtualterminal.Map2DUtils;
import com.loopperfect.buckaroo.virtualterminal.TerminalPixel;

import java.util.Objects;

public final class ListLayout implements Component {

    public final Map2D<TerminalPixel> bullet;
    public final ImmutableList<Component> elements;

    private ListLayout(final Map2D<TerminalPixel> bullet, final ImmutableList<Component> elements) {
        Preconditions.checkNotNull(bullet);
        Preconditions.checkNotNull(elements);
        Preconditions.checkArgument(elements.stream().noneMatch(Objects::isNull));
        this.bullet = bullet;
        this.elements = elements;
    }

    public boolean equals(final ListLayout other) {
        Preconditions.checkNotNull(other);
        return this == other || Objects.equals(elements, other.elements);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj ||
            obj != null &&
                obj instanceof ListLayout &&
                equals((ListLayout)obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(elements);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("elements", elements)
            .toString();
    }

    @Override
    public Map2D<TerminalPixel> render(final int width) {

        Preconditions.checkArgument(width >= 0);

        final TerminalPixel background = TerminalPixel.of(' ');

        final ImmutableList<Map2D<TerminalPixel>> elementRenders = elements.stream()
            .map(x -> x.render(width - bullet.width()))
            .collect(ImmutableList.toImmutableList());

        final int height = elementRenders.stream()
            .mapToInt(x -> Math.max(bullet.height(), x.height()))
            .reduce(0, (x, y) -> x + y);

        Map2D<TerminalPixel> render = Map2D.of(width, height, TerminalPixel.class, background);

        int y = 0;

        for (final Map2D<TerminalPixel> elementRender : elementRenders) {
            render = Map2DUtils.drawOnBackground(render, 0, y, bullet);
            render = Map2DUtils.drawOnBackground(render, bullet.width(), y, elementRender);
            y += elementRender.height();
        }

        return render;
    }

    public static ListLayout of(final Map2D<TerminalPixel> bullet, final Iterable<Component> elements) {
        return new ListLayout(bullet, ImmutableList.copyOf(elements));
    }

    public static ListLayout of(final Component... elements) {
        return new ListLayout(Text.of(" * ").render(3), ImmutableList.copyOf(elements));
    }

    public static ListLayout of(final Iterable<Component> elements) {
        return new ListLayout(Text.of(" * ").render(3), ImmutableList.copyOf(elements));
    }
}
