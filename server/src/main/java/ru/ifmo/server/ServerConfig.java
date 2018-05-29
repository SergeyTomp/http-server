package ru.ifmo.server;

import java.util.*;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

/**
 * Holds server configs: local port, handler mappings, etc.
 */
public class ServerConfig {
    /** Default local port. */
    public static final int DFLT_PORT = 8081;

    private int port = DFLT_PORT;
    private Map<String, Handler> handlers;
    private Map<String, Class<? extends Handler>> handlerClasses;
    private int socketTimeout;
    private Collection<Class<?>> classes;
    private CompressionType compressionType;

    public ServerConfig() {
        handlers = new HashMap<>();
        classes = new HashSet<>();
        handlerClasses = new HashMap<>();
    }

    public ServerConfig(ServerConfig config) {
        this();
        port = config.port;
        handlers = new HashMap<>(config.handlers);
        socketTimeout = config.socketTimeout;
        classes = new HashSet<>(config.classes);
        handlerClasses = new HashMap<>(config.handlerClasses);
        compressionType = config.compressionType;
    }

    /**
     * @return Local port.
     */
    public int getPort() {
        return port;
    }

    /**
     * Define local port.
     *
     * @param port TCP port.
     * @return Itself for chaining.
     */
    public ServerConfig setPort(int port) {
        this.port = port;

        return this;
    }

    /**
     * Add handler mapping.
     *
     * @param path Path which will be associated with this handler.
     * @param handler Request handler.
     * @return Itself for chaining.
     */
    public ServerConfig addHandler(String path, Handler handler) {
        handlers.put(path, handler);

        return this;
    }
    public ServerConfig addHandlerClass(String path, Class<? extends Handler> hndCls) {
        handlerClasses.put(path, hndCls);
        return this;
    }
    public ServerConfig addHandlerClasses(Map<String, Class<? extends Handler>> hndCls){
        handlerClasses.putAll(hndCls);
        return this;
    }
    /**
     * Add handler mappings.
     *
     * @param handlers Map paths to handlers.
     * @return Itself for chaining.
     */
    public ServerConfig addHandlers(Map<String, Handler> handlers) {
        this.handlers.putAll(handlers);

        return this;
    }

    Handler handler(String path) {
        return handlers.get(path);
    }

    public CompressionType getCompressionType() {
        return compressionType;
    }
    public ServerConfig setCompression(CompressionType compression) {
        this.compressionType = compression;

        return this;
    }

    /**
     * @return Current handler mapping.
     */
    public Map<String, Handler> getHandlers() {
        return handlers;
    }

    /**
     * Set handler mappings.
     *
     * @param handlers Handler mappings.
     */
    public void setHandlers(Map<String, Handler> handlers) {
        this.handlers = handlers;
    }

    /**
     * @return Socket timeout value.
     */
    public int getSocketTimeout() {
        return socketTimeout;
    }

    /**
     * Set socket timeout. By default it's unlimited.
     *
     * @param socketTimeout Socket timeout, 0 means no timeout.
     * @return Itself for chaining.
     */
    public ServerConfig setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;

        return this;
    }

    public ServerConfig addClasses(Collection<Class<?>> classes) {
        this.classes.addAll(classes);

        return this;
    }

    public void addClass(Class<?> cls) {
        this.classes.add(cls);
    }

    public Collection<Class<?>> getClasses() {
        return classes;
    }
    public Map<String, Class<? extends Handler>> getHandlerClasses() {
        return handlerClasses;}

    public void setErrorPage(int code, String pageName) {
        InputStream in = Server.class.getClassLoader().getResourceAsStream(pageName);
        BufferedReader rdr = new BufferedReader(new InputStreamReader(in));
        List<String> lines = rdr.lines().collect(toList());
        StringBuilder sb = new StringBuilder();
        lines.forEach(sb::append);
        CustomErrorResponse.coderespMap.put(code, sb.toString());
    }

    @Override
    public String toString() {
        return "ServerConfig{" +
                "port=" + port +
                ", handlers=" + handlers +
                ", socketTimeout=" + socketTimeout +
                ", compressionType=" + compressionType +
                '}';
    }
}
