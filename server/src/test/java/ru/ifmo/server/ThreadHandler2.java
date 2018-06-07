package ru.ifmo.server;
public class ThreadHandler2  implements Handler{

//        public static final String OPEN_HTML = "<html><body>";
//        public static final String CLOSE_HTML = "</html></body>";
//        public static final String TEST_RESPONSE = OPEN_HTML + "<html><body>Test response-2";

        @Override
        public void handle(Request request, Response response) throws Exception {
            MultithreadingTest.isFinishedClient2True();
//            response.getWriter().write((OK_HEADER + TEST_RESPONSE + CLOSE_HTML));

        }
    }
