/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.rpc.AuthWriteLogout;
import com.github.copilot.generated.rpc.CatalogClientContract;
import com.github.copilot.generated.rpc.CatalogSearchParams;
import com.github.copilot.generated.rpc.CatalogSearchRequest;
import com.github.copilot.generated.rpc.McpPlanInstallParams;
import com.github.copilot.generated.rpc.McpPlanInstallRequest;
import com.github.copilot.generated.rpc.McpPlanScope;
import com.github.copilot.generated.rpc.RpcCaller;
import com.github.copilot.generated.rpc.ServerRpc;
import com.github.copilot.generated.rpc.SessionAccountsSetParams;
import com.github.copilot.generated.rpc.FeatureFlagServiceSnapshot;
import com.github.copilot.generated.rpc.SessionFeatureFlagsReadParams;
import com.github.copilot.generated.rpc.SessionFeatureFlagsReadResult;
import com.github.copilot.generated.rpc.SessionOpenOptions;
import com.github.copilot.generated.rpc.SessionOptionsUpdateParams;
import com.github.copilot.generated.rpc.SessionPermissionsSetRequiredParams;
import com.github.copilot.generated.rpc.SessionRpc;

/**
 * Existing callers of requests that gained fields keep their records, while new
 * callers use the extensible request class on the same wire method.
 */
class LegacyRequestCompatibilityTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void featureFlagReadPreservesExistingConstructorsAndDefaultWireMode() throws Exception {
        var params = new SessionFeatureFlagsReadParams("legacy", true, List.of("CONTROLLED"), List.of(), 4L);
        assertEquals(
                MAPPER.valueToTree(Map.of("sessionId", "legacy", "waitForExpResponse", true, "flags",
                        List.of("CONTROLLED"), "overrides", List.of(), "afterRevision", 4L)),
                MAPPER.valueToTree(params));
        assertNotNull(SessionFeatureFlagsReadParams.class.getConstructor(String.class, Boolean.class, List.class,
                List.class, Long.class));

