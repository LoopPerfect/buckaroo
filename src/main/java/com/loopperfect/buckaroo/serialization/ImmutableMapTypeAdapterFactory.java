package com.loopperfect.buckaroo.serialization;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public final class ImmutableMapTypeAdapterFactory implements TypeAdapterFactory {

    @SuppressWarnings("unchecked")
    @Override public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> typeToken) {

        final Type type = typeToken.getType();

        if (typeToken.getRawType() != ImmutableMap.class
            || !(type instanceof ParameterizedType)) {
            return null;
        }

        final com.google.common.reflect.TypeToken<ImmutableMap<?, ?>> betterToken =
            (com.google.common.reflect.TypeToken<ImmutableMap<?, ?>>)
                com.google.common.reflect.TypeToken.of(typeToken.getType());

        final TypeAdapter<HashMap<?, ?>> hashMapAdapter =
            (TypeAdapter<HashMap<?, ?>>) gson.getAdapter(
                TypeToken.get(betterToken.getSupertype(Map.class).getSubtype(HashMap.class)
                    .getType()));

        return new TypeAdapter<T>() {

            @Override public void write(final JsonWriter out, final T value) throws IOException {
                HashMap<?, ?> hashMap = Maps.newHashMap((Map<?, ?>) value);
                hashMapAdapter.write(out, hashMap);
            }

            @Override public T read(final JsonReader in) throws IOException {
                HashMap<?, ?> hashMap = hashMapAdapter.read(in);
                return (T) ImmutableMap.copyOf(hashMap);
            }
        };
    }
}