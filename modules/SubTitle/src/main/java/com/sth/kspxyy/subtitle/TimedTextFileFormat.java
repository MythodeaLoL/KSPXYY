package com.sth.kspxyy.subtitle;

import java.io.IOException;
import java.io.InputStream;

public interface TimedTextFileFormat {
    TimedTextObject parseFile(String fileName, InputStream is) throws IOException;
}
