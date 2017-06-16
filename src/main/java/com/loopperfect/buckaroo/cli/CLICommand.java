package com.loopperfect.buckaroo.cli;

import com.loopperfect.buckaroo.Context;
import com.loopperfect.buckaroo.Event;
import io.reactivex.Observable;

import java.nio.file.FileSystem;
import java.util.function.Function;

public interface CLICommand {

    Function<Context, Observable<Event>> routine();
}
