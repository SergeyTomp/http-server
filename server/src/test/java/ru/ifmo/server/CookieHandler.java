package ru.ifmo.server;

public class CookieHandler  implements Handler {
    @Override
    public void handle(Request request, Response response) throws Exception {
        response.getWriter().write(request.getHeaders().get("Cookie"));
    }
}
