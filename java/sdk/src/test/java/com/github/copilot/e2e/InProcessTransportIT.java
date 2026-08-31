/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.AllowCopilotExperimental;
import com.github.copilot.CopilotClient;
import com.github.copilot.CopilotSession;
import com.github.copilot.E2ETestContext;
import com.github.copilot.ffi.InProcessEnvGuard;
import com.github.copilot.generated.AssistantMessageEvent;
import com.github.copilot.generated.SessionIdleEvent;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.PingResponse;
import com.github.copilot.rpc.RuntimeConnection;
import com.github.copilot.rpc.SessionConfig;

/**
 * Failsafe integration test for the in-process (FFI) transport.
 *
 * <p>
 * Loads the real {@code runtime.node} native library into this test process via
 * {@link com.github.copilot.ffi.FfiRuntimeHost}. Coverage includes both a
 * purely local {@code ping} round-trip and a replay-backed session turn that
 * exercises authentication, tool initialization, model traffic, and
 * asynchronous session event delivery over the FFI transport.
 *
 * <p>
 * {@link InProcessEnvGuard} demonstrates how the harness redirects the native
 * runtime's HTTP traffic to the replay proxy (via {@code COPILOT_API_URL}) for
 * tests that need session/message round trips over the in-process transport:
 * the native library reads environment variables from the live OS process
 * environment block, not from the JVM's {@code System.getenv()} snapshot, so
 * only a JNA-backed native call can make it visible to code already loaded
 * in-process.
 *
 * <p>
 * Run with {@code mvn verify -Pinprocess} from the {@code java} reactor root,
 * which builds the {@code copilot-sdk-java-runtime} artifact and sets
 * {@code COPILOT_CLI_PATH} to the pinned CLI whose sibling {@code runtime.node}
 * this test loads, and forces {@code forkCount=1} because the FFI host and env
 * guard mutate process-global state.
 *
 * <p>
 * {@link RequireInProcess} disables this test unless the {@code -Pinprocess}
 * profile is active: without it, the {@code copilot-sdk-java-runtime}
 * classifier JAR providing {@code runtime.node} is not on the classpath, so the
 * test would fail with a {@code FileNotFoundException} rather than being
 * skipped.
 */
@AllowCopilotExperimental
@RequireInProcess
class InProcessTransportIT {

    private static E2ETestContext ctx;

    @BeforeAll
    static void setup() throws Exception {
        ctx = E2ETestContext.create();
    }

    @AfterAll
    static void teardown() throws Exception {
        if (ctx != null) {
            ctx.close();
        }
    }

    @Test
    void shouldStartPingAndStopOverInProcessFfi() throws Exception {
        // Route the native runtime's HTTP traffic (should it make any) at the
        // replay proxy, mirroring how a session-level in-process test would
        // redirect COPILOT_API_URL. `ping` never reaches the network, but this
        // demonstrates the guard's intended usage for future in-process tests.
        // COPILOT_CLI_PATH is intentionally NOT set here: NativeRuntimeLoader and
        // CopilotClient.resolveInProcessEntrypoint() read it via
        // System.getenv(), which is a JVM-startup-time snapshot that native
        // setenv() calls made after the JVM starts cannot update — it must be
        // set before the JVM starts (see the -Pinprocess Maven profile).
        try (InProcessEnvGuard envGuard = new InProcessEnvGuard(Map.of("COPILOT_API_URL", ctx.getProxyUrl()))) {
            CopilotClientOptions options = new CopilotClientOptions().setConnection(RuntimeConnection.forInProcess());
            try (CopilotClient client = new CopilotClient(options)) {
                client.start().get();

                PingResponse pong = client.ping("ffi message").get();
                assertEquals("pong: ffi message", pong.message());
                assertNotNull(pong.timestamp());

                client.stop().get();
            }
        }
    }

    @Test
    void shouldCreateSessionAndCompleteTurnOverInProcessFfi() throws Exception {
        ctx.configureForTest("session", "should_receive_session_events");

        try (CopilotClient client = ctx.createClient();
                CopilotSession session = client
                        .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                        .get(30, TimeUnit.SECONDS)) {
            var idleReceived = new CompletableFuture<SessionIdleEvent>();
            session.on(SessionIdleEvent.class, idleReceived::complete);

            AssistantMessageEvent response = session.sendAndWait(new MessageOptions().setPrompt("What is 100+200?"))
                    .get(60, TimeUnit.SECONDS);

            assertNotNull(response);
            assertEquals("100 + 200 = 300", response.getData().content());
            assertNotNull(idleReceived.get(5, TimeUnit.SECONDS));
        }
    }
}
