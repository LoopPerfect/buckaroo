package com.loopperfect.buckaroo;

import com.loopperfect.buckaroo.virtualterminal.Color;
import com.loopperfect.buckaroo.virtualterminal.components.*;

public final class NotAProjectDirectoryException extends Exception implements RenderableException {

    public NotAProjectDirectoryException(final Throwable cause) {
        super(cause);
    }

    @Override
    public Component render() {
        final Component causeComponent = getCause() == null ?
            StackLayout.of() :
            RenderableException.render(getCause());
        return StackLayout.of(
            Text.of("Project not found! ", Color.RED),
            causeComponent,
            Text.of("These might be helpful: "),
            ListLayout.of(
                Text.of("Check that you are in the correct directory"),
                FlowLayout.of(
                    Text.of("Ensure that a "),
                    Text.of("buckaroo.json", Color.MAGENTA),
                    Text.of(" file exists"))));
    }

    public static NotAProjectDirectoryException wrap(final Throwable throwable) {
        if (throwable instanceof NotAProjectDirectoryException) {
            return (NotAProjectDirectoryException) throwable;
        }
        return new NotAProjectDirectoryException(throwable);
    }
}
