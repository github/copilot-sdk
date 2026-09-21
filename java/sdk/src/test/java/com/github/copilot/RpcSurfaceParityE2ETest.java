/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.rpc.*;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.RuntimeConnection;
import com.github.copilot.rpc.SessionConfig;

@AllowCopilotExperimental
class RpcSurfaceParityE2ETest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long TIMEOUT_SECONDS = 30;
    private static final int EXPECTED_RPC_METHOD_COUNT = 412;
    private static final String EXPECTED_RPC_SIGNATURE_SHA256 = "bac5728c9bafd33b4ac05f9f3df2ab6330603e251a91f25f7bd12abee6e4017b";
    private static final Map<String, Integer> EXPECTED_METHODS_BY_DECLARING_TYPE = Map.ofEntries(
            Map.entry("RpcCaller", 2), Map.entry("ServerAccountApi", 6), Map.entry("ServerAgentRegistryApi", 1),
            Map.entry("ServerAgentsApi", 2), Map.entry("ServerCatalogApi", 2), Map.entry("ServerCommandsApi", 1),
            Map.entry("ServerExtensionsApi", 3), Map.entry("ServerHooksApi", 1), Map.entry("ServerInstructionsApi", 2),
            Map.entry("ServerLlmInferenceApi", 3), Map.entry("ServerManagedSettingsApi", 2),
            Map.entry("ServerMcpApi", 2), Map.entry("ServerMcpConfigApi", 7), Map.entry("ServerModelsApi", 3),
            Map.entry("ServerPluginsApi", 7), Map.entry("ServerPluginsBuiltinApi", 1),
            Map.entry("ServerPluginsMarketplacesApi", 6), Map.entry("ServerRpc", 3), Map.entry("ServerRuntimeApi", 1),
            Map.entry("ServerSecretsApi", 1), Map.entry("ServerSessionFsApi", 1), Map.entry("ServerSessionsApi", 34),
            Map.entry("ServerSkillsApi", 2), Map.entry("ServerSkillsConfigApi", 2), Map.entry("ServerToolsApi", 1),
            Map.entry("ServerUserSettingsApi", 3), Map.entry("SessionAgentApi", 7),
            Map.entry("SessionAutopilotObjectiveApi", 1), Map.entry("SessionCanvasActionApi", 1),
            Map.entry("SessionCanvasApi", 4), Map.entry("SessionCanvasProviderApi", 2),
            Map.entry("SessionCommandsApi", 8), Map.entry("SessionCompletionsApi", 2),
            Map.entry("SessionConnectorsApi", 9), Map.entry("SessionContentExclusionApi", 1),
            Map.entry("SessionDebugApi", 1), Map.entry("SessionEventLogApi", 4), Map.entry("SessionExtensionsApi", 5),
            Map.entry("SessionFactoryApi", 13), Map.entry("SessionFactoryJournalApi", 2),
            Map.entry("SessionFleetApi", 1), Map.entry("SessionGitHubAuthApi", 10), Map.entry("SessionHistoryApi", 10),
            Map.entry("SessionInstructionsApi", 1), Map.entry("SessionLimitPredictionApi", 2),
            Map.entry("SessionManagedSettingsApi", 1), Map.entry("SessionLspApi", 1), Map.entry("SessionMcpApi", 18),
            Map.entry("SessionMcpAppsApi", 6), Map.entry("SessionMcpHeadersApi", 1), Map.entry("SessionMcpOauthApi", 5),
            Map.entry("SessionMcpResourcesApi", 3), Map.entry("SessionMetadataApi", 11), Map.entry("SessionModeApi", 2),
            Map.entry("SessionModelApi", 8), Map.entry("SessionNameApi", 3), Map.entry("SessionOptionsApi", 1),
            Map.entry("SessionPermissionsApi", 10), Map.entry("SessionPermissionsFolderTrustApi", 2),
            Map.entry("SessionPermissionsLocationsApi", 3), Map.entry("SessionPermissionsPathsApi", 5),
            Map.entry("SessionPermissionsUrlsApi", 1), Map.entry("SessionPlanApi", 5),
            Map.entry("SessionPluginsApi", 8), Map.entry("SessionPluginsMarketplacesApi", 6),
            Map.entry("SessionProviderApi", 3), Map.entry("SessionQueueApi", 20), Map.entry("SessionRemoteApi", 3),
            Map.entry("SessionRpc", 9), Map.entry("SessionSandboxApi", 2), Map.entry("SessionScheduleApi", 9),
            Map.entry("SessionSettingsApi", 2), Map.entry("SessionShellApi", 4), Map.entry("SessionSkillsApi", 6),
            Map.entry("SessionTasksApi", 13), Map.entry("SessionTelemetryApi", 2), Map.entry("SessionToolsApi", 8),
            Map.entry("SessionUiApi", 10), Map.entry("SessionUsageApi", 1), Map.entry("SessionVisibilityApi", 2),
            Map.entry("SessionWorkflowApi", 13), Map.entry("SessionWorkflowJournalApi", 2),
            Map.entry("SessionWorkspacesApi", 20));

    @Test
    void everyGeneratedRpcMethodHasRequestCaptureCoverageAndStableStructuralInventory() throws Exception {
        var caller = new RpcSurfaceTestCli.RecordingCaller();
        var targets = new LinkedHashMap<Class<?>, RpcTarget>();
        collectTargets(new ServerRpc(caller), "", targets, new IdentityHashMap<>());
        collectTargets(new SessionRpc(caller, "surface-session"), "session", targets, new IdentityHashMap<>());

        var methods = targets.values().stream()
                .flatMap(target -> rpcMethods(target.instance().getClass()).stream()
                        .map(method -> new TargetMethod(target, method)))
                .sorted(Comparator.comparing(TargetMethod::signature)).toList();
        var callerMethods = rpcMethods(RpcCaller.class);
        Map<String, Integer> counts = methods.stream()
                .collect(Collectors.groupingBy(method -> method.method().getDeclaringClass().getSimpleName(),
                        TreeMap::new, Collectors.summingInt(ignored -> 1)));
        counts.put(RpcCaller.class.getSimpleName(), callerMethods.size());
        assertEquals(EXPECTED_METHODS_BY_DECLARING_TYPE, counts,
                "Generated public RPC methods changed; map each new signature to a capture test or documented exclusion");
        assertEquals(EXPECTED_RPC_METHOD_COUNT, methods.size() + callerMethods.size());
        assertEquals(EXPECTED_RPC_SIGNATURE_SHA256,
                sha256(java.util.stream.Stream
                        .concat(methods.stream().map(TargetMethod::signature),
                                callerMethods.stream().map(RpcSurfaceParityE2ETest::signature))
                        .sorted().collect(Collectors.joining("\n"))));

        for (TargetMethod targetMethod : methods) {
            caller.clear();
            var method = targetMethod.method();
            var arguments = Arrays.stream(method.getParameterTypes()).map(RpcSurfaceParityE2ETest::fixture).toArray();
            var future = assertInstanceOf(CompletableFuture.class,
                    invoke(method, targetMethod.target().instance(), arguments), targetMethod.signature());
            assertNotNull(future);

            var call = assertSingleCall(caller, targetMethod.signature());
            var expectedMethod = targetMethod.target().prefix().isEmpty()
                    ? method.getName()
                    : targetMethod.target().prefix() + "." + method.getName();
            assertEquals(expectedMethod, call.method(), targetMethod.signature());
            assertNotNull(call.resultType(), targetMethod.signature());
            if (expectedMethod.startsWith("session.")) {
                assertEquals("surface-session", MAPPER.valueToTree(call.params()).path("sessionId").asText(),
                        targetMethod.signature());
            }
        }
    }

    @Test
    void rpcCallerOverloadsHaveDirectContractCoverage() throws Exception {
        var calls = new java.util.concurrent.CopyOnWriteArrayList<RpcSurfaceTestCli.RecordingCaller.Call>();
        RpcCaller caller = new RpcCaller() {
            @Override
            public <T> CompletableFuture<T> invoke(String method, Object params, Class<T> resultType) {
                calls.add(new RpcSurfaceTestCli.RecordingCaller.Call(method, params, resultType));
                if (resultType == JsonNode.class) {
                    return CompletableFuture.completedFuture(resultType.cast(json("""
                            {"value":"deserialized"}
                            """)));
                }
                return CompletableFuture.completedFuture(null);
            }
        };

        caller.invoke("contract.class", Map.of("kind", "class"), Void.class).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        var result = caller
                .invoke("contract.javaType", Map.of("kind", "javaType"),
                        MAPPER.getTypeFactory().constructMapType(Map.class, String.class, String.class))
                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        caller.invoke("contract.javaTypeVoid", Map.of("kind", "javaTypeVoid"),
                MAPPER.getTypeFactory().constructType(Void.class)).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

        assertEquals(Map.of("value", "deserialized"), result);
        assertEquals(List.of("contract.class", "contract.javaType", "contract.javaTypeVoid"),
                calls.stream().map(RpcSurfaceTestCli.RecordingCaller.Call::method).toList());
        assertEquals(List.of(Void.class, JsonNode.class, Void.class),
                calls.stream().map(RpcSurfaceTestCli.RecordingCaller.Call::resultType).toList());
    }

    @Test
    void omittedNamespaceMethodsUseExactGeneratedEntryPoints() {
        var caller = new RpcSurfaceTestCli.RecordingCaller();
        var rpc = new SessionRpc(caller, "direct-session");

        rpc.permissions.paths.list();
        rpc.permissions.paths.add(params(SessionPermissionsPathsAddParams.class, "{}"));
        rpc.permissions.paths.updatePrimary(params(SessionPermissionsPathsUpdatePrimaryParams.class, "{}"));
        rpc.permissions.paths.isPathWithinAllowedDirectories(
                params(SessionPermissionsPathsIsPathWithinAllowedDirectoriesParams.class, "{}"));
        rpc.permissions.paths
                .isPathWithinWorkspace(params(SessionPermissionsPathsIsPathWithinWorkspaceParams.class, "{}"));
        rpc.permissions.urls.setUnrestrictedMode(params(SessionPermissionsUrlsSetUnrestrictedModeParams.class, "{}"));

        rpc.plan.read();
        rpc.plan.update(params(SessionPlanUpdateParams.class, "{}"));
        rpc.plan.delete();
        rpc.plan.readSqlTodos();
        rpc.plan.readSqlTodosWithDependencies();

        rpc.provider.getEndpoint();
        rpc.provider.getEndpoint(params(SessionProviderGetEndpointParams.class, "{}"));
        rpc.provider.add(params(SessionProviderAddParams.class, "{}"));

        rpc.queue.pendingItems();
        rpc.queue.snapshot();
        rpc.queue.moveItem(params(SessionQueueMoveItemParams.class, "{}"));
        rpc.queue.insertAt(params(SessionQueueInsertAtParams.class, "{}"));
        rpc.queue.removeAt(params(SessionQueueRemoveAtParams.class, "{}"));
        rpc.queue.updateText(params(SessionQueueUpdateTextParams.class, "{}"));
        rpc.queue.duplicateAt(params(SessionQueueDuplicateAtParams.class, "{}"));
        rpc.queue.setDrainPaused(params(SessionQueueSetDrainPausedParams.class, "{}"));
        rpc.queue.sendNow(params(SessionQueueSendNowParams.class, "{}"));
        rpc.queue.hasPending();
        rpc.queue.beginDeferredIdleDrain(params(SessionQueueBeginDeferredIdleDrainParams.class, "{}"));
        rpc.queue.finishDeferredIdleDrain(params(SessionQueueFinishDeferredIdleDrainParams.class, "{}"));
        rpc.queue.deferSessionIdle(params(SessionQueueDeferSessionIdleParams.class, "{}"));
        rpc.queue.removeMostRecent();
        rpc.queue.clear();
        rpc.queue.consumeSystemNotifications(params(SessionQueueConsumeSystemNotificationsParams.class, "{}"));
        rpc.queue.enqueueResumePending();
        rpc.queue.process();

        rpc.remote.enable(params(SessionRemoteEnableParams.class, "{}"));
        rpc.remote.disable();
        rpc.remote.notifySteerableChanged(params(SessionRemoteNotifySteerableChangedParams.class, "{}"));
        rpc.sandbox.getEnforcementStatus();
        rpc.sandbox.disableForSession(params(SessionSandboxDisableForSessionParams.class, "{}"));
        rpc.settings.snapshot();
        rpc.settings.evaluatePredicate(params(SessionSettingsEvaluatePredicateParams.class, "{}"));
        rpc.shell.exec(params(SessionShellExecParams.class, "{}"));
        rpc.shell.kill(params(SessionShellKillParams.class, "{}"));
        rpc.shell.executeUserRequested(params(SessionShellExecuteUserRequestedParams.class, "{}"));
        rpc.shell.cancelUserRequested(params(SessionShellCancelUserRequestedParams.class, "{}"));

        rpc.skills.list();
        rpc.skills.getInvoked();
        rpc.skills.enable(params(SessionSkillsEnableParams.class, "{}"));
        rpc.skills.disable(params(SessionSkillsDisableParams.class, "{}"));
        rpc.skills.reload();
        rpc.skills.ensureLoaded();

        rpc.tasks.startAgent(params(SessionTasksStartAgentParams.class, "{}"));
        rpc.tasks.list();
        rpc.tasks.register(params(SessionTasksRegisterParams.class, "{}"));
        rpc.tasks.update(params(SessionTasksUpdateParams.class, "{}"));
        rpc.tasks.refresh();
        rpc.tasks.waitForPending();
        rpc.tasks.getProgress(params(SessionTasksGetProgressParams.class, "{}"));
        rpc.tasks.getCurrentPromotable();
        rpc.tasks.promoteToBackground(params(SessionTasksPromoteToBackgroundParams.class, "{}"));
        rpc.tasks.promoteCurrentToBackground();
        rpc.tasks.cancel(params(SessionTasksCancelParams.class, "{}"));
        rpc.tasks.remove(params(SessionTasksRemoveParams.class, "{}"));
        rpc.tasks.sendMessage(params(SessionTasksSendMessageParams.class, "{}"));

        rpc.telemetry.getEngagementId();
        rpc.telemetry.setFeatureOverrides(params(SessionTelemetrySetFeatureOverridesParams.class, "{}"));
        rpc.tools.execute(params(SessionToolsExecuteParams.class, "{}"));
        rpc.tools.getBuiltinDescriptors(params(SessionToolsGetBuiltinDescriptorsParams.class, "{}"));
        rpc.tools.taskCompleteEventData(params(SessionToolsTaskCompleteEventDataParams.class, "{}"));
        rpc.tools.handlePendingToolCall(params(SessionToolsHandlePendingToolCallParams.class, "{}"));
        rpc.tools.initializeAndValidate();
        rpc.tools.getCurrentMetadata();
        rpc.tools.set(params(SessionToolsSetParams.class, "{}"));
        rpc.tools.updateSubagentSettings(params(SessionToolsUpdateSubagentSettingsParams.class, "{}"));

        rpc.ui.ephemeralQuery(params(SessionUiEphemeralQueryParams.class, "{}"));
        rpc.ui.elicitation(params(SessionUiElicitationParams.class, "{}"));
        rpc.ui.handlePendingElicitation(params(SessionUiHandlePendingElicitationParams.class, "{}"));
        rpc.ui.handlePendingUserInput(params(SessionUiHandlePendingUserInputParams.class, "{}"));
        rpc.ui.handlePendingSampling(params(SessionUiHandlePendingSamplingParams.class, "{}"));
        rpc.ui.handlePendingAutoModeSwitch(params(SessionUiHandlePendingAutoModeSwitchParams.class, "{}"));
        rpc.ui.handlePendingSessionLimitsExhausted(
                params(SessionUiHandlePendingSessionLimitsExhaustedParams.class, "{}"));
        rpc.ui.handlePendingExitPlanMode(params(SessionUiHandlePendingExitPlanModeParams.class, "{}"));
        rpc.ui.registerDirectAutoModeSwitchHandler();
        rpc.ui.unregisterDirectAutoModeSwitchHandler(
                params(SessionUiUnregisterDirectAutoModeSwitchHandlerParams.class, "{}"));

        rpc.visibility.get();
        rpc.visibility.set(params(SessionVisibilitySetParams.class, "{}"));
        rpc.workspaces.getWorkspace();
        rpc.workspaces.updateMetadata(params(SessionWorkspacesUpdateMetadataParams.class, "{}"));
        rpc.workspaces.ensure(params(SessionWorkspacesEnsureParams.class, "{}"));
        rpc.workspaces.listFiles();
        rpc.workspaces.readFile(params(SessionWorkspacesReadFileParams.class, "{}"));
        rpc.workspaces.createFile(params(SessionWorkspacesCreateFileParams.class, "{}"));
        rpc.workspaces.statFile(params(SessionWorkspacesStatFileParams.class, "{}"));
        rpc.workspaces.createDirectory(params(SessionWorkspacesCreateDirectoryParams.class, "{}"));
        rpc.workspaces.removePath(params(SessionWorkspacesRemovePathParams.class, "{}"));
        rpc.workspaces.renamePath(params(SessionWorkspacesRenamePathParams.class, "{}"));
        rpc.workspaces.listCheckpoints();
        rpc.workspaces.readCheckpoint(params(SessionWorkspacesReadCheckpointParams.class, "{}"));
        rpc.workspaces.addSummary(params(SessionWorkspacesAddSummaryParams.class, "{}"));
        rpc.workspaces.truncateSummaries(params(SessionWorkspacesTruncateSummariesParams.class, "{}"));
        rpc.workspaces.readAutopilotObjective();
        rpc.workspaces.writeAutopilotObjective(params(SessionWorkspacesWriteAutopilotObjectiveParams.class, "{}"));
        rpc.workspaces.deleteAutopilotObjective();
        rpc.workspaces.autopilotObjectiveExists();
        rpc.workspaces.saveLargePaste(params(SessionWorkspacesSaveLargePasteParams.class, "{}"));
        rpc.workspaces.diff(params(SessionWorkspacesDiffParams.class, "{}"));

        assertEquals(104, caller.calls().size());
        assertTrue(caller.calls().stream().allMatch(call -> call.method().startsWith("session.")));
        assertTrue(caller.calls().stream().allMatch(
                call -> "direct-session".equals(MAPPER.valueToTree(call.params()).path("sessionId").asText())));
        var methods = caller.calls().stream().map(RpcSurfaceTestCli.RecordingCaller.Call::method)
                .collect(Collectors.toSet());
        assertTrue(methods.containsAll(Set.of("session.permissions.paths.list",
                "session.permissions.urls.setUnrestrictedMode", "session.plan.readSqlTodosWithDependencies",
                "session.provider.add", "session.queue.process", "session.remote.notifySteerableChanged",
                "session.sandbox.disableForSession", "session.settings.evaluatePredicate",
                "session.shell.cancelUserRequested", "session.skills.ensureLoaded", "session.tasks.sendMessage",
                "session.telemetry.setFeatureOverrides", "session.tools.updateSubagentSettings",
                "session.ui.unregisterDirectAutoModeSwitchHandler", "session.visibility.set",
                "session.workspaces.diff")));
    }

    @Test
    void remainingGeneratedMethodsUseExactEntryPoints() {
        var caller = new RpcSurfaceTestCli.RecordingCaller();
        var server = new ServerRpc(caller);
        var rpc = new SessionRpc(caller, "remaining-session");

        rpc.agent.list(params(SessionAgentListParams.class, "{}"));
        rpc.autopilotObjective.getState();
        rpc.canvas.action.invoke(params(SessionCanvasActionInvokeParams.class, "{}"));
        rpc.canvas.close(params(SessionCanvasCloseParams.class, "{}"));
        rpc.canvas.list();
        rpc.canvas.listOpen();
        rpc.canvas.open(params(SessionCanvasOpenParams.class, "{}"));
        rpc.canvas.provider.register(params(SessionCanvasProviderRegisterParams.class, "{}"));
        rpc.canvas.provider.unregister(params(SessionCanvasProviderUnregisterParams.class, "{}"));
        rpc.commands.enqueue(params(SessionCommandsEnqueueParams.class, "{}"));
        rpc.commands.execute(params(SessionCommandsExecuteParams.class, "{}"));
        rpc.commands.finalizeInvocationEffect(params(SessionCommandsFinalizeInvocationEffectParams.class, "{}"));
        rpc.commands.list(params(SessionCommandsListParams.class, "{}"));
        rpc.commands.respondToQueuedCommand(params(SessionCommandsRespondToQueuedCommandParams.class, "{}"));
        rpc.completions.getTriggerCharacters();
        rpc.eventLog.registerInterest(params(SessionEventLogRegisterInterestParams.class, "{}"));
        rpc.eventLog.releaseInterest(params(SessionEventLogReleaseInterestParams.class, "{}"));
        rpc.eventLog.tail();
        rpc.extensions.sendAttachmentsToMessage(params(SessionExtensionsSendAttachmentsToMessageParams.class, "{}"));
        rpc.factory.cancel(params(SessionFactoryCancelParams.class, "{}"));
        rpc.factory.getRunDetail(params(SessionFactoryGetRunDetailParams.class, "{}"));
        rpc.factory.getRunProgress(params(SessionFactoryGetRunProgressParams.class, "{}"));
        rpc.factory.listRuns(params(SessionFactoryListRunsParams.class, "{}"));
        rpc.factory.pauseAtCheckpoint(params(SessionFactoryPauseAtCheckpointParams.class, "{}"));
        rpc.factory.resumeFromTool(params(SessionFactoryResumeFromToolParams.class, "{}"));
        rpc.factory.runFromTool(params(SessionFactoryRunFromToolParams.class, "{}"));
        rpc.gitHubAuth.getAllAuthAvailable();
        rpc.gitHubAuth.getCurrentAuthInfo();
        rpc.gitHubAuth.lastAuthErrors();
        rpc.gitHubAuth.login(params(SessionGitHubAuthLoginParams.class, "{}"));
        rpc.gitHubAuth.logout();
        rpc.gitHubAuth.logoutUser(params(SessionGitHubAuthLogoutUserParams.class, "{}"));
        rpc.gitHubAuth.refreshCopilotUser();
        rpc.gitHubAuth.setCredentials(params(SessionGitHubAuthSetCredentialsParams.class, "{}"));
        rpc.gitHubAuth.switchToAuth(params(SessionGitHubAuthSwitchToAuthParams.class, "{}"));
        rpc.history.abortManualCompaction();
        rpc.history.cancelBackgroundCompaction();
        rpc.history.compact(params(SessionHistoryCompactParams.class, "{}"));
        rpc.history.summarizeForHandoff();
        rpc.instructions.getSources();
        rpc.limitPrediction.predict();
        rpc.lsp.initialize(params(SessionLspInitializeParams.class, "{}"));
        rpc.mcp.apps.diagnose(params(SessionMcpAppsDiagnoseParams.class, "{}"));
        rpc.mcp.apps.getHostContext();
        rpc.mcp.apps.listTools(params(SessionMcpAppsListToolsParams.class, "{}"));
        rpc.mcp.apps.readResource(params(SessionMcpAppsReadResourceParams.class, "{}"));
        rpc.mcp.apps.setHostContext(params(SessionMcpAppsSetHostContextParams.class, "{}"));
        rpc.mcp.cancelSamplingExecution(params(SessionMcpCancelSamplingExecutionParams.class, "{}"));
        rpc.mcp.configureGitHub(params(SessionMcpConfigureGitHubParams.class, "{}"));
        rpc.mcp.executeSampling(params(SessionMcpExecuteSamplingParams.class, "{}"));
        rpc.mcp.isServerRunning(params(SessionMcpIsServerRunningParams.class, "{}"));
        rpc.mcp.oauth.probe(params(SessionMcpOauthProbeParams.class, "{}"));
        rpc.mcp.registerExternalClient(params(SessionMcpRegisterExternalClientParams.class, "{}"));
        rpc.mcp.reloadWithConfig(params(SessionMcpReloadWithConfigParams.class, "{}"));
        rpc.mcp.removeGitHub();
        rpc.mcp.restartServer(params(SessionMcpRestartServerParams.class, "{}"));
        rpc.mcp.setEnvValueMode(params(SessionMcpSetEnvValueModeParams.class, "{}"));
        rpc.mcp.stopServer(params(SessionMcpStopServerParams.class, "{}"));
        rpc.mcp.unregisterExternalClient(params(SessionMcpUnregisterExternalClientParams.class, "{}"));
        rpc.metadata.activity();
        rpc.metadata.contextInfo(params(SessionMetadataContextInfoParams.class, "{}"));
        rpc.metadata.isProcessing();
        rpc.metadata.recomputeContextTokens(params(SessionMetadataRecomputeContextTokensParams.class, "{}"));
        rpc.metadata.recordContextChange(params(SessionMetadataRecordContextChangeParams.class, "{}"));
        rpc.metadata.setWorkingDirectory(params(SessionMetadataSetWorkingDirectoryParams.class, "{}"));
        rpc.metadata.snapshot();
        rpc.metadata.updateClientMetadata(params(SessionMetadataUpdateClientMetadataParams.class, "{}"));
        rpc.model.applyStartupOverlay(params(SessionModelApplyStartupOverlayParams.class, "{}"));
        rpc.model.list();
        rpc.model.list(params(SessionModelListParams.class, "{}"));
        rpc.model.setReasoningEffort(params(SessionModelSetReasoningEffortParams.class, "{}"));
        rpc.name.get();
        rpc.name.set(params(SessionNameSetParams.class, "{}"));
        rpc.name.setAuto(params(SessionNameSetAutoParams.class, "{}"));
        rpc.options.update(params(SessionOptionsUpdateParams.class, "{}"));
        rpc.permissions.configure(params(SessionPermissionsConfigureParams.class, "{}"));
        rpc.permissions.folderTrust.addTrusted(params(SessionPermissionsFolderTrustAddTrustedParams.class, "{}"));
        rpc.permissions.folderTrust.isTrusted(params(SessionPermissionsFolderTrustIsTrustedParams.class, "{}"));
        rpc.permissions.getMode();
        rpc.permissions.locations.addToolApproval(params(SessionPermissionsLocationsAddToolApprovalParams.class, "{}"));
        rpc.permissions.locations.apply(params(SessionPermissionsLocationsApplyParams.class, "{}"));
        rpc.permissions.locations.resolve(params(SessionPermissionsLocationsResolveParams.class, "{}"));
        rpc.permissions.modifyRules(params(SessionPermissionsModifyRulesParams.class, "{}"));
        rpc.permissions.notifyPromptShown(params(SessionPermissionsNotifyPromptShownParams.class, "{}"));
        rpc.permissions.pendingRequests();
        rpc.permissions.resetSessionApprovals(params(SessionPermissionsResetSessionApprovalsParams.class, "{}"));
        rpc.permissions.setMode(params(SessionPermissionsSetModeParams.class, "{}"));
        rpc.permissions.setRequired(params(SessionPermissionsSetRequiredParams.class, "{}"));
        rpc.plugins.reload();
        rpc.plugins.reload(params(SessionPluginsReloadParams.class, "{}"));
        rpc.schedule.add(params(SessionScheduleAddParams.class, "{}"));
        rpc.schedule.addAt(params(SessionScheduleAddAtParams.class, "{}"));
        rpc.schedule.addCron(params(SessionScheduleAddCronParams.class, "{}"));
        rpc.schedule.addSelfPaced(params(SessionScheduleAddSelfPacedParams.class, "{}"));
        rpc.schedule.hasSelfPaced();
        rpc.schedule.hydrate();
        rpc.schedule.list();
        rpc.schedule.rearmSelfPaced(params(SessionScheduleRearmSelfPacedParams.class, "{}"));
        rpc.schedule.stop(params(SessionScheduleStopParams.class, "{}"));
        rpc.sendMessages(params(SessionSendMessagesParams.class, "{}"));
        rpc.sendSystemNotification(params(SessionSendSystemNotificationParams.class, "{}"));
        rpc.shutdown(params(SessionShutdownParams.class, "{}"));
        rpc.suspend();

        server.account.getQuota(params(AccountGetQuotaParams.class, "{}"));
        server.agentRegistry.spawn(params(AgentRegistrySpawnParams.class, "{}"));
        server.connect(params(ConnectParams.class, "{}"));
        server.extensions.disable(params(ExtensionsDisableParams.class, "{}"));
        server.extensions.discover();
        server.extensions.enable(params(ExtensionsEnableParams.class, "{}"));
        server.mcp.config.disable(params(McpConfigDisableParams.class, "{}"));
        server.mcp.config.enable(params(McpConfigEnableParams.class, "{}"));
        server.mcp.config.reload();
        server.models.list(params(ModelsListParams.class, "{}"));
        server.plugins.disable(params(PluginsDisableParams.class, "{}"));
        server.plugins.enable(params(PluginsEnableParams.class, "{}"));
        server.plugins.install(params(PluginsInstallParams.class, "{}"));
        server.plugins.list();
        server.plugins.marketplaces.add(params(PluginsMarketplacesAddParams.class, "{}"));
        server.plugins.marketplaces.browse(params(PluginsMarketplacesBrowseParams.class, "{}"));
        server.plugins.marketplaces.list();
        server.plugins.marketplaces.refresh();
        server.plugins.marketplaces.refresh(params(PluginsMarketplacesRefreshParams.class, "{}"));
        server.plugins.marketplaces.remove(params(PluginsMarketplacesRemoveParams.class, "{}"));
        server.plugins.uninstall(params(PluginsUninstallParams.class, "{}"));
        server.plugins.update(params(PluginsUpdateParams.class, "{}"));
        server.plugins.updateAll();
        server.runtime.shutdown();
        server.sessions.configureSessionExtensions(params(SessionsConfigureSessionExtensionsParams.class, "{}"));
        server.sessions.delete(params(SessionsDeleteParams.class, "{}"));
        server.sessions.getBoardEntryCount(params(SessionsGetBoardEntryCountParams.class, "{}"));
        server.sessions.getMetadata(params(SessionsGetMetadataParams.class, "{}"));
        server.sessions.getRemoteControlStatus();
        server.sessions.list(params(SessionsListParams.class, "{}"));
        server.sessions.listNonEmptySessionIds(params(SessionsListNonEmptySessionIdsParams.class, "{}"));
        server.sessions.open((SessionsOpenParams) fixture(SessionsOpenParams.class));
        server.sessions.readPersistedEvents(params(SessionsReadPersistedEventsParams.class, "{}"));
        server.sessions.setRemoteControlSteering(params(SessionsSetRemoteControlSteeringParams.class, "{}"));
        server.sessions.startRemoteControl(params(SessionsStartRemoteControlParams.class, "{}"));
        server.sessions.stopRemoteControl();
        server.sessions.stopRemoteControl(params(SessionsStopRemoteControlParams.class, "{}"));
        server.sessions.transferRemoteControl(params(SessionsTransferRemoteControlParams.class, "{}"));

        assertEquals(141, caller.calls().size());
        assertTrue(caller.calls().stream().filter(call -> call.method().startsWith("session.")).allMatch(
                call -> "remaining-session".equals(MAPPER.valueToTree(call.params()).path("sessionId").asText())));
    }

    @Test
    void protocolErrorsPreserveCodeAndMessage() throws Exception {
        try (var runtime = new RpcSurfaceTestCli(request -> {
            if ("connect".equals(request.path("method").asText())) {
                return json("""
                        {"ok":true,"protocolVersion":3,"version":"rpc-surface-test"}
                        """);
            }
            if ("runtime.shutdown".equals(request.path("method").asText())) {
                return MAPPER.createObjectNode();
            }
            throw RpcSurfaceTestCli.error(-32042, "rpc surface rejected", json("""
                    {"reason":"policy","retryable":false}
                    """));
        }); var client = createClient(runtime)) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            var failure = assertThrows(ExecutionException.class, () -> client.getRpc().catalog
                    .search(params(CatalogSearchParams.class, "{}")).get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            var rpcFailure = assertInstanceOf(JsonRpcException.class, failure.getCause());
            assertEquals(-32042, rpcFailure.getCode());
            assertEquals("rpc surface rejected", rpcFailure.getMessage());
            assertEquals(1, runtime.requestCount("catalog.search"));
            assertTrue(parameters(runtime, "catalog.search").isObject());
        }
    }

    @Test
    void serverRpcsSerializeRequestsAndProjectNestedResults() throws Exception {
        try (var runtime = new RpcSurfaceTestCli(RpcSurfaceParityE2ETest::handle); var client = createClient(runtime)) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            var rpc = client.getRpc();

            rpc.registerExtensionLaunchProvider().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            var command = rpc.commands.list().get(TIMEOUT_SECONDS, TimeUnit.SECONDS).commands().get(0);
            assertEquals("rpc-command", command.name());
            assertEquals(List.of("rpc"), command.aliases());
            assertTrue(command.allowDuringAgentExecution());
            assertTrue(command.schedulable());

            var hooks = rpc.hooks.discover(params(HooksDiscoverParams.class, """
                    {"projectPaths":["Q:\\\\rpc-project"],"excludeHostHooks":true}
                    """)).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertEquals(List.of("rpc-warning"), hooks.warnings());
            assertTrue(hooks.hooks().isEmpty());
            assertTrue(hooks.errors().isEmpty());

            assertTrue(rpc.llmInference.setProvider().get(TIMEOUT_SECONDS, TimeUnit.SECONDS).success());
            assertEquals("strict",
                    ((Map<?, ?>) rpc.managedSettings.read().get(TIMEOUT_SECONDS, TimeUnit.SECONDS).settingsJson())
                            .get("policy"));

            var plan = assertInstanceOf(McpPlanInstallPlanned.class,
                    rpc.mcp.planInstall(params(McpPlanInstallParams.class, """
                            {
                              "contract":{"protocolVersion":3,"requiredCapabilities":["mcp-install-planning"]},
                              "source":{"kind":"candidate","candidateHandle":"candidate-1","searchId":"search-1"},
                              "scope":"user"
                            }
                            """)).get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            assertEquals("plan-1", plan.getPlan().planHandle());
            assertTrue(plan.getPlan().reloadRequired());
            assertEquals(3L, plan.getNegotiated().runtimeProtocolVersion());

            assertEquals("built-in-model",
                    rpc.models.getBuiltInCatalog().get(TIMEOUT_SECONDS, TimeUnit.SECONDS).models().get(0).id());
            rpc.plugins.builtin.set(new PluginsBuiltinSetParams(List.of("Q:\\rpc-plugins"))).get(TIMEOUT_SECONDS,
                    TimeUnit.SECONDS);

            var metadata = rpc.sessions
                    .getClientMetadata(
                            new SessionsGetClientMetadataParams(List.of("persisted-session"), List.of("rpc/key")))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            assertEquals("ok", ((Map<?, ?>) metadata.get(0)).get("status"));
            assertEquals("rpc-value", ((Map<?, ?>) ((Map<?, ?>) metadata.get(0)).get("metadata")).get("rpc/key"));

            rpc.skills.config.setSkillDisabled(new SkillsConfigSetSkillDisabledParams("skill-one", true))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            assertCalledOnce(runtime, "registerExtensionLaunchProvider", "commands.list", "hooks.discover",
                    "llmInference.setProvider", "managedSettings.read", "mcp.planInstall", "models.getBuiltInCatalog",
                    "plugins.builtin.set", "sessions.getClientMetadata", "skills.config.setSkillDisabled");
            assertTrue(parameters(runtime, "hooks.discover").path("excludeHostHooks").asBoolean());
            assertEquals("candidate", parameters(runtime, "mcp.planInstall").path("source").path("kind").asText());
            assertEquals("skill-one", parameters(runtime, "skills.config.setSkillDisabled").path("name").asText());
        }
    }

    @Test
    void sessionControlRpcsSerializeRequestsAndProjectUnionsAndState() throws Exception {
        try (var runtime = new RpcSurfaceTestCli(RpcSurfaceParityE2ETest::handle); var client = createClient(runtime)) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            try (var session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                var rpc = session.getRpc();

                rpc.agent.setPrompt(new SessionAgentSetPromptParams(null, "agent-1", "Use the RPC prompt."))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

                var exclusion = rpc.contentExclusion
                        .checkPaths(
                                new SessionContentExclusionCheckPathsParams(null, List.of("/rpc-workspace/file.txt")))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertTrue(exclusion.available());
                assertFalse(exclusion.checks().get(0).excluded());

                var logs = rpc.debug.collectLogs(params(SessionDebugCollectLogsParams.class, """
                        {
                          "destination":{"kind":"directory","outputDirectory":"/rpc-debug"},
                          "include":{"events":true,"processLogs":false,"shellLogs":true},
                          "additionalEntries":[{"bundlePath":"host/diagnostic.txt","kind":"file",
                            "path":"/diagnostic.txt","required":true}]
                        }
                        """)).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertEquals(DebugCollectLogsResultKind.DIRECTORY, logs.kind());
                assertEquals(123L, logs.entries().get(0).sizeBytes());
                assertEquals("not found", logs.skippedEntries().get(0).reason());

                assertEquals(4L, rpc.history.clearContext(new SessionHistoryClearContextParams(null, "Reset context."))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS).messagesCleared());

                var prediction = assertInstanceOf(SessionLimitPredictionResultUnavailable.class,
                        rpc.limitPrediction.predict(params(SessionLimitPredictionPredictParams.class, """
                                {"request":{"clientType":"sdk","modelId":"model-a"}}
                                """)).get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
                assertEquals(SessionLimitPredictionUnavailableReason.AUTO_UNRESOLVED, prediction.getReason());

                var metadata = rpc.metadata.getClientMetadata().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertNotNull(metadata);

                var allowed = rpc.model
                        .setAllowedModels(new SessionModelSetAllowedModelsParams(null, List.of("model-a", "model-b")))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertEquals(List.of("model-a", "model-b"), allowed.allowedModels());
                assertEquals("model-a", allowed.fallbackModel());

                var tier = rpc.model
                        .switchAutoTier(new SessionModelSwitchAutoTierParams(null, AutoTier.INTELLIGENCE, null))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertEquals(ModelSwitchAutoTierStatus.PENDING, tier.status());
                assertEquals(AutoTier.INTELLIGENCE, tier.effectiveAutoTier());
                assertEquals(AutoTier.BALANCE, tier.supersededAutoTier());

                var enforcement = rpc.sandbox.getEnforcementStatus().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertTrue(enforcement.required());
                assertFalse(enforcement.blocked());
                assertEquals("managed-policy", enforcement.reason());

                var disabled = rpc.sandbox
                        .disableForSession(new SessionSandboxDisableForSessionParams(null, "sandbox-request-1", null))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertTrue(disabled.success());
                assertFalse(disabled.enabled());

                assertTrue(rpc.abort(new SessionAbortParams(null, AbortReason.USER_INITIATED))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS).success());
                assertTrue(rpc.interruptMainTurn(new SessionInterruptMainTurnParams(null, true))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS).interrupted());
                rpc.cancelAllBackgroundAgents().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

                var log = rpc.log(params(SessionLogParams.class, """
                        {"message":"RPC log","level":"warning","type":"rpc","ephemeral":true,
                         "url":"https://example.test/rpc","tip":"Inspect the RPC."}
                        """)).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertEquals("11111111-2222-3333-4444-555555555555", log.eventId().toString());

                assertCalledOnce(runtime, "session.agent.setPrompt", "session.contentExclusion.checkPaths",
                        "session.debug.collectLogs", "session.history.clearContext", "session.limitPrediction.predict",
                        "session.metadata.getClientMetadata", "session.model.setAllowedModels",
                        "session.model.switchAutoTier", "session.sandbox.getEnforcementStatus",
                        "session.sandbox.disableForSession", "session.abort", "session.interruptMainTurn",
                        "session.cancelAllBackgroundAgents", "session.log");
                assertEquals(session.getSessionId(), parameters(runtime, "session.log").path("sessionId").asText());
                assertTrue(parameters(runtime, "session.interruptMainTurn").path("flushQueued").asBoolean());
            }
        }
    }

    @Test
    void factoryAndMcpRpcsSerializeRequestsAndProjectStateTransitions() throws Exception {
        try (var runtime = new RpcSurfaceTestCli(RpcSurfaceParityE2ETest::handle); var client = createClient(runtime)) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            try (var session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                var rpc = session.getRpc();

                var run = rpc.factory.run(params(SessionFactoryRunParams.class, """
                        {"name":"rpc-factory","args":{"input":42},
                         "options":{"limits":{"maxAiCredits":2.5,"maxConcurrentSubagents":2,
                           "maxTotalSubagents":4,"timeoutSeconds":30},
                           "logPhaseNames":true,"notifyOnComplete":false}}
                        """)).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertEquals("factory-run-1", run.runId());
                assertEquals(FactoryRunStatus.RUNNING, run.status());
                assertEquals(1L, run.attempt());

                var resumed = rpc.factory.resume(params(SessionFactoryResumeParams.class, """
                        {"runId":"factory-run-1","limits":{"maxTotalSubagents":8},
                         "notifyOnComplete":true,"logPhaseNames":false}
                        """)).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertEquals("rpc-factory", resumed.factoryName());
                assertEquals(2L, resumed.run().attempt());

                assertEquals(FactoryRunStatus.RUNNING,
                        rpc.factory.getRun(new SessionFactoryGetRunParams(null, "factory-run-1"))
                                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS).status());
                assertEquals(FactoryRunStatus.PAUSED,
                        rpc.factory.pause(new SessionFactoryPauseParams(null, "factory-run-1"))
                                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS).status());

                rpc.factory.log(params(SessionFactoryLogParams.class, """
                        {"runId":"factory-run-1","executionToken":"execution-token-1",
                         "lines":[{"kind":"log","seq":7,"text":"Factory progress"}]}
                        """)).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                var agent = rpc.factory.agent(params(SessionFactoryAgentParams.class, """
                        {"runId":"factory-run-1","executionToken":"execution-token-1",
                         "prompt":"Complete the RPC task.",
                         "options":{"agent":"explore","label":"rpc-agent","model":"model-a",
                           "reasoningEffort":"high"}}
                        """)).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertEquals("agent-result", ((Map<?, ?>) agent.result()).get("answer"));

                var journal = rpc.factory.journal.get(
                        new SessionFactoryJournalGetParams(null, "factory-run-1", "execution-token-1", "checkpoint"))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertTrue(journal.hit());
                assertEquals(7, ((Map<?, ?>) journal.resultJson()).get("checkpoint"));
                rpc.factory.journal.put(params(SessionFactoryJournalPutParams.class, """
                        {"runId":"factory-run-1","executionToken":"execution-token-1",
                         "key":"checkpoint","resultJson":{"checkpoint":8}}
                        """)).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);

                assertTrue(
                        rpc.mcp.moveLoadingToBackground().get(TIMEOUT_SECONDS, TimeUnit.SECONDS).movedToBackground());
                rpc.mcp.startServer(params(SessionMcpStartServerParams.class, """
                        {"serverName":"rpc-server","config":{"command":"node","args":["server.js"]}}
                        """)).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                rpc.mcp.oauth
                        .authenticationStateChanged(
                                new SessionMcpOauthAuthenticationStateChangedParams(null, "rpc-server", true))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertTrue(rpc.mcp.oauth.respond(new SessionMcpOauthRespondParams(null, "oauth-request-1"))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS).success());

                var resources = rpc.mcp.resources
                        .list(new SessionMcpResourcesListParams(null, "rpc-server", "resource-cursor"))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertEquals("resource-next", resources.nextCursor());
                assertEquals("RPC resource", resources.resources().get(0).name());
                var templates = rpc.mcp.resources
                        .listTemplates(
                                new SessionMcpResourcesListTemplatesParams(null, "rpc-server", "template-cursor"))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertEquals("file://rpc/{name}", templates.resourceTemplates().get(0).uriTemplate());
                var content = rpc.mcp.resources
                        .read(new SessionMcpResourcesReadParams(null, "rpc-server", "file://rpc/resource.txt"))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS).contents().get(0);
                assertEquals("resource-content", content.text());
                assertEquals("assistant", content.meta().get("audience"));

                assertCalledOnce(runtime, "session.factory.run", "session.factory.resume", "session.factory.getRun",
                        "session.factory.pause", "session.factory.log", "session.factory.agent",
                        "session.factory.journal.get", "session.factory.journal.put",
                        "session.mcp.moveLoadingToBackground", "session.mcp.startServer",
                        "session.mcp.oauth.authenticationStateChanged", "session.mcp.oauth.respond",
                        "session.mcp.resources.list", "session.mcp.resources.listTemplates",
                        "session.mcp.resources.read");
                assertEquals(42, parameters(runtime, "session.factory.run").path("args").path("input").asInt());
                assertEquals("resource-cursor",
                        parameters(runtime, "session.mcp.resources.list").path("cursor").asText());
            }
        }
    }

    @Test
    void taskToolAndWorkspaceRpcsSerializeMutationsAndProjectResults() throws Exception {
        try (var runtime = new RpcSurfaceTestCli(RpcSurfaceParityE2ETest::handle); var client = createClient(runtime)) {
            client.start().get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            try (var session = client
                    .createSession(new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL))
                    .get(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                var rpc = session.getRpc();

                var registered = rpc.tasks.register(params(SessionTasksRegisterParams.class, """
                        {"type":"client","clientTaskId":"client-task-1","description":"RPC task",
                         "cancellable":true,"displayName":"RPC Task"}
                        """)).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertTrue(registered.created());
                assertEquals("task-1", registered.task().id());
                assertEquals("RPC owner", registered.task().owner().displayName());

                var updated = rpc.tasks.update(params(SessionTasksUpdateParams.class, """
                        {"id":"task-1","sequence":1,
                         "update":{"kind":"progress","message":"Halfway","percentage":50,
                           "phase":"work","status":"running"}}
                        """)).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertTrue(updated.applied());
                assertEquals(1L, updated.task().sequence());

                rpc.tools.execute(params(SessionToolsExecuteParams.class, """
                        {"name":"rpc_tool","arguments":{"value":"input"},"toolCallId":"tool-call-1"}
                        """)).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                var descriptors = rpc.tools
                        .getBuiltinDescriptors(params(SessionToolsGetBuiltinDescriptorsParams.class, """
                                {"reduceUserIntervention":true,"includeAuthor":true,"skillEmbeddingEnabled":false,
                                 "shellConfig":{"displayName":"PowerShell","shellType":"powershell",
                                   "shellToolName":"shell","listShellsToolName":"list_shells",
                                   "readShellToolName":"read_shell","stopShellToolName":"stop_shell",
                                   "descriptionLines":["Runs shell commands."]},
                                 "shellSupportsPowerShell7Syntax":true,"shellTimeoutMs":1234,
                                 "backgroundTaskNotificationsEnabled":true}
                                """)).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertEquals("rpc_builtin", descriptors.tools().get(0).name());
                assertEquals(BuiltinToolInputSchemaType.OBJECT, descriptors.tools().get(0).inputSchema().type());

                rpc.tools.set(params(SessionToolsSetParams.class, """
                        {"tools":[{"name":"rpc_external","title":"RPC External",
                          "description":"External RPC tool","parameters":{"type":"object"},
                          "isTerminal":false,"overridesBuiltInTool":false,"skipPermission":true}]}
                        """)).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                var completion = rpc.tools
                        .taskCompleteEventData(params(SessionToolsTaskCompleteEventDataParams.class, """
                                {"arguments":{"objectiveId":17},
                                 "result":{"resultType":"success","textResultForLlm":"RPC task complete",
                                   "sessionLog":"Completion logged."}}
                                """)).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertEquals(17L, completion.objectiveId());
                assertEquals(TaskCompletionOutcome.COMPLETED, completion.outcome());
                assertTrue(completion.success());

                var workspace = rpc.workspaces.updateMetadata(params(SessionWorkspacesUpdateMetadataParams.class, """
                        {"context":{"owner":"rpc-test"},"name":"Updated RPC workspace"}
                        """)).get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertEquals("/rpc-workspace", workspace.path());
                assertEquals("Updated RPC workspace", workspace.workspace().name());
                assertEquals("RPC workspace", rpc.workspaces.ensure(params(SessionWorkspacesEnsureParams.class, """
                        {"context":{"owner":"rpc-test"}}
                        """)).get(TIMEOUT_SECONDS, TimeUnit.SECONDS).workspace().name());
                var stat = rpc.workspaces.statFile(new SessionWorkspacesStatFileParams(null, "folder/file.txt"))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertTrue(stat.isFile());
                assertEquals(42L, stat.size());

                rpc.workspaces.createDirectory(new SessionWorkspacesCreateDirectoryParams(null, "folder/nested", true))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                rpc.workspaces
                        .renamePath(
                                new SessionWorkspacesRenamePathParams(null, "folder/file.txt", "folder/renamed.txt"))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                rpc.workspaces.removePath(new SessionWorkspacesRemovePathParams(null, "folder", true, true))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
                assertEquals("RPC summary", rpc.workspaces
                        .addSummary(new SessionWorkspacesAddSummaryParams(null, "RPC summary", "Summary content"))
                        .get(TIMEOUT_SECONDS, TimeUnit.SECONDS).summary().get("title"));
                assertEquals("Truncated RPC workspace",
                        rpc.workspaces.truncateSummaries(new SessionWorkspacesTruncateSummariesParams(null, 2L))
                                .get(TIMEOUT_SECONDS, TimeUnit.SECONDS).workspace().name());

                assertCalledOnce(runtime, "session.tasks.register", "session.tasks.update", "session.tools.execute",
                        "session.tools.getBuiltinDescriptors", "session.tools.set",
                        "session.tools.taskCompleteEventData", "session.workspaces.updateMetadata",
                        "session.workspaces.ensure", "session.workspaces.statFile",
                        "session.workspaces.createDirectory", "session.workspaces.renamePath",
                        "session.workspaces.removePath", "session.workspaces.addSummary",
                        "session.workspaces.truncateSummaries");
                assertTrue(parameters(runtime, "session.tools.set").path("tools").get(0).path("skipPermission")
                        .asBoolean());
                assertTrue(parameters(runtime, "session.workspaces.removePath").path("force").asBoolean());
            }
        }
    }

    private static CopilotClient createClient(RpcSurfaceTestCli runtime) {
        var client = new CopilotClient(
                new CopilotClientOptions().setConnection(RuntimeConnection.forInProcess()).setUseLoggedInUser(false));
        client.setInProcessTransportFactory(options -> runtime.open());
        return client;
    }

    private static JsonNode handle(JsonNode request) {
        String method = request.path("method").asText();
        return switch (method) {
            case "connect" -> json("""
                    {"ok":true,"protocolVersion":3,"version":"rpc-surface-test"}
                    """);
            case "session.create" -> sessionCreate(request);
            case "session.detach" -> json("""
                    {"success":true}
                    """);
            case "commands.list" -> json("""
                    {"commands":[{"name":"rpc-command","description":"RPC command","aliases":["rpc"],
                      "allowDuringAgentExecution":true,"experimental":false,
                      "input":{"hint":"<value>","preserveMultilineInput":false,"required":true},
                      "kind":"builtin","schedulable":true}]}
                    """);
            case "hooks.discover" -> json("""
                    {"hooks":[],"warnings":["rpc-warning"],"errors":[]}
                    """);
            case "llmInference.setProvider" -> json("""
                    {"success":true}
                    """);
            case "managedSettings.read" -> json("""
                    {"settingsJson":{"policy":"strict"},"errorMessage":null}
                    """);
            case "mcp.planInstall" -> json("""
                    {"kind":"planned","plan":{"planHandle":"plan-1","planHandleExpiresAt":"2026-09-18T12:00:00Z",
                      "transportChoices":[],"configurationChanges":[],"reloadRequired":true,
                      "requiresInteractiveConfiguration":false},
                     "negotiated":{"runtimeProtocolVersion":3,"grantedCapabilities":[]}}
                    """);
            case "models.getBuiltInCatalog" -> json("""
                    {"models":[{"id":"built-in-model","name":"Built-in Model","family":"test-family"}]}
                    """);
            case "sessions.getClientMetadata" -> json("""
                    [{"status":"ok","sessionId":"persisted-session","metadata":{"rpc/key":"rpc-value"}}]
                    """);
            case "session.contentExclusion.checkPaths" -> json("""
                    {"available":true,"checks":[{"path":"/rpc-workspace/file.txt","excluded":false}]}
                    """);
            case "session.debug.collectLogs" -> json("""
                    {"kind":"directory","path":"/rpc-debug",
                     "entries":[{"bundlePath":"host/diagnostic.txt","sizeBytes":123,"source":"additional"}],
                     "skippedEntries":[{"bundlePath":"host/missing.txt","path":"/missing.txt","reason":"not found"}]}
                    """);
            case "session.history.clearContext" -> json("""
                    {"messagesCleared":4}
                    """);
            case "session.limitPrediction.predict" -> json("""
                    {"kind":"unavailable","reason":"auto_unresolved"}
                    """);
            case "session.metadata.getClientMetadata" -> json("""
                    {"rpc/key":"rpc-value","rpc/other":"other-value"}
                    """);
            case "session.model.setAllowedModels" -> json("""
                    {"allowedModels":["model-a","model-b"],"effectiveAllowedModels":["model-a"],
                     "fallbackModel":"model-a","modelId":"model-a"}
                    """);
            case "session.model.switchAutoTier" -> json("""
                    {"status":"pending","activatingAutoTier":"intelligence","effectiveAutoTier":"intelligence",
                     "pendingAutoTier":null,"supersededAutoTier":"balance"}
                    """);
            case "session.sandbox.getEnforcementStatus" -> json("""
                    {"required":true,"blocked":false,"reason":"managed-policy"}
                    """);
            case "session.sandbox.disableForSession" -> json("""
                    {"success":true,"enabled":false}
                    """);
            case "session.abort" -> json("""
                    {"success":true,"error":null}
                    """);
            case "session.interruptMainTurn" -> json("""
                    {"interrupted":true}
                    """);
            case "session.log" -> json("""
                    {"eventId":"11111111-2222-3333-4444-555555555555"}
                    """);
            case "session.factory.run", "session.factory.getRun" -> json("""
                    {"runId":"factory-run-1","status":"running","attempt":1,
                     "result":{"value":"running"},"snapshot":{"step":1}}
                    """);
            case "session.factory.resume" -> json("""
                    {"factoryName":"rpc-factory","run":{"runId":"factory-run-1","status":"running",
                      "attempt":2,"snapshot":{"step":3}}}
                    """);
            case "session.factory.pause" -> json("""
                    {"runId":"factory-run-1","status":"paused","attempt":1,
                     "reason":"caller requested pause","snapshot":{"step":2}}
                    """);
            case "session.factory.agent" -> json("""
                    {"result":{"answer":"agent-result"}}
                    """);
            case "session.factory.journal.get" -> json("""
                    {"hit":true,"resultJson":{"checkpoint":7}}
                    """);
            case "session.mcp.moveLoadingToBackground" -> json("""
                    {"movedToBackground":true}
                    """);
            case "session.mcp.oauth.respond" -> json("""
                    {"success":true}
                    """);
            case "session.mcp.resources.list" -> json("""
                    {"nextCursor":"resource-next","resources":[{"uri":"file://rpc/resource.txt",
                      "name":"RPC resource","description":"Resource description","mimeType":"text/plain",
                      "size":16,"title":"RPC Resource"}]}
                    """);
            case "session.mcp.resources.listTemplates" -> json("""
                    {"nextCursor":"template-next","resourceTemplates":[{"uriTemplate":"file://rpc/{name}",
                      "name":"RPC template","description":"Template description","mimeType":"text/plain",
                      "title":"RPC Template"}]}
                    """);
            case "session.mcp.resources.read" -> json("""
                    {"contents":[{"uri":"file://rpc/resource.txt","mimeType":"text/plain",
                      "text":"resource-content","blob":null,"_meta":{"audience":"assistant"}}]}
                    """);
            case "session.tasks.register" -> json("""
                    {"created":true,"reclaimed":false,"task":{"id":"task-1","type":"client",
                      "clientTaskId":"client-task-1","description":"RPC task","displayName":"RPC Task",
                      "activeStartedAt":"2026-09-18T12:00:00.500Z","activeTimeMs":500,"canCancel":true,
                      "executionMode":"background","owner":{"displayName":"RPC owner","joinId":"join-1",
                        "kind":"sdk","participantId":"participant-1","presence":"connected","source":"rpc-test"},
                      "sequence":0,"status":"running","startedAt":"2026-09-18T12:00:00Z",
                      "updatedAt":"2026-09-18T12:00:01Z"}}
                    """);
            case "session.tasks.update" -> json("""
                    {"applied":true,"duplicate":false,"task":{"id":"task-1","type":"client",
                      "clientTaskId":"client-task-1","description":"RPC task","displayName":"RPC Task",
                      "activeTimeMs":500,"canCancel":true,"executionMode":"background",
                      "owner":{"displayName":"RPC owner","joinId":"join-1","kind":"sdk",
                        "participantId":"participant-1","presence":"connected","source":"rpc-test"},
                      "sequence":1,"status":"running","startedAt":"2026-09-18T12:00:00Z",
                      "updatedAt":"2026-09-18T12:00:01Z"}}
                    """);
            case "session.tools.getBuiltinDescriptors" -> json("""
                    {"tools":[{"name":"rpc_builtin","description":"RPC built-in tool",
                      "hasSummariseIntention":true,"inputSchema":{"type":"object"},
                      "instructions":"Use the RPC built-in.","isTerminal":false,"safeForTelemetry":true,
                      "title":"RPC Built-in","type":"test"}]}
                    """);
            case "session.tools.taskCompleteEventData" -> json("""
                    {"objectiveId":17,"outcome":"completed","reason":"completed","success":true,
                     "summary":"RPC task complete"}
                    """);
            case "session.workspaces.updateMetadata" -> workspace("Updated RPC workspace");
            case "session.workspaces.ensure" -> workspace("RPC workspace");
            case "session.workspaces.statFile" -> json("""
                    {"birthtimeMs":1000,"isDirectory":false,"isFile":true,"mtimeMs":2000,"size":42}
                    """);
            case "session.workspaces.addSummary" -> json("""
                    {"summary":{"number":3,"title":"RPC summary","content":"Summary content"},
                     "workspace":{"id":"workspace-1","cwd":"/rpc-workspace","name":"RPC workspace"}}
                    """);
            case "session.workspaces.truncateSummaries" -> workspace("Truncated RPC workspace");
            default -> MAPPER.createObjectNode();
        };
    }

    private static JsonNode workspace(String name) {
        return json("""
                {"path":"/rpc-workspace","workspace":{"id":"workspace-1","cwd":"/rpc-workspace",
                  "git_root":"/rpc-workspace","branch":"rpc-branch","name":"%s","client_name":"rpc-client",
                  "created_at":"2026-09-18T11:00:00Z","remote_steerable":true}}
                """.formatted(name));
    }

    private static JsonNode sessionCreate(JsonNode request) {
        var result = MAPPER.createObjectNode();
        result.put("sessionId", request.path("params").path("sessionId").asText());
        result.put("workspacePath", "/rpc-workspace");
        result.putNull("capabilities");
        return result;
    }

    private static void collectTargets(Object instance, String prefix, Map<Class<?>, RpcTarget> targets,
            IdentityHashMap<Object, Boolean> visited) throws IllegalAccessException {
        if (visited.put(instance, Boolean.TRUE) != null) {
            return;
        }
        targets.put(instance.getClass(), new RpcTarget(instance, prefix));
        for (var field : instance.getClass().getFields()) {
            if (field.getType().getPackageName().equals(ServerRpc.class.getPackageName())
                    && field.getType().getSimpleName().endsWith("Api")) {
                var child = field.get(instance);
                var childPrefix = prefix.isEmpty() ? field.getName() : prefix + "." + field.getName();
                collectTargets(child, childPrefix, targets, visited);
            }
        }
    }

    private static List<Method> rpcMethods(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods()).filter(method -> Modifier.isPublic(method.getModifiers()))
                .filter(method -> method.getReturnType() == CompletableFuture.class)
                .sorted(Comparator.comparing(RpcSurfaceParityE2ETest::signature)).toList();
    }

    private static Object invoke(Method method, Object instance, Object[] arguments) {
        try {
            return method.invoke(instance, arguments);
        } catch (IllegalAccessException e) {
            throw new AssertionError("Could not invoke " + signature(method), e);
        } catch (InvocationTargetException e) {
            throw new AssertionError("Generated wrapper failed before dispatch for " + signature(method), e.getCause());
        }
    }

    private static Object fixture(Class<?> type) {
        if (type == String.class) {
            return "caller-supplied-session";
        }
        if (type == boolean.class || type == Boolean.class) {
            return true;
        }
        if (type == byte.class || type == Byte.class) {
            return (byte) 1;
        }
        if (type == short.class || type == Short.class) {
            return (short) 1;
        }
        if (type == int.class || type == Integer.class) {
            return 1;
        }
        if (type == long.class || type == Long.class) {
            return 1L;
        }
        if (type == float.class || type == Float.class) {
            return 1F;
        }
        if (type == double.class || type == Double.class) {
            return 1D;
        }
        if (type.isEnum()) {
            return type.getEnumConstants()[0];
        }
        if (JsonNode.class.isAssignableFrom(type)) {
            return MAPPER.createObjectNode().put("fixture", true);
        }
        if (List.class.isAssignableFrom(type)) {
            return List.of();
        }
        if (Map.class.isAssignableFrom(type)) {
            return Map.of();
        }
        if (type == Object.class) {
            return Map.of("fixture", "value");
        }
        if (type.isRecord()) {
            try {
                RecordComponent[] components = type.getRecordComponents();
                var constructor = type.getDeclaredConstructor(
                        Arrays.stream(components).map(RecordComponent::getType).toArray(Class<?>[]::new));
                var arguments = Arrays.stream(components).map(component -> fixtureComponent(component.getType()))
                        .toArray();
                return constructor.newInstance(arguments);
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("Could not construct RPC params " + type.getName(), e);
            }
        }
        var subTypes = type.getAnnotation(com.fasterxml.jackson.annotation.JsonSubTypes.class);
        if (subTypes != null && subTypes.value().length > 0) {
            return fixture(subTypes.value()[0].value());
        }
        if (!Modifier.isAbstract(type.getModifiers()) && !type.isInterface()) {
            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("Could not construct RPC params " + type.getName(), e);
            }
        }
        throw new AssertionError("Unmapped generated RPC parameter type " + type.getName());
    }

    private static Object fixtureComponent(Class<?> type) {
        if (Map.class.isAssignableFrom(type)) {
            return null;
        }
        if (type.isPrimitive() || type == String.class || Number.class.isAssignableFrom(type) || type == Boolean.class
                || type.isEnum() || List.class.isAssignableFrom(type) || type == Object.class
                || JsonNode.class.isAssignableFrom(type)) {
            return fixture(type);
        }
        return null;
    }

    private static RpcSurfaceTestCli.RecordingCaller.Call assertSingleCall(RpcSurfaceTestCli.RecordingCaller caller,
            String signature) {
        assertEquals(1, caller.calls().size(), signature);
        return caller.calls().get(0);
    }

    private static String signature(Method method) {
        return method.getDeclaringClass().getSimpleName() + "#" + method.getName() + "("
                + Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName).collect(Collectors.joining(","))
                + ")";
    }

    private static String sha256(String value) {
        try {
            return java.util.HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    private record RpcTarget(Object instance, String prefix) {
    }

    private record TargetMethod(RpcTarget target, Method method) {

        String signature() {
            return RpcSurfaceParityE2ETest.signature(method);
        }
    }

    private static <T> T params(Class<T> type, String json) {
        try {
            return MAPPER.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new AssertionError("Invalid test parameters for " + type.getSimpleName(), e);
        }
    }

    private static JsonNode json(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException e) {
            throw new AssertionError("Invalid fake runtime JSON", e);
        }
    }

    private static JsonNode parameters(RpcSurfaceTestCli runtime, String method) {
        return runtime.request(method).path("params");
    }

    private static void assertCalledOnce(RpcSurfaceTestCli runtime, String... methods) {
        for (String method : methods) {
            assertEquals(1, runtime.requestCount(method), "Unexpected call count for " + method);
            if (method.startsWith("session.")) {
                assertFalse(parameters(runtime, method).path("sessionId").asText().isBlank(),
                        "Expected sessionId for " + method);
            }
        }
    }
}
