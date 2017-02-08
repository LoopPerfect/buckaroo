package com.loopperfect.buckaroo;

import com.loopperfect.buckaroo.routines.ListRecipes;

public final class Main {

    public static void main(final String[] args) {

        if (args.length == 1 && args[0].trim().equalsIgnoreCase("recipes")) {
            new ListRecipes().run();
            return;
        }

        System.out.println("Buck, Buck, Buckaroo!");
    }
}
