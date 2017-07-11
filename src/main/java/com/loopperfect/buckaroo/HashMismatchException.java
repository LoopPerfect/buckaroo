package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.google.common.hash.HashCode;
import com.loopperfect.buckaroo.virtualterminal.Color;
import com.loopperfect.buckaroo.virtualterminal.components.*;

public final class HashMismatchException extends Exception implements RenderableException {

    public final HashCode expected;
    public final HashCode actual;

    public HashMismatchException(final HashCode expected, final HashCode actual) {

        super();

        Preconditions.checkNotNull(expected);
        Preconditions.checkNotNull(actual);
        Preconditions.checkArgument(!expected.equals(actual));

        this.expected = expected;
        this.actual = actual;
    }

    @Override
    public String getMessage() {
        return "Hash mismatch! Expected " + expected + ", but got " + actual + ". ";
    }

    @Override
    public Component render() {
        return StackLayout.of(
            Text.of("Hash mismatch! \n", Color.RED),
            ListLayout.of(
                StackLayout.of(
                    Text.of("Expected: "),
                    Text.of(expected.toString(), Color.BLUE)),
                StackLayout.of(
                    Text.of("Actual: "),
                    Text.of(actual.toString(), Color.BLUE))),
            Text.of("Possible causes for this exception are: "),
            ListLayout.of(
                Text.of("The cookbook specified an incorrect hash"),
                Text.of("The source delivered the wrong file"),
                Text.of("An error occured while downloading the file")),
            FlowLayout.of(
                Text.of("NOTE: ", Color.GREEN),
                Text.of("Run "),
                Text.of("buckaroo resolve", Color.MAGENTA),
                Text.of(" to ensure that your lock-file is up-to-date. ")));
    }
}
