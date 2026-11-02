/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLSession;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.RuntimeConnection;

class CopilotRequestHandlerProtocolTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int DEADLINE_SECONDS = 5;

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"logical-invocation", ""})
    void httpStartPreservesInvocationIdentityAcrossPhysicalRetries(String invocationId) throws Exception {
        var handler = new MetadataHandler(false);
        try (var peer = new ProtocolPeer(handler)) {
            for (String requestId : List.of("physical-first", "physical-retry")) {
                peer.startRequest(requestId, invocationId, false);
                peer.sendChunk(requestId, "request-body", null, true);
                JsonNode head = peer.nextMethod();
                assertEquals(200, head.path("params").path("status").asInt());
                peer.ack(head);
                JsonNode end = peer.nextMethod();
                assertTrue(end.path("params").path("end").asBoolean());
                assertFalse(end.path("params").has("error"), end.toString());
                peer.ack(end);
                CopilotRequestContext ctx = handler.contexts.poll(DEADLINE_SECONDS, TimeUnit.SECONDS);
                assertNotNull(ctx);
                assertEquals(invocationId, ctx.agentInvocationId());
                assertEquals(invocationId, ctx.withUrl("http://rewritten.test").agentInvocationId());
                assertEquals(invocationId, ctx.withHeaders(Map.of()).agentInvocationId());
                assertSame(ctx.cancellation(), ctx.withUrl("http://rewritten.test").cancellation());
                assertEquals(requestId, ctx.requestId());
                assertEquals("session", ctx.sessionId());
                assertEquals("trajectory", ctx.agentId());
                assertEquals("parent", ctx.parentAgentId());
                assertEquals("conversation-agent", ctx.interactionType());
                assertEquals(List.of("one", "two"), ctx.headers().get("x-multiple"));
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void websocketRetainsQueuedAndLiveInvocationIdentityAndLegacyHooks(boolean legacy) throws Exception {
        var handler = new MetadataHandler(legacy);
        try (var peer = new ProtocolPeer(handler)) {
            peer.sendChunk("reused-socket", "queued", "queued-invocation", false);
            peer.startRequest("reused-socket", "opening-invocation", true);
            JsonNode head = peer.nextMethod();
            assertEquals(101, head.path("params").path("status").asInt());
            peer.ack(head);
            CopilotRequestContext connection = handler.contexts.poll(DEADLINE_SECONDS, TimeUnit.SECONDS);
            assertNotNull(connection);
            assertEquals("opening-invocation", connection.agentInvocationId());
            assertEquals("opening-invocation", connection.withHeaders(Map.of()).agentInvocationId());
            assertMessage(handler, connection, "queued", "queued-invocation", legacy, false);

            for (String id : new String[]{"later-invocation", "later-invocation", null, ""}) {
                boolean binary = "later-invocation".equals(id);
                peer.sendChunk("reused-socket", "message", id, false, binary);
                assertMessage(handler, connection, "message", id, legacy, binary);
            }
            assertEquals("opening-invocation", connection.agentInvocationId());
            peer.sendChunk("reused-socket", "", null, true);
            JsonNode end = peer.nextMethod();
            assertTrue(end.path("params").path("end").asBoolean());
            assertFalse(end.path("params").has("error"), end.toString());
            peer.ack(end);
        }
    }

    private static void assertMessage(MetadataHandler handler, CopilotRequestContext connection, String text, String id,
            boolean legacy, boolean binary) throws Exception {
        ReceivedMessage received = handler.messages.poll(DEADLINE_SECONDS, TimeUnit.SECONDS);
        assertNotNull(received);
        assertEquals(text, received.message().text());
        assertEquals(binary, received.message().binary());
        if (legacy) {
            assertNull(received.context());
        } else {
            CopilotRequestContext ctx = received.context();
            assertNotNull(ctx);
            assertNotSame(connection, ctx);
            assertEquals(id, ctx.agentInvocationId());
            assertEquals(connection.requestId(), ctx.requestId());
            assertEquals(connection.sessionId(), ctx.sessionId());
            assertEquals(connection.agentId(), ctx.agentId());
            assertSame(connection.cancellation(), ctx.cancellation());
        }
    }

    private record ReceivedMessage(CopilotWebSocketMessage message, CopilotRequestContext context) {
    }

    private static final class MetadataHandler extends CopilotRequestHandler {
        private final boolean legacy;
        private final BlockingQueue<CopilotRequestContext> contexts = new LinkedBlockingQueue<>();
        private final BlockingQueue<ReceivedMessage> messages = new LinkedBlockingQueue<>();

        MetadataHandler(boolean legacy) {
            this.legacy = legacy;
        }

        @Override
        protected HttpResponse<InputStream> sendRequest(HttpRequest request, CopilotRequestContext ctx) {
            contexts.add(ctx);
            assertEquals(List.of("one", "two"), request.headers().allValues("x-multiple"));
            assertEquals(12, request.bodyPublisher().orElseThrow().contentLength());
            return new EmptyResponse(new ByteArrayInputStream(new byte[0]), request);
        }

        @Override
        protected CopilotWebSocketHandler openWebSocket(CopilotRequestContext ctx) {
            contexts.add(ctx);
            return legacy ? new LegacySocket(ctx, messages) : new ContextSocket(ctx, messages);
        }
    }

    private static class LegacySocket extends CopilotWebSocketHandler {
        protected final BlockingQueue<ReceivedMessage> messages;

        LegacySocket(CopilotRequestContext ctx, BlockingQueue<ReceivedMessage> messages) {
            super(ctx);
            this.messages = messages;
        }

        @Override
        public void sendRequestMessage(CopilotWebSocketMessage message) {
            messages.add(new ReceivedMessage(message, null));
        }
    }

    private static final class ContextSocket extends LegacySocket {
        ContextSocket(CopilotRequestContext ctx, BlockingQueue<ReceivedMessage> messages) {
            super(ctx, messages);
        }

        @Override
        public void sendRequestMessage(CopilotWebSocketMessage message, CopilotRequestContext ctx) {
            messages.add(new ReceivedMessage(message, ctx));
        }
    }

    private static final class ProtocolPeer implements AutoCloseable {
        private final CopilotClient client;
        private final Socket socket;
        private int nextId = 100000;

        ProtocolPeer(CopilotRequestHandler handler) throws Exception {
            try (var listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
                listener.setSoTimeout(DEADLINE_SECONDS * 1000);
                client = new CopilotClient(new CopilotClientOptions()
                        .setConnection(RuntimeConnection.forUri("http://127.0.0.1:" + listener.getLocalPort()))
                        .setRequestHandler(handler));
                var starting = client.start();
                socket = listener.accept();
                socket.setSoTimeout(DEADLINE_SECONDS * 1000);
                JsonNode connect = nextMethod();
                assertEquals("connect", connect.path("method").asText());
                respond(connect, MAPPER.valueToTree(Map.of("ok", true, "protocolVersion", 3, "version", "test")));
                JsonNode provider = nextMethod();
                assertEquals("llmInference.setProvider", provider.path("method").asText());
                respond(provider, MAPPER.valueToTree(Map.of("success", true)));
                starting.get(DEADLINE_SECONDS, TimeUnit.SECONDS);
            }
        }

        void startRequest(String requestId, String invocationId, boolean websocket) throws Exception {
            ObjectNode params = MAPPER.createObjectNode().put("requestId", requestId)
                    .put("method", websocket ? "GET" : "POST").put("url", "http://unused.test")
                    .put("sessionId", "session").put("agentId", "trajectory").put("parentAgentId", "parent")
                    .put("interactionType", "conversation-agent").put("transport", websocket ? "websocket" : "http");
            params.set("headers", MAPPER.valueToTree(Map.of("x-multiple", List.of("one", "two"))));
            if (invocationId != null) {
                params.put("agentInvocationId", invocationId);
            }
            request("llmInference.httpRequestStart", params);
        }

        void sendChunk(String requestId, String data, String invocationId, boolean end) throws Exception {
            sendChunk(requestId, data, invocationId, end, false);
        }

        void sendChunk(String requestId, String data, String invocationId, boolean end, boolean binary)
                throws Exception {
            if (binary) {
                data = Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
            }
            ObjectNode params = MAPPER.createObjectNode().put("requestId", requestId).put("data", data).put("end", end);
            params.put("binary", binary);
            if (invocationId != null) {
                params.put("agentInvocationId", invocationId);
            }
            request("llmInference.httpRequestChunk", params);
        }

        void request(String method, JsonNode params) throws Exception {
            ObjectNode frame = MAPPER.createObjectNode().put("jsonrpc", "2.0").put("id", nextId++).put("method",
                    method);
            frame.set("params", params);
            write(frame);
        }

        JsonNode nextMethod() throws Exception {
            while (true) {
                InputStream in = socket.getInputStream();
                var header = new ByteArrayOutputStream();
                while (!header.toString(StandardCharsets.US_ASCII).endsWith("\r\n\r\n")) {
                    int b = in.read();
                    assertNotEquals(-1, b, "Unexpected protocol EOF");
                    header.write(b);
                }
                String text = header.toString(StandardCharsets.US_ASCII);
                int length = Integer.parseInt(text.split("\r\n")[0].split(":")[1].trim());
                byte[] body = in.readNBytes(length);
                assertEquals(length, body.length);
                JsonNode frame = MAPPER.readTree(body);
                if (frame.has("method")) {
                    return frame;
                }
                assertFalse(frame.has("error"), frame.toString());
            }
        }

        void ack(JsonNode frame) throws Exception {
            respond(frame, MAPPER.valueToTree(Map.of("accepted", true)));
        }

        void respond(JsonNode frame, JsonNode result) throws Exception {
            ObjectNode reply = MAPPER.createObjectNode().put("jsonrpc", "2.0");
            reply.set("id", frame.get("id"));
            reply.set("result", result);
            write(reply);
        }

        void write(JsonNode frame) throws Exception {
            byte[] body = MAPPER.writeValueAsBytes(frame);
            var out = socket.getOutputStream();
            out.write(("Content-Length: " + body.length + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            out.write(body);
            out.flush();
        }

        @Override
        public void close() throws Exception {
            try {
                client.forceStop().get(DEADLINE_SECONDS, TimeUnit.SECONDS);
            } finally {
                socket.close();
                client.close();
            }
        }
    }

    private record EmptyResponse(InputStream body, HttpRequest request) implements HttpResponse<InputStream> {
        @Override
        public int statusCode() {
            return 200;
        }
        @Override
        public Optional<HttpResponse<InputStream>> previousResponse() {
            return Optional.empty();
        }
        @Override
        public HttpHeaders headers() {
            return HttpHeaders.of(Map.of(), (name, value) -> true);
        }
        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }
        @Override
        public URI uri() {
            return request.uri();
        }
        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }
}
