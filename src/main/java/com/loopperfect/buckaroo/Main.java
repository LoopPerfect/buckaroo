package com.loopperfect.buckaroo;

import com.loopperfect.buckaroo.routines.CreateProjectSkeleton;

import java.util.Optional;

public final class Main {

    public static void main(final String[] args) {

        if (args.length == 1 && args[0].trim().equalsIgnoreCase("init")) {

            final Routine<Exception> routine = new CreateProjectSkeleton();

            final Optional<Exception> result = routine.execute();

            if (result.isPresent()) {
                result.get().printStackTrace();
            }

            return;
        }

        System.out.println("Buck, Buck, Buckaroo! \uD83E\uDD20");
    }
}
