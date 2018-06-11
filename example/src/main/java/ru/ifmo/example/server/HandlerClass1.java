package ru.ifmo.example.server;

import ru.ifmo.server.Handler;
import ru.ifmo.server.Http;
import ru.ifmo.server.Request;
import ru.ifmo.server.Response;

import java.io.Writer;
import static ru.ifmo.server.Http.TEXT_PLAIN;

public class HandlerClass1 implements Handler {

    public void handle(Request request, Response response) throws Exception {

        response.setHeader("HELLO", "WORLD!");
        response.setStatusCode(Http.SC_OK);
        response.setContentType(TEXT_PLAIN + "; UTF-8");
        //Пишем тело
        Writer wr = response.getWriter();
        wr.write("Привет МИР!\r\n");
        wr.write("Досвидания!\r\n");
        wr.flush();
    }
}
