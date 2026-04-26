/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.json.CopilotClientOptions;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.PingResponse;
import com.github.copilot.sdk.json.SessionConfig;
import com.github.copilot.sdk.json.SessionLifecycleEvent;
import com.github.copilot.sdk.json.SessionLifecycleEventTypes;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CopilotClient.
 *
 * Note: These tests require the Copilot CLI to be installed. Set the
 * COPILOT_CLI_PATH environment variable to the path to the CLI, or run 'npm
 * install' in the nodejs directory.
 */
public class CopilotClientTest {

    private static String cliPath;

    @BeforeAll
    static void setup() {
        cliPath = TestUtil.findCliPath();
    }

    @Test
    void testClientConstruction() {
        var client = new CopilotClient();
        assertEquals(ConnectionState.DISCONNECTED, client.getState());
        client.close();
    }

    @Test
    void testClientConstructionWithOptions() {
        var options = new CopilotClientOptions().setCliPath("/path/to/cli").setLogLevel("debug").setAutoStart(false);

        var client = new CopilotClient(options);
        assertEquals(ConnectionState.DISCONNECTED, client.getState());
        client.close();
    }

    @Test
    void testCliUrlAutoCorrectsUseStdio() {
        var options = new CopilotClientOptions().setCliUrl("localhost:3000").setUseStdio(true);

        // Should NOT throw - useStdio is auto-corrected to false when cliUrl is set
        var client = new CopilotClient(options);
        assertFalse(options.isUseStdio(), "useStdio should be auto-corrected to false when cliUrl is set");
        client.close();
    }

    @Test
    void testCliUrlOnlyConstruction() {
        var options = new CopilotClientOptions().setCliUrl("localhost:4321");

        // Should work without explicitly setting useStdio to false
        var client = new CopilotClient(options);
        assertEquals(ConnectionState.DISCONNECTED, client.getState());
        assertFalse(options.isUseStdio(), "useStdio should be auto-corrected to false when cliUrl is set");
        client.close();
    }

    @Test
    void testCliUrlMutualExclusionWithCliPath() {
        var options = new CopilotClientOptions().setCliUrl("localhost:3000").setCliPath("/path/to/cli");

        assertThrows(IllegalArgumentException.class, () -> new CopilotClient(options));
    }

    @Test
    void testStartAndConnectUsingStdio() throws Exception {
        assertNotNull(cliPath, "Copilot CLI not found in PATH or COPILOT_CLI_PATH");

        try (var client = new CopilotClient(new CopilotClientOptions().setCliPath(cliPath).setUseStdio(true))) {
            client.start().get();
            assertEquals(ConnectionState.CONNECTED, client.getState());

            PingResponse pong = client.ping("test message").get();
            assertEquals("pong: test message", pong.message());
            assertTrue(pong.timestamp() >= 0);

            client.stop().get();
            assertEquals(ConnectionState.DISCONNECTED, client.getState());
        }
    }

    @Test
    void testShouldReportErrorWithStderrWhenCliFailsToStart() throws Exception {
        assertNotNull(cliPath, "Copilot CLI not found in PATH or COPILOT_CLI_PATH");

        var options = new CopilotClientOptions().setCliPath(cliPath)
                .setCliArgs(new String[]{"--nonexistent-flag-for-testing"}).setUseStdio(true);

        try (var client = new CopilotClient(options)) {
            Exception ex = assertThrows(Exception.class, () -> client.start().get());
            Throwable root = ex instanceof ExecutionException && ex.getCause() != null ? ex.getCause() : ex;
            String message = root.getMessage();
            assertNotNull(message);
            assertTrue(message.toLowerCase().contains("stderr") || message.toLowerCase().contains("unexpectedly"),
                    "Error should include stderr or unexpected exit details: " + message);
        }
    }

    @Test
    void testStartAndConnectUsingTcp() throws Exception {
        assertNotNull(cliPath, "Copilot CLI not found in PATH or COPILOT_CLI_PATH");

        try (var client = new CopilotClient(new CopilotClientOptions().setCliPath(cliPath).setUseStdio(false))) {
            client.start().get();
            assertEquals(ConnectionState.CONNECTED, client.getState());

            PingResponse pong = client.ping("test message").get();
            assertEquals("pong: test message", pong.message());

            client.stop().get();
        }
    }

