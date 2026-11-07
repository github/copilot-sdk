/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.generated.rpc;

import static org.junit.jupiter.api.Assertions.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.AllowCopilotExperimental;
import com.github.copilot.TestUtil;
import com.github.copilot.generated.PolicyHelperCapabilities;
import com.github.copilot.generated.PolicyHelperFailure;
import com.github.copilot.generated.PolicyHelperFailureDetail;
import com.github.copilot.generated.PolicyHelperHelpLink;
import com.github.copilot.generated.SessionManagedSettingsResolvedEvent.SessionManagedSettingsResolvedEventData;

/**
 * Tests for generated RPC param and result record types. Exercises
 * constructors, field accessors, and enum variants to provide JaCoCo coverage
 * of the generated code without requiring network access.
 */
class GeneratedRpcRecordsCoverageTest {

    static Stream<Arguments> customizationLegacyConstructors() {
        return Stream.of(
                Arguments.of(
                        new AgentInfo("agent", "Agent", "Description", "/agent.md", "agent-id", null, true, false,
                                List.of("view"), "model", null, null, null, null, null, "Prompt", null),
                        """
                                {"name":"agent","displayName":"Agent","description":"Description","path":"/agent.md",
                                 "id":"agent-id","userInvocable":true,"disableModelInvocation":false,
                                 "tools":["view"],"model":"model","prompt":"Prompt"}
                                """, "{\"pluginId\":\"builtin:tools\"}"),
                Arguments.of(new DiscoveredExtensionPlugin("tools"), "{\"name\":\"tools\"}",
                        "{\"id\":\"builtin:tools\"}"),
                Arguments.of(
                        new DiscoveredHook("hook", HookType.PRETOOLUSE, HookOrigin.PLUGIN, "tools", "/project", true,
                                "disable-hook"),
                        """
                                {"id":"hook","hookType":"preToolUse","origin":"plugin","source":"tools",
                                 "projectPath":"/project","enabled":true,"disableKey":"disable-hook"}
                                """, "{\"pluginId\":\"builtin:tools\",\"definitionFileUri\":\"file:///hooks.json\"}"),
                Arguments.of(new InstalledPluginInfo("tools", "catalog", null, "1.0", true, "/catalog", "source", false,
                        null, true, "Description", null, "https://example.com/icon"), """
                                {"name":"tools","marketplace":"catalog","version":"1.0","enabled":true,
                                 "installedFrom":"/catalog","source":"source","managed":false,"installed":true,
                                 "description":"Description","iconUrl":"https://example.com/icon"}
                                """, "{\"id\":\"mp:catalog::tools\",\"rootPath\":\"/plugins/tools\"}"),
                Arguments.of(
                        new InstructionSource("instruction", "Instructions", "/instructions.md", "Content",
                                InstructionSourceType.PLUGIN, InstructionSourceLocation.PLUGIN, List.of("*.ts"),
                                "Description", false, "/project"),
                        """
                                {"id":"instruction","label":"Instructions","sourcePath":"/instructions.md","content":"Content",
                                 "type":"plugin","location":"plugin","applyTo":["*.ts"],"description":"Description",
                                 "defaultDisabled":false,"projectPath":"/project"}
                                """,
                        "{\"pluginId\":\"builtin:tools\"}"),
                Arguments.of(new Plugin("tools", "catalog", "1.0", true, null, "/catalog", "source", false, null, true),
                        """
                                {"name":"tools","marketplace":"catalog","version":"1.0","enabled":true,
                                 "installedFrom":"/catalog","source":"source","managed":false,"installed":true}
                                """, "{\"id\":\"mp:catalog::tools\",\"rootPath\":\"/plugins/tools\"}"),
                Arguments.of(
                        new ServerSkill("skill", "command", "Description", SkillSource.PLUGIN, true, true, "/SKILL.md",
                                "/project", "argument"),
                        """
                                {"name":"skill","commandName":"command","description":"Description","source":"plugin",
                                 "userInvocable":true,"enabled":true,"path":"/SKILL.md","projectPath":"/project",
                                 "argumentHint":"argument"}
                                """, "{\"pluginId\":\"builtin:tools\"}"),
                Arguments.of(
                        new Skill("skill", "declared", "command", "Description", SkillSource.PLUGIN, null, true, true,
                                "/SKILL.md", "tools", "argument"),
                        """
                                {"name":"skill","declaredName":"declared","commandName":"command","description":"Description",
                                 "source":"plugin","userInvocable":true,"enabled":true,"path":"/SKILL.md",
                                 "pluginName":"tools","argumentHint":"argument"}
                                """,
                        "{\"pluginId\":\"builtin:tools\"}"));
    }

