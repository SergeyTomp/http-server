package ru.ifmo.server.scan;

import ru.ifmo.server.HttpMethod;
import ru.ifmo.server.Request;
import ru.ifmo.server.Response;
import ru.ifmo.server.URL;


public class InvalidModifier {
    @URL(method = HttpMethod.GET, value = "/scan")
    private void failParameters(Request request, Response response) {
    }
}
