package ru.ifmo.server;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.ifmo.server.util.Utils;

import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import static org.apache.commons.io.FilenameUtils.*;
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
    private Map<String, ReflectHandler> classHandlers;
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);
    private Thread killSess;
    private Map<String, Session> sessions = new ConcurrentHashMap<>();

    private Server(ServerConfig config) {
        this.config = new ServerConfig(config);
        classHandlers = new HashMap<>();
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

    public static Server start() {
        return start(new ConfigLoader().load());
    }

    public static Server start(File file) {
        return start(new ConfigLoader().load(file));
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
            server.addHandlerClass(config.getHandlerClasses());
            server.scanHandlersClass(config.getClasses());
            server.openConnection();
            server.startAcceptor();
            server.connectionProcessingPool = Executors.newCachedThreadPool();
            LOG.info("Server started on port: {}", config.getPort());
            server.startSessionKiller();
            return server;
        } catch (IOException e) {
            throw new ServerException("Cannot start server on port: " + config.getPort());
        }
    }

    private void addHandlerClass(Map<String, Class<? extends Handler>> handlerClasses) throws ServerException {

        for (Map.Entry<String, Class<? extends Handler>> entry : handlerClasses.entrySet()) {
            String url = entry.getKey();
            Class<? extends Handler> value = entry.getValue();

            Handler handler;
            String name = value.getName();
            Class<?> cls;

            try {
                cls = Class.forName(name);
            } catch (ClassNotFoundException e) {
                throw new ServerException("No class with the specified name was found", e);
            }

            if (Handler.class.isAssignableFrom(cls)) {

                try {
                    handler = value.getConstructor().newInstance();
                } catch (NoSuchMethodException e) {
                    throw new ServerException("This class " + cls.getSimpleName() + "does not contains empty constructor");
                } catch (Exception e) {
                    throw new ServerException("Error when creating instance of the class", e);
                }

                if (!config.getHandlers().containsKey(url)){
                    config.addHandler(url, handler);
                }

            } else {
                throw new ServerException("This class " + cls.getSimpleName() + " cannot be implemented as Handler");
            }
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
        sessions.clear();
    }

    private class ReflectHandler {
        Method meth;
        Object obj;
        HttpMethod[] httpMethods;

        ReflectHandler(Object obj, Method meth, HttpMethod[] httpMethods) {
            assert meth != null;
            assert obj != null;
            assert httpMethods != null && httpMethods.length != 0;

            this.meth = meth;
            this.obj = obj;
            this.httpMethods = httpMethods;
        }
        boolean isApplicable(HttpMethod method) {
            return Arrays.stream(httpMethods).anyMatch(m -> m == method);
        }
    }

    private void scanHandlersClass(Collection<Class<?>> classes) {
        Collection<Class<?>> classList = new ArrayList<>(classes);

        for (Class<?> cls : classList) {
            try {
                for (Method method : cls.getDeclaredMethods()) {
                    URL annot = method.getAnnotation(URL.class);
                    if (annot != null) {
                        Class<?>[] params = method.getParameterTypes();
                        Class<?> methodType = method.getReturnType();

                        if (params.length == 2
                                && methodType.equals(void.class)
                                && Modifier.isPublic(method.getModifiers())
                                && params[0].equals(Request.class)
                                && params[1].equals(Response.class)) {
                            String path = annot.value();
                            HttpMethod[] meth = annot.method();
                            ReflectHandler reflectHandler = new ReflectHandler(cls.getConstructor().newInstance(), method, meth);
                            classHandlers.put(path, reflectHandler);
                        } else {
                            throw new ServerException("Invalid @URL annotated method: " + cls.getSimpleName() + "." + method.getName() + "(). "
                                    + "Valid method must be public, void and accept only two arguments: Request and Response.");
                        }
                    }
                }
            } catch (ReflectiveOperationException e) {
                throw new ServerException("Unable initialize @URL annotated handlers. ", e);
            }
        }
    }


    private void processReflectHandler(ReflectHandler refHand, Request req, Response resp, Socket sock) throws IOException {
        try {
            refHand.meth.invoke(refHand.obj, req, resp);
            sendResponse(resp, req);
        } catch (Exception e) {
            if (LOG.isDebugEnabled())
                LOG.error("Error invoke method:" + refHand.meth, e);

            respond(SC_SERVER_ERROR, "Server Error", htmlMessage(SC_SERVER_ERROR + " Server error"),
                    sock.getOutputStream());
        }
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
            String htmlMsg = CustomErrorResponse.coderespMap.get(SC_BAD_REQUEST) == null ? SC_BAD_REQUEST + " Malformed URL"
                    : CustomErrorResponse.coderespMap.get(SC_BAD_REQUEST);
            respond(SC_BAD_REQUEST, "Malformed URL", htmlMessage(htmlMsg),
                    sock.getOutputStream());
            return;
        } catch (Exception e) {
            LOG.error("Error parsing request", e);
            String htmlMsg = CustomErrorResponse.coderespMap.get(SC_SERVER_ERROR) == null ? SC_SERVER_ERROR + " Server error"
                    : CustomErrorResponse.coderespMap.get(SC_SERVER_ERROR);
            respond(SC_SERVER_ERROR, "Server Error", htmlMessage(htmlMsg),
                    sock.getOutputStream());
            return;
        }

        if (!isMethodSupported(req.method)) {
            String htmlMsg = CustomErrorResponse.coderespMap.get(SC_NOT_IMPLEMENTED) == null ? SC_NOT_IMPLEMENTED + " Method \""
                    + req.method + "\" is not supported" : CustomErrorResponse.coderespMap.get(SC_NOT_IMPLEMENTED);
            respond(SC_NOT_IMPLEMENTED, "Not Implemented", htmlMessage(htmlMsg), sock.getOutputStream());
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


                String htmlMsg = CustomErrorResponse.coderespMap.get(SC_SERVER_ERROR) == null ? SC_SERVER_ERROR + " Server error"
                        : CustomErrorResponse.coderespMap.get(SC_SERVER_ERROR);
                respond(SC_SERVER_ERROR, "Server Error", htmlMessage(htmlMsg),
                        sock.getOutputStream());
            }
        }
        else if (classHandlers.get(req.getPath()) != null){
            ReflectHandler reflectHandler = classHandlers.get(req.getPath());
            if (reflectHandler != null && reflectHandler.isApplicable(req.method))
                processReflectHandler(reflectHandler, req, resp, sock);
        }
        else {
            String path = config.getStaticDirectory() + File.separatorChar + req.getPath().substring(1);
            if (new File(path).exists()) {
                fileHandlers(path, resp);
                sendResponse(resp, req);

            } else {
                respond(SC_NOT_FOUND, "Not Found", htmlMessage(SC_NOT_FOUND + " Not found"),
                        sock.getOutputStream());
            }
        }
    }

    private void sendResponse(Response resp, Request req) {
        try {
            if (resp.printWriter != null)
                resp.printWriter.flush();

            if (resp.byteOut != null) {
                if (config.getCompressionType() != null && isCompressionSupported(req)) {
                    resp.byteOut = compress(resp.byteOut);
                    resp.setHeader(Http.CONTENT_ENCODING, config.getCompressionType().toString().toLowerCase());
                }
                resp.setContentLength(resp.byteOut.size());
            }

            if (resp.getStatusCode() == 0) {
                resp.setStatusCode(Http.SC_OK);
            }

            OutputStream out = resp.getSocketOutputStream();
            Writer pw = new BufferedWriter(new OutputStreamWriter(out));
            pw.write((Http.OK_HEADER_PLUS + resp.getStatusCode() + CRLF));
            if (resp.headers != null) {
                for (Map.Entry e : resp.headers.entrySet()) {
                    pw.write(e.getKey() + ": " + e.getValue() + CRLF);
                }
            }
                resp.addCookie(new Cookie(SESSION_COOKIENAME, req.getSession().getId()));

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
            pw.flush();
            if (resp.byteOut != null){
                out.write(resp.byteOut.toByteArray());}
            out.flush();
        } catch (Exception e) {
            throw new ServerException("Fail to get output stream", e);
        }

}
    private Request parseRequest(Socket socket) throws IOException, URISyntaxException {
        InputStreamReader reader = new InputStreamReader(socket.getInputStream());
        Request req = new Request(socket, sessions);
        StringBuilder sb = new StringBuilder(READER_BUF_SIZE);

        while (readLine(reader, sb) > 0) {
            if (req.method == null)
                parseRequestLine(req, sb);
            else
                parseHeader(req, sb);
            sb.setLength(0);
        }

        if (isPOSTorPUT(req)) {
            readBody(reader, sb, req);
            if (req.headers.get(CONTENT_TYPE).contains(URL_ENCODED)){
                parseArgs(req, req.body);
            }
        }
        return req;
    }

    private boolean isPOSTorPUT(Request req) {
        return (req.getMethod() == HttpMethod.POST || req.getMethod() == HttpMethod.PUT) && req.headers.get(CONTENT_TYPE) != null && (
                req.headers.get(CONTENT_TYPE).contains(URL_ENCODED)
                || req.headers.get(CONTENT_TYPE).contains(TEXT_PLAIN));
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
        parseArgs(req, query);
    }

    private void parseArgs(Request req, String query) {
        int start;
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

    private void readBody(InputStreamReader reader, StringBuilder sb, Request request) throws IOException {
        int contentLength = Integer.parseInt(request.headers.get("Content-Length"));

        char[] buf = new char[1024];
        int len;
        int count = 0;

        while ((len = reader.read(buf)) > 0) {
            sb.append(new String(buf, 0, len));

            count += len;
            if (count == contentLength)
                break;
        }

        request.addBody(sb.toString());
    }

    private void respond(int code, String statusMsg, String content, OutputStream out) throws IOException {
        out.write(("HTTP/1.0" + SPACE + code + SPACE + statusMsg + CRLF + CRLF + content).getBytes());
        out.flush();
    }
    private boolean isCompressionSupported(Request req) {
        String unPursedTypes = req.getHeaders().get(Http.ACCEPT_ENCODING);
        String[] parsedTypes = unPursedTypes.replaceAll("\\p{Punct}", " ")
                .trim().toUpperCase().split("\\s");

        for (String str : parsedTypes) {
            if (str.length() > 0 && config.getCompressionType() != null
                    && CompressionType.valueOf(str) == config.getCompressionType())
                return true;
        }
        return false;
    }

    private ByteArrayOutputStream compress(ByteArrayOutputStream bodyBytes) throws IOException {

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OutputStream compressor = null;
        if (config.getCompressionType() == CompressionType.GZIP) {
            compressor = new GZIPOutputStream(outputStream);
        }

        if (config.getCompressionType() == CompressionType.DEFLATE) {
            compressor = new DeflaterOutputStream(outputStream);
        }

        assert compressor != null;

        compressor.write(bodyBytes.toByteArray());
        compressor.close();

        return outputStream;
    }

    /**
     * Invokes {@link #stop()}. Usable in try-with-resources.
     *
     * @throws IOException Should be never thrown.
     */
    public void close() throws IOException {
        stop();
    }

    private boolean isMethodSupported(HttpMethod method) {
        return method == HttpMethod.GET || method == HttpMethod.POST || method == HttpMethod.HEAD || method == HttpMethod.PUT;
    }

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
            try {
                if (LOG.isDebugEnabled())
                    LOG.debug("New connection opened " + Thread.currentThread().getName());

                processConnection(sock);
                Thread.currentThread().interrupt();
            }
            catch (IOException e) {
                LOG.error("Error input / output during data transfer", e);
            }
            finally {
                try {
                    sock.close();
                    if (LOG.isDebugEnabled())
                        LOG.debug("Connection closed " + Thread.currentThread().getName());
                } catch (IOException e) {
                    LOG.error("Error closing the socket", e);
                }
            }
        }
    }

    private void fileHandlers(String filepath, Response resp) throws IOException {
        File file = new File(filepath);
        String extension =  getExtension(filepath);
        long contentlength = file.length();
        String contentType = null;
        switch (extension) {
            case "txt":
                contentType = TEXT_PLAIN;
                break;
            case "html":
                contentType = TEXT_HTML;
                break;
            case "jpg":
                contentType = IMAGE_JPEG;
                break;
            case "pdf":
                contentType = APPLICATION_PDF;
                break;
            case "png":
                contentType = IMAGE_PNG;
                break;
            case "css":
                contentType = TEXT_CSS;
                break;
            case "js":
                contentType = APPLICATION_JS;
                break;
        }
        resp.setHeader(CONTENT_LENGTH, String.valueOf(contentlength));
        resp.setContentLength(contentlength);
        resp.setHeader(CONTENT_TYPE, contentType);
        resp.setContentType(contentType);
        getFileContent(file, resp);
    }

    private void getFileContent(File file, Response response) throws IOException {
        String thisLine;
        try (BufferedReader br =
                     new BufferedReader(new FileReader(file))) {
            while ((thisLine = br.readLine()) != null) {
                response.getOutputStream().write(thisLine.getBytes());
            }

        }
    }

}
