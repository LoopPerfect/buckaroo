package com.loopperfect.buckaroo.virtualterminal.components;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.virtualterminal.Map2D;
import com.loopperfect.buckaroo.virtualterminal.Map2DUtils;
import com.loopperfect.buckaroo.virtualterminal.TerminalPixel;
import com.loopperfect.buckaroo.virtualterminal.UnicodeChar;

import java.util.Arrays;
import java.util.Objects;

public final class FlowLayout implements Component {

    private final TerminalPixel backgroundPixel;
    private final ImmutableList<Component> children;

    private FlowLayout(final TerminalPixel backgroundPixel, final ImmutableList<Component> children) {
        this.backgroundPixel = Preconditions.checkNotNull(backgroundPixel);
        this.children = Preconditions.checkNotNull(children);
    }

    public boolean equals(final FlowLayout other) {
        Preconditions.checkNotNull(other);
        return this == other || Objects.equals(backgroundPixel, other.backgroundPixel) &&
            Objects.equals(children, other.children);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj || obj != null &&
            obj instanceof FlowLayout &&
            equals((FlowLayout)obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(backgroundPixel, children);
    }

    @Override
    public Map2D<TerminalPixel> render(final int width) {

        final ImmutableList<Map2D<TerminalPixel>> childRenders = children.stream()
            .map(x -> x.render(width))
            .map(x -> x.width() > width ? Map2DUtils.clip(x, width, x.height()) : x)
            .collect(ImmutableList.toImmutableList());

        int x = 0;
        int y = 1;
        int maxWidth = 0;

        for (final Map2D<TerminalPixel> childRender : childRenders) {
            if (x + childRender.width() > width) {
                x = 0;
                y++;
            }
            x += childRender.width();
            if (x > maxWidth) {
                maxWidth = x;
            }
        }

        int maxHeight = y;

        Map2D<TerminalPixel> render = Map2D.of(maxWidth, maxHeight, TerminalPixel.class, backgroundPixel);

        x = 0;
        y = 0;

        for (final Map2D<TerminalPixel> childRender : childRenders) {
            if (x + childRender.width() > width) {
                x = 0;
                y++;
            }
            render = Map2DUtils.drawOnBackground(render, x, y, childRender);
            x += childRender.width();
        }

        return render;
    }

    public static FlowLayout of(final TerminalPixel background, final ImmutableList<Component> components) {
        return new FlowLayout(background, components);
    }

    public static FlowLayout of(final TerminalPixel background, final Component... components) {
        return new FlowLayout(
            background,
            Arrays.stream(components).collect(ImmutableList.toImmutableList()));
    }

    public static FlowLayout of(final Component... components) {
        return new FlowLayout(
            TerminalPixel.of(UnicodeChar.of(' ')),
            Arrays.stream(components).collect(ImmutableList.toImmutableList()));
    }

    public static FlowLayout of(final Iterable<Component> components) {
        return new FlowLayout(
            TerminalPixel.of(UnicodeChar.of(' ')),
            ImmutableList.copyOf(components));
    }
}