        var snapshot = MAPPER.readValue("""
                {"serviceId":"local","revision":1,"active":true,"expResponseResolved":true,"flags":{"CONTROLLED":false},
                 "expFlags":{},"response":{},"autoApprovalTargeted":false,
                 "reasoningSummariesArm":{"offByDefault":false,"hintEnabled":false},
                 "gptDefaultModelEnabled":false}
                """, FeatureFlagServiceSnapshot.class);
        var result = new SessionFeatureFlagsReadResult(snapshot, Map.of("CONTROLLED", false), List.of(false));
        assertEquals(MAPPER.valueToTree(
                Map.of("snapshot", snapshot, "flags", Map.of("CONTROLLED", false), "overrides", List.of(false))),
                MAPPER.valueToTree(result));
        assertNotNull(SessionFeatureFlagsReadResult.class.getConstructor(FeatureFlagServiceSnapshot.class, Map.class,
                List.class));
    }

    @Test
    void permissionRequiredPreservesThePublishedConstructor() throws Exception {
        var params = new SessionPermissionsSetRequiredParams("legacy", true);
        assertEquals("legacy", params.sessionId());
        assertTrue(params.required());
        assertNull(params.providerId());
        assertEquals(MAPPER.valueToTree(Map.of("sessionId", "legacy", "required", true)), MAPPER.valueToTree(params));
        assertNotNull(SessionPermissionsSetRequiredParams.class.getConstructor(String.class, Boolean.class));
    }

    @Test
    void permissionProviderUsesTheSameMethodAndPreservesBothBooleanValues() {
        var caller = new RecordingCaller();
        var session = new SessionRpc(caller, "permission-session");
        for (boolean required : new boolean[]{false, true}) {
            session.permissions.setRequired(SessionPermissionsSetRequiredParams.builder()
                    .sessionId("permission-session").required(required).providerId("provider").build());
            var call = caller.calls.get(caller.calls.size() - 1);
            assertEquals("session.permissions.setRequired", call.method());
            assertEquals(
                    MAPPER.valueToTree(
                            Map.of("sessionId", "permission-session", "required", required, "providerId", "provider")),
                    call.params());
        }
    }

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
        return CatalogClientContract.builder().protocolVersion(3L).requiredCapabilities(List.of("mcp-install-planning"))
                .build();
    }

    private static Map<String, String> candidate() {
        return Map.of("kind", "candidate", "candidateHandle", "candidate", "searchId", "search");
    }

    @Test
    void sessionOptionsRetainThePreFeatureFlagConstructor() {
        var options = new SessionOpenOptions("session", null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, false, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null);

        assertEquals("session", options.sessionId());
        assertEquals(Boolean.FALSE, options.allowMultipleToolCalls());
        assertNull(options.featureFlagConfiguration());
        assertFalse(MAPPER.valueToTree(options).has("featureFlagConfiguration"));
    }

    @Test
    void sessionOptionsRetainThePreviousCanonicalConstructor() throws ReflectiveOperationException {
        assertPreviousCanonicalConstructor(SessionOpenOptions.class, 85, "expAssignments", "featureFlagConfiguration");
    }

    @Test
    void sessionUpdatesRetainThePreviousCanonicalConstructor() throws ReflectiveOperationException {
        assertPreviousCanonicalConstructor(SessionOptionsUpdateParams.class, 66, "includedBuiltinAgents", "settings");
    }

    private static void assertPreviousCanonicalConstructor(Class<?> type, int previousFieldCount, String nullableField,
            String addedField) throws ReflectiveOperationException {
        var omissionConstructor = Arrays.stream(type.getConstructors())
                .filter(constructor -> constructor.getParameterCount() == previousFieldCount)
                .filter(constructor -> constructor.getParameterTypes()[previousFieldCount - 1] != Set.class).findFirst()
                .orElseThrow();
        var previousTypes = Arrays.copyOf(omissionConstructor.getParameterTypes(), previousFieldCount + 1);
        previousTypes[previousFieldCount] = Set.class;
        var canonicalConstructor = type.getConstructor(previousTypes);
        var arguments = new Object[previousFieldCount + 1];
        arguments[0] = "session";
        var omitted = MAPPER.valueToTree(omissionConstructor.newInstance(Arrays.copyOf(arguments, previousFieldCount)));
        assertFalse(omitted.has(nullableField));
        assertFalse(omitted.has(addedField));

        for (Set<String> explicitNulls : Arrays.<Set<String>>asList(new HashSet<>(Set.of(nullableField)), null)) {
            arguments[previousFieldCount] = explicitNulls;
            var value = canonicalConstructor.newInstance(arguments);
            assertEquals(explicitNulls == null ? Set.of() : Set.of(nullableField),
                    type.getMethod("$explicitNulls").invoke(value));
            var wire = MAPPER.valueToTree(value);
            assertFalse(wire.has(addedField));
            if (explicitNulls == null) {
                assertFalse(wire.has(nullableField));
            } else {
                assertEquals(Set.of(nullableField), explicitNulls);
                assertTrue(wire.has(nullableField));
                assertTrue(wire.get(nullableField).isNull());
            }
        }
    }

    @Test
    void legacyAccountCommandsConvertJsonAndPreserveTypedValues() throws ReflectiveOperationException {
        var constructor = SessionAccountsSetParams.class.getConstructor(String.class, Object.class);
        var command = Map.of("kind", "logout", "accountId", "account");
        for (Object legacy : List.<Object>of(command, MAPPER.valueToTree(command))) {
            var params = constructor.newInstance("session", legacy);

            assertEquals("account", assertInstanceOf(AuthWriteLogout.class, params.command()).getAccountId());
            assertEquals(MAPPER.valueToTree(Map.of("sessionId", "session", "command", command)),
                    MAPPER.valueToTree(params));
        }

        var typed = AuthWriteLogout.builder().accountId("account").build();
        assertSame(typed, constructor.newInstance("session", typed).command());
        assertNull(constructor.newInstance("session", (Object) null).command());
        var failure = assertThrows(InvocationTargetException.class,
                () -> constructor.newInstance("session", Map.of("kind", "unknown")));
        assertInstanceOf(IllegalArgumentException.class, failure.getCause());
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
