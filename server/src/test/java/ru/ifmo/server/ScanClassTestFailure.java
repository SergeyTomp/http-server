package ru.ifmo.server;

import org.apache.commons.io.IOUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ru.ifmo.server.scan.*;

import java.util.ArrayList;
import java.util.Collection;

public class ScanClassTestFailure {
    private static Server server;
    private CloseableHttpClient client;
    private static ServerConfig cfg;

    @Before
    public void init() {
        client = HttpClients.createDefault();
    }

    @After
    public void close() {
        IOUtils.closeQuietly(client);
        client = null;

        IOUtils.closeQuietly(server);
        server = null;
    }

    @Test(expected = ServerException.class)
    public void testInvalidParameters() throws ServerException {
        Collection<Class<?>> classes = new ArrayList<>();
        classes.add(InvalidParameters.class);
        cfg = new ServerConfig().addClasses(classes);
        server = Server.start(cfg);
    }

    @Test(expected = ServerException.class)
    public void testInvalidType() throws ServerException {
        Collection<Class<?>> classes = new ArrayList<>();
        classes.add(InvalidType.class);
        cfg = new ServerConfig().addClasses(classes);
        server = Server.start(cfg);
    }

    @Test(expected = ServerException.class)
    public void testInvalidModifier() throws ServerException {
        Collection<Class<?>> classes = new ArrayList<>();
        classes.add(InvalidModifier.class);
        cfg = new ServerConfig().addClasses(classes);
        server = Server.start(cfg);
    }
}
