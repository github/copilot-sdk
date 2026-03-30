/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.events.AssistantMessageEvent;
import com.github.copilot.sdk.json.MessageOptions;

/**
 * Verifies the documented contract that {@code timeoutMs <= 0} means "no
 * timeout" in {@link CopilotSession#sendAndWait(MessageOptions, long)}.
 */
public class ZeroTimeoutContractTest {

    @SuppressWarnings("unchecked")
    @Test
    void sendAndWaitWithZeroTimeoutShouldNotTimeOut() throws Exception {
        // Build a session via reflection (package-private constructor)
        var ctor = CopilotSession.class.getDeclaredConstructor(String.class, JsonRpcClient.class, String.class);
        ctor.setAccessible(true);

        var mockRpc = mock(JsonRpcClient.class);
        when(mockRpc.invoke(any(), any(), any())).thenAnswer(invocation -> {
            Object method = invocation.getArgument(0);
            if ("session.destroy".equals(method)) {
                // Make session.close() non-blocking by completing destroy immediately
                return CompletableFuture.completedFuture(null);
            }
            // For other calls (e.g., message send), return an incomplete future so the
            // sendAndWait result does not complete due to a mock response.
            return new CompletableFuture<>();
        });

        try (var session = ctor.newInstance("zero-timeout-test", mockRpc, null)) {

            // Per the Javadoc: timeoutMs of 0 means "no timeout".
            // The future should NOT complete with TimeoutException.
            CompletableFuture<AssistantMessageEvent> result = session
                    .sendAndWait(new MessageOptions().setPrompt("test"), 0);

            // Give the scheduler a chance to fire if it was (incorrectly) scheduled
            Thread.sleep(200);

            // The future should still be pending — not timed out
            assertFalse(result.isDone(), "Future should not be done; timeoutMs=0 means no timeout per Javadoc");
        }
    }
}
