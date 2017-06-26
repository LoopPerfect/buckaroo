package com.loopperfect.buckaroo.virtualterminal;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import org.fusesource.jansi.Ansi;

import java.util.Objects;

public final class TerminalPixel {

    public final UnicodeChar character;
    public final Color foreground;
    public final Color background;

    private TerminalPixel(final UnicodeChar character, final Color foreground, final Color background) {

        super();

        this.character = Preconditions.checkNotNull(character);
        this.foreground = Preconditions.checkNotNull(foreground);
        this.background = Preconditions.checkNotNull(background);
    }

    public boolean equals(final TerminalPixel other) {
        Preconditions.checkNotNull(other);
        return (this == other) ||
            (character.equals(other.character) &&
                foreground.equals(other.foreground) &&
                background.equals(other.background));
    }

    public TerminalPixel setCharacter(final UnicodeChar character) {
        Preconditions.checkNotNull(character);
        if (this.character.equals(character)) {
            return this;
        }
        return TerminalPixel.of(character, foreground, background);
    }

    public TerminalPixel setForeground(final Color foreground) {
        Preconditions.checkNotNull(foreground);
        if (this.foreground.equals(foreground)) {
            return this;
        }
        return TerminalPixel.of(character, foreground, background);
    }

    public TerminalPixel setBackground(final Color background) {
        Preconditions.checkNotNull(background);
        if (this.background.equals(background)) {
            return this;
        }
        return TerminalPixel.of(character, foreground, background);
    }

    @Override
    public int hashCode() {
        return Objects.hash(character, foreground, background);
    }

    @Override
    public boolean equals(final Object obj) {
        return this == obj ||
            (obj != null &&
            obj instanceof TerminalPixel &&
            equals((TerminalPixel) obj));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("character", character)
            .add("foreground", foreground)
            .add("background", background)
            .toString();
    }

    public static TerminalPixel of(final UnicodeChar character, final Color foreground, final Color background) {
        return new TerminalPixel(character, foreground, background);
    }

    public static TerminalPixel of(final UnicodeChar character) {
        return new TerminalPixel(character, Color.DEFAULT, Color.TRANSPARENT);
    }

    public static TerminalPixel of(final char character) {
        return new TerminalPixel(UnicodeChar.of(character), Color.DEFAULT, Color.TRANSPARENT);
    }

    public static TerminalPixel fill(final Color bg) {
        return new TerminalPixel(UnicodeChar.of(' '), Color.DEFAULT, bg);
    }

    public static TerminalPixel fill(final Color bg, char c) {
        return new TerminalPixel(UnicodeChar.of(c), Color.DEFAULT, bg);
    }
}
