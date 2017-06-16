package com.loopperfect.buckaroo.events;

import com.loopperfect.buckaroo.Event;

public final class PostRequestEvent extends Event {

    private PostRequestEvent() {

    }

    // TODO: fields, hashCode, equals, toString

    public static PostRequestEvent of() {
        return new PostRequestEvent();
    }
}
