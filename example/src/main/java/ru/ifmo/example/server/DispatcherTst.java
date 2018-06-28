package ru.ifmo.example.server;

import ru.ifmo.server.*;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * Simple example that shows basic usage.
 */
public class DispatcherTst {
    private static class RequestDispatcher implements Dispatcher {
        @Override
        public String dispatch(Request request, Response response) {
            return "/dispatcher";
        }
    }
    public static void main(String[] args) throws URISyntaxException, IOException {
        ServerConfig config = new ServerConfig();
        config.addHandlerClass("/index", HandlerClass1.class);
        config.addHandlerClass("/index/ifmo", HandlerClass2.class);
        config.addHandlerClass("/dispatcher", DispatcherTestHandler.class);
        config.setCompression(CompressionType.GZIP);
        config.setDispatcher(new RequestDispatcher());
        Server.start(config);
    }
}
