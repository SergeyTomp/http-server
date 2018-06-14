package ru.ifmo.server;

public class SuccessPostPutHandler implements Handler {
    public static final String OPEN_HTML = "<html><body>";
    public static final String CLOSE_HTML = "</html></body>";

    public static final String TEST_RESPONSE = OPEN_HTML + "<html><body>Test response";

    @Override
    public void handle(Request request, Response response) throws Exception {
        response.getWriter().write((TEST_RESPONSE +
                "<br>" + request.getBody() + CLOSE_HTML));
    }
}
