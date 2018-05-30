package ru.ifmo.server;

import ru.ifmo.server.util.Utils;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;
import static ru.ifmo.server.Http.CONTENT_LENGTH;
import static ru.ifmo.server.Http.CONTENT_TYPE;

/**
 * Provides {@link java.io.OutputStream} ro respond to client.
 */
public class Response {

    final Socket socket;
    private int statusCode;
    private Map<String, String> headers = new HashMap<>();
    final ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    Writer printWriter;


    Response(Socket socket) {
        this.socket = socket;
    }
    public void setContentType (String s){
        headers.put(CONTENT_TYPE, s);
    }
    public void setContentLength(long l){
        headers.put(CONTENT_LENGTH, String.valueOf(l));
    }
    public void setStatusCode (int c){
        if (c < Http.SC_CONTINUE || c > Http.SC_NOT_IMPLEMENTED)
            throw new ServerException("Not valid http status code: " + c);
        statusCode = c;
    }

    public void setHeaders (Map<String, String> h){
        headers.putAll(h);
    }
    public void setHeader(String k, String v){
        headers.put(k,v);
    }
    public Map<String, String> getHeaders (){
        return unmodifiableMap(headers);
    }
    public int getStatusCode (){
        return statusCode;
    }

    /**
     * @return {@link OutputStream} connected to the client.
     */
    // OutputStream для Server для отправки сформированного ответа
    OutputStream getSocketOutputStream() {
        try {
            return socket.getOutputStream();
        }
        catch (IOException e) {
            Utils.closeQuiet(socket);
            throw new ServerException("Cannot get outputstream", e);
        }
    }

    public OutputStream getOutputStream() {
        return byteOut;
    }

    // Writer для редактирования handler.handle, там через него пишем в тело ответа.
    public Writer getWriter(){
        if (printWriter == null){
            printWriter = new OutputStreamWriter(byteOut);
        }
        return printWriter;
    }
}

