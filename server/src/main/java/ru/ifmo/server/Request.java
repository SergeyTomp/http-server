package ru.ifmo.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Keeps request information: method, headers, params
 * and provides {@link java.io.InputStream} to get additional data
 * from client.
 */
public class Request {
    final Socket socket;
    HttpMethod method;
    URI path;

    private Map<String, String> headers;
    private Map<String, String> args;
    private Map<String, String> cookies; //сделать Map string-Cookie

    Request(Socket socket) {
        this.socket = socket;

    }
    /**
     * @return {@link InputStream} connected to the client.
     */
    public InputStream getInputStream() {
        try {
            return socket.getInputStream();
        } catch (IOException e) {
            throw new ServerException("Unable retrieve input stream.", e);
        }
    }
    /**
     * @return HTTP method of this request.
     */
    public HttpMethod getMethod() {
        return method;
    }
    /**
     * @return Request path.
     */
    public String getPath() {
        return path.getPath();
    }
    public Map<String, String> getHeaders() {
        if (headers == null){
            return Collections.emptyMap();}
        return Collections.unmodifiableMap(headers);
    }
    void addHeader(String key, String value) {
        if (headers == null){
            headers = new LinkedHashMap<>();}
        headers.put(key, value);
    }
    void addArgument(String key, String value) {
        if (args == null){
            args = new LinkedHashMap<>();}
        args.put(key, value);
    }
    /**
     * @return Arguments passed to this request.
     */
    public Map<String, String> getArguments() {
        if (args == null)
            return Collections.emptyMap();
        return Collections.unmodifiableMap(args);
    }
    void setCookie(String name, String value) {
        if (cookies == null) {
            cookies = new HashMap<>();
        }
        cookies.put(name, value);
    }
    public Map<String, String> getCookies() {
        if (getHeaders().get("Cookie") == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(cookies);
    }
    public String getCookieValue(String key) {
        return cookies.get(key);
    }
    @Override
    public String toString() {
        return "Request{" +
                "socket=" + socket +
                ", method=" + method +
                ", path=" + path +
                ", headers=" + headers +
                ", args=" + args +
                '}';
    }
}
