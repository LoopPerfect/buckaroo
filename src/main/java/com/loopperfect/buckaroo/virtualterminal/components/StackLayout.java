package com.loopperfect.buckaroo.virtualterminal.components;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.virtualterminal.Map2D;
import com.loopperfect.buckaroo.virtualterminal.Map2DUtils;
import com.loopperfect.buckaroo.virtualterminal.TerminalPixel;
import com.loopperfect.buckaroo.virtualterminal.UnicodeChar;

import java.util.Arrays;
import java.util.stream.Collectors;

public final class StackLayout implements Component {

    public final TerminalPixel background;
    public final ImmutableList<Component> components;

    private StackLayout(final TerminalPixel background, final ImmutableList<Component> components) {
        this.background = Preconditions.checkNotNull(background);
        this.components = Preconditions.checkNotNull(components);
    }

    @Override
    public Map2D<TerminalPixel> render(final int width) {

        Preconditions.checkArgument(width >= 0);

        final ImmutableList<Map2D<TerminalPixel>> componentRenders = components.stream()
            .map(x -> x.render(width))
            .collect(ImmutableList.toImmutableList());

        final int height = componentRenders.stream()
            .mapToInt(Map2D::height)
            .reduce(0, (x, y) -> x + y);

        Map2D<TerminalPixel> render = Map2D.of(width, height, TerminalPixel.class, background);
        int y = 0;

        for (final Map2D<TerminalPixel> componentRender : componentRenders) {
            render = Map2DUtils.drawOn(render, 0, y, componentRender);
            y += componentRender.height();
        }

        return render;
    }

    public static StackLayout of(final TerminalPixel background, final ImmutableList<Component> components) {
        return new StackLayout(background, components);
    }

    public static StackLayout of(final TerminalPixel background, final Component... components) {
        return new StackLayout(
            background,
            Arrays.stream(components).collect(ImmutableList.toImmutableList()));
    }

    public static StackLayout of(final Component... components) {
        return new StackLayout(
            TerminalPixel.of(UnicodeChar.of(' ')),
            Arrays.stream(components).collect(ImmutableList.toImmutableList()));
    }
}