    @Test
    void testForceStopWithoutCleanup() throws Exception {
        assertNotNull(cliPath, "Copilot CLI not found in PATH or COPILOT_CLI_PATH");

        try (var client = new CopilotClient(new CopilotClientOptions().setCliPath(cliPath))) {
            client.createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();
            client.forceStop().get();

            assertEquals(ConnectionState.DISCONNECTED, client.getState());
        }
    }

    @Test
    void testGitHubTokenOptionAccepted() {
        var options = new CopilotClientOptions().setCliPath("/path/to/cli").setGitHubToken("gho_test_token");

        assertEquals("gho_test_token", options.getGitHubToken());
    }

    @Test
    void testUseLoggedInUserDefaultsToNull() {
        var options = new CopilotClientOptions().setCliPath("/path/to/cli");

        assertNull(options.getUseLoggedInUser());
    }

    @Test
    void testExplicitUseLoggedInUserFalse() {
        var options = new CopilotClientOptions().setCliPath("/path/to/cli").setUseLoggedInUser(false);

        assertEquals(false, options.getUseLoggedInUser());
    }

    @Test
    void testExplicitUseLoggedInUserTrueWithGitHubToken() {
        var options = new CopilotClientOptions().setCliPath("/path/to/cli").setGitHubToken("gho_test_token")
                .setUseLoggedInUser(true);

        assertEquals(true, options.getUseLoggedInUser());
    }

    @Test
    void testGitHubTokenWithCliUrlThrows() {
        var options = new CopilotClientOptions().setCliUrl("localhost:8080").setGitHubToken("gho_test_token");

        assertThrows(IllegalArgumentException.class, () -> new CopilotClient(options));
    }

    @Test
    void testUseLoggedInUserWithCliUrlThrows() {
        var options = new CopilotClientOptions().setCliUrl("localhost:8080").setUseLoggedInUser(false);

        assertThrows(IllegalArgumentException.class, () -> new CopilotClient(options));
    }

    @Test
    void testSessionIdleTimeoutSecondsDefaultsToNull() {
        var options = new CopilotClientOptions();

        assertNull(options.getSessionIdleTimeoutSeconds());
    }

    @Test
    void testSessionIdleTimeoutSecondsOptionAccepted() {
        var options = new CopilotClientOptions().setSessionIdleTimeoutSeconds(600);

        assertEquals(600, options.getSessionIdleTimeoutSeconds());
    }

    // ===== onLifecycle tests =====

    /**
     * Gets the internal LifecycleEventManager from a CopilotClient via reflection
     * so we can dispatch events for testing.
     */
    private static LifecycleEventManager getLifecycleManager(CopilotClient client) throws Exception {
        Field f = CopilotClient.class.getDeclaredField("lifecycleManager");
        f.setAccessible(true);
        return (LifecycleEventManager) f.get(client);
    }

    private static SessionLifecycleEvent lifecycleEvent(String type) {
        var e = new SessionLifecycleEvent();
        e.setType(type);
        e.setSessionId("test-session-id");
        return e;
    }

    @Test
    void testOnLifecycleWildcardReceivesAllEvents() throws Exception {
        try (var client = new CopilotClient()) {
            var received = new ArrayList<SessionLifecycleEvent>();
            client.onLifecycle(received::add);

            LifecycleEventManager mgr = getLifecycleManager(client);
            mgr.dispatch(lifecycleEvent(SessionLifecycleEventTypes.CREATED));
            mgr.dispatch(lifecycleEvent(SessionLifecycleEventTypes.DELETED));

            assertEquals(2, received.size());
            assertEquals(SessionLifecycleEventTypes.CREATED, received.get(0).getType());
            assertEquals(SessionLifecycleEventTypes.DELETED, received.get(1).getType());
        }
    }

    @Test
    void testOnLifecycleTypedReceivesOnlyMatchingEvents() throws Exception {
        try (var client = new CopilotClient()) {
            var received = new ArrayList<SessionLifecycleEvent>();
            client.onLifecycle(SessionLifecycleEventTypes.CREATED, received::add);

            LifecycleEventManager mgr = getLifecycleManager(client);
            mgr.dispatch(lifecycleEvent(SessionLifecycleEventTypes.CREATED));
            mgr.dispatch(lifecycleEvent(SessionLifecycleEventTypes.DELETED));

            assertEquals(1, received.size());
            assertEquals(SessionLifecycleEventTypes.CREATED, received.get(0).getType());
        }
    }

