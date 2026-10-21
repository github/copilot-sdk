/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.generated.rpc;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Generated dispatch and serialization supplement TypeScript's real MCP tests.
 */
class McpPromptsTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void promptResultsPreserveOpaqueContentAndOptionalFields() throws Exception {
        Path directory = Path.of("").toAbsolutePath();
        Path fixture = null;
        while (directory != null) {
            Path candidate = directory.resolve("test/harness/mcp-prompt-fixtures.json");
            if (Files.exists(candidate)) {
                fixture = candidate;
                break;
            }
            directory = directory.getParent();
        }
        assertNotNull(fixture, "Cannot find shared MCP prompt fixture");
        JsonNode fixtures = mapper.readTree(fixture.toFile());
        for (String page : List.of("firstPage", "secondPage")) {
            var result = mapper.treeToValue(fixtures.get(page), SessionMcpPromptsListResult.class);
            assertEquals(fixtures.get(page), mapper.valueToTree(result));
        }
        var result = mapper.treeToValue(fixtures.get("richPrompt"), SessionMcpPromptsGetResult.class);
        assertEquals(fixtures.get("richPrompt"), mapper.valueToTree(result));
    }

    @Test
    void promptNamespaceDispatchesBoundSessionAndPreservesOptionalArguments() throws Exception {
        record Call(String method, JsonNode params, Class<?> resultType) {
        }
        List<Call> calls = new ArrayList<>();
        RpcCaller caller = new RpcCaller() {
            @Override
            public <T> CompletableFuture<T> invoke(String method, Object params, Class<T> resultType) {
                calls.add(new Call(method, mapper.valueToTree(params), resultType));
                return CompletableFuture.completedFuture(null);
            }
        };
        var session = new SessionRpc(caller, "bound-session");
        session.mcp.prompts.list(new SessionMcpPromptsListParams("foreign-session", "fixture", null)).join();
        session.mcp.prompts.list(new SessionMcpPromptsListParams("foreign-session", "fixture", "page:2/opaque+cursor="))
                .join();
        session.mcp.prompts.get(new SessionMcpPromptsGetParams("foreign-session", "fixture", "rich", null)).join();
        session.mcp.prompts.get(new SessionMcpPromptsGetParams("foreign-session", "fixture", "rich", Map.of())).join();
        session.mcp.prompts.get(new SessionMcpPromptsGetParams("foreign-session", "fixture", "rich",
                Map.of("topic", "日本語", "style", ""))).join();

        assertEquals(5, calls.size());
        for (int i = 0; i < calls.size(); i++) {
            Call call = calls.get(i);
            assertEquals("bound-session", call.params().get("sessionId").asText());
            assertEquals("fixture", call.params().get("serverName").asText());
            assertEquals(i < 2 ? "session.mcp.prompts.list" : "session.mcp.prompts.get", call.method());
            assertEquals(i < 2 ? SessionMcpPromptsListResult.class : SessionMcpPromptsGetResult.class,
                    call.resultType());
        }
        assertFalse(calls.get(0).params().has("cursor"));
        assertEquals("page:2/opaque+cursor=", calls.get(1).params().get("cursor").asText());
        assertEquals("rich", calls.get(2).params().get("promptName").asText());
        assertFalse(calls.get(2).params().has("arguments"));
        assertEquals(mapper.readTree("{}"), calls.get(3).params().get("arguments"));
        assertEquals(mapper.readTree("{\"topic\":\"日本語\",\"style\":\"\"}"), calls.get(4).params().get("arguments"));
    }
}
