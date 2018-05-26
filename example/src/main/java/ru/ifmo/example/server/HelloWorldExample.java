package ru.ifmo.example.server;

import ru.ifmo.server.*;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/**
 * Simple hello world example.
 */
public class HelloWorldExample {
    public static void main(String[] args) {
        ServerConfig config = new ServerConfig()
                .addHandler("/index", new Handler() {
                    @Override
                    public void handle(Request request, Response response) throws Exception {

                        response.setBody("Привет МИР!".getBytes());
                        response.setHeader("HELLO", "WORLD!");
                        response.setStatusCode(Http.SC_OK);
                        response.setContentType("text/plain");
                        Writer writer = response.getWriter();
                        writer.flush();
                        OutputStream os = response.getOutputStream();
                        Writer writer2 = new OutputStreamWriter(os);
                        writer2.write("KUKU");
                        writer2.flush();
//                        os.write("Досвидания".getBytes());
//                        os.flush();
//                        writer.write(Http.OK_HEADER + "Hello World!");
//                        Writer writer = new OutputStreamWriter(response.getOutputStream());
//                        writer.write(Http.OK_HEADER + "Hello World!");
                        writer.flush();
//                        response.getWriter().write("Hello World!");
//                        response.getWriter().write("IFMO university");
//
//                        response.addHeader("Content-Type", "text/html");
                    }
                });

        Server.start(config);
    }
}