    @Test
    void testOnLifecycleUnsubscribeStopsDelivery() throws Exception {
        try (var client = new CopilotClient()) {
            var received = new ArrayList<SessionLifecycleEvent>();
            AutoCloseable sub = client.onLifecycle(received::add);

            LifecycleEventManager mgr = getLifecycleManager(client);
            mgr.dispatch(lifecycleEvent(SessionLifecycleEventTypes.CREATED));
            assertEquals(1, received.size());

            sub.close();

            mgr.dispatch(lifecycleEvent(SessionLifecycleEventTypes.DELETED));
            assertEquals(1, received.size(), "Should not receive events after unsubscribe");
        }
    }

    @Test
    void testOnLifecycleTypedUnsubscribeStopsDelivery() throws Exception {
        try (var client = new CopilotClient()) {
            var received = new ArrayList<SessionLifecycleEvent>();
            AutoCloseable sub = client.onLifecycle(SessionLifecycleEventTypes.UPDATED, received::add);

            LifecycleEventManager mgr = getLifecycleManager(client);
            mgr.dispatch(lifecycleEvent(SessionLifecycleEventTypes.UPDATED));
            assertEquals(1, received.size());

            sub.close();

            mgr.dispatch(lifecycleEvent(SessionLifecycleEventTypes.UPDATED));
            assertEquals(1, received.size(), "Should not receive events after unsubscribe");
        }
    }

    @Test
    void testOnLifecycleMultipleHandlers() throws Exception {
        try (var client = new CopilotClient()) {
            var wildcard = new ArrayList<SessionLifecycleEvent>();
            var typed = new ArrayList<SessionLifecycleEvent>();

            client.onLifecycle(wildcard::add);
            client.onLifecycle(SessionLifecycleEventTypes.CREATED, typed::add);

            LifecycleEventManager mgr = getLifecycleManager(client);
            mgr.dispatch(lifecycleEvent(SessionLifecycleEventTypes.CREATED));

            assertEquals(1, wildcard.size());
            assertEquals(1, typed.size());
        }
    }

    // ===== getState() coverage =====

    @Test
    void testGetStateErrorAfterFailedStart() throws Exception {
        // Use a non-existent CLI path to trigger a startup failure
        var options = new CopilotClientOptions().setCliPath("/nonexistent/path/to/cli").setAutoStart(false);

        try (var client = new CopilotClient(options)) {
            // Manually start to trigger the error
            CompletableFuture<Void> startFuture = client.start();

            // Wait for the start to fail
            try {
                startFuture.get();
            } catch (ExecutionException e) {
                // Expected
            }

            assertEquals(ConnectionState.ERROR, client.getState());
        }
    }

    @Test
    void testGetStateConnectingDuringStart() throws Exception {
        // Use a non-existent CLI path; the future won't complete immediately
        var options = new CopilotClientOptions().setCliPath("/nonexistent/path/to/cli").setAutoStart(false);

        try (var client = new CopilotClient(options)) {
            // Start is async - grab state before completion
            client.start();

            // The state should be either CONNECTING or ERROR depending on timing
            ConnectionState state = client.getState();
            assertTrue(state == ConnectionState.CONNECTING || state == ConnectionState.ERROR,
                    "State should be CONNECTING or ERROR, was: " + state);
        }
    }

    // ===== ensureConnected throws when autoStart=false and not connected =====

    @Test
    void testEnsureConnectedThrowsWhenNotStartedAndAutoStartDisabled() {
        var options = new CopilotClientOptions().setAutoStart(false);

        try (var client = new CopilotClient(options)) {
            // Calling ping (which calls ensureConnected) without start() should throw
            assertThrows(IllegalStateException.class, () -> client.ping("test"));
        }
    }

    // ===== close() idempotency =====

    @Test
    void testCloseIsIdempotent() {
        var client = new CopilotClient();

        // First close
        client.close();
        // Second close should not throw
        assertDoesNotThrow(() -> client.close());
    }

    @Test
    void testCloseAfterFailedStart() throws Exception {
        var options = new CopilotClientOptions().setCliPath("/nonexistent/path/to/cli").setAutoStart(false);
        var client = new CopilotClient(options);

        CompletableFuture<Void> startFuture = client.start();
        try {
            startFuture.get();
        } catch (ExecutionException e) {
            // Expected
        }

        // close() after a failed start should not throw
        assertDoesNotThrow(() -> client.close());
    }

    // ===== stop() with no connection =====

