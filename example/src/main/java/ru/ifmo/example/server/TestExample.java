package ru.ifmo.example.server;

import ru.ifmo.server.*;
import java.io.Writer;

public class TestExample {
    public static void main(String[] args) {
        ServerConfig config = new ServerConfig()
                .addHandler("/index", new Handler() {
                    @Override
                    public void handle(Request request, Response response) throws Exception {

                        response.setHeader("HELLO", "WORLD!");
                        response.setStatusCode(Http.SC_OK);
                        response.setContentType("text/plain");
                        //Пишем тело
                        Writer wr = response.getWriter();
                        wr.write("Привет МИР!\r\n");
                        wr.write("Досвидания!\r\n");
                        wr.flush();
                    }
                }).addHandler("/index/ifmo", new Handler() {
                    @Override
                    public void handle(Request request, Response response) throws Exception {
                        response.setHeader("FIRST", "HANDLER!");
                        response.setStatusCode(Http.SC_OK);
                        response.setContentType("text/plain");
                        //Пишем тело
                        Writer wr = response.getWriter();
                        wr.write("Тест первого обработчика запроса!\r\n");
                        wr.write("Удачно!\r\n");
                        wr.flush();
                        }
                    });
        Server.start(config);
    }
}
