package ru.ifmo.server.scan;

import ru.ifmo.server.HttpMethod;
import ru.ifmo.server.Request;
import ru.ifmo.server.Response;
import ru.ifmo.server.URL;


public class InvalidType {
    @URL(method = HttpMethod.GET, value = "/scan")
    public String failType(Request request, Response response) {
        return "";
    }
}
