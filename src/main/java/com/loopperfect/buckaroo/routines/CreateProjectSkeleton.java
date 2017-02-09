package com.loopperfect.buckaroo.routines;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.loopperfect.buckaroo.BuckarooException;
import com.loopperfect.buckaroo.Identifier;
import com.loopperfect.buckaroo.Project;
import com.loopperfect.buckaroo.Routine;
import com.loopperfect.buckaroo.serialization.Serializers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

public final class CreateProjectSkeleton implements Routine {

    @Override
    public void execute() throws BuckarooException {

        final File workingDirectory = new File(System.getProperty("user.dir"));

        System.out.println("Creating a skeleton project in " + workingDirectory + "... ");

        // Create a projectToInstall
        final Optional<String> projectNameString = Routines.requestString(
                "What is the name of your project?",
                "That is not a valid project name",
                Identifier::isValid);

        if (!projectNameString.isPresent()) {
            throw new BuckarooException("Could not get a project name");
        }

        final Identifier projectName = Identifier.of(projectNameString.get());

        System.out.println(projectName);

        // TODO: Get license

        final Project project = new Project(projectName, Optional.empty(), ImmutableMap.of());

        final Gson gson = Serializers.gson(true);

        final String serializedProject = gson.toJson(project);

        System.out.println("Creating files and folders... ");

        // Create buckaroo.json file
        try {
            Files.write(Paths.get("buckaroo.json"), serializedProject.getBytes(), StandardOpenOption.CREATE);
        } catch (final IOException e) {
            throw new BuckarooException(e);
        }

        // Create buckaroo folder
        final File modulesFolder = new File("buckaroo");

        modulesFolder.mkdir();

        System.out.println("Done! ");
    }
}
