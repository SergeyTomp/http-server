package ru.ifmo.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.server.util.Utils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ru.ifmo.server.Http.*;
import static ru.ifmo.server.Session.SESSION_COOKIENAME;
import static ru.ifmo.server.util.Utils.htmlMessage;

/**
 * Ifmo Web Server.
 * <p>
 * To start server use {@link #start(ServerConfig)} and register at least
 * one handler to process HTTP requests.
 * Usage example:
 * <pre>
 * {@code
 * ServerConfig config = new ServerConfig()
 *      .addHandler("/index", new Handler() {
 *          public void handle(Request request, Response response) throws Exception {
 *              Writer writer = new OutputStreamWriter(response.getOutputStream());
 *              writer.write(Http.OK_HEADER + "Hello World!");
 *              writer.flush();
 *          }
 *      });
 *
 * Server server = Server.start(config);
 *      }
 *     </pre>
 * </p>
 * <p>
 * To stop the server use {@link #stop()} or {@link #close()} methods.
 * </p>
 *
 * @see ServerConfig
 */
public class Server implements Closeable {
    private static final char LF = '\n';
    private static final char CR = '\r';
    private static final String CRLF = "" + CR + LF;
    private static final char AMP = '&';
    private static final char EQ = '=';
    private static final char HEADER_VALUE_SEPARATOR = ':';
    private static final char SPACE = ' ';
    private static final int READER_BUF_SIZE = 1024;

