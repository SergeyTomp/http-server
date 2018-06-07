package ru.ifmo.server;

import ru.ifmo.server.util.Utils;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static ru.ifmo.server.Http.CONTENT_LENGTH;
import static ru.ifmo.server.Http.CONTENT_TYPE;

/**
 * Provides {@link java.io.OutputStream} ro respond to client.
 */
public class Response {

    final Socket socket;
    private int statusCode;
    private Map<String, String> headers;
    ByteArrayOutputStream byteOut;
    Map<String, Cookie> cookieMap;

    Writer printWriter;

    Response(Socket socket) {
        this.socket = socket;
    }

    public void addCookie(Cookie cookie) {
        if (cookieMap == null) {
            cookieMap = new HashMap<>();
        }
        cookieMap.put(cookie.getKey(), cookie);
    }

    public void setContentType(String s) {
        headers.put(CONTENT_TYPE, s);
    }

    public void setContentLength(long l) {
        headers.put(CONTENT_LENGTH, String.valueOf(l));
    }

    public void setStatusCode(int c) {
        if (c < Http.SC_CONTINUE || c > Http.SC_NOT_IMPLEMENTED)
            throw new ServerException("Not valid http status code: " + c);
        statusCode = c;
    }

    public void setHeaders(Map<String, String> h) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.putAll(h);
    }

    public void setHeader(String key, String val) {
        if (headers == null) {
            headers = new HashMap<>();
        }
        headers.put(key, val);
    }

    public Map<String, String> getHeaders() {
        if (headers == null) {
            headers = new HashMap<>();
        }
        return headers;
    }

    public int getStatusCode() {
        return statusCode;
    }

    /**
     * @return {@link OutputStream} connected to the client.
     */
    // OutputStream для Server для отправки сформированного ответа
    OutputStream getSocketOutputStream() {
        try {
            return socket.getOutputStream();
        } catch (IOException e) {
            Utils.closeQuiet(socket);
            throw new ServerException("Cannot get outputstream", e);
        }
    }

    public OutputStream getOutputStream() {
        if (byteOut == null) {
            byteOut = new ByteArrayOutputStream();
        }
        return byteOut;
    }

    // Writer для редактирования handler.handle, там через него пишем в тело ответа.
    public Writer getWriter() {
        if (printWriter == null) {
            printWriter = new OutputStreamWriter(getOutputStream());
        }
        return printWriter;
    }

}

