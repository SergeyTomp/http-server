package ru.ifmo.server;

import java.io.IOException;

public class ThreadHandler1 implements Handler{
//        public static final String OPEN_HTML = "<html><body>";
//        public static final String CLOSE_HTML = "</html></body>";
//        public static final String TEST_RESPONSE = OPEN_HTML + "<html><body>Test response-1";

        @Override
        public void handle(Request request, Response response) throws IOException {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                System.out.println("Thread is Interrupted during sleep");
            }
            MultithreadingTest.isFinishedClient1True();
//            response.getWriter().write((OK_HEADER + TEST_RESPONSE + CLOSE_HTML));
            MultithreadingTest.notifyMonitor();
        }
    }