    private final ServerConfig config;
    private ServerSocket socket;
    private ExecutorService acceptorPool;
    private ExecutorService connectionProcessingPool;
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);
    private Thread killSess;
    private Map<String, Session> sessions = new ConcurrentHashMap<>();

    private Server(ServerConfig config) {
        this.config = new ServerConfig(config);
    }

    Map<String, Session> getSessions() {
        return sessions;
    }

    void setSessions(String key, Session session) {
        sessions.put(key, session);
    }

    void removeSession(String key) {
        sessions.remove(key);
    }

    private void startSessionKiller() {
        SessionKiller sessionKiller = new SessionKiller(sessions);
        killSess = new Thread(sessionKiller);
        killSess.start();

        LOG.info("Session killer process started, session will be deleted by timeout.");
    }

    /**
     * Starts server according to config. If null passed
     * defaults will be used.
     *
     * @param config Server config or null.
     * @return Server instance.
     * @see ServerConfig
     */
    public static Server start(ServerConfig config) {
        if (config == null)
            config = new ServerConfig();

        try {
            if (LOG.isDebugEnabled())
                LOG.debug("Starting server with config: {}", config);

            Server server = new Server(config);
            server.openConnection();
            server.startAcceptor();
            LOG.info("Server started on port: {}", config.getPort());
            server.startSessionKiller();
            return server;
        } catch (IOException e) {
            throw new ServerException("Cannot start server on port: " + config.getPort());
        }
    }

    private void openConnection() throws IOException {
        socket = new ServerSocket(config.getPort());
    }

    private void startAcceptor() {
        acceptorPool = Executors.newSingleThreadExecutor(new ServerThreadFactory("con-acceptor"));
        acceptorPool.submit(new ConnectionHandler());
    }

    /**
     * Stops the server.
     */
    public void stop() {
        acceptorPool.shutdownNow();
        connectionProcessingPool.shutdownNow();
        killSess.interrupt();
        Utils.closeQuiet(socket);
        socket = null;
    }

    private void processConnection(Socket sock) throws IOException {
        if (LOG.isDebugEnabled())
            LOG.debug("Accepting connection on: {}", sock);

        Request req;
        try {
            req = parseRequest(sock);

            if (LOG.isDebugEnabled())
                LOG.debug("Parsed request: {}", req);
        } catch (URISyntaxException e) {
            if (LOG.isDebugEnabled())
                LOG.error("Malformed URL", e);
            respond(SC_BAD_REQUEST, "Malformed URL", htmlMessage(SC_BAD_REQUEST + " Malformed URL"),
                    sock.getOutputStream());
            return;
        } catch (Exception e) {
            LOG.error("Error parsing request", e);
            respond(SC_SERVER_ERROR, "Server Error", htmlMessage(SC_SERVER_ERROR + " Server error"),
                    sock.getOutputStream());
            return;
        }

        if (!isMethodSupported(req.method)) {
            respond(SC_NOT_IMPLEMENTED, "Not Implemented", htmlMessage(SC_NOT_IMPLEMENTED + " Method \""
                    + req.method + "\" is not supported"), sock.getOutputStream());
            return;
        }

        Handler handler = config.handler(req.getPath());
        Response resp = new Response(sock);

        if (handler != null) {
            try {
                handler.handle(req, resp);
                sendResponse(resp, req);
            } catch (Exception e) {
                if (LOG.isDebugEnabled())
                    LOG.error("Server error:", e);
                respond(SC_SERVER_ERROR, "Server Error", htmlMessage(SC_SERVER_ERROR + " Server error"),
                        sock.getOutputStream());
            }
        } else
            respond(SC_NOT_FOUND, "Not Found", htmlMessage(SC_NOT_FOUND + " Not found"),
                    sock.getOutputStream());
    }

    private void sendResponse(Response resp, Request req) {
        try {
            if (resp.printWriter != null)
                resp.printWriter.flush();

            if (resp.getHeaders().get(CONTENT_LENGTH) == null) {
                resp.getOutputStream();
                resp.setContentLength(resp.byteOut.size());
            }
            if (resp.getStatusCode() == 0) {
                resp.setStatusCode(Http.SC_OK);
            }

            OutputStream out = resp.getSocketOutputStream();
            Writer pw = new BufferedWriter(new OutputStreamWriter(out));
            pw.write((Http.OK_HEADER_PLUS + resp.getStatusCode() + CRLF));
            for (Map.Entry e : resp.getHeaders().entrySet()) {
                pw.write(e.getKey() + ": " + e.getValue() + CRLF);
            }

            if (req.getSession() != null) {
                resp.addCookie(new Cookie(SESSION_COOKIENAME, req.getSession().getId()));
            }

            for (Map.Entry<String, Cookie> entry : resp.cookieMap.entrySet()) {
                StringBuilder cookieLine = new StringBuilder();
                cookieLine.append(entry.getKey()).append("=").append(entry.getValue().getValue());
                if (entry.getValue().getMaxAge() != 0) {
                    cookieLine.append(";Max-Age=").append(entry.getValue().getMaxAge());
                }
                if (entry.getValue().getDomain() != null) {
                    cookieLine.append(";DOMAIN=").append(entry.getValue().getDomain());
                }
                if (entry.getValue().getPath() != null) {
                    cookieLine.append(";PATH=").append(entry.getValue().getPath());
                }
                pw.write("Set-Cookie:" + SPACE + cookieLine.toString() + CRLF);
            }
            pw.write(CRLF);
            pw.write(resp.getOutputStream().toString());
            pw.flush();
//            out.write(resp.byteOut.toByteArray());
//            out.flush();
        } catch (Exception e) {
            throw new ServerException("Fail to get output stream", e);
        }
    }

    private Request parseRequest(Socket socket) throws IOException, URISyntaxException {
        InputStreamReader reader = new InputStreamReader(socket.getInputStream());
        Request req = new Request(socket,sessions);
        StringBuilder sb = new StringBuilder(READER_BUF_SIZE);

        while (readLine(reader, sb) > 0) {
            if (req.method == null)
                parseRequestLine(req, sb);
            else
                parseHeader(req, sb);
            sb.setLength(0);
        }
        return req;
    }

    private void parseRequestLine(Request req, StringBuilder sb) throws URISyntaxException {
        int start = 0;
        int len = sb.length();

        for (int i = 0; i < len; i++) {
            if (sb.charAt(i) == SPACE) {
                if (req.method == null)
                    req.method = HttpMethod.valueOf(sb.substring(start, i));
                else if (req.path == null) {
                    req.path = new URI(sb.substring(start, i));
                    break; // Ignore protocol for now
                }
                start = i + 1;
            }
        }

        assert req.method != null : "Request method can't be null";
        assert req.path != null : "Request path can't be null";

        String query = req.path.getQuery();
        if (query != null) {
            start = 0;
            String key = null;

            for (int i = 0; i < query.length(); i++) {
                boolean last = i == query.length() - 1;

                if (key == null && query.charAt(i) == EQ) {
                    key = query.substring(start, i);

                    start = i + 1;
                } else if (key != null && (query.charAt(i) == AMP || last)) {
                    req.addArgument(key, query.substring(start, last ? i + 1 : i));

                    key = null;
                    start = i + 1;
                }
            }

            if (key != null)
                req.addArgument(key, null);
        }
    }

    private void parseHeader(Request req, StringBuilder sb) {
        String key = null;
        int len = sb.length();
        int start = 0;

        for (int i = 0; i < len; i++) {
            if (sb.charAt(i) == HEADER_VALUE_SEPARATOR) {
                key = sb.substring(start, i).trim();
                start = i + 1;
                break;
            }
        }
        req.addHeader(key, sb.substring(start, len).trim());

        if ("Cookie".equals(key)) {
            String[] pairs = sb.substring(start, len).trim().split("; ");
            for (int i = 0; i < pairs.length; i++) {
                String pair = pairs[i];
                String[] keyValue = pair.split("=");
                req.mapCookie(keyValue[0], new Cookie(keyValue[0], keyValue[1]));
            }
        }
    }

    private int readLine(InputStreamReader in, StringBuilder sb) throws IOException {
        int c;
        int count = 0;
        while ((c = in.read()) >= 0) {
            if (c == LF)
                break;
            sb.append((char) c);
            count++;
        }
        if (count > 0 && sb.charAt(count - 1) == CR)
            sb.setLength(--count);
        if (LOG.isTraceEnabled())
            LOG.trace("Read line: {}", sb.toString());
        return count;
    }

    private void respond(int code, String statusMsg, String content, OutputStream out) throws IOException {
        out.write(("HTTP/1.0" + SPACE + code + SPACE + statusMsg + CRLF + CRLF + content).getBytes());
        out.flush();
    }

    /**
     * Invokes {@link #stop()}. Usable in try-with-resources.
     *
     * @throws IOException Should be never thrown.
     */
    public void close() {
        stop();
    }

    private boolean isMethodSupported(HttpMethod method) {
        return method == HttpMethod.GET;
    }

