/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.CapabilitiesChangedUI;
import com.github.copilot.generated.rpc.SessionUiRegisterDirectAutoModeSwitchHandlerParams;
import com.github.copilot.generated.rpc.SessionUiRegisterDirectAutoModeSwitchHandlerResult;
import com.github.copilot.generated.rpc.SessionsFindByPrefixParams;
import com.github.copilot.generated.rpc.SessionsFindByPrefixResult;
import com.github.copilot.rpc.AutoModeSwitchHandler;
import com.github.copilot.rpc.AutoModeSwitchRequest;
import com.github.copilot.rpc.AutoModeSwitchResponse;
import com.github.copilot.rpc.CloudSessionOptions;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.GitHubTokenProviderResult;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.PermissionRequestResult;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.SectionOverride;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.SessionHooks;
import com.github.copilot.rpc.SystemMessageConfig;

class AutoModeRecoveryTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Map<String, Object> AUTO_MODE_RECOVERY = Map.of("requestId", "offer-1", "originalModel",
            "explicit-model", "allowAlways", false, "interactionId", "interaction-1", "originatingMessageId", "user-1");

    @Test
    void prefixLookupKeepsLegacyRecordsMethodReferencesAndNullCalls() throws Exception {
        var canonical = "abcdef12-abcd-4123-8123-123456789abc";
        try (var runtime = new Runtime(null);
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()))) {
            runtime.findByPrefixReply = CompletableFuture.completedFuture(Map.of("sessionId", canonical));
            client.start().get(5, TimeUnit.SECONDS);
            Function<SessionsFindByPrefixParams, CompletableFuture<SessionsFindByPrefixResult>> find = client
                    .getRpc().sessions::findByPrefix;
            assertEquals(canonical,
                    find.apply(new SessionsFindByPrefixParams("abcdef12")).get(5, TimeUnit.SECONDS).sessionId());
            assertEquals(canonical, client.getRpc().sessions.findByPrefix(null).get(5, TimeUnit.SECONDS).sessionId());
        }
    }

    @Test
    void failedCleanupDoesNotFenceADistinctCustomId() throws Exception {
        try (var runtime = new Runtime(null);
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()))) {
            runtime.detachReply = CompletableFuture.completedFuture(Map.of("success", false));
            assertThrows(ExecutionException.class,
                    () -> client
                            .createSession(
                                    new SessionConfig().setSessionId("session-1")
                                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                                            .setAutoModeRecovery(true)
                                            .setOnAutoModeSwitch((request, invocation) -> CompletableFuture
                                                    .completedFuture(AutoModeSwitchResponse.NO)))
                            .get(5, TimeUnit.SECONDS));
            runtime.detachReply = null;
            try (var sibling = client.createSession(new SessionConfig().setSessionId("session-123")
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get(5, TimeUnit.SECONDS)) {
                assertEquals("session-123", sibling.getSessionId());
            }
        }
    }

    @Test
    void uiCapabilityChangesKeepThePublishedConstructor() throws Exception {
        var capabilities = new CapabilitiesChangedUI(true, false, true);
        assertTrue(capabilities.elicitation());
        assertFalse(capabilities.mcpApps());
        assertTrue(capabilities.canvases());
        assertNull(capabilities.autoModeRecovery());
        var recovered = MAPPER.readValue("{\"autoModeRecovery\":true,\"canvases\":false}", CapabilitiesChangedUI.class);
        assertTrue(recovered.autoModeRecovery());
        assertFalse(recovered.canvases());
    }

    @Test
    void directRegistrationRecordsKeepThePublishedConstructors() throws Exception {
        var params = new SessionUiRegisterDirectAutoModeSwitchHandlerParams("legacy-session");
        assertEquals("legacy-session", params.sessionId());
        assertNull(params.autoModeRecovery());
        assertEquals(MAPPER.readTree("{\"sessionId\":\"legacy-session\"}"), MAPPER.valueToTree(params));
        var result = new SessionUiRegisterDirectAutoModeSwitchHandlerResult("legacy-handle");
        assertEquals("legacy-handle", result.handle());
        assertNull(result.autoModeRecovery());
        assertEquals(MAPPER.readTree("{\"handle\":\"legacy-handle\"}"), MAPPER.valueToTree(result));
        assertTrue(MAPPER.readValue("{\"sessionId\":\"recovery-session\",\"autoModeRecovery\":true}",
                SessionUiRegisterDirectAutoModeSwitchHandlerParams.class).autoModeRecovery());
        assertTrue(MAPPER.readValue("{\"handle\":\"recovery-handle\",\"autoModeRecovery\":true}",
                SessionUiRegisterDirectAutoModeSwitchHandlerResult.class).autoModeRecovery());
    }

    private static CompletableFuture<CopilotSession> open(CopilotClient client, boolean resume, boolean enabled,
            AutoModeSwitchHandler handler) {
        return resume
                ? client.resumeSession("session-1", new ResumeSessionConfig()
                        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setOnEvent(event -> {
                        }).setAutoModeRecovery(enabled).setOnAutoModeSwitch(handler).clone())
                : client.createSession(
                        new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setOnEvent(event -> {
                        }).setAutoModeRecovery(enabled).setOnAutoModeSwitch(handler).clone());
    }

    @Test
    void rejectsEnrollmentWithoutAHandlerBeforeConnecting() throws Exception {
        for (boolean resume : List.of(false, true)) {
            try (var client = new CopilotClient(new CopilotClientOptions().setCliUrl("127.0.0.1:1"))) {
                var error = assertThrows(ExecutionException.class,
                        () -> open(client, resume, true, null).get(5, TimeUnit.SECONDS));
                assertInstanceOf(IllegalArgumentException.class, error.getCause());
                assertTrue(error.getCause().getMessage().contains("requires onAutoModeSwitch"));
            }
        }
    }

    @Test
    void directRegistrationAcceptsRecoveryWithoutACallerSuppliedSessionId() throws Exception {
        try (var runtime = new Runtime(null);
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()));
                var session = client
                        .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                        .get(5, TimeUnit.SECONDS)) {
            var ui = session.getRpc().ui;
            var legacy = ui.registerDirectAutoModeSwitchHandler().get(5, TimeUnit.SECONDS);
            assertEquals("direct-handler", legacy.handle());
            assertNull(legacy.autoModeRecovery());
            assertTrue(ui.registerDirectAutoModeSwitchHandler(true).get(5, TimeUnit.SECONDS).autoModeRecovery());
            assertFalse(ui.registerDirectAutoModeSwitchHandler(false).get(5, TimeUnit.SECONDS).autoModeRecovery());
            assertTrue(ui
                    .registerDirectAutoModeSwitchHandler(SessionUiRegisterDirectAutoModeSwitchHandlerParams.builder()
                            .sessionId("ignored-session").autoModeRecovery(true).build())
                    .get(5, TimeUnit.SECONDS).autoModeRecovery());

            var registrations = runtime.requests.stream().filter(
                    request -> request.path("method").asText().equals("session.ui.registerDirectAutoModeSwitchHandler"))
                    .map(request -> request.path("params")).toList();
            assertEquals(
                    List.of(MAPPER.valueToTree(Map.of("sessionId", session.getSessionId())),
                            MAPPER.valueToTree(Map.of("sessionId", session.getSessionId(), "autoModeRecovery", true)),
                            MAPPER.valueToTree(Map.of("sessionId", session.getSessionId(), "autoModeRecovery", false)),
                            MAPPER.valueToTree(Map.of("sessionId", session.getSessionId(), "autoModeRecovery", true))),
                    registrations);
        }
    }

    @Test
    void unsupportedEnrollmentDetachesAndRemovesTheCallbackOwner() throws Exception {
        for (boolean resume : List.of(false, true)) {
            for (Boolean acknowledged : new Boolean[]{null, false}) {
                try (var runtime = new Runtime(acknowledged);
                        var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()))) {
                    var error = assertThrows(ExecutionException.class, () -> open(client, resume, true,
                            (request, invocation) -> CompletableFuture.completedFuture(AutoModeSwitchResponse.NO))
                            .get(5, TimeUnit.SECONDS));
                    assertTrue(error.getCause().getMessage().contains("does not support autoModeRecovery"));
                    var detach = runtime.requests.stream()
                            .filter(r -> r.path("method").asText().equals("session.detach")).toList();
                    assertEquals(1, detach.size());
                    assertFalse(runtime.requests.stream()
                            .anyMatch(r -> r.path("method").asText().equals("session.delete")));
                    var response = runtime
                            .request("autoModeSwitch.request",
                                    Map.of("sessionId", detach.get(0).at("/params/sessionId").asText()))
                            .get(5, TimeUnit.SECONDS);
                    assertTrue(response.path("error").path("message").asText().contains("Unknown session"));
                }
            }
        }
    }

    @Test
    void rejectedEnrollmentHoldsSameIdAdmissionThroughDetach() throws Exception {
        for (boolean firstResume : List.of(false, true)) {
            for (boolean nextResume : List.of(false, true)) {
                try (var runtime = new Runtime(null);
                        var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()))) {
                    runtime.detachReply = new CompletableFuture<>();
                    var rejected = openWithId(client, firstResume, "shared", true);
                    runtime.detachRequested.get(5, TimeUnit.SECONDS);
                    var next = openWithId(client, nextResume, "shared", false);
                    try (var unrelated = openWithId(client, false, "unrelated", false).get(5, TimeUnit.SECONDS)) {
                        try {
                            assertFalse(next.isDone(), "Same-ID initialization overtook rejected enrollment cleanup");
                            assertEquals(1,
                                    runtime.requests.stream().filter(r -> isInitialization(r, "shared")).count());
                        } finally {
                            runtime.detachReply.complete(Map.of("success", true));
                        }
                        assertThrows(ExecutionException.class, () -> rejected.get(5, TimeUnit.SECONDS));
                        try (var resumed = next.get(5, TimeUnit.SECONDS)) {
                            assertEquals("shared", resumed.getSessionId());
                        }
                    }
                }
            }
        }
    }

    @Test
    void failedRecoveryCleanupFencesBothCreateAndResumeOnTheConnection() throws Exception {
        for (String failure : List.of("rejected", "null", "rpc", "timeout")) {
            try (var runtime = new Runtime(null);
                    var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()))) {
                runtime.detachReply = new CompletableFuture<>();
                var rejected = openWithId(client, false, "shared", true);
                runtime.detachRequested.get(5, TimeUnit.SECONDS);
                switch (failure) {
                    case "rejected" -> runtime.detachReply.complete(Map.of("success", false, "error", "detach denied"));
                    case "null" -> runtime.detachReply.complete(null);
                    case "rpc" -> runtime.detachReply.completeExceptionally(new IllegalStateException("detach failed"));
                    case "timeout" -> {
                    }
                }
                var error = assertThrows(ExecutionException.class, () -> rejected.get(10, TimeUnit.SECONDS));
                assertTrue(error.getCause().getMessage().contains("does not support autoModeRecovery"));
                assertEquals(1, error.getCause().getSuppressed().length);
                // A late acknowledgment cannot retract a cleanup failure or make reuse safe.
                runtime.detachReply.complete(Map.of("success", true));
                for (boolean resume : List.of(false, true)) {
                    var blocked = assertThrows(ExecutionException.class,
                            () -> openWithId(client, resume, "shared", false).get(5, TimeUnit.SECONDS));
                    assertTrue(blocked.getCause().getMessage().contains("failed Auto recovery cleanup"),
                            blocked.toString());
                }
                assertEquals(1, runtime.requests.stream().filter(r -> isInitialization(r, "shared")).count());
                try (var unrelated = openWithId(client, false, "unrelated", false).get(5, TimeUnit.SECONDS)) {
                    assertEquals("unrelated", unrelated.getSessionId());
                }
            }
        }
    }

    @Test
    void rejectedCloudEnrollmentDeletesTheServerAssignedSessionAndReportsRollbackErrors() throws Exception {
        for (boolean deleteSuccess : List.of(true, false)) {
            try (var runtime = new Runtime(null);
                    var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()))) {
                runtime.deleteSuccess = deleteSuccess;
                var error = assertThrows(ExecutionException.class,
                        () -> client
                                .createSession(
                                        new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                                                .setCloud(new CloudSessionOptions()).setAutoModeRecovery(true)
                                                .setOnAutoModeSwitch((request, invocation) -> CompletableFuture
                                                        .completedFuture(AutoModeSwitchResponse.NO)))
                                .get(5, TimeUnit.SECONDS));
                assertTrue(error.getCause().getMessage().contains("does not support autoModeRecovery"));
                var deletes = runtime.requests.stream().filter(r -> r.path("method").asText().equals("session.delete"))
                        .toList();
                assertEquals(1, deletes.size());
                assertEquals("cloud-session", deletes.get(0).at("/params/sessionId").asText());
                if (!deleteSuccess) {
                    assertEquals(1, error.getCause().getSuppressed().length);
                    assertTrue(error.getCause().getSuppressed()[0].getMessage().contains("delete denied"));
                }
            }
        }
    }

    @Test
    void rejectedCloudEnrollmentHoldsAdmissionThroughDeletion() throws Exception {
        try (var runtime = new Runtime(null);
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()))) {
            runtime.deleteReply = new CompletableFuture<>();
            var rejected = client.createSession(new SessionConfig()
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setCloud(new CloudSessionOptions())
                    .setAutoModeRecovery(true).setOnAutoModeSwitch(
                            (request, invocation) -> CompletableFuture.completedFuture(AutoModeSwitchResponse.NO)));
            runtime.deleteRequested.get(5, TimeUnit.SECONDS);
            var next = openWithId(client, true, "cloud-session", false);
            try (var unrelated = openWithId(client, false, "unrelated", false).get(5, TimeUnit.SECONDS)) {
                try {
                    assertFalse(next.isDone(), "Same-ID resume overtook cloud rollback");
                    assertFalse(runtime.requests.stream().anyMatch(r -> isInitialization(r, "cloud-session")));
                } finally {
                    runtime.deleteReply.complete(Map.of("success", true));
                }
                assertThrows(ExecutionException.class, () -> rejected.get(5, TimeUnit.SECONDS));
                try (var resumed = next.get(5, TimeUnit.SECONDS)) {
                    assertEquals("cloud-session", resumed.getSessionId());
                }
            }
        }
    }

    @Test
    void stalledCloudRollbackFailsAndFencesSameIdEvenAfterLateDeletionAcknowledgment() throws Exception {
        try (var runtime = new Runtime(null);
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()))) {
            runtime.deleteReply = new CompletableFuture<>();
            var rejected = client.createSession(new SessionConfig()
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setCloud(new CloudSessionOptions())
                    .setAutoModeRecovery(true).setOnAutoModeSwitch(
                            (request, invocation) -> CompletableFuture.completedFuture(AutoModeSwitchResponse.NO)));
            runtime.deleteRequested.get(5, TimeUnit.SECONDS);
            var queued = openWithId(client, true, "cloud-session", false);
            try {
                var error = assertThrows(ExecutionException.class, () -> rejected.get(10, TimeUnit.SECONDS));
                assertTrue(error.getCause().getMessage().contains("does not support autoModeRecovery"));
                assertEquals(1, error.getCause().getSuppressed().length);
                var cleanup = error.getCause().getSuppressed()[0];
                assertTrue(cleanup.getMessage().contains("Failed to delete cloud session cloud-session"),
                        cleanup.toString());
                assertInstanceOf(java.util.concurrent.TimeoutException.class, cleanup.getCause());
                var blocked = assertThrows(ExecutionException.class, () -> queued.get(5, TimeUnit.SECONDS));
                assertTrue(blocked.getCause().getMessage().contains("failed Auto recovery cleanup"));
            } finally {
                runtime.deleteReply.complete(Map.of("success", true));
            }
            // The unrelated response drains the late deletion reply on the same connection.
            try (var unrelated = openWithId(client, false, "unrelated", false).get(5, TimeUnit.SECONDS)) {
                assertEquals("unrelated", unrelated.getSessionId());
                for (boolean resume : List.of(false, true)) {
                    var blocked = assertThrows(ExecutionException.class,
                            () -> openWithId(client, resume, "cloud-session", false).get(5, TimeUnit.SECONDS));
                    assertTrue(blocked.getCause().getMessage().contains("failed Auto recovery cleanup"));
                }
                assertFalse(runtime.requests.stream().anyMatch(r -> isInitialization(r, "cloud-session")));
                assertEquals(1, runtime.requests.stream()
                        .filter(r -> r.path("method").asText().equals("session.delete")).count());
            }
        }
    }

    @Test
    void rejectedCallerAssignedCloudEnrollmentDoesNotDeleteTheSession() throws Exception {
        try (var runtime = new Runtime(null);
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()))) {
            assertThrows(ExecutionException.class, () -> client
                    .createSession(new SessionConfig()
                            .setSessionId("caller-cloud").setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                            .setCloud(new CloudSessionOptions()).setAutoModeRecovery(true).setOnAutoModeSwitch((request,
                                    invocation) -> CompletableFuture.completedFuture(AutoModeSwitchResponse.NO)))
                    .get(5, TimeUnit.SECONDS));
            assertFalse(runtime.requests.stream().anyMatch(r -> r.path("method").asText().equals("session.delete")));
        }
    }

    @Test
    void prefixResumesCannotBeOvertakenByRejectedRecoveryCleanup() throws Exception {
        for (boolean recoveryFirst : List.of(false, true)) {
            for (boolean uppercase : List.of(false, true)) {
                var canonical = "a" + UUID.randomUUID().toString().substring(1);
                var prefix = uppercase
                        ? canonical.substring(0, 8).toUpperCase(java.util.Locale.ROOT)
                        : canonical.substring(0, 8);
                try (var runtime = new Runtime(null);
                        var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()))) {
                    runtime.resumeId = canonical;
                    runtime.persistedSessionIds.add(canonical);
                    runtime.resumeReply = new CompletableFuture<>();
                    var first = openWithId(client, true, recoveryFirst ? canonical : prefix, recoveryFirst);
                    runtime.resumeRequested.get(5, TimeUnit.SECONDS);
                    var second = openWithId(client, true, recoveryFirst ? prefix : canonical, !recoveryFirst);
                    try {
                        try (var unrelated = openWithId(client, false, "unrelated", false).get(5, TimeUnit.SECONDS)) {
                            assertEquals("unrelated", unrelated.getSessionId());
                            assertEquals(1,
                                    runtime.requests.stream()
                                            .filter(request -> "session.resume".equals(request.path("method").asText()))
                                            .count());
                        }
                    } finally {
                        runtime.resumeReply.complete(Map.of("sessionId", canonical));
                    }
                    var rejected = recoveryFirst ? first : second;
                    var accepted = recoveryFirst ? second : first;
                    var error = assertThrows(ExecutionException.class, () -> rejected.get(5, TimeUnit.SECONDS));
                    assertTrue(error.getCause().getMessage().contains("does not support autoModeRecovery"));
                    try (var session = accepted.get(5, TimeUnit.SECONDS)) {
                        assertEquals(canonical, session.getSessionId());
                        assertEquals(recoveryFirst ? 1 : 0,
                                runtime.requests.stream()
                                        .filter(request -> "session.detach".equals(request.path("method").asText())
                                                && canonical.equals(request.at("/params/sessionId").asText()))
                                        .count(),
                                "recoveryFirst=" + recoveryFirst + ", uppercase=" + uppercase + ": "
                                        + runtime.requests);
                        var response = runtime.request("autoModeSwitch.request", Map.of("sessionId", canonical)).get(5,
                                TimeUnit.SECONDS);
                        assertEquals("no", response.at("/result/response").asText(), response.toString());
                    }
                }
            }
        }
    }

    @ParameterizedTest
    @CsvSource({"false, supported", "true, supported", "false, missing", "true, missing", "false, false", "true, false",
            "false, rejected", "true, rejected"})
    void prefixResumesWireCanonicalCallbacksBeforeTheResumeReply(boolean hasResident, String outcome) throws Exception {
        var canonical = "abcdef01-2345-4567-89ab-cdef01234567";
        var prefix = "ABCDEF01";
        Boolean acknowledged = switch (outcome) {
            case "supported" -> Boolean.TRUE;
            case "false" -> Boolean.FALSE;
            default -> null;
        };
        try (var runtime = new Runtime(acknowledged);
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()))) {
            runtime.resumeId = canonical;
            runtime.persistedSessionIds.add(canonical);
            runtime.resumeReply = new CompletableFuture<>();
            var permissions = new AtomicInteger();
            var hooks = new AtomicInteger();
            var eventReceived = new CompletableFuture<Void>();
            try (var resident = hasResident
                    ? client.createSession(new SessionConfig().setSessionId(canonical)
                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                            .setOnAutoModeSwitch((request, invocation) -> CompletableFuture
                                    .completedFuture(AutoModeSwitchResponse.YES_ALWAYS)))
                            .get(5, TimeUnit.SECONDS)
                    : null) {
                var resumed = client.resumeSession(prefix, new ResumeSessionConfig().setAutoModeRecovery(true)
                        .setOnPermissionRequest((request, invocation) -> {
                            assertEquals(canonical, invocation.getSessionId());
                            permissions.incrementAndGet();
                            return CompletableFuture.completedFuture(PermissionRequestResult.approveOnce());
                        }).setOnAutoModeSwitch((request, invocation) -> {
                            assertEquals(canonical, invocation.getSessionId());
                            assertNotNull(request.getAutoModeRecovery());
                            return CompletableFuture.completedFuture(AutoModeSwitchResponse.NO);
                        }).setHooks(new SessionHooks().setOnSessionEnd((input, invocation) -> {
                            assertEquals(canonical, invocation.getSessionId());
                            hooks.incrementAndGet();
                            return CompletableFuture.completedFuture(null);
                        }))
                        .setSystemMessage(new SystemMessageConfig().setMode(SystemMessageMode.CUSTOMIZE)
                                .setSections(Map.of("identity",
                                        new SectionOverride().setTransform(
                                                content -> CompletableFuture.completedFuture(content + " resumed")))))
                        .setOnEvent(event -> {
                            if ("session.title_changed".equals(event.getType())) {
                                eventReceived.complete(null);
                            }
                        }));
                try {
                    runtime.resumeRequested.get(5, TimeUnit.SECONDS);
                    var wireResume = runtime.requests.stream()
                            .filter(request -> "session.resume".equals(request.path("method").asText())).findFirst()
                            .orElseThrow();
                    assertEquals(canonical, wireResume.at("/params/sessionId").asText(),
                            "Resume must send the persisted full ID, not re-resolve the prefix");
                    assertEquals(prefix,
                            runtime.requests.stream()
                                    .filter(request -> "sessions.findByPrefix".equals(request.path("method").asText()))
                                    .findFirst().orElseThrow().at("/params/prefix").asText());
                    var auto = runtime
                            .request("autoModeSwitch.request",
                                    Map.of("sessionId", canonical, "autoModeRecovery", AUTO_MODE_RECOVERY))
                            .get(5, TimeUnit.SECONDS);
                    assertEquals("no", auto.at("/result/response").asText(), auto.toString());
                    var permission = runtime
                            .request("permission.request",
                                    Map.of("sessionId", canonical, "permissionRequest",
                                            Map.of("kind", "shell", "fullCommandText", "echo resumed")))
                            .get(5, TimeUnit.SECONDS);
                    assertEquals("approve-once", permission.at("/result/result/kind").asText(), permission.toString());
                    assertEquals(1, permissions.get());
                    var hook = runtime
                            .request("hooks.invoke",
                                    Map.of("sessionId", canonical, "hookType", "sessionEnd", "input", Map.of(
                                            "sessionId", canonical, "timestamp", 0, "cwd", ".", "reason", "user_exit")))
                            .get(5, TimeUnit.SECONDS);
                    assertFalse(hook.has("error"), hook.toString());
                    assertEquals(1, hooks.get());
                    var transformed = runtime
                            .request("systemMessage.transform",
                                    Map.of("sessionId", canonical, "sections",
                                            Map.of("identity", Map.of("content", "original"))))
                            .get(5, TimeUnit.SECONDS);
                    assertEquals("original resumed", transformed.at("/result/sections/identity/content").asText(),
                            transformed.toString());
                    runtime.event(canonical, "session.title_changed", Map.of("title", "during resume"));
                    eventReceived.get(5, TimeUnit.SECONDS);
                    assertFalse(resumed.isDone());
                } finally {
                    if (outcome.equals("rejected")) {
                        runtime.resumeReply.completeExceptionally(new IllegalStateException("resume denied"));
                    } else {
                        runtime.resumeReply.complete(runtime.resumeResponse(canonical));
                    }
                }
                if (outcome.equals("supported")) {
                    try (var session = resumed.get(5, TimeUnit.SECONDS)) {
                        assertEquals(canonical, session.getSessionId());
                    }
                } else {
                    var error = assertThrows(ExecutionException.class, () -> resumed.get(5, TimeUnit.SECONDS));
                    assertTrue(
                            error.getCause().getMessage().contains(
                                    outcome.equals("rejected") ? "resume denied" : "does not support autoModeRecovery"),
                            error.toString());
                    var callback = runtime
                            .request("autoModeSwitch.request",
                                    Map.of("sessionId", canonical, "autoModeRecovery", AUTO_MODE_RECOVERY))
                            .get(5, TimeUnit.SECONDS);
                    if (hasResident) {
                        assertEquals(outcome.equals("rejected") ? "yes_always" : "no",
                                callback.at("/result/response").asText(), callback.toString());
                    } else {
                        assertTrue(callback.path("error").path("message").asText().contains("Unknown session"),
                                callback.toString());
                    }
                    var detached = runtime.requests.stream()
                            .filter(request -> "session.detach".equals(request.path("method").asText())).toList();
                    assertEquals(!hasResident && !outcome.equals("rejected") ? 1 : 0, detached.size());
                    if (!detached.isEmpty()) {
                        assertEquals(canonical, detached.get(0).at("/params/sessionId").asText());
                    }
                    assertFalse(runtime.requests.stream()
                            .anyMatch(request -> "session.delete".equals(request.path("method").asText())));
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"missing", "ambiguous", "rpc"})
    void persistedPrefixAmbiguityAndLookupFailuresNeverReplaceTheResident(String outcome) throws Exception {
        var canonical = "abcdef01-2345-4567-89ab-cdef01234567";
        try (var runtime = new Runtime(true);
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()));
                var resident = client
                        .createSession(new SessionConfig().setSessionId(canonical)
                                .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                                .setOnAutoModeSwitch((request, invocation) -> CompletableFuture
                                        .completedFuture(AutoModeSwitchResponse.YES_ALWAYS)))
                        .get(5, TimeUnit.SECONDS)) {
            runtime.resumeId = canonical;
            if (outcome.equals("ambiguous")) {
                // This persisted session has no local SDK callback owner.
                runtime.persistedSessionIds.add("abcdef01-9999-4567-89ab-cdef01234567");
            } else if (outcome.equals("rpc")) {
                runtime.findByPrefixReply = CompletableFuture
                        .failedFuture(new IllegalStateException("prefix lookup denied"));
            }
            var error = assertThrows(ExecutionException.class,
                    () -> openWithId(client, true, outcome.equals("missing") ? "0123456" : "ABCDEF01", true).get(5,
                            TimeUnit.SECONDS),
                    outcome);
            assertTrue(
                    error.getCause().getMessage()
                            .contains(outcome.equals("rpc") ? "prefix lookup denied" : "No unique persisted session"),
                    error.toString());
            assertEquals(1, runtime.requests.stream()
                    .filter(request -> "sessions.findByPrefix".equals(request.path("method").asText())).count());
            assertFalse(runtime.requests.stream()
                    .anyMatch(request -> List.of("session.resume", "session.detach", "session.delete")
                            .contains(request.path("method").asText())),
                    runtime.requests.toString());
            var callback = runtime.request("autoModeSwitch.request", Map.of("sessionId", canonical)).get(5,
                    TimeUnit.SECONDS);
            assertEquals("yes_always", callback.at("/result/response").asText(), callback.toString());
        }
    }

    @Test
    void onlyNativeAbbreviatedUuidPrefixesUsePersistedLookup() throws Exception {
        var canonical = "abcdef01-2345-4567-89ab-cdef01234567";
        for (String prefix : List.of("abcdef0", "ABCDEF01-2345", canonical.substring(0, 35), "abcdef0--")) {
            try (var runtime = new Runtime(true);
                    var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()))) {
                runtime.resumeId = canonical;
                runtime.findByPrefixReply = CompletableFuture.completedFuture(Map.of("sessionId", canonical));
                try (var session = openWithId(client, true, prefix, true).get(5, TimeUnit.SECONDS)) {
                    assertEquals(canonical, session.getSessionId());
                    assertEquals(1, runtime.requests.stream()
                            .filter(request -> "sessions.findByPrefix".equals(request.path("method").asText())).count(),
                            prefix);
                    assertTrue(runtime.requests.stream().anyMatch(request -> isInitialization(request, canonical)),
                            runtime.requests.toString());
                }
            }
        }
        for (String id : List.of(canonical, canonical.toUpperCase(java.util.Locale.ROOT), "abcdef", "abcdef-0",
                "deadbeef_task1", "arbitrary-sdk-id", "abcdef01\u00e9", "abcdef01\uFF21", canonical + "a")) {
            try (var runtime = new Runtime(true);
                    var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()));
                    var session = openWithId(client, true, id, false).get(5, TimeUnit.SECONDS)) {
                assertEquals(id, session.getSessionId());
                assertFalse(runtime.requests.stream()
                        .anyMatch(request -> "sessions.findByPrefix".equals(request.path("method").asText())), id);
            }
        }
    }

    @Test
    void prefixLookupUsesConfiguredStorageWithAndWithoutRecovery() throws Exception {
        var canonical = "abcdef01-2345-4567-89ab-cdef01234567";
        for (boolean recovery : List.of(false, true)) {
            try (var runtime = new Runtime(true);
                    var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()))) {
                runtime.resumeId = canonical;
                runtime.findByPrefixReply = CompletableFuture.completedFuture(Map.of("sessionId", canonical));
                try (var session = client
                        .resumeSession("abcdef01",
                                new ResumeSessionConfig().setConfigDirectory("configured-home")
                                        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                                        .setAutoModeRecovery(recovery)
                                        .setOnAutoModeSwitch((request, invocation) -> CompletableFuture
                                                .completedFuture(AutoModeSwitchResponse.NO)))
                        .get(5, TimeUnit.SECONDS)) {
                    assertEquals(canonical, session.getSessionId());
                    var lookup = runtime.requests.stream()
                            .filter(request -> "sessions.findByPrefix".equals(request.path("method").asText()))
                            .findFirst();
                    assertEquals(recovery, lookup.isPresent());
                    if (recovery) {
                        assertEquals("configured-home", lookup.orElseThrow().at("/params/configDir").asText());
                    }
                    var resume = runtime.requests.stream()
                            .filter(request -> "session.resume".equals(request.path("method").asText())).findFirst()
                            .orElseThrow();
                    assertEquals("configured-home", resume.at("/params/configDir").asText());
                    assertEquals(recovery ? canonical : "abcdef01", resume.at("/params/sessionId").asText());
                }
            }
        }
    }

    @Test
    void recoveryOffCustomHexIdDoesNotUseHostLookup() throws Exception {
        try (var runtime = new Runtime(true);
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()));
                var session = client
                        .resumeSession("abcdef0",
                                new ResumeSessionConfig().setConfigDirectory("configured-home")
                                        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                        .get(5, TimeUnit.SECONDS)) {
            assertEquals("abcdef0", session.getSessionId());
            assertFalse(runtime.requests.stream()
                    .anyMatch(request -> "sessions.findByPrefix".equals(request.path("method").asText())));
            var resume = runtime.requests.stream()
                    .filter(request -> "session.resume".equals(request.path("method").asText())).findFirst()
                    .orElseThrow();
            assertEquals("abcdef0", resume.at("/params/sessionId").asText());
            assertEquals("configured-home", resume.at("/params/configDir").asText());
        }
    }

    @Test
    void prefixRecoveryKeepsItsAdmissionDuringLookupResumeAndOptions() throws Exception {
        var canonical = "abcdef01-2345-4567-89ab-cdef01234567";
        try (var runtime = new Runtime(true);
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()));
                var resident = client
                        .createSession(new SessionConfig().setSessionId(canonical)
                                .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                                .setOnAutoModeSwitch((request, invocation) -> CompletableFuture
                                        .completedFuture(AutoModeSwitchResponse.YES_ALWAYS)))
                        .get(5, TimeUnit.SECONDS)) {
            runtime.findByPrefixReply = new CompletableFuture<>();
            runtime.resumeReply = new CompletableFuture<>();
            runtime.optionsReply = new CompletableFuture<>();
            var recovery = client.resumeSession("ABCDEF01", new ResumeSessionConfig()
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setAutoModeRecovery(true)
                    .setSkipCustomInstructions(true).setOnAutoModeSwitch(
                            (request, invocation) -> CompletableFuture.completedFuture(AutoModeSwitchResponse.NO)));
            try {
                runtime.findByPrefixRequested.get(5, TimeUnit.SECONDS);
                var next = openWithId(client, true, canonical, false);
                try (var unrelated = openWithId(client, false, "unrelated", false).get(5, TimeUnit.SECONDS)) {
                    assertFalse(next.isDone());
                    assertFalse(runtime.requests.stream()
                            .anyMatch(request -> "session.resume".equals(request.path("method").asText())));
                    var callback = runtime.request("autoModeSwitch.request", Map.of("sessionId", canonical)).get(5,
                            TimeUnit.SECONDS);
                    assertEquals("yes_always", callback.at("/result/response").asText(), callback.toString());
                    runtime.findByPrefixReply.complete(Map.of("sessionId", canonical));
                    runtime.resumeRequested.get(5, TimeUnit.SECONDS);
                    assertFalse(next.isDone());
                    runtime.resumeReply.complete(runtime.resumeResponse(canonical));
                    runtime.optionsRequested.get(5, TimeUnit.SECONDS);
                    assertFalse(next.isDone(), "The prefix permit must cover post-resume options");
                    assertEquals(1, runtime.requests.stream()
                            .filter(request -> "session.resume".equals(request.path("method").asText())).count());
                    runtime.optionsReply.complete(Map.of("success", true));
                    try (var resumed = recovery.get(5, TimeUnit.SECONDS);
                            var ordinary = next.get(5, TimeUnit.SECONDS)) {
                        assertEquals(canonical, resumed.getSessionId());
                        assertEquals(canonical, ordinary.getSessionId());
                    }
                }
            } finally {
                runtime.findByPrefixReply.complete(Map.of("sessionId", canonical));
                runtime.resumeReply.complete(runtime.resumeResponse(canonical));
                runtime.optionsReply.complete(Map.of("success", true));
            }
        }
    }

    @Test
    void prefixRecoveryKeepsItsAdmissionThroughCanonicalDetach() throws Exception {
        var canonical = "abcdef01-2345-4567-89ab-cdef01234567";
        try (var runtime = new Runtime(null);
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()))) {
            runtime.persistedSessionIds.add(canonical);
            runtime.findByPrefixReply = new CompletableFuture<>();
            runtime.detachReply = new CompletableFuture<>();
            var rejected = openWithId(client, true, "ABCDEF01", true);
            try {
                runtime.findByPrefixRequested.get(5, TimeUnit.SECONDS);
                var next = openWithId(client, true, canonical, false);
                try (var unrelated = openWithId(client, false, "unrelated", false).get(5, TimeUnit.SECONDS)) {
                    assertFalse(next.isDone());
                    runtime.findByPrefixReply.complete(Map.of("sessionId", canonical));
                    runtime.detachRequested.get(5, TimeUnit.SECONDS);
                    assertFalse(next.isDone(), "The original prefix permit must cover canonical detach");
                    assertEquals(1, runtime.requests.stream()
                            .filter(request -> "session.resume".equals(request.path("method").asText())).count());
                    assertEquals(canonical,
                            runtime.requests.stream()
                                    .filter(request -> "session.detach".equals(request.path("method").asText()))
                                    .findFirst().orElseThrow().at("/params/sessionId").asText());
                    runtime.detachReply.complete(Map.of("success", true));
                    var error = assertThrows(ExecutionException.class, () -> rejected.get(5, TimeUnit.SECONDS));
                    assertTrue(error.getCause().getMessage().contains("does not support autoModeRecovery"));
                    try (var resumed = next.get(5, TimeUnit.SECONDS)) {
                        assertEquals(canonical, resumed.getSessionId());
                    }
                    assertFalse(runtime.requests.stream()
                            .anyMatch(request -> "session.delete".equals(request.path("method").asText())));
                }
            } finally {
                runtime.findByPrefixReply.complete(Map.of("sessionId", canonical));
                runtime.detachReply.complete(Map.of("success", true));
            }
        }
    }

    @Test
    void residentCallbacksStayWithTheOldOwnerDuringLocalSetupTrace() throws Exception {
        var canonical = UUID.randomUUID().toString();
        var logger = Logger.getLogger(CopilotClient.class.getName());
        var originalLevel = logger.getLevel();
        var setupReached = new CompletableFuture<Void>();
        var releaseSetup = new CompletableFuture<Void>();
        var handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getMessage().contains("resumeSession local setup complete.")
                        && record.getMessage().endsWith("SessionId=" + canonical)) {
                    setupReached.complete(null);
                    releaseSetup.join();
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        try (var runtime = new Runtime(true);
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()));
                var resident = client
                        .createSession(new SessionConfig().setSessionId(canonical)
                                .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                                .setOnAutoModeSwitch((request, invocation) -> CompletableFuture
                                        .completedFuture(AutoModeSwitchResponse.YES_ALWAYS)))
                        .get(5, TimeUnit.SECONDS)) {
            runtime.resumeReply = new CompletableFuture<>();
            logger.addHandler(handler);
            logger.setLevel(Level.FINE);
            var resumed = CompletableFuture.supplyAsync(() -> openWithId(client, true, canonical, true))
                    .thenCompose(result -> result);
            try {
                setupReached.get(5, TimeUnit.SECONDS);
                var callback = runtime.request("autoModeSwitch.request", Map.of("sessionId", canonical)).get(5,
                        TimeUnit.SECONDS);
                assertEquals("yes_always", callback.at("/result/response").asText(), callback.toString());
                assertFalse(runtime.requests.stream()
                        .anyMatch(request -> "session.resume".equals(request.path("method").asText())));
            } finally {
                releaseSetup.complete(null);
                runtime.resumeReply.complete(runtime.resumeResponse(canonical));
            }
            try (var session = resumed.get(5, TimeUnit.SECONDS)) {
                assertEquals(canonical, session.getSessionId());
            }
        } finally {
            releaseSetup.complete(null);
            logger.removeHandler(handler);
            logger.setLevel(originalLevel);
        }
    }

    @Test
    void rejectedNormalizedResumeHoldsReturnedIdAdmissionThroughDetach() throws Exception {
        try (var runtime = new Runtime(null);
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()))) {
            runtime.resumeId = "canonical";
            runtime.detachReply = new CompletableFuture<>();
            var rejected = openWithId(client, true, "alias", true);
            runtime.detachRequested.get(5, TimeUnit.SECONDS);
            var next = openWithId(client, false, "canonical", false);
            try (var unrelated = openWithId(client, false, "unrelated", false).get(5, TimeUnit.SECONDS)) {
                try {
                    assertFalse(next.isDone(), "Same-ID create overtook normalized resume cleanup");
                    assertFalse(runtime.requests.stream().anyMatch(r -> isInitialization(r, "canonical")));
                } finally {
                    runtime.detachReply.complete(Map.of("success", true));
                }
                assertThrows(ExecutionException.class, () -> rejected.get(5, TimeUnit.SECONDS));
                try (var created = next.get(5, TimeUnit.SECONDS)) {
                    assertEquals("canonical", created.getSessionId());
                }
            }
        }
    }

    @Test
    void activeCustomIdCallbackDoesNotRejectAnotherIdsWaitingInitialization() throws Exception {
        var releaseHook = new CompletableFuture<Void>();
        var hookStarted = new CompletableFuture<Void>();
        try (var runtime = new Runtime(null);
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()));
                var resident = client.createSession(new SessionConfig().setSessionId("session-1")
                        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                        .setHooks(new SessionHooks().setOnSessionEnd((input, invocation) -> {
                            hookStarted.complete(null);
                            return releaseHook.thenApply(ignored -> null);
                        }))).get(5, TimeUnit.SECONDS)) {
            var callback = runtime.request("hooks.invoke", Map.of("sessionId", "session-1", "hookType", "sessionEnd",
                    "input", Map.of("sessionId", "session-1", "timestamp", 0, "cwd", ".", "reason", "user_exit")));
            hookStarted.get(5, TimeUnit.SECONDS);
            runtime.detachReply = new CompletableFuture<>();
            var rejected = openWithId(client, false, "session-123", true);
            runtime.detachRequested.get(5, TimeUnit.SECONDS);
            var next = openWithId(client, false, "session-123", false);
            try {
                assertFalse(next.isDone(), "An unrelated callback must not reject the waiting initialization");
                assertEquals(1, runtime.requests.stream().filter(r -> isInitialization(r, "session-123")).count());
            } finally {
                runtime.detachReply.complete(Map.of("success", true));
                releaseHook.complete(null);
            }
            assertFalse(callback.get(5, TimeUnit.SECONDS).has("error"));
            assertThrows(ExecutionException.class, () -> rejected.get(5, TimeUnit.SECONDS));
            try (var created = next.get(5, TimeUnit.SECONDS)) {
                assertEquals("session-123", created.getSessionId());
            }
        } finally {
            releaseHook.complete(null);
        }
    }

    @Test
    void rejectedEnrollmentHookRejectsSameIdReentryWithoutFencingTheSession() throws Exception {
        for (boolean resume : List.of(false, true)) {
            try (var runtime = new Runtime(null);
                    var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()))) {
                runtime.detachReply = new CompletableFuture<>();
                var releaseHook = new CompletableFuture<Void>();
                var hookStarted = new CompletableFuture<Void>();
                var rejected = client.createSession(new SessionConfig().setSessionId("shared")
                        .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setAutoModeRecovery(true)
                        .setOnAutoModeSwitch(
                                (request, invocation) -> CompletableFuture.completedFuture(AutoModeSwitchResponse.NO))
                        .setHooks(new SessionHooks().setOnSessionEnd((input, invocation) -> {
                            hookStarted.complete(null);
                            return releaseHook
                                    .thenComposeAsync(
                                            ignored -> openWithId(client, resume, invocation.getSessionId(), false))
                                    .thenApply(session -> null);
                        })));
                runtime.detachRequested.get(5, TimeUnit.SECONDS);
                var callback = runtime.request("hooks.invoke", Map.of("sessionId", "shared", "hookType", "sessionEnd",
                        "input", Map.of("sessionId", "shared", "timestamp", 0, "cwd", ".", "reason", "user_exit")));
                try {
                    hookStarted.get(5, TimeUnit.SECONDS);
                    try (var unrelated = openWithId(client, false, "unrelated", false).get(5, TimeUnit.SECONDS)) {
                        assertEquals("unrelated", unrelated.getSessionId());
                    }
                    releaseHook.complete(null);
                    var response = callback.get(5, TimeUnit.SECONDS);
                    assertTrue(response.path("error").path("message").asText().contains("session callback"),
                            response.toString());
                    assertEquals(1, runtime.requests.stream().filter(r -> isInitialization(r, "shared")).count());
                } finally {
                    releaseHook.complete(null);
                    runtime.detachReply.complete(Map.of("success", true));
                }
                var error = assertThrows(ExecutionException.class, () -> rejected.get(5, TimeUnit.SECONDS));
                assertTrue(error.getCause().getMessage().contains("does not support autoModeRecovery"));
                assertEquals(0, error.getCause().getSuppressed().length);
                try (var replacement = openWithId(client, resume, "shared", false).get(5, TimeUnit.SECONDS)) {
                    assertEquals("shared", replacement.getSessionId());
                }
            }
        }
    }

    @Test
    void rejectedNormalizedResumeRoutesSessionEndHookUntilDetachCompletes() throws Exception {
        try (var runtime = new Runtime(null);
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()))) {
            runtime.resumeId = "canonical";
            runtime.detachReply = new CompletableFuture<>();
            var hookCalls = new AtomicInteger();
            var rejected = client.resumeSession("prefix", new ResumeSessionConfig()
                    .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setAutoModeRecovery(true)
                    .setOnAutoModeSwitch(
                            (request, invocation) -> CompletableFuture.completedFuture(AutoModeSwitchResponse.NO))
                    .setHooks(new SessionHooks().setOnSessionEnd((input, invocation) -> {
                        assertEquals("canonical", invocation.getSessionId());
                        assertEquals("user_exit", input.reason());
                        hookCalls.incrementAndGet();
                        return CompletableFuture.completedFuture(null);
                    })));
            runtime.detachRequested.get(5, TimeUnit.SECONDS);
            var params = Map.of("sessionId", "canonical", "hookType", "sessionEnd", "input",
                    Map.of("sessionId", "canonical", "timestamp", 0, "cwd", ".", "reason", "user_exit"));
            try {
                var response = runtime.request("hooks.invoke", params).get(5, TimeUnit.SECONDS);
                assertFalse(response.has("error"), "Detach must route canonical session-end hooks: " + response);
                assertEquals(1, hookCalls.get());
            } finally {
                runtime.detachReply.complete(Map.of("success", true));
            }
            assertThrows(ExecutionException.class, () -> rejected.get(5, TimeUnit.SECONDS));
            var retired = runtime.request("hooks.invoke", params).get(5, TimeUnit.SECONDS);
            assertTrue(retired.path("error").path("message").asText().contains("Unknown session"), retired.toString());
            assertEquals(1, hookCalls.get());
            assertFalse(runtime.requests.stream().anyMatch(r -> r.path("method").asText().equals("session.delete")));
            try (var replacement = openWithId(client, false, "canonical", false).get(5, TimeUnit.SECONDS)) {
                assertEquals("canonical", replacement.getSessionId());
            }
        }
    }

    @Test
    void rejectedResumeRpcRestoresTheResidentOwner() throws Exception {
        try (var runtime = new Runtime(null);
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()));
                var resident = open(client, false, false,
                        (request, invocation) -> CompletableFuture.completedFuture(AutoModeSwitchResponse.YES_ALWAYS))
                        .get(5, TimeUnit.SECONDS)) {
            runtime.rejectResume = true;
            assertThrows(ExecutionException.class,
                    () -> openWithId(client, true, resident.getSessionId(), true).get(5, TimeUnit.SECONDS));
            assertFalse(runtime.requests.stream().anyMatch(r -> r.path("method").asText().equals("session.detach")));
            var response = runtime
                    .request("autoModeSwitch.request",
                            Map.of("sessionId", resident.getSessionId(), "errorCode", "model_overloaded"))
                    .get(5, TimeUnit.SECONDS);
            assertEquals("yes_always", response.at("/result/response").asText());
        }
    }

    private static boolean isInitialization(JsonNode request, String id) {
        return List.of("session.create", "session.resume").contains(request.path("method").asText())
                && id.equals(request.at("/params/sessionId").asText());
    }

    private static CompletableFuture<CopilotSession> openWithId(CopilotClient client, boolean resume, String id,
            boolean recovery) {
        AutoModeSwitchHandler handler = (request, invocation) -> CompletableFuture
                .completedFuture(AutoModeSwitchResponse.NO);
        return resume
                ? client.resumeSession(id,
                        new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                                .setAutoModeRecovery(recovery).setOnAutoModeSwitch(handler))
                : client.createSession(
                        new SessionConfig().setSessionId(id).setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                                .setAutoModeRecovery(recovery).setOnAutoModeSwitch(handler));
    }

    @Test
    void acceptedResumeWithoutRecoveryKeepsTheNewCallbackOwner() throws Exception {
        try (var runtime = new Runtime(null);
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()));
                var resident = open(client, false, false,
                        (request, invocation) -> CompletableFuture.completedFuture(AutoModeSwitchResponse.YES_ALWAYS))
                        .get(5, TimeUnit.SECONDS)) {
            var error = assertThrows(ExecutionException.class, () -> client
                    .resumeSession(resident.getSessionId(),
                            new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                                    .setAutoModeRecovery(true)
                                    .setOnAutoModeSwitch((request, invocation) -> CompletableFuture
                                            .completedFuture(AutoModeSwitchResponse.NO))
                                    .setGitHubTokenProvider(args -> CompletableFuture
                                            .completedFuture(GitHubTokenProviderResult.token("resumed-token", 60))))
                    .get(5, TimeUnit.SECONDS));
            assertTrue(error.getCause().getMessage().contains("does not support autoModeRecovery"));
            assertFalse(runtime.requests.stream().anyMatch(r -> r.path("method").asText().equals("session.detach")));
            var response = runtime
                    .request("autoModeSwitch.request",
                            Map.of("sessionId", resident.getSessionId(), "errorCode", "user_weekly_rate_limited"))
                    .get(5, TimeUnit.SECONDS);
            assertEquals("no", response.at("/result/response").asText());
            var resume = runtime.requests.stream().filter(r -> r.path("method").asText().equals("session.resume"))
                    .findFirst().orElseThrow();
            var token = runtime
                    .request("gitHubToken.getToken",
                            Map.of("registrationId", resume.at("/params/gitHubTokenProviderRegistrationId").asText(),
                                    "host", "github.com", "sessionId", resident.getSessionId(), "reason", "refresh"))
                    .get(5, TimeUnit.SECONDS);
            assertEquals("resumed-token", token.at("/result/accessToken").asText());
        }
    }

    @Test
    void acceptedNormalizedResumeWithoutRecoveryKeepsTheNewCallbackOwner() throws Exception {
        try (var runtime = new Runtime(null);
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()))) {
            runtime.resumeId = "normalized-session";
            try (var resident = client
                    .resumeSession("alias",
                            new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                                    .setOnAutoModeSwitch((request, invocation) -> CompletableFuture
                                            .completedFuture(AutoModeSwitchResponse.YES_ALWAYS)))
                    .get(5, TimeUnit.SECONDS)) {
                assertThrows(ExecutionException.class,
                        () -> client
                                .resumeSession("alias",
                                        new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                                                .setAutoModeRecovery(true)
                                                .setOnAutoModeSwitch((request, invocation) -> CompletableFuture
                                                        .completedFuture(AutoModeSwitchResponse.NO)))
                                .get(5, TimeUnit.SECONDS));
                assertFalse(
                        runtime.requests.stream().anyMatch(r -> r.path("method").asText().equals("session.detach")));
                assertEquals(
                        "no", runtime
                                .request("autoModeSwitch.request",
                                        Map.of("sessionId", resident.getSessionId(), "errorCode",
                                                "user_weekly_rate_limited"))
                                .get(5, TimeUnit.SECONDS).at("/result/response").asText());
            }
        }
    }

    @Test
    void rejectedNormalizedResumeDetachesOnlyTheNewReturnedAttachment() throws Exception {
        try (var runtime = new Runtime(null);
                var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()));
                var resident = open(client, false, false,
                        (request, invocation) -> CompletableFuture.completedFuture(AutoModeSwitchResponse.YES_ALWAYS))
                        .get(5, TimeUnit.SECONDS)) {
            runtime.resumeId = "new-normalized-session";
            assertThrows(ExecutionException.class,
                    () -> client
                            .resumeSession(resident.getSessionId(),
                                    new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                                            .setAutoModeRecovery(true)
                                            .setOnAutoModeSwitch((request, invocation) -> CompletableFuture
                                                    .completedFuture(AutoModeSwitchResponse.NO)))
                            .get(5, TimeUnit.SECONDS));
            var detached = runtime.requests.stream().filter(r -> r.path("method").asText().equals("session.detach"))
                    .toList();
            assertEquals(1, detached.size());
            assertEquals(runtime.resumeId, detached.get(0).at("/params/sessionId").asText());
            assertEquals(
                    "yes_always", runtime
                            .request("autoModeSwitch.request",
                                    Map.of("sessionId", resident.getSessionId(), "errorCode",
                                            "user_weekly_rate_limited"))
                            .get(5, TimeUnit.SECONDS).at("/result/response").asText());
        }
    }

    @Test
    void projectsEnrollmentCompleteContextAndCapabilityChanges() throws Exception {
        for (boolean resume : List.of(false, true)) {
            for (boolean enabled : List.of(false, true)) {
                try (var runtime = new Runtime(enabled ? true : null);
                        var client = new CopilotClient(new CopilotClientOptions().setCliUrl(runtime.url()))) {
                    var called = new CompletableFuture<AutoModeSwitchRequest>();
                    var answer = new CompletableFuture<AutoModeSwitchResponse>();
                    try (var session = open(client, resume, enabled, (request, invocation) -> {
                        assertNotNull(invocation.getSessionId());
                        called.complete(request);
                        return answer;
                    }).get(5, TimeUnit.SECONDS)) {
                        var wire = runtime.requests.stream()
                                .filter(r -> r.path("method").asText()
                                        .equals(resume ? "session.resume" : "session.create"))
                                .findFirst().orElseThrow().path("params");
                        assertTrue(wire.path("requestAutoModeSwitch").asBoolean());
                        assertEquals(enabled, wire.path("requestAutoModeRecovery").asBoolean(false));
                        var params = MAPPER.createObjectNode().put("sessionId", session.getSessionId())
                                .put("errorCode", "model_overloaded").put("retryAfterSeconds", 3.5);
                        if (enabled) {
                            params.set("autoModeRecovery", MAPPER.valueToTree(AUTO_MODE_RECOVERY));
                        }
                        var response = runtime.request("autoModeSwitch.request", params);
                        var received = called.get(5, TimeUnit.SECONDS);
                        assertFalse(response.isDone());
                        assertEquals("model_overloaded", received.getErrorCode());
                        assertEquals(3.5, received.getRetryAfterSeconds());
                        if (enabled) {
                            assertEquals(MAPPER.valueToTree(AUTO_MODE_RECOVERY),
                                    MAPPER.valueToTree(received.getAutoModeRecovery()));
                            assertTrue(session.getCapabilities().getUi().getAutoModeRecovery().orElseThrow());
                            runtime.eventAndDrain(session, "capabilities.changed",
                                    Map.of("ui", Map.of("elicitation", false)));
                            assertTrue(session.getCapabilities().getUi().getAutoModeRecovery().orElseThrow());
                            runtime.eventAndDrain(session, "capabilities.changed",
                                    Map.of("ui", Map.of("autoModeRecovery", false)));
                            assertFalse(session.getCapabilities().getUi().getAutoModeRecovery().orElseThrow());
                            assertFalse(session.getCapabilities().getUi().getElicitation().orElseThrow());
                        } else {
                            assertNull(received.getAutoModeRecovery());
                        }
                        answer.complete(enabled ? AutoModeSwitchResponse.YES : AutoModeSwitchResponse.YES_ALWAYS);
                        assertEquals(enabled ? "yes" : "yes_always",
                                response.get(5, TimeUnit.SECONDS).at("/result/response").asText());
                    } finally {
                        answer.complete(AutoModeSwitchResponse.NO);
                    }
                }
            }
        }
    }

    private static final class Runtime implements AutoCloseable {
        private final ServerSocket listener;
        private final Boolean acknowledged;
        private final CompletableFuture<Void> finished = new CompletableFuture<>();
        private final List<JsonNode> requests = new CopyOnWriteArrayList<>();
        private final Map<Integer, CompletableFuture<JsonNode>> replies = new ConcurrentHashMap<>();
        private final AtomicInteger nextId = new AtomicInteger();
        private volatile Socket peer;
        private volatile boolean closed;
        private volatile String resumeId;
        private volatile boolean rejectResume;
        private volatile boolean deleteSuccess = true;
        private volatile CompletableFuture<Map<String, Object>> detachReply;
        private final CompletableFuture<Void> detachRequested = new CompletableFuture<>();
        private volatile CompletableFuture<Map<String, Object>> resumeReply;
        private final CompletableFuture<Void> resumeRequested = new CompletableFuture<>();
        private final List<String> persistedSessionIds = new CopyOnWriteArrayList<>();
        private volatile CompletableFuture<Map<String, Object>> findByPrefixReply;
        private final CompletableFuture<Void> findByPrefixRequested = new CompletableFuture<>();
        private volatile CompletableFuture<Map<String, Object>> optionsReply;
        private final CompletableFuture<Void> optionsRequested = new CompletableFuture<>();
        private volatile CompletableFuture<Map<String, Object>> deleteReply;
        private final CompletableFuture<Void> deleteRequested = new CompletableFuture<>();

        Runtime(Boolean acknowledged) throws Exception {
            this.acknowledged = acknowledged;
            listener = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"));
            new Thread(this::run, "auto-mode-recovery-test").start();
        }

        String url() {
            return "127.0.0.1:" + listener.getLocalPort();
        }

        Map<String, Object> resumeResponse(String sessionId) {
            return acknowledged == null
                    ? Map.of("sessionId", sessionId)
                    : Map.of("sessionId", sessionId, "capabilities",
                            Map.of("ui", Map.of("autoModeRecovery", acknowledged, "elicitation", true)));
        }

        private void replyWhenComplete(JsonNode request, CompletableFuture<Map<String, Object>> reply) {
            reply.whenComplete((response, error) -> {
                try {
                    if (error != null) {
                        send(Map.of("jsonrpc", "2.0", "id", request.get("id"), "error",
                                Map.of("code", -32000, "message", error.getMessage())));
                    } else {
                        send(Map.of("jsonrpc", "2.0", "id", request.get("id"), "result", response));
                    }
                } catch (Exception failure) {
                    finished.completeExceptionally(failure);
                }
            });
        }

        CompletableFuture<JsonNode> request(String method, Object params) throws Exception {
            int id = nextId.incrementAndGet();
            var reply = new CompletableFuture<JsonNode>();
            replies.put(id, reply);
            send(Map.of("jsonrpc", "2.0", "id", id, "method", method, "params", params));
            return reply;
        }

        void eventAndDrain(CopilotSession session, String type, Object data) throws Exception {
            var drained = new CompletableFuture<Void>();
            try (var subscription = session.on(event -> {
                if ("session.title_changed".equals(event.getType())) {
                    drained.complete(null);
                }
            })) {
                event(session.getSessionId(), type, data);
                event(session.getSessionId(), "session.title_changed", Map.of("title", "fence"));
                drained.get(5, TimeUnit.SECONDS);
            }
        }

        private void event(String sessionId, String type, Object data) throws Exception {
            send(Map.of("jsonrpc", "2.0", "method", "session.event", "params",
                    Map.of("sessionId", sessionId, "event", Map.of("id", UUID.randomUUID().toString(), "timestamp",
                            "2026-09-25T12:00:00Z", "type", type, "data", data, "ephemeral", true))));
        }

        private synchronized void send(Object message) throws Exception {
            byte[] body = MAPPER.writeValueAsBytes(message);
            OutputStream out = peer.getOutputStream();
            out.write(("Content-Length: " + body.length + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            out.write(body);
            out.flush();
        }

        private void run() {
            try (Socket socket = listener.accept()) {
                peer = socket;
                JsonNode message;
                while ((message = read(socket.getInputStream())) != null) {
                    if (!message.has("method")) {
                        replies.remove(message.path("id").asInt()).complete(message);
                        continue;
                    }
                    requests.add(message);
                    if ("sessions.findByPrefix".equals(message.path("method").asText()) && findByPrefixReply != null) {
                        replyWhenComplete(message, findByPrefixReply);
                        findByPrefixRequested.complete(null);
                        continue;
                    }
                    if ("session.options.update".equals(message.path("method").asText()) && optionsReply != null) {
                        replyWhenComplete(message, optionsReply);
                        optionsRequested.complete(null);
                        continue;
                    }
                    if ("session.resume".equals(message.path("method").asText()) && resumeReply != null
                            && !resumeRequested.isDone()) {
                        replyWhenComplete(message, resumeReply);
                        resumeRequested.complete(null);
                        continue;
                    }
                    if ("session.detach".equals(message.path("method").asText()) && detachReply != null
                            && !detachRequested.isDone()) {
                        var requestId = message.get("id");
                        detachReply.whenComplete((response, error) -> {
                            try {
                                if (error != null) {
                                    send(Map.of("jsonrpc", "2.0", "id", requestId, "error",
                                            Map.of("code", -32000, "message", error.getMessage())));
                                } else {
                                    var reply = MAPPER.createObjectNode().put("jsonrpc", "2.0");
                                    reply.set("id", requestId);
                                    reply.set("result", MAPPER.valueToTree(response));
                                    send(reply);
                                }
                            } catch (Exception e) {
                                finished.completeExceptionally(e);
                            }
                        });
                        detachRequested.complete(null);
                        continue;
                    }
                    if ("session.resume".equals(message.path("method").asText()) && rejectResume) {
                        send(Map.of("jsonrpc", "2.0", "id", message.get("id"), "error",
                                Map.of("code", -32000, "message", "resume denied")));
                        continue;
                    }
                    if ("session.delete".equals(message.path("method").asText()) && deleteReply != null) {
                        var requestId = message.get("id");
                        deleteReply.thenAccept(response -> {
                            try {
                                send(Map.of("jsonrpc", "2.0", "id", requestId, "result", response));
                            } catch (Exception error) {
                                finished.completeExceptionally(error);
                            }
                        });
                        deleteRequested.complete(null);
                        continue;
                    }
                    var result = MAPPER.createObjectNode();
                    switch (message.path("method").asText()) {
                        case "connect" -> result.put("ok", true).put("protocolVersion", 3).put("version", "test");
                        case "sessions.findByPrefix" -> {
                            var prefix = message.at("/params/prefix").asText().toLowerCase(java.util.Locale.ROOT);
                            var matches = persistedSessionIds.stream().distinct()
                                    .filter(id -> id.toLowerCase(java.util.Locale.ROOT).startsWith(prefix)).toList();
                            if (matches.size() == 1) {
                                result.put("sessionId", matches.get(0));
                            }
                            findByPrefixRequested.complete(null);
                        }
                        case "session.create", "session.resume" -> {
                            result.put("sessionId",
                                    message.path("method").asText().equals("session.resume") && resumeId != null
                                            ? resumeId
                                            : message.at("/params/sessionId").asText("cloud-session"));
                            persistedSessionIds.add(result.path("sessionId").asText());
                            if (acknowledged != null) {
                                result.set("capabilities", MAPPER.valueToTree(
                                        Map.of("ui", Map.of("autoModeRecovery", acknowledged, "elicitation", true))));
                            }
                        }
                        case "session.detach", "session.options.update", "session.skills.reload" ->
                            result.put("success", true);
                        case "session.delete" -> {
                            result.put("success", deleteSuccess);
                            if (!deleteSuccess) {
                                result.put("error", "delete denied");
                            }
                        }
                        case "session.ui.registerDirectAutoModeSwitchHandler" -> {
                            result.put("handle", "direct-handler");
                            if (message.path("params").has("autoModeRecovery")) {
                                result.put("autoModeRecovery", message.at("/params/autoModeRecovery").asBoolean());
                            }
                        }
                        default -> throw new AssertionError("Unexpected RPC: " + message);
                    }
                    send(Map.of("jsonrpc", "2.0", "id", message.get("id"), "result", result));
                }
                finished.complete(null);
            } catch (Throwable error) {
                if (closed) {
                    finished.complete(null);
                } else {
                    finished.completeExceptionally(error);
                }
            }
        }

        private static JsonNode read(InputStream in) throws Exception {
            var header = new StringBuilder();
            while (!header.toString().endsWith("\r\n\r\n")) {
                int next = in.read();
                if (next == -1) {
                    return null;
                }
                header.append((char) next);
            }
            int length = Integer.parseInt(header.toString().trim().substring("Content-Length:".length()).trim());
            return MAPPER.readTree(in.readNBytes(length));
        }

        @Override
        public void close() throws Exception {
            closed = true;
            listener.close();
            if (peer != null) {
                peer.close();
            }
            finished.get(5, TimeUnit.SECONDS);
        }
    }
}
