package ru.ifmo.server;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static ru.ifmo.server.TestUtils.assertStatusCode;

/**
 * Tests main server functionality.
 */
public class ServerTest {
    private static final HttpHost host = new HttpHost("localhost", ServerConfig.DFLT_PORT);

    private static final String SUCCESS_URL = "/test_success";
    private static final String SUCCESS_SESSION = "/test_sess_success";
    private static final String SUCCES_SESSION_OPEN = "/test_sess_open";
    private static final String SUCCES_SESSION_CHECK = "/test_sess_check";
    private static final String NOT_FOUND_URL = "/test_not_found";
    private static final String SERVER_ERROR_URL = "/test_fail";
    private static final String COOKIE_URL = "/test_cookie";

    private static Server server;
    private static CloseableHttpClient client;

    @BeforeClass
    public static void initialize() {
        startAll(defaultConfig());
    }

    public static ServerConfig defaultConfig (){
        ServerConfig cfg = new ServerConfig()
                .addHandler(SUCCESS_URL, new SuccessHandler())
                .addHandler(SERVER_ERROR_URL, new FailHandler())
                .addHandler(SUCCES_SESSION_OPEN, new SessionOpenHandler())
                .addHandler(SUCCES_SESSION_CHECK, new SessionCheckHandler())
                .addHandler(COOKIE_URL, new CookieHandler());
        return cfg;
    }

    public static void startAll(ServerConfig cfg) {
        server = Server.start(cfg);
        client = HttpClients.createDefault();
    }

    @AfterClass
    public static void stop() {
        IOUtils.closeQuietly(server);
        IOUtils.closeQuietly(client);

        server = null;
        client = null;
    }

    @Test
    public void testSuccess() throws Exception {
        // TODO test headers
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
                        SuccessHandler.CLOSE_HTML,
                EntityUtils.toString(response.getEntity()));
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

        Thread.sleep(2000);

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
        HttpPost request = new HttpPost(SUCCESS_URL);

        assertNotImplemented(request);
    }
    @Ignore ("no method developed")
    @Test
    public void testPut() throws Exception {

        URI uri = new URIBuilder(SUCCESS_URL)
                .addParameter("1", "1")
                .addParameter("2", "2")
                .addParameter("testArg1", "testValue1")
                .addParameter("testArg2", "2")
                .addParameter("testArg3", "testVal3")
                .addParameter("testArg4", "")
                .build();

        HttpPost request = new HttpPost(uri);


        List<NameValuePair> pairs = new ArrayList<NameValuePair>();
        pairs.add(new BasicNameValuePair("paramName", "paramValue"));

        request.addHeader("Content-Type", "text/text");

        request.setEntity(new UrlEncodedFormEntity(pairs ));

        CloseableHttpResponse response = client.execute(host, request);

        assertStatusCode(HttpStatus.SC_OK, response);
        assertEquals(SuccessHandler.TEST_RESPONSE +
                        "<br>{1=1, 2=2, testArg1=testValue1, testArg2=2, testArg3=testVal3, testArg4=null}" +
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

        assertStatusCode(HttpStatus.SC_NOT_IMPLEMENTED, response);
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

    private void assertNotImplemented(HttpRequest request) throws Exception {
        CloseableHttpResponse response = client.execute(host, request);

        assertStatusCode(HttpStatus.SC_NOT_IMPLEMENTED, response);
        assertNotNull(EntityUtils.toString(response.getEntity()));
    }


}
