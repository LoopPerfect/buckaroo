package com.loopperfect.buckaroo.serialization;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public final class ImmutableListTypeAdapterFactory implements TypeAdapterFactory {

    @SuppressWarnings("unchecked")
    @Override
    public <T> TypeAdapter<T> create(final Gson gson, final TypeToken<T> typeToken) {

        final Type type = typeToken.getType();

        if (typeToken.getRawType() != ImmutableList.class || !(type instanceof ParameterizedType)) {
            return null;
        }

        final com.google.common.reflect.TypeToken<ImmutableList<?>> betterToken =
            (com.google.common.reflect.TypeToken<ImmutableList<?>>)
                com.google.common.reflect.TypeToken.of(typeToken.getType());

        final TypeAdapter<ArrayList<?>> arrayListAdapter =
            (TypeAdapter<ArrayList<?>>)
                gson.getAdapter(
                    TypeToken.get(
                        betterToken.getSupertype(List.class).getSubtype(ArrayList.class).getType()));

        return new TypeAdapter<T>() {
            @Override
            public void write(final JsonWriter out, final T value) throws IOException {
                ArrayList<?> arrayList = Lists.newArrayList((List<?>) value);
                arrayListAdapter.write(out, arrayList);
            }

            @Override
            public T read(final JsonReader in) throws IOException {
                ArrayList<?> arrayList = arrayListAdapter.read(in);
                return (T) ImmutableList.copyOf(arrayList);
            }
        };
    }
}
