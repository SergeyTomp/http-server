package ru.ifmo.example.server;

import ru.ifmo.server.HandlersClass;
import ru.ifmo.server.Server;
import ru.ifmo.server.ServerConfig;

public class TestExample {
    public static void main(String[] args) {
        ServerConfig config = new ServerConfig();
//        ServerConfig config = new ServerConfig()
//                .addHandler("/index", new Handler() {
//                    @Override
//                    public void handle(Request request, Response response) throws Exception {
//
//                        response.setHeader("HELLO", "WORLD!");
//                        response.setStatusCode(Http.SC_OK);
//                        response.setContentType(TEXT_PLAIN + "; UTF-8");
//                        //Пишем тело
//                        Writer wr = response.getWriter();
//                        wr.write("Привет МИР!\r\n");
//                        wr.write("Досвидания!\r\n");
//                        wr.flush();
//                    }
//                }).addHandler("/index/ifmo", new Handler() {
//                    @Override
//                    public void handle(Request request, Response response) throws Exception {
//                        response.setHeader("FIRST", "HANDLER!");
//                        response.setStatusCode(Http.SC_OK);
//                        response.setContentType(TEXT_PLAIN + "; UTF-8");
//                        response.addCookie(new Cookie("tasty","strawberry", 1));
//                        response.addCookie(new Cookie("yummy","choco", 1));
//                        //Пишем тело
//                        Writer wr = response.getWriter();
//                        wr.write("Тест первого обработчика запроса!\r\n");
//                        wr.write("Удачно!\r\n");
//                        wr.flush();
//                    }
//                });
        config.addClass(HandlersClass.class);
        Server.start(config);
    }
}
