package com.loopperfect.buckaroo;

import com.loopperfect.buckaroo.virtualterminal.Color;
import com.loopperfect.buckaroo.virtualterminal.components.Component;
import com.loopperfect.buckaroo.virtualterminal.components.Text;

public class CookbookUpdateException extends Exception implements RenderableException {

    public CookbookUpdateException(final String message) {
        super(message);
    }

    public CookbookUpdateException(final String message, final Throwable throwable) {
        super(message, throwable);
    }

    public CookbookUpdateException(final Throwable throwable) {
        super(throwable);
    }

    @Override
    public Component render() {
        return Text.of("Error! \n" + this.getCause().toString(), Color.RED);
    }
}
