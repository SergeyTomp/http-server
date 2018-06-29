package ru.ifmo.example.server;

import ru.ifmo.server.Handler;
import ru.ifmo.server.Http;
import ru.ifmo.server.Request;
import ru.ifmo.server.Response;

import java.io.Writer;

import static ru.ifmo.server.Http.TEXT_PLAIN;

public class DispatcherTestHandler implements Handler {
    public void handle(Request request, Response response) throws Exception {

        response.setHeader("HELLO", "DISPATCHER!");
        response.setStatusCode(Http.SC_OK);
        response.setContentType(TEXT_PLAIN + "; UTF-8");
        //Пишем тело
        Writer wr = response.getWriter();
        wr.write("HELLO DISPATCHER!\r\n");
        wr.flush();
    }
}
