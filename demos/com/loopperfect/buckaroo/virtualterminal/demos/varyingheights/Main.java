package com.loopperfect.buckaroo.virtualterminal.demos.varyingheights;

import com.loopperfect.buckaroo.virtualterminal.TerminalBuffer;
import com.loopperfect.buckaroo.virtualterminal.components.Component;
import com.loopperfect.buckaroo.virtualterminal.components.StackLayout;
import com.loopperfect.buckaroo.virtualterminal.components.Text;

/**
 * Created by gaetano on 09/06/17.
 */
public class Main {

    public static void main(final String[] args) throws InterruptedException {
        final TerminalBuffer buffer = new TerminalBuffer();
        final Component c1 = StackLayout.of(
            Text.of("1111"),
            Text.of("2222"),
            Text.of("3333"),
            Text.of("4444")
        );

        final Component c2 = StackLayout.of(
            Text.of("1111"),
            Text.of("3333")
        );

        final Component c3 = StackLayout.of(
            Text.of("#######"),
            Text.of("#######"),
            Text.of("#######")
        );

        for(int i=0; i<10; ++i) {
            buffer.flip(c1.render(100));
            Thread.sleep(500);
            buffer.flip(c2.render(100));
            Thread.sleep(500);
            buffer.flip(c3.render(100));
            Thread.sleep(500);
        }

    }
}
