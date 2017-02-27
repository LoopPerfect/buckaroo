package com.loopperfect.buckaroo;

import com.loopperfect.buckaroo.io.IO;

import java.io.IOException;

public interface Resource {

    String description();

    IO<Either<IOException, String>> fetch();
}
