package com.loopperfect.buckaroo.routines;

import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;

public final class Help {

    public static final IO<Unit> routine = IO.println("Read the docs at ")
        .then(IO.println("https://buckaroo.readthedocs.io/en/latest/cli.html"));
}
