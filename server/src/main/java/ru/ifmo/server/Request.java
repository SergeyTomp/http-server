package ru.ifmo.server;

import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableMap;
import static ru.ifmo.server.Session.SESSION_COOKIENAME;

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
    private Map<String, Cookie> cookieMap;
    private Session session;
    private final Map<String, Session> sessions;
    Map<String, String> headers;
    Map<String, String> args;
    String body;

    Request(Socket socket, Map<String, Session> sessions) {
        this.socket = socket;
        this.sessions = sessions;
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
            return emptyMap();}
        return unmodifiableMap(headers);
    }

    void addBody(String content) {
        body = content;
    }

    public String getBody(){
        return this.body;
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
            return emptyMap();
        return unmodifiableMap(args);
    }
    void mapCookie(String name, Cookie cookie) {
        if (cookieMap == null){
            cookieMap = new HashMap<>();
        }
        cookieMap.put(name, cookie);
    }
    public Map<String, Cookie> getCookies() {
        if (getHeaders().get("Cookie") == null) {
            return emptyMap();
        }
        return unmodifiableMap(cookieMap);
    }
    public String getCookieValue(String key) {
        return cookieMap.get(key).getValue();
    }

    public Session getSession() {
        if (session == null) {
            session = getSession(false); //сначала проверим, нет ли в cookie id открытой сессии
        }
        return session;
    }

    public Session getSession(boolean open) {
        if (!getCookies().containsKey(SESSION_COOKIENAME) || open) {
            session = new Session();
            sessions.put(session.getId(), session);
        } else {
            session = sessions.get(getCookieValue(SESSION_COOKIENAME)); //проверим, точно ли ещё есть на сервере
            if (session == null) {
                session = getSession(true);
            }
        }
        return session;
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
