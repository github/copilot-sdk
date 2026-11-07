/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.copilot.rpc.CloudSessionOptions;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.MemoryConfiguration;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.StoredMemory;

/**
 * Tests that a memory store callback is announced in session.create and
 * session.resume and is served while the runtime handles those requests.
 */
@AllowCopilotExperimental
class MemoryStoreSessionTest {

    private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();

    /**
     * Fake runtime that, while handling session.create or session.resume, sends a
     * memoryStore.store request to the client and waits for its response.
     */
    private static final class FakeRuntime implements AutoCloseable {

        final Socket clientSocket;
        final Socket serverSocket;
        final JsonRpcClient rpcClient;
        final List<JsonNode> requests = new CopyOnWriteArrayList<>();
        final CompletableFuture<JsonNode> storeResponse = new CompletableFuture<>();
        final CompletableFuture<JsonNode> detachStoreResponse = new CompletableFuture<>();
        private final Thread thread;

        FakeRuntime() throws Exception {
            var loopback = InetAddress.getLoopbackAddress();
            try (var ss = new ServerSocket(0, 1, loopback)) {
                clientSocket = new Socket(loopback, ss.getLocalPort());
                serverSocket = ss.accept();
            }
            rpcClient = JsonRpcClient.fromSocket(clientSocket);
            thread = new Thread(this::run);
            thread.setDaemon(true);
            thread.start();
        }

        private void run() {
            try {
                var in = serverSocket.getInputStream();
                var out = serverSocket.getOutputStream();
                JsonNode msg;
                while ((msg = read(in)) != null) {
                    if (!msg.has("method") || !msg.has("id")) {
                        continue;
                    }
                    requests.add(msg);
                    String method = msg.get("method").asText();
                    long id = msg.get("id").asLong();
                    ObjectNode result = MAPPER.createObjectNode();
                    boolean detach = "session.detach".equals(method);
                    if (detach || "session.create".equals(method) || "session.resume".equals(method)) {
                        String sessionId = msg.at("/params/sessionId").asText();
                        ObjectNode params = MAPPER.createObjectNode().put("sessionId", sessionId)
                                .put("subject", "package manager").put("fact", "Use pnpm.")
                                .put("citations", "package.json").put("reason", "The workspace uses pnpm.");
                        write(out, MAPPER.createObjectNode().put("jsonrpc", "2.0").put("id", 9000)
                                .put("method", "memoryStore.store").set("params", params));
                        if (detach) {
                            detachStoreResponse.complete(read(in));
                            result.put("success", true);
                        } else {
                            storeResponse.complete(read(in));
                            result.put("sessionId", sessionId);
                        }
                    }
                    write(out, MAPPER.createObjectNode().put("jsonrpc", "2.0").put("id", id).set("result", result));
                }
            } catch (Exception e) {
                // Socket closed.
            }
        }

        private static JsonNode read(InputStream in) throws Exception {
            var header = new StringBuilder();
            int b;
            while ((b = in.read()) != -1) {
                if (b == '\n' && header.toString().endsWith("\r")) {
                    break;
                }
                header.append((char) b);
            }
            if (b == -1) {
                return null;
            }
            in.read();
            in.read();
            String hdr = header.toString().trim();
            int len = Integer.parseInt(hdr.substring(hdr.indexOf(':') + 1).trim());
            return MAPPER.readTree(in.readNBytes(len));
        }

