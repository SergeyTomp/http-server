package ru.ifmo.server;

import ru.ifmo.server.util.Utils;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides {@link java.io.OutputStream} ro respond to client.
 */
public class Response {

    final Socket socket;
    private int statusCode = 0;
    private byte[] body;
    private Map<String, String> headers = new HashMap<>();
    private boolean getWR = false;
    ByteArrayOutputStream byteOut;

    Response(Socket socket) {
        this.socket = socket;
    }
    public void setContentType (String s){
        headers.put("Content-Type", s);
    }
    public void setContentLength(long l){
        headers.put("Content-Length", String.valueOf(l));
    }
    public void setStatusCode (int c){
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
        return headers;
    }
    public int getStatusCode (){
        return statusCode;
    }

    /**
     * @return {@link OutputStream} connected to the client.
     */
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
    public Writer getWriter(){
        byteOut = new ByteArrayOutputStream();
        return new OutputStreamWriter(byteOut);
    }
}

