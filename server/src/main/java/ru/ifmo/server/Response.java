package ru.ifmo.server;

import ru.ifmo.server.util.Utils;

import java.io.*;
import java.net.Socket;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static ru.ifmo.server.Http.*;

/**
 * Provides {@link java.io.OutputStream} ro respond to client.
 */
public class Response {

    final Socket socket;
    private int statusCode = 0;
    private byte[] body;
    private Map<String, String> headers = new HashMap<>();
    ByteArrayOutputStream byteOut;
    private Writer printWriter;


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
            throw new SecurityException("Not valid http status code:" + c);
        statusCode = c;
    }
    public void setBody (byte[] data){
        setContentLength(data.length);
        body = data;
    }
    public void setHeaders (Map<String, String> h){
        headers.putAll(h);
    }
    public void setHeader(String k, String v){
        headers.put(k,v);
    }
    public Map<String, String> getHeaders (){
        return Collections.unmodifiableMap(headers);
    }
    public int getStatusCode (){
        return statusCode;
    }

    /**
     * @return {@link OutputStream} connected to the client.
     */
    // OutputStream для Server для отправки сформированного ответа
    public OutputStream getOutputStream() {
        try {
            return socket.getOutputStream();
        }
        catch (IOException e) {
            Utils.closeQuiet(socket);
            throw new ServerException("Cannot get outputstream", e);
        }
//        return byteOut;
    }
    // Writer для редактирования handler.handle, там через него пишем в тело ответа.
    public Writer getWriter(){
        byteOut = new ByteArrayOutputStream();
        if (printWriter == null){
            printWriter = new OutputStreamWriter(byteOut);
        }
        return printWriter;
    }
}

