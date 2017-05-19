package com.loopperfect.buckaroo.routines;

import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;

public final class Help {

    public static final IO<Unit> routine = IO.of(c -> c.console().println("Read the docs at "))
        .next(c -> c.console().println("https://buckaroo.readthedocs.io/en/latest/cli.html"));
}
