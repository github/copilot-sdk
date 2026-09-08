/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.rpc.SessionSendParams;
import com.github.copilot.rpc.AgentMode;
import com.github.copilot.rpc.Attachment;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.MessageSource;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SendMessageRequest;
import com.github.copilot.rpc.SessionConfig;

@AllowCopilotExperimental
class MessageSourceTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static Stream<MessageSource> sources() {
        return Stream.of(MessageSource.USER, MessageSource.SYSTEM, MessageSource.agent("Reviewer-7"));
    }

    static Stream<Arguments> sourceValues() {
        return Stream.of(Arguments.of(MessageSource.USER, "user"), Arguments.of(MessageSource.SYSTEM, "system"),
                Arguments.of(MessageSource.agent("Reviewer-7"), "agent-Reviewer-7"),
                Arguments.of(MessageSource.agent("agent-Worker"), "agent-agent-Worker"),
                Arguments.of(MessageSource.agent(" Team/α "), "agent- Team/α "),
                Arguments.of(MessageSource.agent(""), "agent-"));
    }

    static Stream<Arguments> sourcesAndModes() {
        return sources().flatMap(source -> Stream.of("enqueue", "immediate").map(mode -> Arguments.of(source, mode)));
    }

    static Stream<Arguments> sourcesAndWaitModes() {
        return sources()
                .flatMap(source -> Stream.of(null, "enqueue", "immediate").map(mode -> Arguments.of(source, mode)));
    }

    static Stream<Arguments> sourcesAndErrors() {
        return sources().flatMap(source -> Stream.of(Outcome.SESSION_ERROR, Outcome.RPC_ERROR).flatMap(
                outcome -> Stream.of(null, "enqueue", "immediate").map(mode -> Arguments.of(source, outcome, mode))));
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "2147483648", "-1"})
    void malformedContentLengthFailsWithIOException(String value) {
        var input = new ByteArrayInputStream(
                ("Content-Length: " + value + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        var error = assertThrows(IOException.class, () -> SendServer.readMessage(input));
        assertTrue(error.getMessage().startsWith("Invalid Content-Length"));
    }

    @ParameterizedTest
    @MethodSource("sourceValues")
    void sourceUsesLowercaseJson(MessageSource source, String value) throws Exception {
        assertEquals(value, source.getValue());
        assertEquals("\"" + value + "\"", MAPPER.writeValueAsString(source));
        assertEquals(source, MAPPER.readValue("\"" + value + "\"", MessageSource.class));
        assertEquals(source, MessageSource.fromValue(value));
    }

    @Test
    void sourceRejectsUnknownValuesAndAcceptsNull() throws Exception {
        assertNull(MessageSource.fromValue(null));
        assertNull(MAPPER.readValue("null", MessageSource.class));
        assertThrows(IllegalArgumentException.class, () -> MessageSource.fromValue("unknown"));
        assertThrows(IllegalArgumentException.class, () -> MessageSource.fromValue("USER"));
        assertThrows(IOException.class, () -> MAPPER.readValue("\"unknown\"", MessageSource.class));
        assertThrows(NullPointerException.class, () -> MessageSource.agent(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"SYSTEM\"", "\"Agent-worker\"", "\"agent\"", "\"\"", "0", "true", "{}", "[]"})
    void sourceRejectsInvalidJson(String json) {
        assertThrows(IOException.class, () -> MAPPER.readValue(json, MessageSource.class));
    }

    @Test
    void agentSourcesHaveCaseSensitiveValueEquality() {
        var source = MessageSource.agent("Reviewer");
        var same = MessageSource.agent("Reviewer");
        assertEquals(source, same);
        assertEquals(source.hashCode(), same.hashCode());
        assertNotEquals(source, MessageSource.agent("reviewer"));
        assertNotEquals(source, MessageSource.SYSTEM);
    }

    @Test
    void defaultSourceIsOmittedAndCanBeCleared() throws Exception {
        var options = new MessageOptions().setPrompt("hello");
        var request = new SendMessageRequest();
        request.setPrompt("hello");

        assertNull(options.getSource());
        assertNull(options.clone().getSource());
        assertNull(request.getSource());
        assertEquals(MAPPER.readTree("{\"prompt\":\"hello\"}"), MAPPER.valueToTree(options));
        assertEquals(MAPPER.readTree("{\"prompt\":\"hello\"}"), MAPPER.valueToTree(request));

        assertSame(options, options.setSource(MessageSource.SYSTEM));
        options.setSource(null);
        request.setSource(MessageSource.USER);
        request.setSource(null);
        assertFalse(MAPPER.valueToTree(options).has("source"));
        assertFalse(MAPPER.valueToTree(request).has("source"));
    }

    @ParameterizedTest
    @MethodSource("sources")
    void optionsAndRequestRoundTripSource(MessageSource source) throws Exception {
        var options = new MessageOptions().setPrompt("hello").setSource(source);
        var request = new SendMessageRequest();
        request.setPrompt("hello");
        request.setSource(source);

        for (Object value : List.of(options, request)) {
            JsonNode json = MAPPER.valueToTree(value);
            assertEquals(source.getValue(), json.get("source").asText());
            assertEquals(source, MAPPER.treeToValue(json, MessageOptions.class).getSource());
            assertEquals(source, MAPPER.treeToValue(json, SendMessageRequest.class).getSource());
        }
    }

    @ParameterizedTest
    @MethodSource("sources")
    void clonePreservesSourceAndOtherOptions(MessageSource source) {
        var options = fullOptions().setSource(source);
        var copy = options.clone();

        assertNotSame(options, copy);
        assertEquals(source, copy.getSource());
        assertEquals(MAPPER.<JsonNode>valueToTree(options), MAPPER.<JsonNode>valueToTree(copy));
        copy.setSource(null).setPrompt("changed").setAttachments(List.of()).setMode("enqueue")
                .setAgentMode(AgentMode.INTERACTIVE).setRequestHeaders(Map.of()).setDisplayPrompt("changed");
        assertEquals(source, options.getSource());
        assertEquals(MAPPER.<JsonNode>valueToTree(fullOptions().setSource(source)),
                MAPPER.<JsonNode>valueToTree(options));
    }

    @Test
    void sendWithoutSourcePreservesLegacyPayload() throws Exception {
        try (var server = new SendServer(Outcome.IDLE);
                var client = server.createClient();
                var session = client.createSession(sessionConfig()).get(5, TimeUnit.SECONDS)) {
            assertEquals("message-1", session.send(new MessageOptions().setPrompt("hello")).get(5, TimeUnit.SECONDS));
            assertEquals(MAPPER.readTree("{\"sessionId\":\"source-session\",\"prompt\":\"hello\"}"),
                    server.takeSendParams());
            assertEquals("message-1", session.send("hello").get(5, TimeUnit.SECONDS));
            assertFalse(server.takeSendParams().has("source"));
        }
    }

    @ParameterizedTest
    @MethodSource("sourcesAndModes")
    void sendForwardsSourceWithoutChangingOtherOptions(MessageSource source, String mode) throws Exception {
        try (var server = new SendServer(Outcome.IDLE);
                var client = server.createClient();
                var session = client.createSession(sessionConfig()).get(5, TimeUnit.SECONDS)) {
            var options = fullOptions().setMode(mode).setSource(source);
            assertEquals("message-1", session.send(options).get(5, TimeUnit.SECONDS));

            var expected = MAPPER.createObjectNode().put("sessionId", "source-session").put("prompt", "hello")
                    .put("mode", mode).put("agentMode", "plan").put("displayPrompt", "display")
                    .put("source", source.getValue());
            expected.set("attachments", MAPPER.valueToTree(options.getAttachments()));
            expected.set("requestHeaders", MAPPER.valueToTree(Map.of("X-Trace", "trace-id")));
            assertEquals(expected, server.takeSendParams());
        }
    }

    @ParameterizedTest
    @MethodSource("sourcesAndWaitModes")
    void sourceCompletesOnIdleWithoutAssistantMessage(MessageSource source, String mode) throws Exception {
        try (var server = new SendServer(Outcome.IDLE);
                var client = server.createClient();
                var session = client.createSession(sessionConfig()).get(5, TimeUnit.SECONDS)) {
            var options = new MessageOptions().setPrompt("context").setSource(source).setMode(mode);
            var expected = MAPPER.createObjectNode().put("sessionId", "source-session").put("prompt", "context")
                    .put("source", source.getValue());
            if (mode != null) {
                expected.put("mode", mode);
            }
            assertNull(session.sendAndWait(options).get(5, TimeUnit.SECONDS));
            assertEquals(expected, server.takeSendParams());
            assertNull(session.sendAndWait(options, 5_000).get(5, TimeUnit.SECONDS));
            assertEquals(expected, server.takeSendParams());
        }
    }

    @ParameterizedTest
    @MethodSource("sourcesAndErrors")
    void sourceDoesNotSuppressErrors(MessageSource source, Outcome outcome, String mode) throws Exception {
        try (var server = new SendServer(outcome);
                var client = server.createClient();
                var session = client.createSession(sessionConfig()).get(5, TimeUnit.SECONDS)) {
            var pending = session
                    .sendAndWait(new MessageOptions().setPrompt("context").setSource(source).setMode(mode));
            var error = assertThrows(ExecutionException.class, () -> pending.get(5, TimeUnit.SECONDS));
            assertTrue(error.getCause().getMessage().contains("send failed"), error.toString());
            var params = server.takeSendParams();
            assertEquals(source.getValue(), params.get("source").asText());
            if (mode == null) {
                assertFalse(params.has("mode"));
            } else {
                assertEquals(mode, params.get("mode").asText());
            }
        }
    }

    @ParameterizedTest
    @MethodSource("sources")
    @NullSource
    void generatedRawRpcAlreadyForwardsSource(MessageSource source) throws Exception {
        try (var server = new SendServer(Outcome.IDLE);
                var client = server.createClient();
                var session = client.createSession(sessionConfig()).get(5, TimeUnit.SECONDS)) {
            var json = MAPPER.createObjectNode().put("prompt", "hello");
            if (source != null) {
                json.put("source", source.getValue());
            }
            var params = MAPPER.treeToValue(json, SessionSendParams.class);
            assertEquals("message-1", session.getRpc().send(params).get(5, TimeUnit.SECONDS).messageId());

            json.put("sessionId", "source-session");
            assertEquals(json, server.takeSendParams());
        }
    }

    private static MessageOptions fullOptions() {
        return new MessageOptions().setPrompt("hello").setMode("immediate").setAgentMode(AgentMode.PLAN)
                .setAttachments(List.of(new Attachment("file", "/workspace/example.java", "example")))
                .setRequestHeaders(Map.of("X-Trace", "trace-id")).setDisplayPrompt("display");
    }

    private static SessionConfig sessionConfig() {
        return new SessionConfig().setSessionId("source-session").setOnPermissionRequest(PermissionHandler.APPROVE_ALL);
    }

    private enum Outcome {
        IDLE, SESSION_ERROR, RPC_ERROR
    }

    /** Uses the existing loopback JSON-RPC test pattern through public SDK APIs. */
    private static final class SendServer implements AutoCloseable {

        private final ServerSocket listener;
        private final Thread thread;
        private final BlockingQueue<JsonNode> sends = new LinkedBlockingQueue<>();
        private final Outcome outcome;
        private volatile Socket socket;
        private volatile boolean closed;
        private volatile Exception failure;

        SendServer(Outcome outcome) throws IOException {
            this.outcome = outcome;
            listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
            thread = new Thread(this::serve, "message-source-server");
            thread.setDaemon(true);
            thread.start();
        }

        CopilotClient createClient() {
            return new CopilotClient(new CopilotClientOptions().setCliUrl("localhost:" + listener.getLocalPort()));
        }

        JsonNode takeSendParams() throws InterruptedException {
            JsonNode params = sends.poll(5, TimeUnit.SECONDS);
            assertNotNull(params, "Expected session.send");
            return params;
        }

        private void serve() {
            try (Socket accepted = listener.accept()) {
                socket = accepted;
                while (!closed) {
                    JsonNode request = readMessage(accepted.getInputStream());
                    if (request == null) {
                        return;
                    }
                    String method = request.path("method").asText();
                    JsonNode params = request.path("params");
                    Object result = switch (method) {
                        case "connect" -> Map.of("ok", true, "protocolVersion", 3, "version", "test");
                        case "session.create" -> Map.of("sessionId", params.path("sessionId").asText());
                        case "session.send" -> Map.of("messageId", "message-1");
                        case "session.detach" -> Map.of("success", true);
                        default -> Map.of();
                    };
                    boolean send = "session.send".equals(method);
                    if (send) {
                        sends.add(params);
                    }
                    var response = MAPPER.createObjectNode().put("jsonrpc", "2.0");
                    response.set("id", request.get("id"));
                    if (send && outcome == Outcome.RPC_ERROR) {
                        response.set("error", MAPPER.valueToTree(Map.of("code", -32603, "message", "send failed")));
                    } else {
                        response.set("result", MAPPER.valueToTree(result));
                    }
                    writeMessage(response);
                    if (send && outcome != Outcome.RPC_ERROR) {
                        String type = outcome == Outcome.IDLE ? "session.idle" : "session.error";
                        Map<String, Object> data = outcome == Outcome.IDLE
                                ? Map.of()
                                : Map.of("errorType", "test", "message", "send failed");
                        writeMessage(Map.of("jsonrpc", "2.0", "method", "session.event", "params",
                                Map.of("sessionId", params.path("sessionId").asText(), "event",
                                        Map.of("type", type, "id", "00000000-0000-0000-0000-000000000001", "timestamp",
                                                "2026-09-06T00:00:00Z", "data", data))));
                    }
                }
            } catch (Exception ex) {
                if (!closed) {
                    failure = ex;
                }
            }
        }

        private static JsonNode readMessage(InputStream input) throws IOException {
            var header = new StringBuilder();
            while (!header.toString().endsWith("\r\n\r\n")) {
                int b = input.read();
                if (b < 0) {
                    return null;
                }
                header.append((char) b);
            }
            int length;
            try {
                length = Integer.parseInt(header.substring(header.indexOf(":") + 1).trim());
            } catch (NumberFormatException ex) {
                throw new IOException("Invalid Content-Length", ex);
            }
            if (length < 0) {
                throw new IOException("Invalid Content-Length: " + length);
            }
            return MAPPER.readTree(input.readNBytes(length));
        }

        private void writeMessage(Object message) throws IOException {
            byte[] body = MAPPER.writeValueAsBytes(message);
            var output = socket.getOutputStream();
            output.write(("Content-Length: " + body.length + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            output.write(body);
            output.flush();
        }

        @Override
        public void close() throws Exception {
            closed = true;
            listener.close();
            if (socket != null) {
                socket.close();
            }
            thread.join(5000);
            assertFalse(thread.isAlive(), "RPC test server should stop");
            assertNull(failure, "RPC test server failed: " + failure);
        }
    }
}
