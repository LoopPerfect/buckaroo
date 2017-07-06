package com.loopperfect.buckaroo.virtualterminal;

import org.fusesource.jansi.Ansi;

public enum Color {

    BLACK(0, "BLACK", false),
    GRAY(0, "GRAY", true),
    RED(1, "RED", false),
    GREEN(2, "GREEN", false),
    YELLOW(3, "YELLOW", false),
    BLUE(4, "BLUE", false),
    MAGENTA(5, "MAGENTA", false),
    CYAN(6, "CYAN", false),
    WHITE(7, "WHITE", false),
    DEFAULT(9, "DEFAULT", false),
    TRANSPARENT(10, "TRANSPARENT", false);

    private final int value;
    private final String name;
    private final boolean isBright;

    Color(final int index, final String name, final boolean isBright) {
        this.value = index;
        this.name = name;
        this.isBright = isBright;
    }

    public boolean isTransparent() {
        return value == 10;
    }

    public String toString() {
        return this.name;
    }

    public Ansi.Color toAnsi() {
        switch (value) {
            case 0:
                return Ansi.Color.BLACK;
            case 1:
                return Ansi.Color.RED;
            case 2:
                return Ansi.Color.GREEN;
            case 3:
                return Ansi.Color.YELLOW;
            case 4:
                return Ansi.Color.BLUE;
            case 5:
                return Ansi.Color.MAGENTA;
            case 6:
                return Ansi.Color.CYAN;
            case 7:
                return Ansi.Color.WHITE;
            case 9:
                return Ansi.Color.DEFAULT;
        }
        return Ansi.Color.DEFAULT;
    }

    public Ansi foreground() {
        if (isBright) {
            return Ansi.ansi().fgBright(toAnsi());
        }
        return Ansi.ansi().fg(toAnsi());
    }

    public Ansi background() {
        if (isBright) {
            return Ansi.ansi().bgBright(toAnsi());
        }
        return Ansi.ansi().bg(toAnsi());
    }
}