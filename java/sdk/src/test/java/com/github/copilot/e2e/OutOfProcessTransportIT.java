/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.github.copilot.AllowCopilotExperimental;
import com.github.copilot.CopilotClient;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.PingResponse;
import com.github.copilot.rpc.RuntimeConnection;

/**
 * Failsafe smoke test for the managed out-of-process runtime wrapper.
 */
@AllowCopilotExperimental
@RequireInProcess
class OutOfProcessTransportIT {

    @Test
    void shouldStartPingAndStopOverStdio() throws Exception {
        CopilotClientOptions options = new CopilotClientOptions().setConnection(RuntimeConnection.forStdio());
        try (CopilotClient client = new CopilotClient(options)) {
            client.start().get();

            PingResponse pong = client.ping("wrapper message").get();
            assertEquals("pong: wrapper message", pong.message());
            assertNotNull(pong.timestamp());

            client.stop().get();
        }
    }
}
