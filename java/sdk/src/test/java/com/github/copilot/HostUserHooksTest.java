/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.rpc.CopilotClientMode;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.SessionHooks;

class HostUserHooksTest {

    @Test
    void rejectsProtocolThreeWithoutCompatibilityProbe() throws Exception {
        try (var server = new Server();
                var client = new CopilotClient(
                        new CopilotClientOptions().setCliUrl("127.0.0.1:" + server.listener.getLocalPort()))) {
            server.protocolVersion = 3;
            var error = assertThrows(Exception.class, () -> client.start().get(10, TimeUnit.SECONDS));
            assertTrue(error.getCause().getMessage().contains("protocol version mismatch"));
            assertFalse(server.requests.stream().anyMatch(request -> List.of("ping", "session.create", "session.resume")
                    .contains(request.path("method").asText())));
        }
    }

    @Test
    void generatedOptionsPreserveExplicitFalse() throws Exception {
        var mapper = new ObjectMapper();
        var options = mapper.readValue("{\"sessionId\":\"host-hooks\",\"enableHostUserHooks\":false}",
                com.github.copilot.generated.rpc.SessionOptionsUpdateParams.class);
        var wire = mapper.readTree(mapper.writeValueAsBytes(options));
        assertTrue(wire.path("enableHostUserHooks").isBoolean());
        assertFalse(wire.path("enableHostUserHooks").booleanValue());
    }

    @Test
    void initialRequestsResolveAllModesAndOverridesWithoutChangingCallbacks() throws Exception {
        for (var mode : CopilotClientMode.values()) {
            for (Boolean override : new Boolean[]{null, false, true}) {
                var hooks = new SessionHooks()
                        .setOnPreToolUse((input, invocation) -> CompletableFuture.completedFuture(null));
                var create = new SessionConfig().setAvailableTools(List.of()).setHooks(hooks)
                        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL);
                var resume = new ResumeSessionConfig().setAvailableTools(List.of()).setHooks(hooks)
                        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL);
                if (override != null) {
                    create.setEnableHostUserHooks(override);
                    resume.setEnableHostUserHooks(override);
                }
                try (var server = new Server();
                        var client = new CopilotClient(
                                new CopilotClientOptions().setCliUrl("127.0.0.1:" + server.listener.getLocalPort())
                                        .setMode(mode).setCopilotHome(System.getProperty("user.dir")))) {
                    var session = client.createSession(create).get(10, TimeUnit.SECONDS);
                    var resumed = client.resumeSession(session.getSessionId(), resume).get(10, TimeUnit.SECONDS);
                    boolean expected = override != null ? override : mode != CopilotClientMode.EMPTY;
                    for (String method : List.of("session.create", "session.resume")) {
                        var requests = server.requests.stream()
                                .filter(request -> method.equals(request.path("method").asText())).toList();
                        assertEquals(1, requests.size());
                        var params = requests.get(0).path("params");
                        assertTrue(params.path("enableHostUserHooks").isBoolean());
                        assertEquals(expected, params.path("enableHostUserHooks").booleanValue());
                        assertTrue(params.path("hooks").booleanValue());
                    }
                    assertEquals(override, create.getEnableHostUserHooks().orElse(null));
                    assertEquals(override, resume.getEnableHostUserHooks().orElse(null));
                    assertSame(hooks, create.getHooks());
                    assertSame(hooks, resume.getHooks());
                    resumed.close();
                    session.close();
                }
            }
        }
    }

    private static final class Server implements AutoCloseable {
        final ServerSocket listener = new ServerSocket(0);
        final List<JsonNode> requests = new CopyOnWriteArrayList<>();
        final Thread worker;
        volatile Socket socket;
        volatile boolean running = true;
        volatile int protocolVersion = SdkProtocolVersion.get();

        Server() throws Exception {
            worker = new Thread(() -> {
                var mapper = new ObjectMapper();
                try (var accepted = listener.accept()) {
                    socket = accepted;
                    var input = accepted.getInputStream();
                    var output = accepted.getOutputStream();
                    while (running) {
                        var header = new StringBuilder();
                        while (!header.toString().endsWith("\r\n\r\n")) {
                            int value = input.read();
                            if (value == -1) {
                                return;
                            }
                            header.append((char) value);
                        }
                        int length = Integer.parseInt(header.toString().trim().split(":")[1].trim());
                        var request = mapper.readTree(input.readNBytes(length));
                        requests.add(request);
                        if (!request.has("id")) {
                            continue;
                        }
                        var result = mapper.createObjectNode().put("success", true);
                        switch (request.path("method").asText()) {
                            case "connect", "ping" -> result.put("protocolVersion", protocolVersion);
                            case "session.create", "session.resume" ->
                                result.put("sessionId", request.path("params").path("sessionId").asText());
                            default -> {
                            }
                        }
                        var response = mapper.createObjectNode().put("jsonrpc", "2.0");
                        response.set("id", request.get("id"));
                        response.set("result", result);
                        byte[] bytes = mapper.writeValueAsBytes(response);
                        output.write(("Content-Length: " + bytes.length + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                        output.write(bytes);
                        output.flush();
                    }
                } catch (Exception error) {
                    if (running) {
                        throw new AssertionError(error);
                    }
                }
            });
            worker.setDaemon(true);
            worker.start();
        }

        @Override
        public void close() throws Exception {
            running = false;
            listener.close();
            if (socket != null) {
                socket.close();
            }
            worker.join(3000);
        }
    }
}
