package com.loopperfect.buckaroo.serialization;

import com.google.gson.*;
import com.loopperfect.buckaroo.Identifier;
import com.loopperfect.buckaroo.Project;

import java.lang.reflect.Type;
import java.util.Optional;

public final class ProjectDeserializer implements JsonDeserializer<Project> {

    @Override
    public Project deserialize(final JsonElement jsonElement, final Type type, final JsonDeserializationContext context) throws JsonParseException {

        final JsonObject jsonObject = jsonElement.getAsJsonObject();

        final String nameString = jsonObject.get("name").getAsString();

        if (!Identifier.isValid(nameString)) {
            throw new JsonParseException("\"" + nameString + "\" is not a valid identifier");
        }

        final Identifier name = Identifier.of(nameString);

        Optional<String> license;
        if (jsonObject.has("license")) {
            license = Optional.of(jsonObject.get("license").getAsString());
        } else {
            license = Optional.empty();
        }

        return new Project(name, license, null);
    }
}
