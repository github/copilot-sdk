/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.generated.rpc;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.copilot.AllowCopilotExperimental;

/**
 * Coverage tests for generated RPC API classes that are not exercised in
 * {@link RpcWrappersTest}. Uses the same {@link StubCaller} pattern to verify
 * that each API method dispatches the correct RPC method name and passes
 * parameters correctly.
 */
class GeneratedRpcApiCoverageTest {

    @Test
    @AllowCopilotExperimental
    void managedSettings_get_and_recheck_share_result_type() throws Exception {
        var stub = new StubCaller();
        var snapshot = new ObjectMapper().readValue("{}", SessionManagedSettingsGetResult.class);
        stub.nextResult = snapshot;
        var session = new SessionRpc(stub, "policy-session");
        CompletableFuture<SessionManagedSettingsGetResult> current = session.managedSettings.get();
        CompletableFuture<SessionManagedSettingsGetResult> refreshed = session.managedSettings.recheckPolicyHelper();
        assertSame(snapshot, current.join());
        assertSame(snapshot, refreshed.join());
        assertEquals(List.of("session.managedSettings.get", "session.managedSettings.recheckPolicyHelper"),
                stub.calls.stream().map(StubCaller.Call::method).toList());
        for (var call : stub.calls) {
            assertEquals(SessionManagedSettingsGetResult.class, call.resultType());
            assertEquals(Map.of("sessionId", "policy-session"), call.params());
        }
    }

    /** A simple stub {@link RpcCaller} that records every call made to it. */
    private static final class StubCaller implements RpcCaller {

        record Call(String method, Object params, Class<?> resultType) {
        }

        final List<Call> calls = new ArrayList<>();
        Object nextResult = null;

