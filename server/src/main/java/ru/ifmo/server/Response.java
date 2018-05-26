package ru.ifmo.server;

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

    public void setContentType (String s){
        headers.put("Content-Type", s);
//        contentType = s;
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
    ByteArrayOutputStream bout = new ByteArrayOutputStream();

    Response(Socket socket) {
        this.socket = socket;
    }

    /**
     * @return {@link OutputStream} connected to the client.
     */
    public OutputStream getOutputStream() {
        try {
            return socket.getOutputStream();
        }
        catch (IOException e) {
            throw new ServerException("Cannot get output stream", e);
        }
//        return bout;
    }


    public Writer getWriter() throws IOException {
        Writer pw = new OutputStreamWriter(socket.getOutputStream());
        if (!getWR){
            getWR = true;
            pw.write(Http.OK_HEADER_PLUS + ((statusCode == 0) ? Http.SC_OK : statusCode) + "\r\n\r\n");
            if (headers.size() != 0){
                for (Map.Entry e : headers.entrySet()) {
                    pw.write(e.getKey() + ": " + e.getValue() + "\r\n");
                }
            }
            pw.write ("\r\n\r\n");
            if (body != null) {
                pw.write(new String(body) + "\r\n");
            }
        }
        return pw;
    }
}

