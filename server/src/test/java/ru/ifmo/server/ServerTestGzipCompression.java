package ru.ifmo.server;

import org.junit.BeforeClass;

public class ServerTestGzipCompression extends ServerTest{
    @BeforeClass
    public static void initialize() {
        startAll(defaultConfig().setCompression(CompressionType.GZIP));
    }
}
