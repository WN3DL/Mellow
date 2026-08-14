package com.roxiun.mellow.api.seraph;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Assert;
import org.junit.Test;

public class SeraphApiTest {

    @Test
    public void submitBlacklistReportUsesExpectedHeadersAndPayload()
        throws Exception {
        FakeHttpURLConnection connection = new FakeHttpURLConnection(
            new URL("https://api.seraph.si/addsniper"),
            200,
            "{\"success\":true}",
            null
        );

        SeraphApi api = new SeraphApi(null) {
            @Override
            protected HttpURLConnection openConnection(URL url) {
                Assert.assertEquals(
                    "https://api.seraph.si/addsniper",
                    url.toString()
                );
                return connection;
            }
        };

        SeraphApi.BlacklistSubmissionResult result = api.submitBlacklistReport(
            "00000000-0000-0000-0000-000000000123",
            "secret-key",
            SeraphBlacklistReportType.SNIPING,
            "queue sniping"
        );

        Assert.assertTrue(result.isSuccess());
        Assert.assertEquals(200, result.getStatusCode());
        Assert.assertEquals("POST", connection.getRequestMethod());
        Assert.assertTrue(connection.getDoOutput());
        Assert.assertEquals(
            "secret-key",
            connection.getRequestProperty("seraph-api-key")
        );
        Assert.assertEquals(
            "application/json; charset=UTF-8",
            connection.getRequestProperty("Content-Type")
        );

        JsonObject payload = new JsonParser()
            .parse(connection.getWrittenBody())
            .getAsJsonObject();
        Assert.assertEquals(
            "00000000-0000-0000-0000-000000000123",
            payload.get("uuid").getAsString()
        );
        Assert.assertEquals("sniping", payload.get("report_type").getAsString());
        Assert.assertEquals("queue sniping", payload.get("reason").getAsString());
    }

    @Test
    public void fetchClientTypeCachesResolvedMissingResponses() throws Exception {
        AtomicInteger openedConnections = new AtomicInteger();
        Queue<FakeHttpURLConnection> connections = new ArrayDeque<>();
        connections.add(
            new FakeHttpURLConnection(
                new URL("https://api.seraph.si/private-access/client"),
                404,
                null,
                null
            )
        );

        SeraphApi api = new SeraphApi(null) {
            @Override
            protected HttpURLConnection openConnection(URL url) {
                openedConnections.incrementAndGet();
                return connections.remove();
            }
        };

        SeraphApi.ClientTypeLookupResult first = api.fetchClientTypeResult(
            "00000000000000000000000000000001",
            "secret-key"
        );
        SeraphApi.ClientTypeLookupResult second = api.fetchClientTypeResult(
            "00000000000000000000000000000001",
            "secret-key"
        );

        Assert.assertTrue(first.isResolved());
        Assert.assertNull(first.getClientType());
        Assert.assertTrue(second.isResolved());
        Assert.assertNull(second.getClientType());
        Assert.assertEquals(1, openedConnections.get());
    }

    @Test
    public void fetchClientTypeDoesNotCacheTransientFailures() throws Exception {
        AtomicInteger openedConnections = new AtomicInteger();

        SeraphApi api = new SeraphApi(null) {
            @Override
            protected HttpURLConnection openConnection(URL url) throws IOException {
                openedConnections.incrementAndGet();
                return new FakeHttpURLConnection(url, 429, null, null);
            }
        };

        SeraphApi.ClientTypeLookupResult first = api.fetchClientTypeResult(
            "00000000000000000000000000000002",
            "secret-key"
        );
        SeraphApi.ClientTypeLookupResult second = api.fetchClientTypeResult(
            "00000000000000000000000000000002",
            "secret-key"
        );

        Assert.assertFalse(first.isResolved());
        Assert.assertNull(first.getClientType());
        Assert.assertFalse(second.isResolved());
        Assert.assertNull(second.getClientType());
        Assert.assertEquals(2, openedConnections.get());
    }

    private static final class FakeHttpURLConnection extends HttpURLConnection {

        private final int responseCode;
        private final byte[] responseBody;
        private final byte[] errorBody;
        private final ByteArrayOutputStream requestBody =
            new ByteArrayOutputStream();
        private final Map<String, String> requestProperties = new HashMap<>();

        private FakeHttpURLConnection(
            URL url,
            int responseCode,
            String responseBody,
            String errorBody
        ) {
            super(url);
            this.responseCode = responseCode;
            this.responseBody = responseBody == null
                ? new byte[0]
                : responseBody.getBytes(StandardCharsets.UTF_8);
            this.errorBody = errorBody == null
                ? null
                : errorBody.getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public void disconnect() {}

        @Override
        public boolean usingProxy() {
            return false;
        }

        @Override
        public void connect() {}

        @Override
        public void setRequestProperty(String key, String value) {
            requestProperties.put(key, value);
        }

        @Override
        public String getRequestProperty(String key) {
            return requestProperties.get(key);
        }

        @Override
        public OutputStream getOutputStream() {
            return requestBody;
        }

        @Override
        public int getResponseCode() {
            return responseCode;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (responseCode < 200 || responseCode >= 300) {
                throw new IOException("HTTP " + responseCode);
            }
            return new ByteArrayInputStream(responseBody);
        }

        @Override
        public InputStream getErrorStream() {
            if (errorBody == null) {
                return null;
            }
            return new ByteArrayInputStream(errorBody);
        }

        private String getWrittenBody() {
            return new String(requestBody.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
