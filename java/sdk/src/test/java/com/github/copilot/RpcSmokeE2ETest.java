/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.generated.rpc.PingParams;

class RpcSmokeE2ETest {

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
    void testShouldCallRpcPingWithTypedParamsAndResult() throws Exception {
        ctx.configureForTest("rpc_server", "should_call_rpc_ping_with_typed_params_and_result");

        try (var client = ctx.createClient()) {
            client.start().get(30, TimeUnit.SECONDS);

            var result = client.getRpc().ping(new PingParams("typed rpc test")).get(30, TimeUnit.SECONDS);

            assertEquals("pong: typed rpc test", result.message());
            assertNotNull(result.timestamp());
            assertNotNull(result.protocolVersion());
            assertTrue(result.protocolVersion() >= 0);
        }
    }
}
