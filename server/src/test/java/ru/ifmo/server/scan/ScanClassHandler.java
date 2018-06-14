package ru.ifmo.server.scan;

import ru.ifmo.server.HttpMethod;
import ru.ifmo.server.Request;
import ru.ifmo.server.Response;
import ru.ifmo.server.URL;

import java.io.IOException;

public class ScanClassHandler {

    public static final String OPEN_HTML = "<html><body>";
    public static final String CLOSE_HTML = "</html></body>";

    public static final String TEST_RESPONSE = OPEN_HTML + "<html><body>Test response";

    @URL(method = HttpMethod.GET, value = "/scanGET")
    public void indexScanClassGET(Request request, Response response) throws IOException {
        response.getWriter().write((TEST_RESPONSE + "<br>" + request.getPath() + CLOSE_HTML));
    }
}