        private static void write(OutputStream out, JsonNode message) throws Exception {
            byte[] bytes = MAPPER.writeValueAsBytes(message);
            out.write(("Content-Length: " + bytes.length + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(bytes);
            out.flush();
        }

        @Override
        public void close() throws Exception {
            rpcClient.close();
            clientSocket.close();
            serverSocket.close();
            thread.join(3000);
        }
    }

    @SuppressWarnings("unchecked")
    private static CopilotClient connectedClient(FakeRuntime runtime) throws Exception {
        var client = new CopilotClient(new CopilotClientOptions().setAutoStart(false));
        Class<?> connClass = null;
        for (Class<?> c : CopilotClient.class.getDeclaredClasses()) {
            if (c.getSimpleName().equals("Connection")) {
                connClass = c;
            }
        }
        assertNotNull(connClass);
        var ctor = connClass.getDeclaredConstructors()[0];
        ctor.setAccessible(true);
        Object connection = ctor.newInstance(runtime.rpcClient, null, null, null);
        Field connectionField = CopilotClient.class.getDeclaredField("connectionFuture");
        connectionField.setAccessible(true);
        connectionField.set(client, CompletableFuture.completedFuture(connection));

        Field sessionsField = CopilotClient.class.getDeclaredField("sessions");
        sessionsField.setAccessible(true);
        var sessions = (Map<String, CopilotSession>) sessionsField.get(client);
        new RpcHandlerDispatcher(sessions, event -> {
        }, null, new GitHubTokenProviderRegistry()).registerHandlers(runtime.rpcClient);
        return client;
    }

    private static MemoryConfiguration memory(boolean enabled, CopyOnWriteArrayList<StoredMemory> stored) {
        return new MemoryConfiguration().setEnabled(enabled).setStoreMemory(item -> {
            stored.add(item);
            return CompletableFuture.completedFuture(null);
        });
    }

    private static void assertServedDuring(FakeRuntime runtime, String method, String sessionId,
            List<StoredMemory> stored) throws Exception {
        JsonNode request = runtime.requests.stream().filter(r -> method.equals(r.get("method").asText())).findFirst()
                .orElseThrow();
        assertTrue(request.at("/params/hasMemoryStore").asBoolean());
        assertEquals(MAPPER.readTree("{\"enabled\":true}"), request.at("/params/memory"));
        JsonNode storeResponse = runtime.storeResponse.get(5, TimeUnit.SECONDS);
        assertNull(storeResponse.get("error"));
        assertTrue(storeResponse.get("result").isNull());
        assertEquals(List.of(new StoredMemory(sessionId, "package manager", "Use pnpm.", "package.json",
                "The workspace uses pnpm.")), stored);
        assertTrue(runtime.requests.stream()
                .noneMatch(r -> r.get("method").asText().equals("session.memory.configureStore")));
    }

    @Test
    void createSessionAnnouncesAndServesMemoryStoreDuringCreate() throws Exception {
        try (var runtime = new FakeRuntime()) {
            var client = connectedClient(runtime);
            var stored = new CopyOnWriteArrayList<StoredMemory>();
            client.createSession(new SessionConfig().setSessionId("memory-create")
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setMemory(memory(true, stored)))
                    .get(5, TimeUnit.SECONDS);

            assertServedDuring(runtime, "session.create", "memory-create", stored);
            client.close();
        }
    }

    @Test
    void resumeSessionAnnouncesAndServesMemoryStoreDuringResume() throws Exception {
        try (var runtime = new FakeRuntime()) {
            var client = connectedClient(runtime);
            var stored = new CopyOnWriteArrayList<StoredMemory>();
            client.resumeSession("memory-resume", new ResumeSessionConfig()
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setMemory(memory(true, stored)))
                    .get(5, TimeUnit.SECONDS);

            assertServedDuring(runtime, "session.resume", "memory-resume", stored);
            client.close();
        }
    }

    @Test
    void closeServesMemoryStoreUntilDetachCompletes() throws Exception {
        try (var runtime = new FakeRuntime()) {
            var client = connectedClient(runtime);
            var stored = new CopyOnWriteArrayList<StoredMemory>();
            var session = client
                    .createSession(new SessionConfig().setSessionId("memory-close")
                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setMemory(memory(true, stored)))
                    .get(5, TimeUnit.SECONDS);

            session.close();

            JsonNode storeResponse = runtime.detachStoreResponse.get(5, TimeUnit.SECONDS);
            assertNull(storeResponse.get("error"));
            assertEquals(2, stored.size());
            client.close();
        }
    }

    @Test
    void memoryStoreWithoutEnabledMemoryOrWithCloudRejectsBeforeSending() throws Exception {
        try (var runtime = new FakeRuntime()) {
            var client = connectedClient(runtime);
            var stored = new CopyOnWriteArrayList<StoredMemory>();

            var create = assertThrows(ExecutionException.class,
                    () -> client.createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                            .setMemory(memory(false, stored))).get(5, TimeUnit.SECONDS));
            assertEquals("memory.storeMemory requires memory.enabled to be true.", create.getCause().getMessage());

            var resume = assertThrows(ExecutionException.class,
                    () -> client.resumeSession("memory-resume", new ResumeSessionConfig()
                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setMemory(memory(false, stored)))
                            .get(5, TimeUnit.SECONDS));
            assertEquals("memory.storeMemory requires memory.enabled to be true.", resume.getCause().getMessage());

            var cloud = assertThrows(ExecutionException.class,
                    () -> client
                            .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                                    .setCloud(new CloudSessionOptions()).setMemory(memory(true, stored)))
                            .get(5, TimeUnit.SECONDS));
            assertEquals("Memory stores are not supported for cloud sessions.", cloud.getCause().getMessage());

            assertTrue(runtime.requests.isEmpty());
            client.close();
        }

        try (var unstarted = new CopilotClient(new CopilotClientOptions().setAutoStart(false))) {
            var create = assertThrows(ExecutionException.class,
                    () -> unstarted
                            .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                                    .setMemory(memory(false, new CopyOnWriteArrayList<>())))
                            .get(5, TimeUnit.SECONDS));
            assertEquals("memory.storeMemory requires memory.enabled to be true.", create.getCause().getMessage());
        }
    }
}
