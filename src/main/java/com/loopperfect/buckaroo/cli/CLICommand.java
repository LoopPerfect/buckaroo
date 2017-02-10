package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.Unit;
import com.loopperfect.buckaroo.io.IO;

public interface CLICommand {

    IO<Unit> routine();
}
