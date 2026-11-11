/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.rpc.McpIdeConfig;
import com.github.copilot.generated.rpc.McpServerConfigLocal;
import com.github.copilot.generated.rpc.McpServerConfigLocalType;
import com.github.copilot.generated.rpc.RpcCaller;
import com.github.copilot.generated.rpc.SessionMcpEnableParams;
import com.github.copilot.generated.rpc.SessionMcpEnableRequest;
import com.github.copilot.generated.rpc.SessionMcpOauthLoginParams;
import com.github.copilot.generated.rpc.SessionMcpOauthLoginRequest;
import com.github.copilot.generated.rpc.SessionMcpStartServerParams;
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
    void startServerAcceptsTypedLocalAndOrdinaryConfigWithoutChangingCallerData() {
        var caller = new RecordingCaller();
        var session = new SessionRpc(caller, "local-session");
        var ide = McpIdeConfig.builder().ideName("VS Code").workspaceFolder("/workspace").pid(42L)
                .lockFilePath("/runtime/ide.lock").clientVersion("1.0").clientPid(43L).parentPid(44L).build();
        var headers = Map.of("Authorization", "Bearer local");
        var config = McpServerConfigLocal.builder().type(McpServerConfigLocalType.LOCAL_HTTP)
                .socketPath("/runtime/ide.sock").headers(headers).ide(ide).build();
        var ordinary = Map.of("command", "node", "custom", Map.of("enabled", true));

        session.mcp.startServer(SessionMcpStartServerParams.builder().serverName("ide").config(config).build()).join();
        session.mcp.startServer(SessionMcpStartServerParams.builder().serverName("manual").config(ordinary).build())
                .join();

        var call = caller.calls.get(0);
        assertEquals("session.mcp.startServer", call.method());
        assertEquals("local-session", call.params().path("sessionId").asText());
        var payload = call.params().path("config");
        assertEquals("local-http", payload.path("type").asText());
        assertEquals("/runtime/ide.sock", payload.path("socketPath").asText());
        assertEquals("Bearer local", payload.path("headers").path("Authorization").asText());
        assertEquals("VS Code", payload.path("ide").path("ideName").asText());
        assertEquals(42, payload.path("ide").path("pid").asLong());
        assertEquals(MAPPER.valueToTree(ordinary), caller.calls.get(1).params().path("config"));
        assertEquals("/runtime/ide.sock", config.socketPath());
        assertSame(headers, config.headers());
        assertSame(ide, config.ide());
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
