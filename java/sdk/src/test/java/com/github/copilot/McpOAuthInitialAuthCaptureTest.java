/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.github.copilot.generated.McpOauthRequestReason;
import com.github.copilot.rpc.McpAuthInvocation;
import com.github.copilot.rpc.McpAuthRequest;
import com.github.copilot.rpc.McpAuthResult;
import com.github.copilot.rpc.McpAuthToken;

class McpOAuthInitialAuthCaptureTest {
    @Test
    void initialObservationSurvivesLaterRefresh() {
        var handler = new McpOAuthE2ETest.InitialMcpAuthHandler();
        var initialRequest = request("initial-request", McpOauthRequestReason.INITIAL);
        var initialInvocation = new McpAuthInvocation().setSessionId("initial-session");
        var refreshRequest = request("refresh-request", McpOauthRequestReason.REFRESH);
        var refreshInvocation = new McpAuthInvocation().setSessionId("refresh-session");
        var expectedToken = McpAuthResult.token(new McpAuthToken("sdk-host-token", "Bearer", 3600L));

        var initialResponse = handler.handleWithRuntimeRefresh(initialRequest, initialInvocation, false).join();
        var refreshResponse = handler.handleWithRuntimeRefresh(refreshRequest, refreshInvocation, true).join();

        var observed = handler.initialAuth.get();
        assertNotNull(observed);
        assertFalse(observed.runtimeRefreshSupported(), "Initial authentication cannot advertise refresh");
        assertSame(initialRequest, observed.request());
        assertSame(initialInvocation, observed.invocation());
        assertEquals("initial-session", observed.invocation().getSessionId());
        assertSame(refreshRequest, handler.observedRequest.get(), "Diagnostics should retain the latest request");
        assertEquals(expectedToken, initialResponse.legacyResult());
        assertEquals(expectedToken, refreshResponse.legacyResult());
        assertFalse(initialResponse.isRuntimeRefresh());
        assertFalse(refreshResponse.isRuntimeRefresh());
    }

    @Test
    void refreshCannotStandInForInitialAuthentication() {
        var handler = new McpOAuthE2ETest.InitialMcpAuthHandler();
        var invocation = new McpAuthInvocation().setSessionId("session");
        handler.handleWithRuntimeRefresh(request("refresh-request", McpOauthRequestReason.REFRESH), invocation, true)
                .join();
        assertNull(handler.initialAuth.get(), "A refresh is not an INITIAL observation");

        var initialRequest = request("initial-request", McpOauthRequestReason.INITIAL);
        handler.handleWithRuntimeRefresh(initialRequest, invocation, false).join();
        assertSame(initialRequest, handler.initialAuth.get().request());
        assertFalse(handler.initialAuth.get().runtimeRefreshSupported());
    }

    @Test
    void invalidInitialCapabilityStillFailsTheInitialAssertion() {
        var handler = new McpOAuthE2ETest.InitialMcpAuthHandler();
        var initialRequest = request("initial-request", McpOauthRequestReason.INITIAL);
        var invocation = new McpAuthInvocation().setSessionId("session");
        handler.handleWithRuntimeRefresh(initialRequest, invocation, true).join();
        handler.handleWithRuntimeRefresh(request("refresh-request", McpOauthRequestReason.REFRESH), invocation, false)
                .join();

        var observed = handler.initialAuth.get();
        assertSame(initialRequest, observed.request());
        assertTrue(observed.runtimeRefreshSupported(), "The recorder must not normalize an invalid INITIAL capability");
        var failure = assertThrows(AssertionError.class, () -> assertFalse(observed.runtimeRefreshSupported(),
                "Initial authentication cannot advertise refresh"));
        assertTrue(failure.getMessage().contains("Initial authentication cannot advertise refresh"));
    }

    private static McpAuthRequest request(String requestId, McpOauthRequestReason reason) {
        return new McpAuthRequest(requestId, "oauth-protected-mcp", "https://example.com/mcp", reason, null, null,
                null);
    }
}
