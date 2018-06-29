package ru.ifmo.example.server.scan;

import ru.ifmo.server.Handler;
import ru.ifmo.server.Request;
import ru.ifmo.server.Response;

import java.io.IOException;

public class HandlerClassToAdd implements Handler{
    public static final String OPEN_HTML = "<html><body>";
    public static final String CLOSE_HTML = "</html></body>";

    public static final String TEST_RESPONSE = OPEN_HTML + "<html><body>Test response";
    @Override
    public void handle(Request request, Response response) throws IOException {
        response.getWriter().write((TEST_RESPONSE + "<br>" + request.getPath() + CLOSE_HTML));
    }
}
