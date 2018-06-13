package ru.ifmo.server.scan;

import ru.ifmo.server.HttpMethod;
import ru.ifmo.server.Request;
import ru.ifmo.server.URL;


public class InvalidParameters {
    @URL(method = HttpMethod.GET, value = "/scan")
    public void failParameters(Request request) {
    }
}
