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
import com.github.copilot.generated.rpc.CatalogClientContract;
import com.github.copilot.generated.rpc.CatalogSearchParams;
import com.github.copilot.generated.rpc.CatalogSearchRequest;
import com.github.copilot.generated.rpc.McpPlanInstallParams;
import com.github.copilot.generated.rpc.McpPlanInstallRequest;
import com.github.copilot.generated.rpc.McpPlanScope;
import com.github.copilot.generated.rpc.RpcCaller;
import com.github.copilot.generated.rpc.ServerRpc;

/**
 * Existing callers of requests that gained fields keep their records, while new
 * callers use the extensible request class on the same wire method.
 */
class LegacyRequestCompatibilityTest {

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

    private static CatalogClientContract contract() {
        return new CatalogClientContract(3L, List.of("mcp-install-planning"));
    }

    private static Map<String, String> candidate() {
        return Map.of("kind", "candidate", "candidateHandle", "candidate", "searchId", "search");
    }

    @Test
    void existingRecordsKeepTheirLegacyComponents() {
        assertEquals(List.of("contract", "source", "scope"),
                Arrays.stream(McpPlanInstallParams.class.getRecordComponents()).map(component -> component.getName())
                        .toList());
        assertEquals(List.of("contract", "query", "limit", "kinds", "page"),
                Arrays.stream(CatalogSearchParams.class.getRecordComponents()).map(component -> component.getName())
                        .toList());
    }

    @Test
    void existingRecordCallsOmitTheAddedFields() {
        var caller = new RecordingCaller();
        var server = new ServerRpc(caller);

        server.mcp.planInstall(new McpPlanInstallParams(contract(), candidate(), McpPlanScope.USER));
        server.catalog.search(new CatalogSearchParams(contract(), "catalogue query", 4L, null, null));

        assertEquals(2, caller.calls.size());
        var plan = caller.calls.get(0);
        assertEquals("mcp.planInstall", plan.method());
        assertEquals("user", plan.params().path("scope").asText());
        assertFalse(plan.params().has("policySessionId"));
        var search = caller.calls.get(1);
        assertEquals("catalog.search", search.method());
        assertEquals(4, search.params().path("limit").asInt());
        assertFalse(search.params().has("policySessionId"));
    }

    @Test
    void extensibleRequestsUseTheSameWireMethodWithFlatParams() {
        var caller = new RecordingCaller();
        var server = new ServerRpc(caller);

        server.mcp.planInstall(
                new McpPlanInstallRequest(contract(), candidate()).setScope(McpPlanScope.USER).setPolicySessionId("s"));
        server.catalog.search(new CatalogSearchRequest(contract(), "catalogue query").setPolicySessionId("s"));

        var plan = caller.calls.get(0);
        assertEquals("mcp.planInstall", plan.method());
        assertEquals(MAPPER.valueToTree(Map.of("contract",
                Map.of("protocolVersion", 3L, "requiredCapabilities", List.of("mcp-install-planning")), "source",
                candidate(), "scope", "user", "policySessionId", "s")), plan.params());
        var search = caller.calls.get(1);
        assertEquals("catalog.search", search.method());
        assertEquals("catalogue query", search.params().path("query").asText());
        assertEquals("s", search.params().path("policySessionId").asText());
        assertFalse(search.params().has("limit"));
    }

    @Test
    void extensibleRequestsRequireTheirMandatoryInputs() {
        var server = new ServerRpc(new RecordingCaller());

        assertThrows(NullPointerException.class, () -> new McpPlanInstallRequest(null, candidate()));
        assertThrows(NullPointerException.class, () -> new McpPlanInstallRequest(contract(), null));
        assertThrows(NullPointerException.class, () -> new CatalogSearchRequest(contract(), null));
        assertThrows(NullPointerException.class, () -> server.mcp.planInstall((McpPlanInstallRequest) null));
    }
}
