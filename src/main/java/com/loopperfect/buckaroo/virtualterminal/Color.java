package com.loopperfect.buckaroo.virtualterminal;

import org.fusesource.jansi.Ansi;

/**
 * Created by gaetano on 12/06/17.
 */


public enum Color {
    BLACK(0, "BLACK"),
    RED(1, "RED"),
    GREEN(2, "GREEN"),
    YELLOW(3, "YELLOW"),
    BLUE(4, "BLUE"),
    MAGENTA(5, "MAGENTA"),
    CYAN(6, "CYAN"),
    WHITE(7, "WHITE"),
    DEFAULT(9, "DEFAULT"),
    TRANSPARENT(10, "TRANSPARENT");


    private final int value;
    private final String name;

    private Color(int index, String name) {
        this.value = index;
        this.name = name;
    }

    public boolean isTransparent() { return value == 10; }
    public String toString() {
        return this.name;
    }

    public Ansi.Color toAnsi() {
        switch(value) {
            case 0: return Ansi.Color.BLACK;
            case 1: return Ansi.Color.RED;
            case 2: return Ansi.Color.GREEN;
            case 3: return Ansi.Color.YELLOW;
            case 4: return Ansi.Color.BLUE;
            case 5: return Ansi.Color.MAGENTA;
            case 6: return Ansi.Color.CYAN;
            case 7: return Ansi.Color.WHITE;
            case 9: return Ansi.Color.DEFAULT;
        }
        return Ansi.Color.DEFAULT;
    }
}