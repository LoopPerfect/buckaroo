package com.loopperfect.buckaroo;

import com.google.common.base.Preconditions;
import com.loopperfect.buckaroo.virtualterminal.Color;
import com.loopperfect.buckaroo.virtualterminal.components.Component;
import com.loopperfect.buckaroo.virtualterminal.components.Text;

public interface RenderableException {

    Component render();

    static Component render(final Throwable throwable) {
        Preconditions.checkNotNull(throwable);
        if (throwable instanceof RenderableException) {
            return ((RenderableException) throwable).render();
        }
        return Text.of(throwable.getMessage(), Color.RED);
    }
}
