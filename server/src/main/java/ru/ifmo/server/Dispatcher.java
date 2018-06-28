package ru.ifmo.server;

public interface Dispatcher {
    String dispatch(Request req, Response resp);
}
