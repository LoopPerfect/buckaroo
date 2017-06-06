package com.loopperfect.buckaroo.events;

import com.loopperfect.buckaroo.Event;

public final class PostRequestEvent implements Event {

    private PostRequestEvent() {

    }

    // TODO: fields, hashCode, equals, toString

    public static PostRequestEvent of() {
        return new PostRequestEvent();
    }
}
