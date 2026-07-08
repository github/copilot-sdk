/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.generated.rpc.SessionMcpHeadersHandlePendingHeadersRefreshRequestParams;
import com.github.copilot.generated.rpc.SessionUiHandlePendingSessionLimitsExhaustedParams;
import com.github.copilot.generated.rpc.UISessionLimitsExhaustedResponse;
import com.github.copilot.generated.rpc.UISessionLimitsExhaustedResponseAction;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;

class RpcTasksAndHandlersE2ETest {

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
    void testShouldReturnExpectedResultsForMissingPendingHandlerRequestIds() throws Exception {
        ctx.initializeProxy();

        try (var client = ctx.createClient()) {
            try (var session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get()) {
                var sessionLimits = session.getRpc().ui
                        .handlePendingSessionLimitsExhausted(
                                new SessionUiHandlePendingSessionLimitsExhaustedParams(null,
                                        "missing-session-limits-request",
                                        new UISessionLimitsExhaustedResponse(
                                                UISessionLimitsExhaustedResponseAction.UNSET, null, null)))
                        .get(30, TimeUnit.SECONDS);
                assertFalse(sessionLimits.success());

                var headersRefresh = session.getRpc().mcp.headers
                        .handlePendingHeadersRefreshRequest(
                                new SessionMcpHeadersHandlePendingHeadersRefreshRequestParams(null,
                                        "missing-headers-refresh-request",
                                        Map.of("kind", "headers", "headers", Map.of("x-refresh", "missing"))))
                        .get(30, TimeUnit.SECONDS);
                assertFalse(headersRefresh.success());

                var noHeadersRefresh = session.getRpc().mcp.headers
                        .handlePendingHeadersRefreshRequest(
                                new SessionMcpHeadersHandlePendingHeadersRefreshRequestParams(null,
                                        "missing-headers-refresh-none-request", Map.of("kind", "none")))
                        .get(30, TimeUnit.SECONDS);
                assertFalse(noHeadersRefresh.success());
            }
        }
    }
}
