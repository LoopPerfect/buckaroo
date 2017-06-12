package com.loopperfect.buckaroo.virtualterminal.components;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.loopperfect.buckaroo.virtualterminal.Map2D;
import com.loopperfect.buckaroo.virtualterminal.Map2DUtils;
import com.loopperfect.buckaroo.virtualterminal.TerminalPixel;
import com.loopperfect.buckaroo.virtualterminal.UnicodeChar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class FlowLayout implements Component {

    private final TerminalPixel backgroundPixel;
    private final ImmutableList<Component> children;

    private FlowLayout(final TerminalPixel backgroundPixel, final ImmutableList<Component> children) {
        this.backgroundPixel = Preconditions.checkNotNull(backgroundPixel);
        this.children = Preconditions.checkNotNull(children);
    }

    @Override
    public Map2D<TerminalPixel> render(final int width) {

        final ImmutableList<Map2D<TerminalPixel>> childRenders = children.stream()
            .map(x -> x.render(width))
            .collect(ImmutableList.toImmutableList());

        final List<List<Map2D<TerminalPixel>>> lines = new ArrayList<>();

        {
            int x = 0;
            for (final Map2D<TerminalPixel> childRender : childRenders) {
                if (lines.isEmpty()) {
                    lines.add(new ArrayList<>());
                }
                lines.get(lines.size() - 1).add(childRender);
                x += childRender.width();
                if (x >= width) {
                    x = 0;
                    lines.add(new ArrayList<>());
                }
            }
        }

        final int height = lines.stream()
            .mapToInt(x -> x.stream().mapToInt(Map2D::height).max().orElse(0))
            .sum();

        Map2D<TerminalPixel> render = Map2D.of(width, height, TerminalPixel.class, backgroundPixel);

        int y = 0;

        for (final List<Map2D<TerminalPixel>> line : lines) {
            int x = 0;
            int lineHeight = 0;
            for (final Map2D<TerminalPixel> element : line) {
                if (element.height() > lineHeight) {
                    lineHeight = element.height();
                }
                render = Map2DUtils.drawOnBackground(render, x, y, element);
                x += element.width();
            }
            y += lineHeight;
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
