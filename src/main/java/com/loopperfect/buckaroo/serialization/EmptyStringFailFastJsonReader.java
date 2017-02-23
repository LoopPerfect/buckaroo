package com.loopperfect.buckaroo.serialization;

import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.EOFException;
import java.io.IOException;
import java.io.Reader;

public final class EmptyStringFailFastJsonReader extends JsonReader {

    public EmptyStringFailFastJsonReader(final Reader reader) {
        super(reader);
    }

    @Override
    public JsonToken peek() throws IOException {
        try {
            return super.peek();
        } catch (final EOFException e) {
            throw new JsonSyntaxException(e);
        }
    }
}
