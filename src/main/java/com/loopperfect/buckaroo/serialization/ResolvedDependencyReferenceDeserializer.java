package com.loopperfect.buckaroo.serialization;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.gson.*;
import com.loopperfect.buckaroo.RecipeIdentifier;
import com.loopperfect.buckaroo.ResolvedDependencyReference;

import java.lang.reflect.Type;

public final class ResolvedDependencyReferenceDeserializer implements JsonDeserializer<ResolvedDependencyReference> {

    @Override
    public ResolvedDependencyReference deserialize(
        final JsonElement json, final Type type, final JsonDeserializationContext context) throws JsonParseException {

        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(type);
        Preconditions.checkNotNull(context);

        try {
            final String raw = json.getAsJsonPrimitive().getAsString();

            final int split = raw.indexOf("//:");

            final String rawCell = raw.substring(0, split).trim();
            final String rawTarget = raw.substring(split + 3).trim();

            final String rawIdentifier = CharMatcher.is('.').countIn(rawCell) == 2 ?
                rawCell.replaceFirst("[.]", "+").replaceFirst("[.]", "/") :
                rawCell.replaceFirst("[.]", "/");

            final RecipeIdentifier identifier = context.deserialize(
                new JsonPrimitive(rawIdentifier), RecipeIdentifier.class);

            if (identifier.recipe.name.equalsIgnoreCase(rawTarget)) {
                return ResolvedDependencyReference.of(identifier);
            }

            return ResolvedDependencyReference.of(identifier, rawTarget);

        } catch (final Throwable e) {
            throw new JsonParseException(e);
        }
    }
}
