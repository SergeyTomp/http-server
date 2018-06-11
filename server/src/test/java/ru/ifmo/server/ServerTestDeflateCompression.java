package ru.ifmo.server;

import org.junit.BeforeClass;

public class ServerTestDeflateCompression extends ServerTest{
    @BeforeClass
    public static void initialize() {
        startAll(defaultConfig().setCompression(CompressionType.DEFLATE));
    }
}
