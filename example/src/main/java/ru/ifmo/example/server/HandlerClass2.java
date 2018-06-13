package ru.ifmo.example.server;

import ru.ifmo.server.*;

import java.io.Writer;

import static ru.ifmo.server.Http.TEXT_PLAIN;

public class HandlerClass2 implements Handler {

    public void handle(Request request, Response response) throws Exception {
        response.setHeader("FIRST", "HANDLER!");
        response.setStatusCode(Http.SC_OK);
        response.setContentType(TEXT_PLAIN + "; UTF-8");
        response.addCookie(new Cookie("tasty","strawberry", 1));
        response.addCookie(new Cookie("yummy","choco", 1));
        //Пишем тело
        Writer wr = response.getWriter();
        wr.write("Тест первого обработчика запроса!\r\n");
        wr.write("Удачно!\r\n");
        wr.flush();
    }
}
