package com.loopperfect.buckaroo.virtualterminal.components;

import com.loopperfect.buckaroo.virtualterminal.Map2D;
import com.loopperfect.buckaroo.virtualterminal.TerminalPixel;

public interface Component {

    Map2D<TerminalPixel> render(final int width);
}
