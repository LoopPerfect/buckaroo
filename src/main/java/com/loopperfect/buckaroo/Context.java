package com.loopperfect.buckaroo;

import com.google.common.util.concurrent.AbstractScheduledService;
import io.reactivex.Scheduler;

import java.nio.file.FileSystem;

/**
 * Created by gaetano on 16/06/17.
 */
public final class Context {
    public final FileSystem fs;
    public final Scheduler scheduler;

    Context(final FileSystem fs, final Scheduler scheduler){
        this.fs = fs;
        this.scheduler = scheduler;
    }

    public static Context of(final FileSystem fs, final Scheduler scheduler) {
        return new Context(fs, scheduler);
    }
}
