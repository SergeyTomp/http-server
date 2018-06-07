package ru.ifmo.server;

public class SessionCheckHandler implements Handler{
    public static final String OPEN_HTML = "<html><body>";
    public static final String CLOSE_HTML = "</html></body>";

    public static final String TEST_RESPONSE = OPEN_HTML + "<html><body>Test response";

    @Override
    public void handle(Request request, Response response) throws Exception {

        if(request.getSession().getParam("login") != null){
            response.getWriter().write(request.getSession().getParam("login").toString());
        }
        else response.getWriter().write(" ");
    }
}
