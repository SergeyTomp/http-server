package ru.ifmo.server;

import ru.ifmo.server.annotation.URL;

import java.io.Writer;

import static ru.ifmo.server.Http.TEXT_PLAIN;

public class HandlersClass {

    @URL(method = {HttpMethod.GET},value = "/index")
    public void handle1(Request request, Response response) throws Exception {

        response.setHeader("HELLO", "WORLD!");
        response.setStatusCode(Http.SC_OK);
        response.setContentType(TEXT_PLAIN + "; UTF-8");
        //Пишем тело
        Writer wr = response.getWriter();
        wr.write("Привет МИР!\r\n");
        wr.write("Досвидания!\r\n");
        wr.flush();
    }
    @URL(method = {HttpMethod.GET}, value = "/index/ifmo")
    public void handle2(Request request, Response response) throws Exception {
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

