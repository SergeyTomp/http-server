package ru.ifmo.server;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.*;
import ru.ifmo.server.scan.HandlerClassToAdd;
import ru.ifmo.server.scan.ScanClassHandler;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;
import static ru.ifmo.server.TestUtils.assertStatusCode;

/**
 * Tests main server functionality.
 */
public class ServerTest {
    private static final HttpHost host = new HttpHost("localhost", ServerConfig.DFLT_PORT);

    private static final String SUCCESS_URL = "/test_success";
    private static final String SUCCES_SESSION_OPEN = "/test_sess_open";
    private static final String SUCCES_SESSION_CHECK = "/test_sess_check";
    private static final String NOT_FOUND_URL = "/test_not_found";
    private static final String SERVER_ERROR_URL = "/test_fail";
    private static final String COOKIE_URL = "/test_cookie";
    private static final String POST_PUT_URL= "/test_post_put";

    private static Server server;
    private static CloseableHttpClient client;

    @BeforeClass
    public static void initialize() {
        startAll(defaultConfig());
    }

    public static ServerConfig defaultConfig (){
        Collection<Class<?>> classes = new ArrayList<>();
        classes.add(ScanClassHandler.class);
        ServerConfig cfg = new ServerConfig()
                .addHandler(SUCCESS_URL, new SuccessHandler())
                .addHandler(SERVER_ERROR_URL, new FailHandler())
                .addHandler(POST_PUT_URL, new SuccessPostPutHandler());
                .addHandler(SERVER_ERROR_URL, new FailHandler())
                .addHandler(SUCCES_SESSION_OPEN, new SessionOpenHandler())
                .addHandler(SUCCES_SESSION_CHECK, new SessionCheckHandler())
                .addHandler(COOKIE_URL, new CookieHandler())
                .addHandler(SERVER_ERROR_URL, new FailHandler())
                .addClasses(classes)
                .addHandlerClass("/addHandler", HandlerClassToAdd.class);
        return cfg;
    }

    public static void startAll(ServerConfig cfg) {
        server = Server.start(cfg);
    }

    @AfterClass
    public static void stop() {
        IOUtils.closeQuietly(server);
        server = null;
    }

    @Before
    public void init() {
        client = HttpClients.createDefault();
    }

    @After
    public void close() {
        IOUtils.closeQuietly(client);
        client = null;
    }

    @Test
    public void testSuccess() throws Exception {
        URI uri = new URIBuilder(SUCCESS_URL)
                .addParameter("1", "1")
                .addParameter("2", "2")
                .addParameter("testArg1", "testValue1")
                .addParameter("testArg2", "2")
                .addParameter("testArg3", "testVal3")
                .addParameter("testArg4", "")
                .build();

        HttpGet get = new HttpGet(uri);
        CloseableHttpResponse response = client.execute(host, get);
        assertStatusCode(HttpStatus.SC_OK, response);
        assertEquals(SuccessHandler.TEST_RESPONSE +
                        "<br>{1=1, 2=2, testArg1=testValue1, testArg2=2, testArg3=testVal3, testArg4=null}" +
                        SuccessHandler.CLOSE_HTML, EntityUtils.toString(response.getEntity()));
        }

    @Test
    public void testSession () throws URISyntaxException, IOException, InterruptedException {
        URI uri = new URIBuilder(SUCCES_SESSION_OPEN)
                .addParameter("login", "password")
                .build();
        HttpGet get = new HttpGet(uri);
        CloseableHttpResponse response = client.execute(host, get);
        assertStatusCode(HttpStatus.SC_OK, response);

        uri = new URIBuilder(SUCCES_SESSION_CHECK)
                .addParameter("login", "password")
                .build();
        get = new HttpGet(uri);
        response = client.execute(host, get);
        assertStatusCode(HttpStatus.SC_OK, response);
        assertEquals("Session data are invalid","password", EntityUtils.toString(response.getEntity()));

        IOUtils.closeQuietly(client);
        client = null;
        Thread.sleep(2000);
        client = HttpClients.createDefault();
        response = client.execute(host, get);
        assertStatusCode(HttpStatus.SC_OK, response);
        assertNotEquals("Session data are invalid","password", EntityUtils.toString(response.getEntity()));
    }