    @Test
    void testStopWithNoConnectionCompletes() throws Exception {
        try (var client = new CopilotClient(new CopilotClientOptions().setAutoStart(false))) {
            // stop() without start() should complete without error
            client.stop().get();
            assertEquals(ConnectionState.DISCONNECTED, client.getState());
        }
    }

    @Test
    void testForceStopWithNoConnectionCompletes() throws Exception {
        try (var client = new CopilotClient(new CopilotClientOptions().setAutoStart(false))) {
            // forceStop() without start() should complete without error
            client.forceStop().get();
            assertEquals(ConnectionState.DISCONNECTED, client.getState());
        }
    }

    @Test
    void testCloseSessionAfterStoppingClientDoesNotThrow() throws Exception {
        assertNotNull(cliPath, "Copilot CLI not found in PATH or COPILOT_CLI_PATH");

        try (var client = new CopilotClient(new CopilotClientOptions().setCliPath(cliPath))) {
            var session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get();

            // Stop the client first (which closes the RPC connection)
            client.stop().get();

            // Then close the session - should not throw even though RPC is closed
            assertDoesNotThrow(() -> session.close(), "Closing session after client.stop() should not throw exception");

            // Verify session is terminated
            assertThrows(IllegalStateException.class, () -> session.send("test"),
                    "Session should be terminated after close()");
        }
    }

    // ===== start() idempotency =====

    @Test
    void testStartIsIdempotentSingleConnectionAttempt() throws Exception {
        var options = new CopilotClientOptions().setCliPath("/nonexistent/path/to/cli").setAutoStart(false);

        try (var client = new CopilotClient(options)) {
            client.start();
            client.start();

            // Both calls should result in the same state (single connection attempt)
            ConnectionState state = client.getState();
            assertTrue(state == ConnectionState.CONNECTING || state == ConnectionState.ERROR,
                    "State should be CONNECTING or ERROR after start(), was: " + state);
        }
    }

    // ===== null options defaulting =====

    @Test
    void testNullOptionsDefaultsToEmpty() {
        try (var client = new CopilotClient(null)) {
            assertEquals(ConnectionState.DISCONNECTED, client.getState());
        }
    }

    // ===== OnListModels =====

    @Test
    void testListModels_WithCustomHandler_CallsHandler() throws Exception {
        var customModels = new ArrayList<com.github.copilot.sdk.json.ModelInfo>();
        var model = new com.github.copilot.sdk.json.ModelInfo();
        model.setId("my-custom-model");
        customModels.add(model);

        var callCount = new int[]{0};
        var options = new CopilotClientOptions().setOnListModels(() -> {
            callCount[0]++;
            return CompletableFuture.completedFuture(new ArrayList<>(customModels));
        });

        try (var client = new CopilotClient(options)) {
            var models = client.listModels().get();
            assertEquals(1, callCount[0]);
            assertEquals(1, models.size());
            assertEquals("my-custom-model", models.get(0).getId());
        }
    }

    @Test
    void testListModels_WithCustomHandler_CachesResults() throws Exception {
        var customModels = new ArrayList<com.github.copilot.sdk.json.ModelInfo>();
        var model = new com.github.copilot.sdk.json.ModelInfo();
        model.setId("cached-model");
        customModels.add(model);

        var callCount = new int[]{0};
        var options = new CopilotClientOptions().setOnListModels(() -> {
            callCount[0]++;
            return CompletableFuture.completedFuture(new ArrayList<>(customModels));
        });

        try (var client = new CopilotClient(options)) {
            client.listModels().get();
            client.listModels().get();
            assertEquals(1, callCount[0], "Handler should be called only once due to caching");
        }
    }

    @Test
    void testListModels_WithCustomHandler_WorksWithoutStart() throws Exception {
        var customModels = new ArrayList<com.github.copilot.sdk.json.ModelInfo>();
        var model = new com.github.copilot.sdk.json.ModelInfo();
        model.setId("no-start-model");
        customModels.add(model);

        var callCount = new int[]{0};
        var options = new CopilotClientOptions().setOnListModels(() -> {
            callCount[0]++;
            return CompletableFuture.completedFuture(new ArrayList<>(customModels));
        });

        // No start() needed when onListModels is provided
        try (var client = new CopilotClient(options)) {
            var models = client.listModels().get();
            assertEquals(1, callCount[0]);
            assertEquals(1, models.size());
            assertEquals("no-start-model", models.get(0).getId());
        }
    }
}
