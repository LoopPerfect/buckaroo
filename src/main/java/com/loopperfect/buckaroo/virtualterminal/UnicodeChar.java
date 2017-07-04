package com.loopperfect.buckaroo.virtualterminal;

import com.google.common.base.Preconditions;

import java.util.Arrays;
import java.util.Objects;

public final class UnicodeChar {

    public final int characterCode;

    private UnicodeChar(final int characterCode) {
        this.characterCode = characterCode;
    }

    public String unumber() {
        return "U+" + Integer.toHexString(characterCode).toUpperCase();
    }

    public String asString() {
        return Arrays.toString(Character.toChars(characterCode));
    }

    public boolean equals(final UnicodeChar other) {
        Preconditions.checkNotNull(other);
        return characterCode == other.characterCode;
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null &&
            obj instanceof UnicodeChar &&
            equals((UnicodeChar) obj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(characterCode);
    }

    @Override
    public String toString() {
        return characterCode + "(" + Arrays.toString(Character.toChars(characterCode)) + ")";
    }

    public static UnicodeChar of(final char characterCode) {
        return new UnicodeChar(characterCode);
    }

    public static UnicodeChar of(final int characterCode) {
        return new UnicodeChar(characterCode);
    }
}