    @ParameterizedTest
    @MethodSource("customizationLegacyConstructors")
    void customization_records_preserve_legacy_constructors_and_round_trip_metadata(Object legacy, String expectedJson,
            String metadataJson) throws Exception {
        var mapper = new ObjectMapper();
        var expected = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(expectedJson);
        assertEquals(expected, mapper.valueToTree(legacy));
        assertEquals(legacy, mapper.treeToValue(expected, legacy.getClass()));
        expected.setAll((com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(metadataJson));
        assertEquals(expected, mapper.valueToTree(mapper.treeToValue(expected, legacy.getClass())));
    }

    @Test
    void ignoredScriptSafetyAccessorsAreDeprecated() throws Exception {
        for (var recordType : List.of(SessionOpenOptions.class, SessionOptionsUpdateParams.class)) {
            assertTrue(recordType.getMethod("enableScriptSafety").isAnnotationPresent(Deprecated.class),
                    () -> recordType.getSimpleName() + ".enableScriptSafety must be deprecated");
            assertFalse(recordType.getMethod("shell").isAnnotationPresent(Deprecated.class),
                    () -> recordType.getSimpleName() + ".shell must remain supported");
        }
    }

    @Test
    @AllowCopilotExperimental
    void policyHelper_snapshots_and_events_share_dto_types() throws Exception {
        var mapper = new ObjectMapper();
        var json = mapper.readTree("""
                {"policyHelperFailure":{"category":"helper-reported","failureMode":"ignore",
                "severity":"warning","reason":"sessionStart","previousPolicyRetained":false,
                "message":"Invalid configuration","details":[{"path":"setting","message":"Invalid value"}],
                "helpLink":{"url":"https://example.com/help"}},
                "policyHelperCapabilities":{"structuredFailure":true,"recheck":true}}
                """);
        var event = mapper.treeToValue(json, SessionManagedSettingsResolvedEventData.class);
        var snapshot = mapper.treeToValue(json, ManagedSettingsResolvedData.class);
        var get = mapper.treeToValue(json, SessionManagedSettingsGetResult.class);
        var recheck = mapper.treeToValue(json, SessionManagedSettingsGetResult.class);
        List<PolicyHelperFailure> failures = List.of(event.policyHelperFailure(), snapshot.policyHelperFailure(),
                get.policyHelperFailure(), recheck.policyHelperFailure());
        List<PolicyHelperCapabilities> capabilities = List.of(event.policyHelperCapabilities(),
                snapshot.policyHelperCapabilities(), get.policyHelperCapabilities(),
                recheck.policyHelperCapabilities());
        for (PolicyHelperFailure failure : failures) {
            assertEquals(event.policyHelperFailure(), failure);
            PolicyHelperFailureDetail detail = failure.details().get(0);
            PolicyHelperHelpLink link = failure.helpLink();
            assertEquals("setting", detail.path());
            assertEquals("https://example.com/help", link.url());
        }
        for (PolicyHelperCapabilities capability : capabilities) {
            assertEquals(event.policyHelperCapabilities(), capability);
        }
    }

    @Test
    @AllowCopilotExperimental
    void managedSettingsResolve_preserves_legacy_constructors() throws Exception {
        var params = new ManagedSettingsResolveParams("account", null, "app", "/workspace");
        assertEquals("account", params.selectionId());
        assertEquals("app", params.clientName());
        assertEquals("/workspace", params.workingDirectory());
        var mapper = new ObjectMapper();
        var json = mapper.valueToTree(params);
        assertEquals("/workspace", json.get("workingDirectory").asText());
        assertFalse(json.has("policyHelperReason"));
        assertEquals(params, mapper.treeToValue(json, ManagedSettingsResolveParams.class));
        var withoutDirectory = new ManagedSettingsResolveParams("account", null, "app");
        assertNull(withoutDirectory.workingDirectory());
    }

    @Test
    @AllowCopilotExperimental
    void managedPermissions_reuses_event_context_and_evaluated_operations() throws Exception {
        var mapper = new ObjectMapper();
        var eventJson = mapper.readTree("""
                {"permissionsContext":{"permissions":{"_limitToLists":[["https://example.com"]]},"failClosed":false}}
                """);
        var eventData = mapper.treeToValue(eventJson, SessionManagedSettingsResolvedEventData.class);
        var evaluation = mapper.readValue("""
                {"operation":{"kind":"url","url":"https://example.com/a"},"verdict":"allow"}
                """, ManagedPermissionEvaluation.class);
        var params = ManagedSettingsPermissionsEvaluateParams.builder().context(eventData.permissionsContext())
                .operations(List.of(evaluation.operation())).build();
        var paramsJson = mapper.valueToTree(params);
        assertEquals(eventJson.get("permissionsContext"), paramsJson.get("context"));
        assertEquals("url", paramsJson.get("operations").get(0).get("kind").asText());
        assertEquals("https://example.com/a", paramsJson.get("operations").get(0).get("url").asText());
        assertEquals(ManagedPermissionOperation.ManagedPermissionOperationKind.URL, evaluation.operation().kind());
        assertEquals(params, mapper.treeToValue(paramsJson, ManagedSettingsPermissionsEvaluateParams.class));
    }

    // ── Params records ─────────────────────────────────────────────────────

    @Test
    @AllowCopilotExperimental
    void sessionEventLogRead_preserves_legacy_constructor_and_wire_fields() throws Exception {
        var mapper = new ObjectMapper();
        var params = new SessionEventLogReadParams("session", "cursor", 100L, 1234L, List.of("user.message"),
                EventsAgentScope.PRIMARY, List.of("agent"), EventsReadDirection.BACKWARD, false);
        var expected = mapper.readTree("""
                {"sessionId":"session","cursor":"cursor","max":100,"waitMs":1234,
                 "types":["user.message"],"agentScope":"primary","agentIds":["agent"],
                 "direction":"backward","includeEphemeral":false}
                """);

        assertEquals(expected, mapper.readTree(mapper.writeValueAsString(params)));
        assertEquals(params, mapper.treeToValue(expected, SessionEventLogReadParams.class));
    }

    @Test
    void pingParams_record() {
        var params = PingParams.builder().message("hello").build();
        assertEquals("hello", params.message());
        assertNull(PingParams.builder().message(null).build().message());
    }

    @Test
    void pingResult_record() {
        var timestamp = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        var result = PingResult.builder().message("pong").timestamp(timestamp).protocolVersion(2L).build();
        assertEquals("pong", result.message());
        assertEquals(timestamp, result.timestamp());
        assertEquals(2L, result.protocolVersion());
    }

    @Test
    void mcpDiscoverParams_record() {
        var params = McpDiscoverParams.builder().workingDirectory("/workspace").includeEffectiveSource(null).build();
        assertEquals("/workspace", params.workingDirectory());
        assertNull(McpDiscoverParams.builder().workingDirectory(null).includeEffectiveSource(null).build()
                .workingDirectory());
    }

    @Test
    void mcpConfigRemoveParams_record() {
        var params = McpConfigRemoveParams.builder().name("old-server").authClientIdMetadataUrl(null).build();
        assertEquals("old-server", params.name());
    }

    @Test
    void mcpConfigUpdateParams_record() {
        var params = McpConfigUpdateParams.builder().name("my-server").config(Map.of("key", "val")).build();
        assertEquals("my-server", params.name());
        assertNotNull(params.config());
    }

    @Test
    void toolsListParams_record() {
        var params = ToolsListParams.builder().model("gpt-5").build();
        assertEquals("gpt-5", params.model());
        assertNull(ToolsListParams.builder().model(null).build().model());
    }

    @Test
    void sessionsForkParams_record() {
        var params = SessionsForkParams.builder().sessionId("sess-1").toEventId("event-abc").name(null).build();
        assertEquals("sess-1", params.sessionId());
        assertEquals("event-abc", params.toEventId());
    }

    @Test
    void sessionAgentDeselectParams_record() {
        var params = SessionAgentDeselectParams.builder().sessionId("sess-1").build();
        assertEquals("sess-1", params.sessionId());
    }

    @Test
    void sessionAgentGetCurrentParams_record() {
        var params = SessionAgentGetCurrentParams.builder().sessionId("sess-2").build();
        assertEquals("sess-2", params.sessionId());
    }

    @Test
    void sessionAgentListParams_record() {
        var params = SessionAgentListParams.builder().sessionId("sess-3").includeBuiltInAgents(null).includePrompt(null)
                .build();
        assertEquals("sess-3", params.sessionId());
    }

    @Test
    void sessionAgentReloadParams_record() {
        var params = SessionAgentReloadParams.builder().sessionId("sess-4").build();
        assertEquals("sess-4", params.sessionId());
    }

    @Test
    void sessionAgentSelectParams_record() {
        var params = SessionAgentSelectParams.builder().sessionId("sess-5").name("my-agent").build();
        assertEquals("sess-5", params.sessionId());
        assertEquals("my-agent", params.name());
    }

    @Test
    void sessionCommandsHandlePendingCommandParams_record() {
        var params = SessionCommandsHandlePendingCommandParams.builder().sessionId("sess-6").requestId("req-cmd")
                .error("error msg").build();
        assertEquals("sess-6", params.sessionId());
        assertEquals("req-cmd", params.requestId());
        assertEquals("error msg", params.error());
    }

    @Test
    void sessionExtensionsDisableParams_record() {
        var params = SessionExtensionsDisableParams.builder().sessionId("sess-7").id("ext-id-1").build();
        assertEquals("sess-7", params.sessionId());
        assertEquals("ext-id-1", params.id());
    }

    @Test
    void sessionExtensionsEnableParams_record() {
        var params = SessionExtensionsEnableParams.builder().sessionId("sess-8").id("ext-id-2").build();
        assertEquals("sess-8", params.sessionId());
        assertEquals("ext-id-2", params.id());
    }

    @Test
    void sessionExtensionsListParams_record() {
        var params = SessionExtensionsListParams.builder().sessionId("sess-9").build();
        assertEquals("sess-9", params.sessionId());
    }

    @Test
    void sessionExtensionsReloadParams_record() {
        var params = SessionExtensionsReloadParams.builder().sessionId("sess-10").build();
        assertEquals("sess-10", params.sessionId());
    }

    @Test
    void sessionFleetStartParams_record() {
        var params = SessionFleetStartParams.builder().sessionId("sess-11").prompt("fix all bugs").attachments(null)
                .billable(null).wait_(null).build();
        assertEquals("sess-11", params.sessionId());
        assertEquals("fix all bugs", params.prompt());
    }

    @Test
    void sessionFsAppendFileParams_record() {
        var params = SessionFsAppendFileParams.builder().sessionId("sess-12").path(TestUtil.tempPath("log.txt"))
                .content("new line\n").mode(null).build();
        assertEquals("sess-12", params.sessionId());
        assertEquals(TestUtil.tempPath("log.txt"), params.path());
        assertEquals("new line\n", params.content());
        assertNull(params.mode());
    }

    @Test
    void sessionFsExistsParams_record() {
        var params = SessionFsExistsParams.builder().sessionId("sess-13").path(TestUtil.tempPath("file.txt")).build();
        assertEquals("sess-13", params.sessionId());
        assertEquals(TestUtil.tempPath("file.txt"), params.path());
    }

    @Test
    void sessionFsMkdirParams_record() {
        var params = SessionFsMkdirParams.builder().sessionId("sess-14").path(TestUtil.tempPath("newdir"))
                .recursive(true).mode(null).build();
        assertEquals("sess-14", params.sessionId());
        assertEquals(TestUtil.tempPath("newdir"), params.path());
        assertTrue(params.recursive());
        assertNull(params.mode());
    }

    @Test
    void sessionFsReadFileParams_record() {
        var params = SessionFsReadFileParams.builder().sessionId("sess-15").path("/src/Main.java").build();
        assertEquals("sess-15", params.sessionId());
        assertEquals("/src/Main.java", params.path());
    }

    @Test
    void sessionFsReaddirParams_record() {
        var params = SessionFsReaddirParams.builder().sessionId("sess-16").path("/src").build();
        assertEquals("sess-16", params.sessionId());
        assertEquals("/src", params.path());
    }

    @Test
    void sessionFsReaddirWithTypesParams_record() {
        var params = SessionFsReaddirWithTypesParams.builder().sessionId("sess-17").path("/src").build();
        assertEquals("sess-17", params.sessionId());
        assertEquals("/src", params.path());
    }

    @Test
    void sessionFsRenameParams_record() {
        var params = SessionFsRenameParams.builder().sessionId("sess-18").src("/old.txt").dest("/new.txt").build();
        assertEquals("sess-18", params.sessionId());
        assertEquals("/old.txt", params.src());
        assertEquals("/new.txt", params.dest());
    }

    @Test
    void sessionFsRmParams_record() {
        var params = SessionFsRmParams.builder().sessionId("sess-19").path(TestUtil.tempPath("file.txt"))
                .recursive(false).force(true).build();
        assertEquals("sess-19", params.sessionId());
        assertEquals(TestUtil.tempPath("file.txt"), params.path());
        assertFalse(params.recursive());
        assertTrue(params.force());
    }

    @Test
    void sessionFsSetProviderParams_conventions_enum() {
        assertEquals("windows", SessionFsSetProviderConventions.WINDOWS.getValue());
        assertEquals("posix", SessionFsSetProviderConventions.POSIX.getValue());
        assertEquals(SessionFsSetProviderConventions.POSIX, SessionFsSetProviderConventions.fromValue("posix"));
        assertThrows(IllegalArgumentException.class, () -> SessionFsSetProviderConventions.fromValue("unknown"));
    }

    @Test
    void sessionFsSetProviderCapabilities_preserves_legacy_constructor_and_binary_wire_field() throws Exception {
        var mapper = new ObjectMapper();
        var legacy = new SessionFsSetProviderCapabilities(true);
        var legacyJson = mapper.readTree(mapper.writeValueAsString(legacy));
        assertTrue(legacyJson.get("sqlite").asBoolean());
        assertFalse(legacyJson.has("binary"));
        assertEquals(legacy, mapper.treeToValue(legacyJson, SessionFsSetProviderCapabilities.class));

        var binary = SessionFsSetProviderCapabilities.builder().sqlite(false).binary(true).build();
        var binaryJson = mapper.readTree(mapper.writeValueAsString(binary));
        assertFalse(binaryJson.get("sqlite").asBoolean());
        assertTrue(binaryJson.get("binary").asBoolean());
        assertEquals(binary, mapper.treeToValue(binaryJson, SessionFsSetProviderCapabilities.class));
    }

    @Test
    @AllowCopilotExperimental
    void sessionFsReadFileBytes_preserves_base64_wire_content() throws Exception {
        var mapper = new ObjectMapper();
        var params = SessionFsReadFileBytesParams.builder().sessionId("sess-15").path("/image.png").build();
        var paramsJson = mapper.readTree(mapper.writeValueAsString(params));
        assertEquals("sess-15", paramsJson.get("sessionId").asText());
        assertEquals("/image.png", paramsJson.get("path").asText());
        assertEquals(params, mapper.treeToValue(paramsJson, SessionFsReadFileBytesParams.class));

        var result = SessionFsReadFileBytesResult.builder().content("AAECAw==").error(null).build();
        var resultJson = mapper.readTree(mapper.writeValueAsString(result));
        assertEquals("AAECAw==", resultJson.get("content").asText());
        assertFalse(resultJson.has("error"));
        assertEquals(result, mapper.treeToValue(resultJson, SessionFsReadFileBytesResult.class));
    }

    @Test
    @AllowCopilotExperimental
    void sessionFsWriteFileBytes_preserves_base64_wire_content_and_mode() throws Exception {
        var mapper = new ObjectMapper();
        var params = SessionFsWriteFileBytesParams.builder().sessionId("sess-15").path("/image.png").content("AP/+AQ==")
                .mode(384L).build();
        var json = mapper.readTree(mapper.writeValueAsString(params));
        assertEquals("sess-15", json.get("sessionId").asText());
        assertEquals("/image.png", json.get("path").asText());
        assertEquals("AP/+AQ==", json.get("content").asText());
        assertEquals(384L, json.get("mode").asLong());
        assertEquals(params, mapper.treeToValue(json, SessionFsWriteFileBytesParams.class));
    }

    @Test
    void sessionFsStatParams_record() {
        var params = SessionFsStatParams.builder().sessionId("sess-20").path("/etc/hosts").build();
        assertEquals("sess-20", params.sessionId());
        assertEquals("/etc/hosts", params.path());
    }

    @Test
    void sessionFsWriteFileParams_record() {
        var params = SessionFsWriteFileParams.builder().sessionId("sess-21").path(TestUtil.tempPath("out.txt"))
                .content("content here").mode(null).build();
        assertEquals("sess-21", params.sessionId());
        assertEquals(TestUtil.tempPath("out.txt"), params.path());
        assertEquals("content here", params.content());
        assertNull(params.mode());
    }

    @Test
    void sessionHistoryCompactParams_record() {
        var params = SessionHistoryCompactParams.builder().sessionId("sess-22")
                .customInstructions("focus on the API surface")
                .trigger(SessionHistoryCompactParams.SessionHistoryCompactParamsTrigger.MANUAL).tokenLimit(4096L)
                .build();
        assertEquals("sess-22", params.sessionId());
        assertEquals("focus on the API surface", params.customInstructions());
        assertEquals(SessionHistoryCompactParams.SessionHistoryCompactParamsTrigger.MANUAL, params.trigger());
        assertEquals(4096L, params.tokenLimit());
    }

    @Test
    void sessionHistoryTruncateParams_record() {
        var params = SessionHistoryTruncateParams.builder().sessionId("sess-23").eventId("event-id-xyz").build();
        assertEquals("sess-23", params.sessionId());
        assertEquals("event-id-xyz", params.eventId());
    }

    @Test
    void sessionLogParams_record() {
        var params = SessionLogParams.builder().sessionId("sess-24").message("test message").level(SessionLogLevel.INFO)
                .type(null).ephemeral(false).url(null).tip(null).build();
        assertEquals("sess-24", params.sessionId());
        assertEquals("test message", params.message());
        assertEquals(SessionLogLevel.INFO, params.level());
        assertFalse(params.ephemeral());
        assertNull(params.url());
    }

    @Test
    void sessionLogParams_level_enum_all_values() {
        for (var level : SessionLogLevel.values()) {
            assertNotNull(level.getValue());
            assertEquals(level, SessionLogLevel.fromValue(level.getValue()));
        }
    }

    @Test
    void sessionMcpDisableParams_record() {
        var params = SessionMcpDisableParams.builder().sessionId("sess-25").serverName("mcp-server-1").build();
        assertEquals("sess-25", params.sessionId());
        assertEquals("mcp-server-1", params.serverName());
    }

    @Test
    void sessionMcpEnableParams_record() {
        var params = SessionMcpEnableParams.builder().sessionId("sess-26").serverName("mcp-server-2").build();
        assertEquals("sess-26", params.sessionId());
        assertEquals("mcp-server-2", params.serverName());
    }

    @Test
    void sessionMcpReloadParams_record() {
        var params = SessionMcpReloadParams.builder().sessionId("sess-28").build();
        assertEquals("sess-28", params.sessionId());
    }

    @Test
    void sessionModeGetParams_record() {
        var params = SessionModeGetParams.builder().sessionId("sess-29").build();
        assertEquals("sess-29", params.sessionId());
    }

    @Test
    void sessionModeSetParams_record() {
        var params = SessionModeSetParams.builder().sessionId("sess-30").mode(SessionMode.PLAN).expectedMode(null)
                .inheritPlanBaseFromSessionId(null).planModelConfigured(null).planModel(null).planModelProviderId(null)
                .planReasoningEffort(null).planContextTier(null).compactionDecision(null).restorePlanModel(null)
                .persistPlanSelection(null).pickerSettingsContext(null).planExitAction(null).build();
        assertEquals("sess-30", params.sessionId());
        assertEquals(SessionMode.PLAN, params.mode());
    }

    @Test
    void sessionModeSetParams_mode_enum() {
        assertEquals("interactive", SessionMode.INTERACTIVE.getValue());
        assertEquals("plan", SessionMode.PLAN.getValue());
        assertEquals("autopilot", SessionMode.AUTOPILOT.getValue());
        for (var mode : SessionMode.values()) {
            assertEquals(mode, SessionMode.fromValue(mode.getValue()));
        }
        assertThrows(IllegalArgumentException.class, () -> SessionMode.fromValue("unknown-mode"));
    }

    @Test
    void sessionModelGetCurrentParams_record() {
        var params = SessionModelGetCurrentParams.builder().sessionId("sess-31").build();
        assertEquals("sess-31", params.sessionId());
    }

    @Test
    void sessionModelSetAllowedModelsParams_round_trips_exact_ids() throws Exception {
        var mapper = new ObjectMapper();
        var allowedModels = List.of("gpt-5", "azure/gpt-5");
        var params = SessionModelSetAllowedModelsParams.builder().sessionId("sess-model-policy")
                .allowedModels(allowedModels).build();

        assertEquals("sess-model-policy", params.sessionId());
        assertEquals(allowedModels, params.allowedModels());
        var json = mapper.readTree(mapper.writeValueAsString(params));
        assertEquals("sess-model-policy", json.get("sessionId").asText());
        assertEquals(mapper.valueToTree(allowedModels), json.get("allowedModels"));
        assertEquals(params, mapper.treeToValue(json, SessionModelSetAllowedModelsParams.class));
    }

    @Test
    void sessionModelSetAllowedModelsParams_distinguishes_clearing_from_empty_list() throws Exception {
        var mapper = new ObjectMapper();
        var cleared = SessionModelSetAllowedModelsParams.builder().sessionId("sess-model-policy").allowedModels(null)
                .build();

        var untouched = SessionModelSetAllowedModelsParams.builder().sessionId("sess-model-policy").build();
        assertEquals(untouched, mapper.readValue("""
                {"sessionId":"sess-model-policy"}
                """, SessionModelSetAllowedModelsParams.class));
        assertEquals(cleared, mapper.readValue("""
                {"sessionId":"sess-model-policy","allowedModels":null}
                """, SessionModelSetAllowedModelsParams.class));
        assertNotEquals(untouched, cleared);
        var clearedJson = mapper.readTree(mapper.writeValueAsString(cleared));
        assertTrue(clearedJson.has("allowedModels"));
        assertTrue(clearedJson.get("allowedModels").isNull());
        assertFalse(mapper.valueToTree(untouched).has("allowedModels"));

        var empty = SessionModelSetAllowedModelsParams.builder().sessionId("sess-model-policy").allowedModels(List.of())
                .build();
        var emptyJson = mapper.readTree(mapper.writeValueAsString(empty));
        assertTrue(emptyJson.get("allowedModels").isArray());
        assertTrue(emptyJson.get("allowedModels").isEmpty());
        assertEquals(empty, mapper.treeToValue(emptyJson, SessionModelSetAllowedModelsParams.class));
    }

    @Test
    void sessionModelSetAllowedModelsResult_round_trips_policy_and_selection() throws Exception {
        var mapper = new ObjectMapper();
        var json = """
                {"allowedModels":["gpt-5","azure/gpt-5"],"effectiveAllowedModels":["gpt-5"],
                 "fallbackModel":"gpt-5","modelId":"gpt-5"}
                """;

        var result = mapper.readValue(json, SessionModelSetAllowedModelsResult.class);

        assertEquals(List.of("gpt-5", "azure/gpt-5"), result.allowedModels());
        assertEquals(List.of("gpt-5"), result.effectiveAllowedModels());
        assertEquals("gpt-5", result.fallbackModel());
        assertEquals("gpt-5", result.modelId());
        assertEquals(mapper.readTree(json), mapper.valueToTree(result));
    }

    @Test
    void sessionModelSetAllowedModelsResult_all_fields_are_optional() throws Exception {
        var mapper = new ObjectMapper();
        var empty = mapper.readValue("{}", SessionModelSetAllowedModelsResult.class);
        assertNull(empty.allowedModels());
        assertNull(empty.effectiveAllowedModels());
        assertNull(empty.fallbackModel());
        assertNull(empty.modelId());

        for (var json : List.of("{}", """
                {"modelId":"gpt-5"}
                """, """
                {"allowedModels":["gpt-5"]}
                """, """
                {"effectiveAllowedModels":[],"fallbackModel":"gpt-5"}
                """)) {
            var result = mapper.readValue(json, SessionModelSetAllowedModelsResult.class);
            assertEquals(mapper.readTree(json), mapper.valueToTree(result));
        }
    }

    @Test
    void sessionModelSwitchToParams_record() {
        var params = SessionModelSwitchToParams.builder().sessionId("sess-32").modelId("claude-sonnet-5").autoTier(null)
                .reasoningEffort("high").reasoningSummary(null).verbosity(null).modelCapabilities(null)
                .contextTier(null).source(null).deferIfModelChangeQueued(null).compactionDecision(null)
                .runCompactionPreflight(null).repoScope(null).modelChangeScope(null).requireAvailable(null)
                .pickerPersistence(null).build();
        assertEquals("sess-32", params.sessionId());
        assertEquals("claude-sonnet-5", params.modelId());
        assertNull(params.autoTier());
        assertEquals("high", params.reasoningEffort());
        assertNull(params.reasoningSummary());
        assertNull(params.verbosity());
        assertNull(params.modelCapabilities());
        assertNull(params.deferIfModelChangeQueued());
    }

    @Test
    void sessionModelSwitchParams_distinguishRequiredNullFromOmittedOptionalValue() {
        var mapper = new ObjectMapper();
        var switchAutoTier = mapper.valueToTree(
                SessionModelSwitchAutoTierParams.builder().sessionId("sess-32").autoTier(null).source(null).build());
        assertTrue(switchAutoTier.has("autoTier"));
        assertTrue(switchAutoTier.get("autoTier").isNull());

        var switchTo = mapper
                .valueToTree(SessionModelSwitchToParams.builder().sessionId("sess-32").modelId("auto").build());
        assertFalse(switchTo.has("autoTier"));
        assertFalse(switchTo.has("providerId"));
    }

    @Test
    void sessionPermissionsHandlePendingPermissionRequestParams_record() {
        var params = SessionPermissionsHandlePendingPermissionRequestParams.builder().sessionId("sess-33")
                .requestId("req-1").result("allow").decisionContext(null).build();
        assertEquals("sess-33", params.sessionId());
        assertEquals("req-1", params.requestId());
        assertEquals("allow", params.result());
        assertNull(params.decisionContext());
    }

    @Test
    void sessionPlanDeleteParams_record() {
        var params = SessionPlanDeleteParams.builder().sessionId("sess-34").build();
        assertEquals("sess-34", params.sessionId());
    }

    @Test
    void sessionPlanReadParams_record() {
        var params = SessionPlanReadParams.builder().sessionId("sess-35").build();
        assertEquals("sess-35", params.sessionId());
    }

    @Test
    void sessionPlanUpdateParams_record() {
        var params = SessionPlanUpdateParams.builder().sessionId("sess-36").content("# My Plan\n1. Do stuff").build();
        assertEquals("sess-36", params.sessionId());
        assertEquals("# My Plan\n1. Do stuff", params.content());
    }

    @Test
    void sessionPluginsListParams_record() {
        var params = SessionPluginsListParams.builder().sessionId("sess-37").build();
        assertEquals("sess-37", params.sessionId());
    }

    @Test
    void sessionShellExecParams_record() {
        var params = SessionShellExecParams.builder().sessionId("sess-38").command("ls -la").cwd("/workspace")
                .timeout(5000L).build();
        assertEquals("sess-38", params.sessionId());
        assertEquals("ls -la", params.command());
        assertEquals("/workspace", params.cwd());
        assertEquals(5000L, params.timeout());
    }

    @Test
    void sessionShellKillParams_record() {
        var params = SessionShellKillParams.builder().sessionId("sess-39").processId("proc-abc")
                .signal(ShellKillSignal.SIGTERM).build();
        assertEquals("sess-39", params.sessionId());
        assertEquals("proc-abc", params.processId());
        assertEquals(ShellKillSignal.SIGTERM, params.signal());
    }

    @Test
    void sessionShellKillParams_signal_enum() {
        assertEquals("SIGTERM", ShellKillSignal.SIGTERM.getValue());
        assertEquals("SIGKILL", ShellKillSignal.SIGKILL.getValue());
        assertEquals("SIGINT", ShellKillSignal.SIGINT.getValue());
        for (var sig : ShellKillSignal.values()) {
            assertEquals(sig, ShellKillSignal.fromValue(sig.getValue()));
        }
        assertThrows(IllegalArgumentException.class, () -> ShellKillSignal.fromValue("SIGHUP"));
    }

    @Test
    void sessionSkillsDisableParams_record() {
        var params = SessionSkillsDisableParams.builder().sessionId("sess-40").name("my-skill").build();
        assertEquals("sess-40", params.sessionId());
        assertEquals("my-skill", params.name());
    }

    @Test
    void sessionSkillsEnableParams_record() {
        var params = SessionSkillsEnableParams.builder().sessionId("sess-41").name("another-skill").build();
        assertEquals("sess-41", params.sessionId());
        assertEquals("another-skill", params.name());
    }

    @Test
    void sessionSkillsListParams_record() {
        var params = SessionSkillsListParams.builder().sessionId("sess-42").build();
        assertEquals("sess-42", params.sessionId());
    }

    @Test
    void sessionSkillsReloadParams_record() {
        var params = SessionSkillsReloadParams.builder().sessionId("sess-43").build();
        assertEquals("sess-43", params.sessionId());
    }

    @Test
    void sessionToolsHandlePendingToolCallParams_record() {
        var params = SessionToolsHandlePendingToolCallParams.builder().sessionId("sess-44").requestId("req-tool-1")
                .result("result data").error(null).build();
        assertEquals("sess-44", params.sessionId());
        assertEquals("req-tool-1", params.requestId());
        assertEquals("result data", params.result());
        assertNull(params.error());
    }

    @Test
    void sessionUiElicitationParams_record() {
        var params = SessionUiElicitationParams.builder().sessionId("sess-45").mode(null).message("What is your name?")
                .requestedSchema(
                        UIElicitationSchema.builder().type("object").properties(Map.of()).required(List.of()).build())
                .meta(null).task(null).build();
        assertEquals("sess-45", params.sessionId());
        assertEquals("What is your name?", params.message());
        assertEquals("object", params.requestedSchema().type());
    }

    @Test
    void sessionUiHandlePendingElicitationParams_record() {
        var params = SessionUiHandlePendingElicitationParams.builder().sessionId("sess-46").requestId("req-elicit-1")
                .result(UIElicitationResponse.builder().action(UIElicitationResponseAction.ACCEPT).content(Map.of())
                        .build())
                .build();
        assertEquals("sess-46", params.sessionId());
        assertEquals("req-elicit-1", params.requestId());
        assertEquals(UIElicitationResponseAction.ACCEPT, params.result().action());
    }

    @Test
    void sessionUsageGetMetricsParams_record() {
        var params = SessionUsageGetMetricsParams.builder().sessionId("sess-47").build();
        assertEquals("sess-47", params.sessionId());
    }

    // ── Result records ─────────────────────────────────────────────────────

    @ParameterizedTest
    @MethodSource("catalogSearchSharedErrors")
    void catalogSearchResult_shared_errors_round_trip(Class<? extends CatalogSearchResult> subtype, String json)
            throws Exception {
        assertSame(CatalogSearchResult.class, subtype.getSuperclass());
        assertCatalogErrorRoundTrip(CatalogSearchResult.class, subtype, json, CatalogSearchResult::getKind);
    }

    static Stream<Arguments> catalogSearchSharedErrors() {
        return Stream.of(Arguments.of(CatalogNegotiationRefusedError.class, catalogNegotiationRefusedJson()),
                Arguments.of(CatalogInvalidRequestError.class, """
                        {"kind":"invalid-request","field":"query","message":"A query is required"}
                        """), Arguments.of(CatalogUnavailableError.class, """
                        {"kind":"unavailable","reason":"search-unavailable","message":"Search is unavailable"}
                        """));
    }

    @ParameterizedTest
    @MethodSource("catalogSelectionSharedErrors")
    void catalogSelectionResult_shared_errors_round_trip(Class<? extends CatalogSelectionResult> subtype, String json)
            throws Exception {
        assertCatalogErrorRoundTrip(CatalogSelectionResult.class, subtype, json, CatalogSelectionResult::getKind);
    }

    static Stream<Arguments> catalogSelectionSharedErrors() {
        return Stream.of(
                Arguments.of(CatalogSelectionResultCatalogNegotiationRefusedError.class,
                        catalogNegotiationRefusedJson()),
                Arguments.of(CatalogSelectionResultCatalogInvalidRequestError.class, """
                        {"kind":"invalid-request","field":"selectionRef","message":"A selection reference is required"}
                        """), Arguments.of(CatalogSelectionResultCatalogUnavailableError.class, """
                        {"kind":"unavailable","reason":"selection-unavailable","message":"Selection is unavailable"}
                        """));
    }

    @ParameterizedTest
    @MethodSource("mcpPlanInstallSharedErrors")
    void mcpPlanInstallResult_shared_errors_round_trip(Class<? extends McpPlanInstallResult> subtype, String json)
            throws Exception {
        assertCatalogErrorRoundTrip(McpPlanInstallResult.class, subtype, json, McpPlanInstallResult::getKind);
    }

    static Stream<Arguments> mcpPlanInstallSharedErrors() {
        return Stream.of(
                Arguments.of(McpPlanInstallResultCatalogNegotiationRefusedError.class, catalogNegotiationRefusedJson()),
                Arguments.of(McpPlanInstallResultCatalogInvalidRequestError.class, """
                        {"kind":"invalid-request","field":"card","message":"A card is required"}
                        """), Arguments.of(McpPlanInstallResultCatalogAuthenticationRequiredError.class, """
                        {"kind":"authentication-required","reason":"credential-expired","message":"Sign in again"}
                        """), Arguments.of(McpPlanInstallResultCatalogPolicyRejectedError.class, """
                        {"kind":"policy-rejected","source":"enterprise-allowlist","message":"Installation is blocked"}
                        """), Arguments.of(McpPlanInstallResultCatalogNetworkFailureError.class, """
                        {"kind":"network-failure","reason":"rate-limited","statusCode":429,
                         "retryAfterSeconds":30,"message":"Retry later"}
                        """), Arguments.of(McpPlanInstallResultCatalogUnsafeRetrievalError.class, """
                        {"kind":"unsafe-retrieval","reason":"blocked-address","message":"Retrieval is blocked"}
                        """), Arguments.of(McpPlanInstallResultCatalogMalformedCardError.class, """
                        {"kind":"malformed-card","reason":"schema-violation",
                         "mediaType":"application/mcp-server-card+json","message":"The card does not match its schema"}
                        """), Arguments.of(McpPlanInstallResultCatalogContractViolationError.class, """
                        {"kind":"contract-violation","reason":"both-url-and-data","message":"Use one card source"}
                        """), Arguments.of(McpPlanInstallResultCatalogUnavailableError.class, """
                        {"kind":"unavailable","reason":"planning-unavailable","message":"Planning is unavailable"}
                        """));
    }

    private static String catalogNegotiationRefusedJson() {
        return """
                {"kind":"negotiation-refused","reason":"unsupported-capability","runtimeProtocolVersion":3,
                 "minimumSupportedProtocolVersion":3,
                 "supportedCapabilities":["mcp-server-card","legacy-mcp-server-card","ai-skill-discovery",
                                          "agent-plugin-discovery","mcp-install-planning"],
                 "unsupportedCapabilities":["catalog-selection"],"message":"The required capability is unavailable"}
                """;
    }

    private static <T> void assertCatalogErrorRoundTrip(Class<T> root, Class<? extends T> subtype, String json,
            Function<T, String> kind) throws Exception {
        var mapper = new ObjectMapper();
        var expected = mapper.readTree(json);
        assertTrue(root.isAssignableFrom(subtype));

        var result = mapper.readValue(json, root);
        assertInstanceOf(root, result);
        assertEquals(subtype, result.getClass());
        assertEquals(expected.get("kind").asText(), kind.apply(result));

        var serialized = mapper.writeValueAsString(result);
        assertEquals(expected, mapper.readTree(serialized));
        var roundTripped = mapper.readValue(serialized, root);
        assertInstanceOf(root, roundTripped);
        assertEquals(subtype, roundTripped.getClass());
        assertEquals(expected.get("kind").asText(), kind.apply(roundTripped));
        assertEquals(expected, mapper.readTree(mapper.writeValueAsString(roundTripped)));
    }

    @Test
    void pingResult_fields() {
        var ts = OffsetDateTime.now();
        var result = PingResult.builder().message("pong").timestamp(ts).protocolVersion(1L).build();
        assertEquals("pong", result.message());
        assertEquals(ts, result.timestamp());
        assertEquals(1L, result.protocolVersion());
    }

    @Test
    void sessionAgentListResult_with_items() {
        var item = AgentInfo.builder().name("name1").displayName("Name One").description("Desc 1")
                .path("/path/to/agent1").id("agent-1").source(null).userInvocable(null).disableModelInvocation(null)
                .tools(null).model(null).models(null).modelPolicy(null).reasoningEffort(null).mcpServers(null)
                .skills(null).prompt(null).hooks(null).build();
        var result = SessionAgentListResult.builder().agents(List.of(item)).build();
        assertEquals(1, result.agents().size());
        assertEquals("name1", result.agents().get(0).name());
        assertEquals("Name One", result.agents().get(0).displayName());
        assertEquals("Desc 1", result.agents().get(0).description());
        assertEquals("/path/to/agent1", result.agents().get(0).path());
    }

    @Test
    void sessionAgentGetCurrentResult_nested() {
        var agent = AgentInfo.builder().name("agent-1").displayName("Agent One").description("Does things").path(null)
                .id("agent-id").source(null).userInvocable(null).disableModelInvocation(null).tools(null).model(null)
                .models(null).modelPolicy(null).reasoningEffort(null).mcpServers(null).skills(null).prompt(null)
                .hooks(null).build();
        var result = SessionAgentGetCurrentResult.builder().agent(agent).build();
        assertEquals("agent-1", result.agent().name());
        assertEquals("Agent One", result.agent().displayName());
        assertEquals("Does things", result.agent().description());
        assertNull(result.agent().path());
    }

    @Test
    void sessionAgentGetCurrentResult_null_agent() throws Exception {
        var mapper = new ObjectMapper();
        var result = SessionAgentGetCurrentResult.builder().agent(null).build();
        assertNull(result.agent());
        assertEquals(mapper.readTree("{\"agent\":null}"), mapper.valueToTree(result));
        assertEquals(result, mapper.readValue("{\"agent\":null}", SessionAgentGetCurrentResult.class));
        var missing = assertThrows(IllegalStateException.class, () -> SessionAgentGetCurrentResult.builder().build());
        assertEquals("Missing required property: agent", missing.getMessage());
    }

    @Test
    void sessionAgentReloadResult_with_items() {
        var item = AgentInfo.builder().name("a").displayName("A").description("Desc").path("/path/to/a").id("agent-a")
                .source(null).userInvocable(null).disableModelInvocation(null).tools(null).model(null).models(null)
                .modelPolicy(null).reasoningEffort(null).mcpServers(null).skills(null).prompt(null).hooks(null).build();
        var result = SessionAgentReloadResult.builder().agents(List.of(item)).build();
        assertEquals(1, result.agents().size());
        assertEquals("a", result.agents().get(0).name());
    }

    @Test
    void sessionAgentSelectResult_nested() throws Exception {
        Map<String, List<Object>> hooks = Map.of("Stop",
                List.of(Map.of("type", "command", "command", "echo scoped-stop")));
        var agent = AgentInfo.builder().name("selected").displayName("Selected").description("The selected agent")
                .pluginId("mp:catalog::tools").path("/path/to/selected").id("agent-selected").source(null)
                .userInvocable(null).disableModelInvocation(null).tools(null).model(null).models(null).modelPolicy(null)
                .reasoningEffort(null).mcpServers(null).skills(null).prompt(null).hooks(hooks).build();
        var result = SessionAgentSelectResult.builder().agent(agent).build();
        assertEquals("selected", result.agent().name());
        var mapper = new ObjectMapper();
        var json = mapper.writeValueAsString(result);
        assertEquals("mp:catalog::tools", mapper.readTree(json).get("agent").get("pluginId").asText());
        assertEquals("echo scoped-stop",
                mapper.readTree(json).get("agent").get("hooks").get("Stop").get(0).get("command").asText());
        assertEquals(hooks, mapper.readValue(json, SessionAgentSelectResult.class).agent().hooks());
    }

    @Test
    void sessionCommandsHandlePendingCommandResult_record() {
        var result = SessionCommandsHandlePendingCommandResult.builder().success(true).build();
        assertTrue(result.success());
        assertFalse(SessionCommandsHandlePendingCommandResult.builder().success(false).build().success());
    }

    @Test
    void sessionExtensionsListResult_nested() {
        var ext = Extension.builder().id("ext-1").name("My Extension").source(ExtensionSource.PROJECT)
                .status(ExtensionStatus.RUNNING).pid(1234L).build();
        var result = SessionExtensionsListResult.builder().extensions(List.of(ext)).build();
        assertEquals(1, result.extensions().size());
        assertEquals("ext-1", result.extensions().get(0).id());
        assertEquals("My Extension", result.extensions().get(0).name());
        assertEquals(ExtensionSource.PROJECT, result.extensions().get(0).source());
        assertEquals(ExtensionStatus.RUNNING, result.extensions().get(0).status());
        assertEquals(1234L, result.extensions().get(0).pid());
    }

    @Test
    void sessionExtensionsListResult_enums() {
        for (var src : ExtensionSource.values()) {
            assertNotNull(src.getValue());
            assertEquals(src, ExtensionSource.fromValue(src.getValue()));
        }
        for (var status : ExtensionStatus.values()) {
            assertNotNull(status.getValue());
            assertEquals(status, ExtensionStatus.fromValue(status.getValue()));
        }
        assertThrows(IllegalArgumentException.class, () -> ExtensionSource.fromValue("unknown"));
        assertThrows(IllegalArgumentException.class, () -> ExtensionStatus.fromValue("unknown"));
    }

    @Test
    void sessionFleetStartResult_record() {
        var result = SessionFleetStartResult.builder().started(true).build();
        assertTrue(result.started());
        assertFalse(SessionFleetStartResult.builder().started(false).build().started());
    }

    @Test
    void sessionFsExistsResult_record() {
        var result = SessionFsExistsResult.builder().exists(true).build();
        assertTrue(result.exists());
        assertFalse(SessionFsExistsResult.builder().exists(false).build().exists());
    }

    @Test
    void sessionFsReadFileResult_record() {
        var result = SessionFsReadFileResult.builder().content("file content here").error(null).build();
        assertEquals("file content here", result.content());
    }

    @Test
    void sessionFsReaddirResult_record() {
        var result = SessionFsReaddirResult.builder().entries(List.of("file1.txt", "file2.txt")).error(null).build();
        assertEquals(2, result.entries().size());
        assertEquals("file1.txt", result.entries().get(0));
    }

    @Test
    void sessionFsReaddirWithTypesResult_nested() {
        var entry = SessionFsReaddirWithTypesEntry.builder().name("myfile.txt")
                .type(SessionFsReaddirWithTypesEntryType.FILE).build();
        var result = SessionFsReaddirWithTypesResult.builder().entries(List.of(entry)).error(null).build();
        assertEquals(1, result.entries().size());
        assertEquals("myfile.txt", result.entries().get(0).name());
        assertEquals(SessionFsReaddirWithTypesEntryType.FILE, result.entries().get(0).type());
        assertEquals("file", result.entries().get(0).type().getValue());
    }

    @Test
    void sessionFsReaddirWithTypesResult_type_enum() {
        for (var t : SessionFsReaddirWithTypesEntryType.values()) {
            assertNotNull(t.getValue());
            assertEquals(t, SessionFsReaddirWithTypesEntryType.fromValue(t.getValue()));
        }
        assertThrows(IllegalArgumentException.class, () -> SessionFsReaddirWithTypesEntryType.fromValue("symlink"));
    }

    @Test
    void sessionFsSetProviderResult_record() {
        var result = SessionFsSetProviderResult.builder().success(true).build();
        assertTrue(result.success());
        assertFalse(SessionFsSetProviderResult.builder().success(false).build().success());
    }

    @Test
    void sessionFsStatResult_record() {
        var modified = OffsetDateTime.parse("2026-01-01T00:00:00Z");
        var created = OffsetDateTime.parse("2025-12-31T00:00:00Z");
        var result = SessionFsStatResult.builder().isFile(true).isDirectory(false).size(1024L).mtime(modified)
                .birthtime(created).error(null).build();
        assertTrue(result.isFile());
        assertFalse(result.isDirectory());
        assertEquals(1024L, result.size());
        assertEquals(modified, result.mtime());
        assertEquals(created, result.birthtime());
    }

    @Test
    void sessionHistoryCompactResult_nested() {
        var ctx = HistoryCompactContextWindow.builder().tokenLimit(100000L).currentTokens(5000L).messagesLength(20L)
                .systemTokens(1000L).conversationTokens(3000L).toolDefinitionsTokens(500L).build();
        var result = SessionHistoryCompactResult.builder().success(true).tokensRemoved(2000L).messagesRemoved(5L)
                .summaryContent(null).contextWindow(ctx).build();
        assertTrue(result.success());
        assertEquals(2000L, result.tokensRemoved());
        assertEquals(5L, result.messagesRemoved());
        assertNotNull(result.contextWindow());
        assertEquals(100000L, result.contextWindow().tokenLimit());
        assertEquals(5000L, result.contextWindow().currentTokens());
    }

    @Test
    void sessionHistoryTruncateResult_record() {
        var result = SessionHistoryTruncateResult.builder().eventsRemoved(3L).checkpointCleanupFailed(false)
                .checkpointCleanupError(null).build();
        assertEquals(3L, result.eventsRemoved());
        assertEquals(false, result.checkpointCleanupFailed());
        assertNull(result.checkpointCleanupError());
    }

    @Test
    void sessionLogResult_record() {
        var id = UUID.randomUUID();
        var result = SessionLogResult.builder().eventId(id).build();
        assertEquals(id, result.eventId());
    }

    @Test
    void sessionMcpListResult_nested() {
        var metadata = McpServerMetadata.builder().instructions("Use this server for repository operations.").build();
        var server = McpServer.builder().name("my-mcp").status(McpServerStatus.CONNECTED).source(McpServerSource.USER)
                .sourcePlugin(null).sourcePluginVersion(null).displayName(null).error(null).serverMetadata(metadata)
                .build();
        var result = SessionMcpListResult.builder().servers(List.of(server)).host(null).build();
        assertEquals(1, result.servers().size());
        assertEquals("my-mcp", result.servers().get(0).name());
        assertEquals(McpServerStatus.CONNECTED, result.servers().get(0).status());
        assertEquals(McpServerSource.USER, result.servers().get(0).source());
        assertEquals("Use this server for repository operations.",
                result.servers().get(0).serverMetadata().instructions());
    }

    @Test
    void sessionMcpListResult_status_enum_all_values() {
        for (var status : McpServerStatus.values()) {
            assertNotNull(status.getValue());
            assertEquals(status, McpServerStatus.fromValue(status.getValue()));
        }
        assertThrows(IllegalArgumentException.class, () -> McpServerStatus.fromValue("unknown-status"));
    }

    @Test
    void sessionModelGetCurrentResult_record() {
        var result = SessionModelGetCurrentResult.builder().modelId("claude-sonnet-5").providerId(null)
                .planBaseModelId("claude-sonnet-4.5").planBaseProviderId(null).reasoningEffort(null).contextTier(null)
                .autoTier(null).pendingAutoTier(null).activatingAutoTier(null).build();
        assertEquals("claude-sonnet-5", result.modelId());
        assertEquals("claude-sonnet-4.5", result.planBaseModelId());
    }

    @Test
    void sessionModelSwitchToResult_record() {
        var result = SessionModelSwitchToResult.builder().queueId("queue-model-1").modelId("gpt-5").deferred(true)
                .status(null).confirmation(null).persistenceError(null).message(null).warning(null)
                .deprecationWarnings(null).modelState(null).build();
        assertEquals("queue-model-1", result.queueId());
        assertEquals("gpt-5", result.modelId());
        assertEquals(true, result.deferred());
    }

    @Test
    void sessionPermissionsHandlePendingPermissionRequestResult_record() {
        var result = SessionPermissionsHandlePendingPermissionRequestResult.builder().success(true).build();
        assertTrue(result.success());
        assertFalse(SessionPermissionsHandlePendingPermissionRequestResult.builder().success(false).build().success());
    }

    @Test
    void sessionPlanReadResult_record() {
        var result = SessionPlanReadResult.builder().exists(true).content("# Plan\n1. Do stuff")
                .path("/workspace/.plan").build();
        assertTrue(result.exists());
        assertEquals("# Plan\n1. Do stuff", result.content());
        assertEquals("/workspace/.plan", result.path());
    }

    @Test
    void sessionPluginsListResult_nested() {
        var plugin = Plugin.builder().name("my-plugin").marketplace("marketplace-x").version("1.2.3").enabled(true)
                .rootPath("/plugins/my-plugin").id("opaque-plugin").directSourceId(null)
                .installedFrom("/marketplaces/marketplace-x").source(null).managed(true).managedDesiredEnabled(true)
                .installed(true).build();
        var result = SessionPluginsListResult.builder().plugins(List.of(plugin)).build();
        assertEquals(1, result.plugins().size());
        assertEquals("my-plugin", result.plugins().get(0).name());
        assertEquals("marketplace-x", result.plugins().get(0).marketplace());
        assertEquals("1.2.3", result.plugins().get(0).version());
        assertTrue(result.plugins().get(0).enabled());
        assertNull(result.plugins().get(0).directSourceId());
        assertEquals("/plugins/my-plugin", result.plugins().get(0).rootPath());
        assertEquals("/marketplaces/marketplace-x", result.plugins().get(0).installedFrom());
        assertNull(result.plugins().get(0).source());
        assertTrue(result.plugins().get(0).managed());
        assertTrue(result.plugins().get(0).managedDesiredEnabled());
        assertTrue(result.plugins().get(0).installed());
        assertEquals("opaque-plugin", result.plugins().get(0).id());
    }

    @Test
    void sessionShellExecResult_record() {
        var result = SessionShellExecResult.builder().processId("proc-id-123").build();
        assertEquals("proc-id-123", result.processId());
    }

    @Test
    void sessionShellKillResult_record() {
        var result = SessionShellKillResult.builder().killed(true).build();
        assertTrue(result.killed());
        assertFalse(SessionShellKillResult.builder().killed(false).build().killed());
    }

    @Test
    void sessionSkillsListResult_nested() {
        var item = Skill.builder().name("deploy").commandName("deploy").description("Deploy the app")
                .source(SkillSource.PROJECT).userInvocable(true).enabled(true).path("/skills/deploy.md")
                .pluginName(null).argumentHint(null).build();
        var result = SessionSkillsListResult.builder().skills(List.of(item)).build();
        assertEquals(1, result.skills().size());
        assertEquals("deploy", result.skills().get(0).name());
        assertNull(result.skills().get(0).declaredName());
        assertEquals("deploy", result.skills().get(0).commandName());
        assertEquals(SkillSource.PROJECT, result.skills().get(0).source());
        assertNull(result.skills().get(0).mcpOrigin());
        assertTrue(result.skills().get(0).enabled());
    }

    @Test
    void sessionSkillsListResult_mcp_identity() throws Exception {
        var origin = McpSkillOrigin.builder().serverName("deployment").uri("skills://deployment/SKILL.md").build();
        var item = Skill.builder().name("mcp1:qualified").declaredName("deploy").commandName("mcp1:qualified")
                .description("Deploy the app").source(SkillSource.MCP).mcpOrigin(origin).userInvocable(true)
                .enabled(true).build();
        var mapper = new ObjectMapper();
        var result = SessionSkillsListResult.builder().skills(List.of(item)).build();
        var json = mapper.valueToTree(result);
        assertEquals("mcp1:qualified", json.get("skills").get(0).get("name").asText());
        assertEquals("deploy", json.get("skills").get(0).get("declaredName").asText());
        assertEquals("mcp1:qualified", json.get("skills").get(0).get("commandName").asText());
        assertEquals("mcp", json.get("skills").get(0).get("source").asText());
        assertEquals("deployment", json.get("skills").get(0).get("mcpOrigin").get("serverName").asText());
        assertEquals("skills://deployment/SKILL.md", json.get("skills").get(0).get("mcpOrigin").get("uri").asText());
        assertFalse(json.get("skills").get(0).has("path"));
        assertEquals(result, mapper.treeToValue(json, SessionSkillsListResult.class));
    }

    @Test
    void sessionSkillsReloadResult_empty() {
        var result = SessionSkillsReloadResult.builder().warnings(List.of()).errors(List.of()).build();
        assertTrue(result.warnings().isEmpty());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void sessionToolsHandlePendingToolCallResult_record() {
        var result = SessionToolsHandlePendingToolCallResult.builder().success(true).build();
        assertTrue(result.success());
        assertFalse(SessionToolsHandlePendingToolCallResult.builder().success(false).build().success());
    }

    @Test
    void sessionUiElicitationResult_accept() {
        var result = SessionUiElicitationResult.builder().action(UIElicitationResponseAction.ACCEPT)
                .content(Map.of("name", "Alice")).meta(null).build();
        assertEquals(UIElicitationResponseAction.ACCEPT, result.action());
        assertEquals("Alice", result.content().get("name"));
    }

    @Test
    void sessionUiElicitationResult_action_enum() {
        assertEquals("accept", UIElicitationResponseAction.ACCEPT.getValue());
        assertEquals("decline", UIElicitationResponseAction.DECLINE.getValue());
        assertEquals("cancel", UIElicitationResponseAction.CANCEL.getValue());
        for (var a : UIElicitationResponseAction.values()) {
            assertEquals(a, UIElicitationResponseAction.fromValue(a.getValue()));
        }
        assertThrows(IllegalArgumentException.class, () -> UIElicitationResponseAction.fromValue("unknown"));
    }

    @Test
    void sessionUiHandlePendingElicitationResult_record() {
        var result = SessionUiHandlePendingElicitationResult.builder().success(true).build();
        assertTrue(result.success());
    }

    @Test
    void sessionUsageGetMetricsResult_nested() {
        var changes = UsageMetricsCodeChanges.builder().linesAdded(100L).linesRemoved(50L).filesModifiedCount(5L)
                .filesModified(List.of()).build();
        var result = SessionUsageGetMetricsResult.builder().aiCreditsStatus(null).totalPremiumRequestCost(0.5)
                .totalUserRequests(10L).totalNanoAiu(null).tokenDetails(null).totalApiDurationMs(2000L)
                .sessionStartTime(OffsetDateTime.parse("2026-01-01T00:00:00Z")).codeChanges(changes)
                .modelMetrics(Map.of()).providerModelMetrics(null).agentMetrics(null).currentModel("gpt-5")
                .lastCallInputTokens(1000L).lastCallOutputTokens(500L).build();
        assertEquals(0.5, result.totalPremiumRequestCost());
        assertEquals(10L, result.totalUserRequests());
        assertNotNull(result.codeChanges());
        assertEquals(100L, result.codeChanges().linesAdded());
        assertEquals(50L, result.codeChanges().linesRemoved());
        assertEquals(5L, result.codeChanges().filesModifiedCount());
        assertEquals("gpt-5", result.currentModel());
    }

    @Test
    void sessionsForkResult_record() {
        var result = SessionsForkResult.builder().sessionId("forked-sess-id").name(null).build();
        assertEquals("forked-sess-id", result.sessionId());
    }

    // ── Complex nested result records ──────────────────────────────────────

    @Test
    void accountGetQuotaResult_nested() {
        var snapshot = AccountQuotaSnapshot.builder().isUnlimitedEntitlement(false).entitlementRequests(100L)
                .usedRequests(40L).usageAllowedWithExhaustedQuota(true).remainingPercentage(60.0).overage(5.0)
                .overageAllowedWithExhaustedQuota(true)
                .resetDate(java.time.OffsetDateTime.parse("2026-05-01T00:00:00Z")).build();
        var result = AccountGetQuotaResult.builder().quotaSnapshots(Map.of("chat", snapshot)).build();
        assertEquals(1, result.quotaSnapshots().size());
        var s = result.quotaSnapshots().get("chat");
        assertFalse(s.isUnlimitedEntitlement());
        assertEquals(100L, s.entitlementRequests());
        assertEquals(40L, s.usedRequests());
        assertEquals(60.0, s.remainingPercentage());
        assertEquals(5.0, s.overage());
        assertTrue(s.overageAllowedWithExhaustedQuota());
        assertEquals(java.time.OffsetDateTime.parse("2026-05-01T00:00:00Z"), s.resetDate());
    }

    @Test
    void mcpConfigListResult_record() {
        var result = McpConfigListResult.builder().servers(Map.of("server1", "config1")).build();
        assertEquals(1, result.servers().size());
        assertEquals("config1", result.servers().get("server1"));
    }

    @Test
    void mcpDiscoverResult_nested() {
        var server = DiscoveredMcpServer.builder().name("discovered-server").type(DiscoveredMcpServerType.STDIO)
                .source(McpServerSource.USER).sourcePlugin(null).sourcePluginVersion(null).effectiveSource(null)
                .enabled(true).build();
        var result = McpDiscoverResult.builder().servers(List.of(server)).build();
        assertEquals(1, result.servers().size());
        assertEquals("discovered-server", result.servers().get(0).name());
        assertEquals(DiscoveredMcpServerType.STDIO, result.servers().get(0).type());
        assertEquals(McpServerSource.USER, result.servers().get(0).source());
        assertTrue(result.servers().get(0).enabled());
    }

    @Test
    void modelsListResult_nested() {
        var supports = ModelCapabilitiesSupports.builder().thinking(false).vision(true).toolCalls(null)
                .reasoningEffort(false).adaptiveThinking(null).build();
        var limits = ModelCapabilitiesLimits.builder().maxPromptTokens(100000L).maxOutputTokens(8192L)
                .maxContextWindowTokens(128000L).vision(null).build();
        var capabilities = ModelCapabilities.builder().supports(supports).limits(limits).build();
        var policy = ModelPolicy.builder().state(ModelPolicyState.ENABLED).terms(null).build();
        var promo = ModelBillingPromo.builder().id("summer-2026").discountPercent(25.0).endsAt("2026-08-01T00:00:00Z")
                .message("Summer discount").showBanner(true).build();
        var billing = ModelBilling.builder().multiplier(1.0).tokenPrices(null).discountPercent(null).promo(promo)
                .build();
        var modelItem = Model.builder().id("gpt-5").name("GPT-5").capabilities(capabilities).metadata(null).vendor(null)
                .policy(policy).billing(billing).supportedReasoningEfforts(null).defaultReasoningEffort(null)
                .supportedContextTiers(null).modelPickerCategory(null).modelPickerPriceCategory(null).warningText(null)
                .infoMessages(null).warningMessages(null).provider(null).build();
        var result = ModelsListResult.builder().models(List.of(modelItem)).auto(null).imageModels(null).build();

        assertNull(result.auto());
        assertNull(result.decisionModels());
        assertEquals(1, result.models().size());
        assertNull(result.imageModels());
        assertEquals("gpt-5", result.models().get(0).id());
        assertEquals("GPT-5", result.models().get(0).name());
        assertFalse(result.models().get(0).capabilities().supports().thinking());
        assertTrue(result.models().get(0).capabilities().supports().vision());
        assertFalse(result.models().get(0).capabilities().supports().reasoningEffort());
        assertEquals(100000L, result.models().get(0).capabilities().limits().maxPromptTokens());
        assertEquals(ModelPolicyState.ENABLED, result.models().get(0).policy().state());
        assertEquals(Double.valueOf(1.0), result.models().get(0).billing().multiplier());
        assertEquals("summer-2026", result.models().get(0).billing().promo().id());
        assertEquals(Double.valueOf(25.0), result.models().get(0).billing().promo().discountPercent());
        assertEquals("2026-08-01T00:00:00Z", result.models().get(0).billing().promo().endsAt());
        assertEquals("Summer discount", result.models().get(0).billing().promo().message());
        assertTrue(result.models().get(0).billing().promo().showBanner());
    }

    @Test
    void modelsListResult_deserializesAutoTierMetadata() throws Exception {
        var result = new ObjectMapper().readValue("""
                {
                  "models": [],
                  "auto": {
                    "defaultTier": "balance",
                    "tiers": [
                      {"id": "balance", "displayName": "Balance", "description": "Balanced", "type": "auto",
                       "status": {"enabled": true}},
                      {"id": "hydrafusion", "displayName": "High", "description": "Fusion", "type": "fusion",
                       "status": {"enabled": false, "reason": "policy", "message": "Contact admin"}}
                    ]
                  }
                }
                """, ModelsListResult.class);

        assertEquals("balance", result.auto().defaultTier());
        assertNull(result.auto().providerId());
        assertEquals(List.of("balance", "hydrafusion"),
                result.auto().tiers().stream().map(AutoTierDescriptor::id).toList());
        var fusion = result.auto().tiers().get(1);
        assertEquals("High", fusion.displayName());
        assertEquals("fusion", fusion.type());
        assertEquals(AutoTierStatus.builder().enabled(false).pickerVisible(null).reason("policy")
                .message("Contact admin").build(), fusion.status());
    }

    @Test
    void toolsListResult_nested() {
        var tool = Tool.builder().name("bash").namespacedName("bash").description("Run shell commands")
                .parameters(Map.of("type", "object")).instructions("Use for shell commands").safeForTelemetry(true)
                .build();
        var result = ToolsListResult.builder().tools(List.of(tool)).build();
        assertEquals(1, result.tools().size());
        assertEquals("bash", result.tools().get(0).name());
        assertEquals("bash", result.tools().get(0).namespacedName());
        assertEquals("Run shell commands", result.tools().get(0).description());
        assertEquals("Use for shell commands", result.tools().get(0).instructions());
        assertEquals(Boolean.TRUE, result.tools().get(0).safeForTelemetry());
    }

    // ── SessionModelSwitchToParams nested records ──────────────────────────

    @Test
    void sessionModelSwitchToParams_nested_records() {
        var limitsVision = ModelCapabilitiesOverrideLimitsVision.builder()
                .supportedMediaTypes(List.of("image/png", "image/jpeg")).maxPromptImages(10L)
                .maxPromptImageSize(5000000L).build();
        var limits = ModelCapabilitiesOverrideLimits.builder().maxPromptTokens(100000L).maxOutputTokens(8192L)
                .maxContextWindowTokens(128000L).vision(limitsVision).build();
        var supports = ModelCapabilitiesOverrideSupports.builder().vision(true).toolCalls(null).reasoningEffort(true)
                .adaptiveThinking(null).build();
        var capabilities = ModelCapabilitiesOverride.builder().supports(supports).limits(limits).build();
        var params = SessionModelSwitchToParams.builder().sessionId("sess-m").modelId("gpt-5").autoTier(null)
                .reasoningEffort(null).reasoningSummary(null).verbosity(null).modelCapabilities(capabilities)
                .contextTier(null).source(null).deferIfModelChangeQueued(null).compactionDecision(null)
                .runCompactionPreflight(null).repoScope(null).modelChangeScope(null).requireAvailable(null)
                .pickerPersistence(null).build();

        assertEquals("gpt-5", params.modelId());
        assertNotNull(params.modelCapabilities());
        assertTrue(params.modelCapabilities().supports().vision());
        assertTrue(params.modelCapabilities().supports().reasoningEffort());
        assertEquals(100000L, params.modelCapabilities().limits().maxPromptTokens());
        assertEquals(2, params.modelCapabilities().limits().vision().supportedMediaTypes().size());
    }

    // ── SessionUiElicitationParams nested record ───────────────────────────

    @Test
    void sessionUiElicitationParams_nested_schema() {
        var schema = UIElicitationSchema.builder().type("object").properties(Map.of("name", Map.of("type", "string")))
                .required(List.of("name")).build();
        var params = SessionUiElicitationParams.builder().sessionId("sess-elicit").mode(null)
                .message("Please fill form").requestedSchema(schema).meta(null).task(null).build();
        assertEquals("sess-elicit", params.sessionId());
        assertEquals("object", params.requestedSchema().type());
        assertTrue(params.requestedSchema().required().contains("name"));
    }

    // ── SessionUiHandlePendingElicitationParams nested enum ────────────────

    @Test
    void sessionUiHandlePendingElicitationParamsResult_action_enum() {
        for (var action : UIElicitationResponseAction.values()) {
            assertNotNull(action.getValue());
            assertEquals(action, UIElicitationResponseAction.fromValue(action.getValue()));
        }
        assertThrows(IllegalArgumentException.class, () -> UIElicitationResponseAction.fromValue("unknown"));
    }
}