        @Override
        @SuppressWarnings("unchecked")
        public <T> CompletableFuture<T> invoke(String method, Object params, Class<T> resultType) {
            calls.add(new Call(method, params, resultType));
            return CompletableFuture.completedFuture((T) nextResult);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("newRpcDispatches")
    void newRpcEndpoints_dispatch_expected_method_params_and_result_type(String expectedMethod,
            Class<?> expectedResultType, String expectedParams,
            BiFunction<ServerRpc, SessionRpc, CompletableFuture<?>> invoke) throws Exception {
        var stub = new StubCaller();
        invoke.apply(new ServerRpc(stub), new SessionRpc(stub, "bound-session")).join();

        assertEquals(1, stub.calls.size());
        var call = stub.calls.get(0);
        assertEquals(expectedMethod, call.method());
        assertEquals(expectedResultType, call.resultType());
        var mapper = new ObjectMapper();
        assertEquals(mapper.readTree(expectedParams), mapper.readTree(mapper.writeValueAsString(call.params())));
    }

    static Stream<Arguments> newRpcDispatches() {
        return Stream.of(dispatch("session.mcp.list", SessionMcpListResult.class, """
                {"sessionId":"bound-session"}
                """, (server, session) -> session.mcp.list()),
                dispatch("session.mcp.listConfigured", SessionMcpListConfiguredResult.class, """
                        {"sessionId":"bound-session"}
                        """, (server, session) -> session.mcp.listConfigured()),
                dispatch("catalog.select", CatalogSelectionResult.class, """
                        {"contract":{"protocolVersion":3,"requiredCapabilities":["catalog-selection"]},
                         "sessionId":"catalog-session","selectionRef":"selection-1","outcome":"selected"}
                        """,
                        (server, session) -> server.catalog.select(CatalogSelectParams.builder()
                                .contract(CatalogClientContract.builder().protocolVersion(3L)
                                        .requiredCapabilities(List.of("catalog-selection")).build())
                                .sessionId("catalog-session").selectionRef("selection-1")
                                .outcome(CatalogSelectionDecision.SELECTED).build())),
                dispatch("session.managedSettings.get", SessionManagedSettingsGetResult.class, """
                        {"sessionId":"bound-session"}
                        """, (server, session) -> session.managedSettings.get()),
                dispatch("session.instructions.reload", SessionInstructionsGetSourcesResult.class, """
                        {"sessionId":"bound-session"}
                        """, (server, session) -> session.instructions.reload()),
                dispatch("session.customizations.reload", CustomizationsReloadResult.class, """
                        {"sessionId":"bound-session"}
                        """, (server, session) -> session.customizations.reload()),
                dispatch("session.plugins.install", SessionPluginsInstallResult.class, """
                        {"sessionId":"bound-session","source":"plugin@market"}
                        """,
                        (server, session) -> session.plugins.install(SessionPluginsInstallParams.builder()
                                .sessionId("foreign-session").source("plugin@market").build())),
                dispatch("session.plugins.uninstall", Void.class, """
                        {"sessionId":"bound-session","name":"plugin","directSourceId":"source-1"}
                        """,
                        (server, session) -> session.plugins.uninstall(SessionPluginsUninstallParams.builder()
                                .sessionId("foreign-session").name("plugin").directSourceId("source-1").build())),
                dispatch("session.plugins.update", SessionPluginsUpdateResult.class, """
                        {"sessionId":"bound-session","name":"plugin@market"}
                        """,
                        (server, session) -> session.plugins.update(SessionPluginsUpdateParams.builder()
                                .sessionId("foreign-session").name("plugin@market").build())),
                dispatch("session.plugins.enable", Void.class, """
                        {"sessionId":"bound-session","names":["first@market","second@market"]}
                        """,
                        (server, session) -> session.plugins.enable(SessionPluginsEnableParams.builder()
                                .sessionId("foreign-session").names(List.of("first@market", "second@market")).build())),
                dispatch("session.plugins.disable", Void.class, """
                        {"sessionId":"bound-session","names":["plugin@market"]}
                        """,
                        (server, session) -> session.plugins.disable(SessionPluginsDisableParams.builder()
                                .sessionId("foreign-session").names(List.of("plugin@market")).build())),
                dispatch("session.plugins.marketplaces.list", SessionPluginsMarketplacesListResult.class, """
                        {"sessionId":"bound-session"}
                        """, (server, session) -> session.plugins.marketplaces.list()),
                dispatch("session.plugins.marketplaces.add", SessionPluginsMarketplacesAddResult.class, """
                        {"sessionId":"bound-session","source":"./market","workingDirectory":"/workspace"}
                        """,
                        (server, session) -> session.plugins.marketplaces
                                .add(SessionPluginsMarketplacesAddParams.builder().sessionId("foreign-session")
                                        .source("./market").workingDirectory("/workspace").build())),
                dispatch("session.plugins.marketplaces.remove", SessionPluginsMarketplacesRemoveResult.class, """
                        {"sessionId":"bound-session","name":"market","force":true}
                        """,
                        (server, session) -> session.plugins.marketplaces.remove(SessionPluginsMarketplacesRemoveParams
                                .builder().sessionId("foreign-session").name("market").force(true).build())),
                dispatch("session.plugins.marketplaces.browse", SessionPluginsMarketplacesBrowseResult.class, """
                        {"sessionId":"bound-session","name":"market"}
                        """,
                        (server, session) -> session.plugins.marketplaces.browse(SessionPluginsMarketplacesBrowseParams
                                .builder().sessionId("foreign-session").name("market").build())),
                dispatch("session.plugins.marketplaces.refresh", SessionPluginsMarketplacesRefreshResult.class, """
                        {"sessionId":"bound-session","name":"market"}
                        """, (server,
                        session) -> session.plugins.marketplaces.refresh(SessionPluginsMarketplacesRefreshParams
                                .builder().sessionId("foreign-session").name("market").build())),
                dispatch("session.plugins.marketplaces.refresh", SessionPluginsMarketplacesRefreshResult.class, """
                        {"sessionId":"bound-session"}
                        """, (server, session) -> session.plugins.marketplaces.refresh(null)),
                dispatch("session.queue.withdrawMessage", SessionQueueWithdrawMessageResult.class, """
                        {"sessionId":"bound-session","messageId":"message-1","expectedPrompt":"original"}
                        """,
                        (server, session) -> session.queue.withdrawMessage(
                                SessionQueueWithdrawMessageParams.builder().sessionId("foreign-session")
                                        .messageId("message-1").expectedPrompt("original").build())),
                dispatch("session.queue.appendSteering", SessionQueueAppendSteeringResult.class, """
                        {"sessionId":"bound-session","messageId":"message-1","expectedPrompt":"original",
                         "agentMode":"interactive","prompt":"more","displayPrompt":"preview","attachments":[]}
                        """,
                        (server, session) -> session.queue.appendSteering(SessionQueueAppendSteeringParams.builder()
                                .sessionId("foreign-session").messageId("message-1").expectedPrompt("original")
                                .agentMode(SendAgentMode.INTERACTIVE).prompt("more").displayPrompt("preview")
                                .attachments(List.of()).build())),
                dispatch("session.workflow.run", SessionWorkflowRunResult.class, """
                        {"sessionId":"bound-session","name":"workflow-1","args":{"topic":"input"}}
                        """, (server,
                        session) -> session.workflow.run(SessionWorkflowRunParams.builder().sessionId("foreign-session")
                                .name("workflow-1").args(Map.of("topic", "input")).options(null).build())),
                dispatch("session.workflow.resume", SessionWorkflowResumeResult.class, """
                        {"sessionId":"bound-session","runId":"run-1","notifyOnComplete":true,"logPhaseNames":false}
                        """,
                        (server, session) -> session.workflow.resume(
                                SessionWorkflowResumeParams.builder().sessionId("foreign-session").runId("run-1")
                                        .limits(null).notifyOnComplete(true).logPhaseNames(false).build())),
                dispatch("session.workflow.runFromTool", WorkflowRunResult.class, """
                        {"sessionId":"bound-session","name":"workflow-1","args":{"topic":"input"},"toolCallId":"tool-1"}
                        """,
                        (server, session) -> session.workflow.runFromTool(SessionWorkflowRunFromToolParams.builder()
                                .sessionId("foreign-session").name("workflow-1").args(Map.of("topic", "input"))
                                .options(null).toolCallId("tool-1").build())),
                dispatch("session.workflow.resumeFromTool", WorkflowResumeResult.class, """
                        {"sessionId":"bound-session","runId":"run-1","toolCallId":"tool-1"}
                        """,
                        (server, session) -> session.workflow.resumeFromTool(
                                SessionWorkflowResumeFromToolParams.builder().sessionId("foreign-session")
                                        .runId("run-1").limits(null).toolCallId("tool-1").build())),
                dispatch("session.workflow.getRun", SessionWorkflowGetRunResult.class, """
                        {"sessionId":"bound-session","runId":"run-1"}
                        """,
                        (server, session) -> session.workflow.getRun(SessionWorkflowGetRunParams.builder()
                                .sessionId("foreign-session").runId("run-1").build())),
                dispatch("session.workflow.listRuns", SessionWorkflowListRunsResult.class, """
                        {"sessionId":"bound-session","afterSeq":2,"limit":5}
                        """,
                        (server, session) -> session.workflow.listRuns(SessionWorkflowListRunsParams.builder()
                                .sessionId("foreign-session").afterSeq(2L).beforeSeq(null).limit(5L).build())),
                dispatch("session.workflow.getRunDetail", SessionWorkflowGetRunDetailResult.class, """
                        {"sessionId":"bound-session","runId":"run-1"}
                        """,
                        (server, session) -> session.workflow.getRunDetail(SessionWorkflowGetRunDetailParams.builder()
                                .sessionId("foreign-session").runId("run-1").build())),
                dispatch("session.workflow.getRunProgress", SessionWorkflowGetRunProgressResult.class, """
                        {"sessionId":"bound-session","runId":"run-1","phaseId":"phase-1","afterSeq":2,"limit":5}
                        """,
                        (server, session) -> session.workflow.getRunProgress(SessionWorkflowGetRunProgressParams
                                .builder().sessionId("foreign-session").runId("run-1").phaseId("phase-1").afterSeq(2L)
                                .beforeSeq(null).limit(5L).build())),
                dispatch("session.workflow.cancel", SessionWorkflowCancelResult.class, """
                        {"sessionId":"bound-session","runId":"run-1"}
                        """,
                        (server, session) -> session.workflow.cancel(SessionWorkflowCancelParams.builder()
                                .sessionId("foreign-session").runId("run-1").build())),
                dispatch("session.workflow.pause", SessionWorkflowPauseResult.class, """
                        {"sessionId":"bound-session","runId":"run-1"}
                        """,
                        (server, session) -> session.workflow.pause(SessionWorkflowPauseParams.builder()
                                .sessionId("foreign-session").runId("run-1").build())),
                dispatch("session.workflow.pauseAtCheckpoint", WorkflowPauseCheckpointResult.class,
                        """
                                {"sessionId":"bound-session","runId":"run-1","executionToken":"execution-1","key":"checkpoint-1"}
                                """,
                        (server, session) -> session.workflow.pauseAtCheckpoint(
                                SessionWorkflowPauseAtCheckpointParams.builder().sessionId("foreign-session")
                                        .runId("run-1").executionToken("execution-1").key("checkpoint-1").build())),
                dispatch("session.workflow.log", Void.class, """
                        {"sessionId":"bound-session","runId":"run-1","executionToken":"execution-1",
                         "lines":[{"seq":1,"kind":"log","text":"progress"}]}
                        """,
                        (server, session) -> session.workflow.log(SessionWorkflowLogParams.builder()
                                .sessionId("foreign-session").runId("run-1").executionToken("execution-1")
                                .lines(List.of(WorkflowLogLine.builder().seq(1L).kind(WorkflowLogLineKind.LOG)
                                        .text("progress").build()))
                                .build())),
                dispatch("session.workflow.agent", SessionWorkflowAgentResult.class,
                        """
                                {"sessionId":"bound-session","workflowRunId":"run-1","executionToken":"execution-1","prompt":"inspect","opts":{}}
                                        """,
                        (server, session) -> session.workflow
                                .agent(SessionWorkflowAgentParams.builder().sessionId("foreign-session")
                                        .workflowRunId("run-1").executionToken("execution-1").prompt("inspect")
                                        .opts(com.github.copilot.generated.rpc.WorkflowAgentOptions.builder().build())
                                        .build())),
                dispatch("session.workflow.journal.get", SessionWorkflowJournalGetResult.class, """
                        {"sessionId":"bound-session","runId":"run-1","executionToken":"execution-1","key":"journal-1"}
                        """,
                        (server, session) -> session.workflow.journal
                                .get(SessionWorkflowJournalGetParams.builder().sessionId("foreign-session")
                                        .runId("run-1").executionToken("execution-1").key("journal-1").build())),
                dispatch("session.workflow.journal.put", Void.class, """
                        {"sessionId":"bound-session","runId":"run-1","executionToken":"execution-1","key":"journal-1",
                         "resultJson":{"answer":42}}
                        """,
                        (server, session) -> session.workflow.journal.put(SessionWorkflowJournalPutParams.builder()
                                .sessionId("foreign-session").runId("run-1").executionToken("execution-1")
                                .key("journal-1").resultJson(Map.of("answer", 42)).build())));
    }

    private static Arguments dispatch(String method, Class<?> resultType, String expectedParams,
            BiFunction<ServerRpc, SessionRpc, CompletableFuture<?>> invoke) {
        return Arguments.of(method, resultType, expectedParams, invoke);
    }

    // ── ServerRpc additional methods ───────────────────────────────────────

    @Test
    void serverRpc_tools_list_invokes_correct_method() {
        var stub = new StubCaller();
        var server = new ServerRpc(stub);

        var params = ToolsListParams.builder().model("gpt-5").build();
        server.tools.list(params);

        assertEquals(1, stub.calls.size());
        assertEquals("tools.list", stub.calls.get(0).method());
        assertSame(params, stub.calls.get(0).params());
    }

    @Test
    void serverRpc_sessionFs_setProvider_invokes_correct_method() {
        var stub = new StubCaller();
        var server = new ServerRpc(stub);

        var params = SessionFsSetProviderParams.builder().initialCwd("/workspace").sessionStatePath("/state")
                .conventions(SessionFsSetProviderConventions.POSIX).capabilities(null).build();
        server.sessionFs.setProvider(params);

        assertEquals(1, stub.calls.size());
        assertEquals("sessionFs.setProvider", stub.calls.get(0).method());
        assertSame(params, stub.calls.get(0).params());
    }

    @Test
    void serverRpc_sessions_fork_invokes_correct_method() {
        var stub = new StubCaller();
        var server = new ServerRpc(stub);

        var params = SessionsForkParams.builder().sessionId("parent-session-id").toEventId(null).name(null).build();
        server.sessions.fork(params);

        assertEquals(1, stub.calls.size());
        assertEquals("sessions.fork", stub.calls.get(0).method());
        assertSame(params, stub.calls.get(0).params());
    }

    @Test
    void serverRpc_mcp_config_update_invokes_correct_method() {
        var stub = new StubCaller();
        var server = new ServerRpc(stub);

        var params = McpConfigUpdateParams.builder().name("myServer").config("new-config").build();
        server.mcp.config.update(params);

        assertEquals(1, stub.calls.size());
        assertEquals("mcp.config.update", stub.calls.get(0).method());
        assertSame(params, stub.calls.get(0).params());
    }

    @Test
    void serverRpc_mcp_config_remove_invokes_correct_method() {
        var stub = new StubCaller();
        var server = new ServerRpc(stub);

        var params = McpConfigRemoveParams.builder().name("myServer").authClientIdMetadataUrl(null).build();
        server.mcp.config.remove(params);

        assertEquals(1, stub.calls.size());
        assertEquals("mcp.config.remove", stub.calls.get(0).method());
        assertSame(params, stub.calls.get(0).params());
    }

    // ── SessionRpc.model ───────────────────────────────────────────────────

    @Test
    void sessionRpc_model_setAllowedModels_merges_sessionId_and_returns_result() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-model-policy");
        var allowedModels = List.of("gpt-5", "azure/gpt-5");
        var expectedResult = SessionModelSetAllowedModelsResult.builder().allowedModels(allowedModels)
                .effectiveAllowedModels(List.of("gpt-5")).fallbackModel("gpt-5").modelId("gpt-5").build();
        stub.nextResult = expectedResult;
        var request = SessionModelSetAllowedModelsParams.builder().sessionId("ignored-session")
                .allowedModels(allowedModels).build();

        assertSame(expectedResult, session.model.setAllowedModels(request).join());

        assertEquals(1, stub.calls.size());
        var call = stub.calls.get(0);
        assertEquals("session.model.setAllowedModels", call.method());
        assertEquals(SessionModelSetAllowedModelsResult.class, call.resultType());
        var params = assertInstanceOf(ObjectNode.class, call.params());
        assertEquals("sess-model-policy", params.get("sessionId").asText());
        assertEquals(2, params.get("allowedModels").size());
        assertEquals("gpt-5", params.get("allowedModels").get(0).asText());
        assertEquals("azure/gpt-5", params.get("allowedModels").get(1).asText());
        assertEquals("ignored-session", request.sessionId());
    }