//    private class ConnectionHandler implements Runnable {
//
//        public void run() {
//            connectionProcessingPool = Executors.newCachedThreadPool();
//            while (!Thread.currentThread().isInterrupted()) {
//                try (Socket sock = socket.accept()) {
//                    sock.setSoTimeout(config.getSocketTimeout());
//                    connectionProcessingPool.submit(new NewConnection(sock));
//                } catch (Exception e) {
//                    if (!Thread.currentThread().isInterrupted())
//                        LOG.error("Error accepting connection", e);
//                }
//            }
//        }
//    }

    private class ConnectionHandler implements Runnable {

        public void run() {
            connectionProcessingPool = Executors.newCachedThreadPool();
            while (!Thread.currentThread().isInterrupted()) {
                try {Socket sock = socket.accept();
                    sock.setSoTimeout(config.getSocketTimeout());
                    connectionProcessingPool.submit(new NewConnection(sock));
                } catch (Exception e) {
                    if (!Thread.currentThread().isInterrupted())
                        LOG.error("Error accepting connection", e);
                }
            }
        }
    }

    private class NewConnection implements Runnable {
        Socket sock;
        NewConnection(Socket sock) {
            this.sock = sock;
        }
        @Override
        public void run() {

            while (!Thread.currentThread().isInterrupted()) {
                try {
                    LOG.info("New connection opened " + Thread.currentThread().getName());
                    processConnection(sock);
                } catch (IOException e) {
                    LOG.error("Error input / output during data transfer", e);
                } finally {
                    try {
                        sock.close();
                        LOG.info("Connection closed " + Thread.currentThread().getName());
                        Thread.currentThread().interrupt();
                    } catch (IOException e) {
                        if (!Thread.currentThread().isInterrupted())
                            LOG.error("Error accepting connection", e);
                        LOG.error("Error closing the socket", e);
                    }
                }
            }
        }
    }
}
