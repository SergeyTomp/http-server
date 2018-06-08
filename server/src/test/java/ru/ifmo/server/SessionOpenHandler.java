package ru.ifmo.server;

public class SessionOpenHandler implements Handler {

    @Override
    public void handle(Request request, Response response) throws Exception {

        request.getSession().setData("login", request.getArguments().get("login"));
    }
}
