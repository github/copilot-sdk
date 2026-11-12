/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.rpc.RpcCaller;
import com.github.copilot.generated.rpc.SessionMcpEnableParams;
import com.github.copilot.generated.rpc.SessionMcpEnableRequest;
import com.github.copilot.generated.rpc.SessionMcpOauthLoginParams;
import com.github.copilot.generated.rpc.SessionMcpOauthLoginRequest;
import com.github.copilot.generated.rpc.SessionRpc;

class OwnedMcpLegacyRequestCompatibilityTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final class RecordingCaller implements RpcCaller {
        record Call(String method, JsonNode params) {
        }

        final List<Call> calls = new ArrayList<>();

        @Override
        public <T> CompletableFuture<T> invoke(String method, Object params, Class<T> resultType) {
            calls.add(new Call(method, MAPPER.valueToTree(params)));
            return CompletableFuture.completedFuture(null);
        }
    }

    @Test
    void existingRecordsKeepTheirReleasedComponents() {
        assertEquals(List.of("sessionId", "serverName"),
                Arrays.stream(SessionMcpEnableParams.class.getRecordComponents()).map(c -> c.getName()).toList());
        assertEquals(
                List.of("sessionId", "serverName", "forceReauth", "clientName", "callbackSuccessMessage", "clientId",
                        "clientSecret", "publicClient", "grantType"),
                Arrays.stream(SessionMcpOauthLoginParams.class.getRecordComponents()).map(c -> c.getName()).toList());
    }

    @Test
    void existingRecordCallsOmitOwnedIdentity() {
        var caller = new RecordingCaller();
        var session = new SessionRpc(caller, "owned-session");

        session.mcp.enable(new SessionMcpEnableParams(null, "manual"));
        session.mcp.oauth
                .login(new SessionMcpOauthLoginParams(null, "manual", null, null, null, null, null, null, null));

        assertEquals("session.mcp.enable", caller.calls.get(0).method());
        assertFalse(caller.calls.get(0).params().has("expectedInstallationId"));
        assertEquals("session.mcp.oauth.login", caller.calls.get(1).method());
        assertFalse(caller.calls.get(1).params().has("expectedInstallationId"));
        assertFalse(caller.calls.get(1).params().has("loginId"));
    }

    @Test
    void ownedIdentityUsesTheSameWireMethodWithFlatParams() {
        var caller = new RecordingCaller();
        var session = new SessionRpc(caller, "owned-session");
        var installation = "a".repeat(32);

        session.mcp.enable(new SessionMcpEnableRequest("owned").setExpectedInstallationId(installation));
        session.mcp.oauth.login(new SessionMcpOauthLoginRequest("owned").setExpectedInstallationId(installation)
                .setLoginId("b".repeat(32)));

        var enable = caller.calls.get(0);
        assertEquals("session.mcp.enable", enable.method());
        assertEquals("owned-session", enable.params().path("sessionId").asText());
        assertEquals("owned", enable.params().path("serverName").asText());
        assertEquals(installation, enable.params().path("expectedInstallationId").asText());
        var login = caller.calls.get(1);
        assertEquals("session.mcp.oauth.login", login.method());
        assertEquals("owned-session", login.params().path("sessionId").asText());
        assertEquals(installation, login.params().path("expectedInstallationId").asText());
        assertEquals("b".repeat(32), login.params().path("loginId").asText());
    }
}
