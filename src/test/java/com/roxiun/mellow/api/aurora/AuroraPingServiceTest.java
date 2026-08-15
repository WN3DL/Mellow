package com.roxiun.mellow.api.aurora;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.Assert;
import org.junit.Test;

public class AuroraPingServiceTest {

    @Test
    public void fetchPingDoesNotSendAnApiKey() throws IOException {
        AtomicReference<Request> capturedRequest = new AtomicReference<>();
        OkHttpClient client = new OkHttpClient.Builder()
            .addInterceptor(chain -> {
                capturedRequest.set(chain.request());
                return response(
                    chain.request(),
                    "{\"success\":true,\"data\":[{\"avg\":87}]}"
                );
            })
            .build();

        AuroraPingService service = new AuroraPingService(client);

        Assert.assertEquals(87, service.fetchPingBlocking("compact-uuid"));
        Assert.assertEquals(
            "compact-uuid",
            capturedRequest.get().url().queryParameter("uuid")
        );
        Assert.assertNull(capturedRequest.get().url().queryParameter("key"));
    }

    private static Response response(Request request, String body) {
        return new Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(ResponseBody.create(MediaType.parse("application/json"), body))
            .build();
    }
}
