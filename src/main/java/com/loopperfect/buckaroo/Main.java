package com.loopperfect.buckaroo;

import java.io.File;
import java.io.IOException;

public final class Main {

    public static void main(final String[] args) {

//        System.out.println("Hello, world. ");


        // Create a basic project structure in the working directory...

        final File workingDirectory = new File(System.getProperty("user.dir"));

        System.out.println("Running in " + workingDirectory + "... ");


        // Create buckaroo.json file
        try {
            new File("buckaroo.json").createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Create buckaroo folder
        new File("buckaroo").mkdir();

    }
}
