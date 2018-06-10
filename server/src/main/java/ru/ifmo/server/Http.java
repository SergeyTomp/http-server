package ru.ifmo.server;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP constants.
 */
public class Http {
    public static final int SC_CONTINUE = 100;
    public static final int SC_OK = 200;
    public static final int SC_MULTIPLE_CHOICES = 300;
    public static final int SC_BAD_REQUEST = 400;
    public static final int SC_NOT_FOUND = 404;
    public static final int SC_SERVER_ERROR = 500;
    public static final int SC_NOT_IMPLEMENTED = 501;

    /** OK header that preceded rest response data. */
    public static final String OK_HEADER = "HTTP/1.0 200 OK\r\n\r\n";
    public static final String OK_HEADER_PLUS = "HTTP/1.0 ";

    /** Header names */
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String ACCEPT_ENCODING = "Accept-Encoding";
    public static final String CONTENT_ENCODING = "Content-Encoding";

    /** Mime types */
    public static final String TEXT_PLAIN = "text/plain";
    public static final String URL_ENCODED = "application/x-www-form-urlencoded";
}

class CustomErrorResponse{
   static Map<Integer, String> coderespMap = new HashMap<>();
}