    @Test
    void sessionRpc_model_setAllowedModels_sends_null_to_clear_host_restriction() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-model-policy");

        session.model
                .setAllowedModels(
                        SessionModelSetAllowedModelsParams.builder().sessionId(null).allowedModels(null).build())
                .join();

        assertEquals(1, stub.calls.size());
        assertEquals("session.model.setAllowedModels", stub.calls.get(0).method());
        var params = assertInstanceOf(ObjectNode.class, stub.calls.get(0).params());
        assertEquals("sess-model-policy", params.get("sessionId").asText());
        assertTrue(params.has("allowedModels"));
        assertTrue(params.get("allowedModels").isNull());
    }

    @Test
    void sessionRpc_model_setAllowedModels_omits_untouched_host_restriction() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-model-policy");

        session.model.setAllowedModels(SessionModelSetAllowedModelsParams.builder().build()).join();

        assertEquals(1, stub.calls.size());
        assertEquals("session.model.setAllowedModels", stub.calls.get(0).method());
        var params = assertInstanceOf(ObjectNode.class, stub.calls.get(0).params());
        assertEquals("sess-model-policy", params.get("sessionId").asText());
        assertFalse(params.has("allowedModels"));
    }

    @Test
    void sessionRpc_model_setAllowedModels_preserves_explicit_empty_list() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-model-policy");

        session.model
                .setAllowedModels(
                        SessionModelSetAllowedModelsParams.builder().sessionId(null).allowedModels(List.of()).build())
                .join();

        assertEquals(1, stub.calls.size());
        assertEquals("session.model.setAllowedModels", stub.calls.get(0).method());
        var params = assertInstanceOf(ObjectNode.class, stub.calls.get(0).params());
        assertEquals("sess-model-policy", params.get("sessionId").asText());
        assertTrue(params.get("allowedModels").isArray());
        assertTrue(params.get("allowedModels").isEmpty());
    }

    // ── SessionRpc.mode ────────────────────────────────────────────────────

    @Test
    void sessionRpc_mode_get_injects_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-mode");

        session.mode.get();

        assertEquals(1, stub.calls.size());
        assertEquals("session.mode.get", stub.calls.get(0).method());
        var params = stub.calls.get(0).params();
        assertInstanceOf(Map.class, params);
        assertEquals("sess-mode", ((Map<?, ?>) params).get("sessionId"));
    }

    @Test
    void sessionRpc_mode_set_merges_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-mode-set");

        var modeParams = SessionModeSetParams.builder().sessionId(null).mode(SessionMode.INTERACTIVE).expectedMode(null)
                .inheritPlanBaseFromSessionId(null).planModelConfigured(null).planModel(null).planModelProviderId(null)
                .planReasoningEffort(null).planContextTier(null).compactionDecision(null).restorePlanModel(null)
                .persistPlanSelection(null).pickerSettingsContext(null).planExitAction(null).build();
        session.mode.set(modeParams);

        assertEquals(1, stub.calls.size());
        assertEquals("session.mode.set", stub.calls.get(0).method());
        var params = (com.fasterxml.jackson.databind.node.ObjectNode) stub.calls.get(0).params();
        assertEquals("sess-mode-set", params.get("sessionId").asText());
    }

    // ── SessionRpc.plan ────────────────────────────────────────────────────

    @Test
    void sessionRpc_plan_read_injects_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-plan");

        session.plan.read();

        assertEquals(1, stub.calls.size());
        assertEquals("session.plan.read", stub.calls.get(0).method());
        var params = (Map<?, ?>) stub.calls.get(0).params();
        assertEquals("sess-plan", params.get("sessionId"));
    }

    @Test
    void sessionRpc_plan_update_merges_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-plan-upd");

        var planParams = SessionPlanUpdateParams.builder().sessionId(null).content("# My Plan").build();
        session.plan.update(planParams);

        assertEquals(1, stub.calls.size());
        assertEquals("session.plan.update", stub.calls.get(0).method());
        var params = (com.fasterxml.jackson.databind.node.ObjectNode) stub.calls.get(0).params();
        assertEquals("sess-plan-upd", params.get("sessionId").asText());
    }

    @Test
    void sessionRpc_plan_delete_injects_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-plan-del");

        session.plan.delete();

        assertEquals(1, stub.calls.size());
        assertEquals("session.plan.delete", stub.calls.get(0).method());
        var params = (Map<?, ?>) stub.calls.get(0).params();
        assertEquals("sess-plan-del", params.get("sessionId"));
    }

    // ── SessionRpc.workspace ───────────────────────────────────────────────

    @Test
    void sessionRpc_workspace_listFiles_injects_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-ws");

        session.workspaces.listFiles();

        assertEquals(1, stub.calls.size());
        assertEquals("session.workspaces.listFiles", stub.calls.get(0).method());
        var params = (Map<?, ?>) stub.calls.get(0).params();
        assertEquals("sess-ws", params.get("sessionId"));
    }

    @Test
    void sessionRpc_workspace_readFile_merges_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-ws-rf");

        var rfParams = SessionWorkspacesReadFileParams.builder().sessionId(null).path("/src/Main.java").build();
        session.workspaces.readFile(rfParams);

        assertEquals(1, stub.calls.size());
        assertEquals("session.workspaces.readFile", stub.calls.get(0).method());
        var params = (com.fasterxml.jackson.databind.node.ObjectNode) stub.calls.get(0).params();
        assertEquals("sess-ws-rf", params.get("sessionId").asText());
        assertEquals("/src/Main.java", params.get("path").asText());
    }

    @Test
    void sessionRpc_workspace_createFile_merges_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-ws-cf");

        var cfParams = SessionWorkspacesCreateFileParams.builder().sessionId(null).path("/new/file.txt")
                .content("content").build();
        session.workspaces.createFile(cfParams);

        assertEquals(1, stub.calls.size());
        assertEquals("session.workspaces.createFile", stub.calls.get(0).method());
        var params = (com.fasterxml.jackson.databind.node.ObjectNode) stub.calls.get(0).params();
        assertEquals("sess-ws-cf", params.get("sessionId").asText());
    }

    // ── SessionRpc.fleet ───────────────────────────────────────────────────

    @Test
    void sessionRpc_fleet_start_merges_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-fleet");

        var fleetParams = SessionFleetStartParams.builder().sessionId(null).prompt("fix all bugs").attachments(null)
                .billable(null).wait_(null).build();
        session.fleet.start(fleetParams);

        assertEquals(1, stub.calls.size());
        assertEquals("session.fleet.start", stub.calls.get(0).method());
        var params = (com.fasterxml.jackson.databind.node.ObjectNode) stub.calls.get(0).params();
        assertEquals("sess-fleet", params.get("sessionId").asText());
        assertEquals("fix all bugs", params.get("prompt").asText());
    }

    // ── SessionRpc.skills ──────────────────────────────────────────────────

    @Test
    void sessionRpc_skills_list_injects_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-skills");

        session.skills.list();

        assertEquals(1, stub.calls.size());
        assertEquals("session.skills.list", stub.calls.get(0).method());
        var params = (Map<?, ?>) stub.calls.get(0).params();
        assertEquals("sess-skills", params.get("sessionId"));
    }

    @Test
    void sessionRpc_skills_enable_merges_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-skills-en");

        var enableParams = SessionSkillsEnableParams.builder().sessionId(null).name("my-skill").build();
        session.skills.enable(enableParams);

        assertEquals(1, stub.calls.size());
        assertEquals("session.skills.enable", stub.calls.get(0).method());
        var params = (com.fasterxml.jackson.databind.node.ObjectNode) stub.calls.get(0).params();
        assertEquals("sess-skills-en", params.get("sessionId").asText());
        assertEquals("my-skill", params.get("name").asText());
    }

    @Test
    void sessionRpc_skills_disable_merges_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-skills-dis");

        var disableParams = SessionSkillsDisableParams.builder().sessionId(null).name("my-skill").build();
        session.skills.disable(disableParams);

        assertEquals(1, stub.calls.size());
        assertEquals("session.skills.disable", stub.calls.get(0).method());
        var params = (com.fasterxml.jackson.databind.node.ObjectNode) stub.calls.get(0).params();
        assertEquals("sess-skills-dis", params.get("sessionId").asText());
    }

    @Test
    void sessionRpc_skills_reload_injects_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-skills-rel");

        session.skills.reload();

        assertEquals(1, stub.calls.size());
        assertEquals("session.skills.reload", stub.calls.get(0).method());
        var params = (Map<?, ?>) stub.calls.get(0).params();
        assertEquals("sess-skills-rel", params.get("sessionId"));
    }

    // ── SessionRpc.mcp ─────────────────────────────────────────────────────

    @Test
    void sessionRpc_mcp_list_injects_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-mcp");

        session.mcp.list();

        assertEquals(1, stub.calls.size());
        assertEquals("session.mcp.list", stub.calls.get(0).method());
        var mapper = new ObjectMapper();
        assertEquals(mapper.valueToTree(Map.of("sessionId", "sess-mcp")),
                mapper.valueToTree(stub.calls.get(0).params()));
    }

    @Test
    void sessionRpc_mcp_enable_merges_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-mcp-en");

        var enableParams = SessionMcpEnableParams.builder().sessionId(null).serverName("my-mcp-server").build();
        session.mcp.enable(enableParams);

        assertEquals(1, stub.calls.size());
        assertEquals("session.mcp.enable", stub.calls.get(0).method());
        var params = (com.fasterxml.jackson.databind.node.ObjectNode) stub.calls.get(0).params();
        assertEquals("sess-mcp-en", params.get("sessionId").asText());
        assertEquals("my-mcp-server", params.get("serverName").asText());
    }

    @Test
    void sessionRpc_mcp_disable_merges_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-mcp-dis");

        var disableParams = SessionMcpDisableParams.builder().sessionId(null).serverName("my-mcp-server").build();
        session.mcp.disable(disableParams);

        assertEquals(1, stub.calls.size());
        assertEquals("session.mcp.disable", stub.calls.get(0).method());
        var params = (com.fasterxml.jackson.databind.node.ObjectNode) stub.calls.get(0).params();
        assertEquals("sess-mcp-dis", params.get("sessionId").asText());
        assertEquals("my-mcp-server", params.get("serverName").asText());
    }

    @Test
    void sessionRpc_mcp_reload_injects_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-mcp-rel");

        session.mcp.reload();

        assertEquals(1, stub.calls.size());
        assertEquals("session.mcp.reload", stub.calls.get(0).method());
        var params = (Map<?, ?>) stub.calls.get(0).params();
        assertEquals("sess-mcp-rel", params.get("sessionId"));
    }

    // ── SessionRpc.plugins ─────────────────────────────────────────────────

    @Test
    void sessionRpc_plugins_list_injects_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-plugins");

        session.plugins.list();

        assertEquals(1, stub.calls.size());
        assertEquals("session.plugins.list", stub.calls.get(0).method());
        var params = (Map<?, ?>) stub.calls.get(0).params();
        assertEquals("sess-plugins", params.get("sessionId"));
    }

    // ── SessionRpc.extensions ──────────────────────────────────────────────

    @Test
    void sessionRpc_extensions_list_injects_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-ext");

        session.extensions.list();

        assertEquals(1, stub.calls.size());
        assertEquals("session.extensions.list", stub.calls.get(0).method());
        var params = (Map<?, ?>) stub.calls.get(0).params();
        assertEquals("sess-ext", params.get("sessionId"));
    }

    @Test
    void sessionRpc_extensions_enable_merges_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-ext-en");

        var enableParams = SessionExtensionsEnableParams.builder().sessionId(null).id("github.ext-id").build();
        session.extensions.enable(enableParams);

        assertEquals(1, stub.calls.size());
        assertEquals("session.extensions.enable", stub.calls.get(0).method());
        var params = (com.fasterxml.jackson.databind.node.ObjectNode) stub.calls.get(0).params();
        assertEquals("sess-ext-en", params.get("sessionId").asText());
        assertEquals("github.ext-id", params.get("id").asText());
    }

    @Test
    void sessionRpc_extensions_disable_merges_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-ext-dis");

        var disableParams = SessionExtensionsDisableParams.builder().sessionId(null).id("github.ext-id").build();
        session.extensions.disable(disableParams);

        assertEquals(1, stub.calls.size());
        assertEquals("session.extensions.disable", stub.calls.get(0).method());
        var params = (com.fasterxml.jackson.databind.node.ObjectNode) stub.calls.get(0).params();
        assertEquals("sess-ext-dis", params.get("sessionId").asText());
    }

    @Test
    void sessionRpc_extensions_reload_injects_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-ext-rel");

        session.extensions.reload();

        assertEquals(1, stub.calls.size());
        assertEquals("session.extensions.reload", stub.calls.get(0).method());
        var params = (Map<?, ?>) stub.calls.get(0).params();
        assertEquals("sess-ext-rel", params.get("sessionId"));
    }

    // ── SessionRpc.tools ───────────────────────────────────────────────────

    @Test
    void sessionRpc_tools_handlePendingToolCall_merges_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-tools");

        var toolParams = SessionToolsHandlePendingToolCallParams.builder().sessionId(null).requestId("req-123")
                .result("ok").error(null).build();
        session.tools.handlePendingToolCall(toolParams);

        assertEquals(1, stub.calls.size());
        assertEquals("session.tools.handlePendingToolCall", stub.calls.get(0).method());
        var params = (com.fasterxml.jackson.databind.node.ObjectNode) stub.calls.get(0).params();
        assertEquals("sess-tools", params.get("sessionId").asText());
        assertEquals("req-123", params.get("requestId").asText());
    }

    // ── SessionRpc.commands ────────────────────────────────────────────────

    @Test
    void sessionRpc_commands_handlePendingCommand_merges_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-cmds");

        var cmdParams = SessionCommandsHandlePendingCommandParams.builder().sessionId(null).requestId("req-cmd-456")
                .error(null).build();
        session.commands.handlePendingCommand(cmdParams);

        assertEquals(1, stub.calls.size());
        assertEquals("session.commands.handlePendingCommand", stub.calls.get(0).method());
        var params = (com.fasterxml.jackson.databind.node.ObjectNode) stub.calls.get(0).params();
        assertEquals("sess-cmds", params.get("sessionId").asText());
        assertEquals("req-cmd-456", params.get("requestId").asText());
    }

    // ── SessionRpc.ui ──────────────────────────────────────────────────────

    @Test
    void sessionRpc_ui_elicitation_merges_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-ui");

        var uiParams = SessionUiElicitationParams.builder().sessionId(null).mode(null).message("Please provide info")
                .requestedSchema(com.github.copilot.generated.rpc.UIElicitationSchema.builder().type("object")
                        .properties(Map.of()).required(List.of()).build())
                .meta(null).task(null).build();
        session.ui.elicitation(uiParams);

        assertEquals(1, stub.calls.size());
        assertEquals("session.ui.elicitation", stub.calls.get(0).method());
        var params = (com.fasterxml.jackson.databind.node.ObjectNode) stub.calls.get(0).params();
        assertEquals("sess-ui", params.get("sessionId").asText());
        assertEquals("Please provide info", params.get("message").asText());
        assertEquals("object", params.path("requestedSchema").path("type").asText());
    }

    @Test
    void sessionRpc_ui_handlePendingElicitation_merges_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-ui-elicit");

        var elicitParams = SessionUiHandlePendingElicitationParams.builder().sessionId(null).requestId("req-elicit-789")
                .result(com.github.copilot.generated.rpc.UIElicitationResponse.builder()
                        .action(UIElicitationResponseAction.ACCEPT).content(Map.of()).build())
                .build();
        session.ui.handlePendingElicitation(elicitParams);

        assertEquals(1, stub.calls.size());
        assertEquals("session.ui.handlePendingElicitation", stub.calls.get(0).method());
        var params = (com.fasterxml.jackson.databind.node.ObjectNode) stub.calls.get(0).params();
        assertEquals("sess-ui-elicit", params.get("sessionId").asText());
        assertEquals("req-elicit-789", params.get("requestId").asText());
        assertEquals("accept", params.path("result").path("action").asText());
    }

    // ── SessionRpc.permissions ─────────────────────────────────────────────

    @Test
    void sessionRpc_permissions_handlePendingPermissionRequest_merges_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-perm");

        var permParams = SessionPermissionsHandlePendingPermissionRequestParams.builder().sessionId(null)
                .requestId("req-perm-1").result("allow").decisionContext(null).build();
        session.permissions.handlePendingPermissionRequest(permParams);

        assertEquals(1, stub.calls.size());
        assertEquals("session.permissions.handlePendingPermissionRequest", stub.calls.get(0).method());
        var params = (com.fasterxml.jackson.databind.node.ObjectNode) stub.calls.get(0).params();
        assertEquals("sess-perm", params.get("sessionId").asText());
        assertEquals("req-perm-1", params.get("requestId").asText());
    }

    // ── SessionRpc.shell ───────────────────────────────────────────────────

    @Test
    void sessionRpc_shell_exec_merges_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-shell");

        var shellParams = SessionShellExecParams.builder().sessionId(null).command("ls -la").cwd("/workspace")
                .timeout(null).build();
        session.shell.exec(shellParams);

        assertEquals(1, stub.calls.size());
        assertEquals("session.shell.exec", stub.calls.get(0).method());
        var params = (com.fasterxml.jackson.databind.node.ObjectNode) stub.calls.get(0).params();
        assertEquals("sess-shell", params.get("sessionId").asText());
        assertEquals("ls -la", params.get("command").asText());
    }

    @Test
    void sessionRpc_shell_kill_merges_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-shell-kill");

        var killParams = SessionShellKillParams.builder().sessionId(null).processId("proc-123").signal(null).build();
        session.shell.kill(killParams);

        assertEquals(1, stub.calls.size());
        assertEquals("session.shell.kill", stub.calls.get(0).method());
        var params = (com.fasterxml.jackson.databind.node.ObjectNode) stub.calls.get(0).params();
        assertEquals("sess-shell-kill", params.get("sessionId").asText());
        assertEquals("proc-123", params.get("processId").asText());
    }

    // ── SessionRpc.history ─────────────────────────────────────────────────

    @Test
    void sessionRpc_history_compact_injects_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-hist");

        session.history.compact();

        assertEquals(1, stub.calls.size());
        assertEquals("session.history.compact", stub.calls.get(0).method());
        var params = (com.fasterxml.jackson.databind.node.ObjectNode) stub.calls.get(0).params();
        assertEquals("sess-hist", params.get("sessionId").asText());
    }

    @Test
    void sessionRpc_history_truncate_merges_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-hist-trunc");

        var truncParams = SessionHistoryTruncateParams.builder().sessionId(null).eventId("event-id-abc").build();
        session.history.truncate(truncParams);

        assertEquals(1, stub.calls.size());
        assertEquals("session.history.truncate", stub.calls.get(0).method());
        var params = (com.fasterxml.jackson.databind.node.ObjectNode) stub.calls.get(0).params();
        assertEquals("sess-hist-trunc", params.get("sessionId").asText());
        assertEquals("event-id-abc", params.get("eventId").asText());
    }

    // ── SessionRpc.usage ───────────────────────────────────────────────────

    @Test
    void sessionRpc_usage_getMetrics_injects_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-usage");

        session.usage.getMetrics();

        assertEquals(1, stub.calls.size());
        assertEquals("session.usage.getMetrics", stub.calls.get(0).method());
        var params = (Map<?, ?>) stub.calls.get(0).params();
        assertEquals("sess-usage", params.get("sessionId"));
    }

    // ── SessionRpc.agent additional methods ────────────────────────────────

    @Test
    void sessionRpc_agent_getCurrent_injects_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-agent-gc");

        session.agent.getCurrent();

        assertEquals(1, stub.calls.size());
        assertEquals("session.agent.getCurrent", stub.calls.get(0).method());
        var params = (Map<?, ?>) stub.calls.get(0).params();
        assertEquals("sess-agent-gc", params.get("sessionId"));
    }

    @Test
    void sessionRpc_agent_deselect_injects_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-agent-des");

        session.agent.deselect();

        assertEquals(1, stub.calls.size());
        assertEquals("session.agent.deselect", stub.calls.get(0).method());
        var params = (Map<?, ?>) stub.calls.get(0).params();
        assertEquals("sess-agent-des", params.get("sessionId"));
    }

    @Test
    void sessionRpc_agent_reload_injects_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-agent-rel");

        session.agent.reload();

        assertEquals(1, stub.calls.size());
        assertEquals("session.agent.reload", stub.calls.get(0).method());
        var params = (Map<?, ?>) stub.calls.get(0).params();
        assertEquals("sess-agent-rel", params.get("sessionId"));
    }

    // ── SessionRpc.log (top-level) ─────────────────────────────────────────

    @Test
    void sessionRpc_log_merges_sessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "sess-log");

        var logParams = SessionLogParams.builder().sessionId(null).message("Hello from test").level(null).type(null)
                .ephemeral(null).url(null).tip(null).build();
        session.log(logParams);

        assertEquals(1, stub.calls.size());
        assertEquals("session.log", stub.calls.get(0).method());
        var params = (com.fasterxml.jackson.databind.node.ObjectNode) stub.calls.get(0).params();
        assertEquals("sess-log", params.get("sessionId").asText());
        assertEquals("Hello from test", params.get("message").asText());
    }

    // ── SessionFs server-side methods (via SessionRpc) ─────────────────────
    // SessionFs methods are accessed via ServerRpc.sessionFs; these tests
    // cover the remaining SessionFs param records used server-side.

    @Test
    void serverRpc_sessionFs_setProvider_params_record() {
        var params = SessionFsSetProviderParams.builder().initialCwd("/workspace").sessionStatePath("/state")
                .conventions(SessionFsSetProviderConventions.POSIX).capabilities(null).build();
        assertEquals("/workspace", params.initialCwd());
        assertEquals("/state", params.sessionStatePath());
        assertEquals(SessionFsSetProviderConventions.POSIX, params.conventions());
        assertNull(params.capabilities());
    }

    @Test
    void sessionsForkParams_record() {
        var params = SessionsForkParams.builder().sessionId("parent-id").toEventId("event-123").name(null).build();
        assertEquals("parent-id", params.sessionId());
        assertEquals("event-123", params.toEventId());
    }

    // ── SessionLogParams enum ──────────────────────────────────────────────

    @Test
    void sessionLogParams_level_enum_values() {
        assertEquals("info", SessionLogLevel.INFO.getValue());
        assertEquals("warning", SessionLogLevel.WARNING.getValue());
        assertEquals("error", SessionLogLevel.ERROR.getValue());
    }

    @Test
    void sessionLogParams_level_enum_fromValue() {
        assertEquals(SessionLogLevel.INFO, SessionLogLevel.fromValue("info"));
        assertEquals(SessionLogLevel.WARNING, SessionLogLevel.fromValue("warning"));
        assertEquals(SessionLogLevel.ERROR, SessionLogLevel.fromValue("error"));
    }

    @Test
    void sessionLogParams_level_enum_fromValue_unknown_throws() {
        assertThrows(IllegalArgumentException.class, () -> SessionLogLevel.fromValue("unknown-level"));
    }
}