    @Test
    public void testCookie() throws Exception {

        HttpGet req = new HttpGet(COOKIE_URL);
        req.setHeader("Cookie", "somename=somevalue");
        CloseableHttpResponse response = client.execute(host, req);
        assertEquals("somename=somevalue", EntityUtils.toString(response.getEntity()));
    }

    @Test
    public void testNotFound() throws Exception {
        HttpGet get = new HttpGet(NOT_FOUND_URL);
        CloseableHttpResponse response = client.execute(host, get);
        assertStatusCode(HttpStatus.SC_NOT_FOUND, response);
        assertNotNull(EntityUtils.toString(response.getEntity()));
    }

    @Test
    public void testServerError() throws Exception {
        HttpGet get = new HttpGet(SERVER_ERROR_URL);
        CloseableHttpResponse response = client.execute(host, get);
        assertStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR, response);
        assertNotNull(EntityUtils.toString(response.getEntity()));
    }

    @Test
    public void testPost() throws Exception {
        HttpPost request = new HttpPost(POST_PUT_URL);


        String bodey = "Some body";

        request.addHeader("Content-Type", "text/plain");

        request.setEntity(new StringEntity(bodey, "UTF-8"));

        CloseableHttpResponse response = client.execute(host, request);

        assertStatusCode(HttpStatus.SC_OK, response);
        assertEquals(SuccessHandler.TEST_RESPONSE +
                        "<br>Some body" +
                        SuccessHandler.CLOSE_HTML,
                EntityUtils.toString(response.getEntity()));
    }
    @Ignore("no method developed")
    @Test
    public void testPut() throws Exception {

        HttpPost request = new HttpPost(POST_PUT_URL);


        String bodey = "Some body";

        request.addHeader("Content-Type", "text/plain");

        request.setEntity(new StringEntity(bodey, "UTF-8"));

        CloseableHttpResponse response = client.execute(host, request);
        assertStatusCode(HttpStatus.SC_OK, response);
        assertEquals(SuccessHandler.TEST_RESPONSE +
                        "<br>Some body" +
                        SuccessHandler.CLOSE_HTML,
                EntityUtils.toString(response.getEntity()));
    }

    @Test
    public void testDelete() throws Exception {
        HttpRequest request = new HttpDelete(SUCCESS_URL);
        assertNotImplemented(request);
    }

    @Test
    public void testHead() throws Exception {
        HttpRequest request = new HttpHead(SUCCESS_URL);
        CloseableHttpResponse response = client.execute(host, request);

        assertStatusCode(HttpStatus.SC_OK, response);
    }

    @Test
    public void testOptions() throws Exception {
        HttpRequest request = new HttpOptions(SUCCESS_URL);
        assertNotImplemented(request);
    }

    @Test
    public void testTrace() throws Exception {
        HttpRequest request = new HttpTrace(SUCCESS_URL);
        assertNotImplemented(request);
    }

    @Test
    public void testPatch() throws Exception {
        HttpRequest request = new HttpPatch(SUCCESS_URL);
        assertNotImplemented(request);
    }

    @Test
    public void testScanClassGET() throws IOException, URISyntaxException {
        URI uri = new URI("/scanGET");
        HttpGet get = new HttpGet(uri);
        CloseableHttpResponse response = client.execute(host, get);
        assertStatusCode(HttpStatus.SC_OK, response);
        assertEquals(SuccessHandler.TEST_RESPONSE + "<br>/scanGET" + SuccessHandler.CLOSE_HTML,
                EntityUtils.toString(response.getEntity()));
    }

    @Test
    public void testAddHandlersClasses() throws URISyntaxException, IOException {
        URI uri = new URI("/addHandler");
        HttpGet get = new HttpGet(uri);
        CloseableHttpResponse response = client.execute(host, get);
        assertStatusCode(HttpStatus.SC_OK, response);
        assertEquals(SuccessHandler.TEST_RESPONSE + "<br>/addHandler" + SuccessHandler.CLOSE_HTML,
                EntityUtils.toString(response.getEntity()));
    }

    private void assertNotImplemented(HttpRequest request) throws Exception {
        CloseableHttpResponse response = client.execute(host, request);
        assertStatusCode(HttpStatus.SC_NOT_IMPLEMENTED, response);
        assertNotNull(EntityUtils.toString(response.getEntity()));
    }
}
