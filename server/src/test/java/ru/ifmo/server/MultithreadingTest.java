package ru.ifmo.server;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class MultithreadingTest {

    private static final HttpHost host = new HttpHost("localhost", ServerConfig.DFLT_PORT);
    static final Object monitor = new Object();

    private static final String SUCCESS_URL1 = "/test_success1";
    private static final String SUCCESS_URL2 = "/test_success2";
    private static final HttpGet get1 = new HttpGet(SUCCESS_URL1);
    private static final HttpGet get2 = new HttpGet(SUCCESS_URL2);
    private static Server server;
    private static CloseableHttpClient client1;
    private static CloseableHttpClient client2;

    static volatile boolean isFinishedThread1 = false;
    static volatile boolean isFinishedThread2 = false;

    @BeforeClass
    public static void initialize() {
        ServerConfig cfg = new ServerConfig()
                .addHandler(SUCCESS_URL1, new ThreadHandler1())
                .addHandler(SUCCESS_URL2, new ThreadHandler2());

        server = Server.start(cfg);
        client1 = HttpClients.createDefault();
        client2 = HttpClients.createDefault();
    }

    @AfterClass
    public static void stop() {
        IOUtils.closeQuietly(server);
        IOUtils.closeQuietly(client1);
        IOUtils.closeQuietly(client2);

        server = null;
        client1 = null;
        client2 = null;

        isFinishedThread1 = false;
        isFinishedThread2 = false;
    }

    public class RequestHandler implements Runnable {
        CloseableHttpClient client;
        HttpHost host;
        HttpGet get;

        public RequestHandler(CloseableHttpClient client, HttpHost host, HttpGet get) {
            this.client = client;
            this.host = host;
            this.get = get;
        }

        public void run() {
            try {
                this.client.execute(this.host, this.get);
            } catch (IOException e) {
                System.out.println("Error trying to process request..");
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testProcessConnection() throws InterruptedException {
        new Thread(new RequestHandler(client1, host, get1)).start();
        new Thread(new RequestHandler(client2, host, get2)).start();
        synchronized (monitor){
            monitor.wait();
        }
        assertEquals(false, isFinishedThread1);
        assertEquals(true, isFinishedThread2);
        synchronized (monitor){
            monitor.wait();
        }
        assertEquals(true, isFinishedThread1);
        assertEquals(true, isFinishedThread2);
    }
}