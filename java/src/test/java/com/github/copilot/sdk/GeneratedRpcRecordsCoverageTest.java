/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.sdk.generated.rpc.AccountGetQuotaResult;
import com.github.copilot.sdk.generated.rpc.McpConfigListResult;
import com.github.copilot.sdk.generated.rpc.McpConfigRemoveParams;
import com.github.copilot.sdk.generated.rpc.McpConfigUpdateParams;
import com.github.copilot.sdk.generated.rpc.McpDiscoverParams;
import com.github.copilot.sdk.generated.rpc.McpDiscoverResult;
import com.github.copilot.sdk.generated.rpc.ModelsListResult;
import com.github.copilot.sdk.generated.rpc.PingParams;
import com.github.copilot.sdk.generated.rpc.PingResult;
import com.github.copilot.sdk.generated.rpc.SessionAgentDeselectParams;
import com.github.copilot.sdk.generated.rpc.SessionAgentDeselectResult;
import com.github.copilot.sdk.generated.rpc.SessionAgentGetCurrentParams;
import com.github.copilot.sdk.generated.rpc.SessionAgentGetCurrentResult;
import com.github.copilot.sdk.generated.rpc.SessionAgentListParams;
import com.github.copilot.sdk.generated.rpc.SessionAgentListResult;
import com.github.copilot.sdk.generated.rpc.SessionAgentReloadParams;
import com.github.copilot.sdk.generated.rpc.SessionAgentReloadResult;
import com.github.copilot.sdk.generated.rpc.SessionAgentSelectParams;
import com.github.copilot.sdk.generated.rpc.SessionAgentSelectResult;
import com.github.copilot.sdk.generated.rpc.SessionCommandsHandlePendingCommandParams;
import com.github.copilot.sdk.generated.rpc.SessionCommandsHandlePendingCommandResult;
import com.github.copilot.sdk.generated.rpc.SessionExtensionsDisableParams;
import com.github.copilot.sdk.generated.rpc.SessionExtensionsDisableResult;
import com.github.copilot.sdk.generated.rpc.SessionExtensionsEnableParams;
import com.github.copilot.sdk.generated.rpc.SessionExtensionsEnableResult;
import com.github.copilot.sdk.generated.rpc.SessionExtensionsListParams;
import com.github.copilot.sdk.generated.rpc.SessionExtensionsListResult;
import com.github.copilot.sdk.generated.rpc.SessionExtensionsReloadParams;
import com.github.copilot.sdk.generated.rpc.SessionExtensionsReloadResult;
import com.github.copilot.sdk.generated.rpc.SessionFleetStartParams;
import com.github.copilot.sdk.generated.rpc.SessionFleetStartResult;
import com.github.copilot.sdk.generated.rpc.SessionFsAppendFileParams;
import com.github.copilot.sdk.generated.rpc.SessionFsExistsParams;
import com.github.copilot.sdk.generated.rpc.SessionFsExistsResult;
import com.github.copilot.sdk.generated.rpc.SessionFsMkdirParams;
import com.github.copilot.sdk.generated.rpc.SessionFsReadFileParams;
import com.github.copilot.sdk.generated.rpc.SessionFsReadFileResult;
import com.github.copilot.sdk.generated.rpc.SessionFsReaddirParams;
import com.github.copilot.sdk.generated.rpc.SessionFsReaddirResult;
import com.github.copilot.sdk.generated.rpc.SessionFsReaddirWithTypesParams;
import com.github.copilot.sdk.generated.rpc.SessionFsReaddirWithTypesResult;
import com.github.copilot.sdk.generated.rpc.SessionFsRenameParams;
import com.github.copilot.sdk.generated.rpc.SessionFsRmParams;
import com.github.copilot.sdk.generated.rpc.SessionFsSetProviderParams;
import com.github.copilot.sdk.generated.rpc.SessionFsSetProviderResult;
import com.github.copilot.sdk.generated.rpc.SessionFsStatParams;
import com.github.copilot.sdk.generated.rpc.SessionFsStatResult;
import com.github.copilot.sdk.generated.rpc.SessionFsWriteFileParams;
import com.github.copilot.sdk.generated.rpc.SessionHistoryCompactParams;
import com.github.copilot.sdk.generated.rpc.SessionHistoryCompactResult;
import com.github.copilot.sdk.generated.rpc.SessionHistoryTruncateParams;
import com.github.copilot.sdk.generated.rpc.SessionHistoryTruncateResult;
import com.github.copilot.sdk.generated.rpc.SessionLogParams;
import com.github.copilot.sdk.generated.rpc.SessionLogResult;
import com.github.copilot.sdk.generated.rpc.SessionMcpDisableParams;
import com.github.copilot.sdk.generated.rpc.SessionMcpDisableResult;
import com.github.copilot.sdk.generated.rpc.SessionMcpEnableParams;
import com.github.copilot.sdk.generated.rpc.SessionMcpEnableResult;
import com.github.copilot.sdk.generated.rpc.SessionMcpListParams;
import com.github.copilot.sdk.generated.rpc.SessionMcpListResult;
import com.github.copilot.sdk.generated.rpc.SessionMcpReloadParams;
import com.github.copilot.sdk.generated.rpc.SessionMcpReloadResult;
import com.github.copilot.sdk.generated.rpc.SessionModeGetParams;
import com.github.copilot.sdk.generated.rpc.SessionModeGetResult;
import com.github.copilot.sdk.generated.rpc.SessionModeSetParams;
import com.github.copilot.sdk.generated.rpc.SessionModeSetResult;
import com.github.copilot.sdk.generated.rpc.SessionModelGetCurrentParams;
import com.github.copilot.sdk.generated.rpc.SessionModelGetCurrentResult;
import com.github.copilot.sdk.generated.rpc.SessionModelSwitchToParams;
import com.github.copilot.sdk.generated.rpc.SessionModelSwitchToResult;
import com.github.copilot.sdk.generated.rpc.SessionPermissionsHandlePendingPermissionRequestParams;
import com.github.copilot.sdk.generated.rpc.SessionPermissionsHandlePendingPermissionRequestResult;
import com.github.copilot.sdk.generated.rpc.SessionPlanDeleteParams;
import com.github.copilot.sdk.generated.rpc.SessionPlanDeleteResult;
import com.github.copilot.sdk.generated.rpc.SessionPlanReadParams;
import com.github.copilot.sdk.generated.rpc.SessionPlanReadResult;
import com.github.copilot.sdk.generated.rpc.SessionPlanUpdateParams;
import com.github.copilot.sdk.generated.rpc.SessionPlanUpdateResult;
import com.github.copilot.sdk.generated.rpc.SessionPluginsListParams;
import com.github.copilot.sdk.generated.rpc.SessionPluginsListResult;
import com.github.copilot.sdk.generated.rpc.SessionShellExecParams;
import com.github.copilot.sdk.generated.rpc.SessionShellExecResult;
import com.github.copilot.sdk.generated.rpc.SessionShellKillParams;
import com.github.copilot.sdk.generated.rpc.SessionShellKillResult;
import com.github.copilot.sdk.generated.rpc.SessionSkillsDisableParams;
import com.github.copilot.sdk.generated.rpc.SessionSkillsDisableResult;
import com.github.copilot.sdk.generated.rpc.SessionSkillsEnableParams;
import com.github.copilot.sdk.generated.rpc.SessionSkillsEnableResult;
import com.github.copilot.sdk.generated.rpc.SessionSkillsListParams;
import com.github.copilot.sdk.generated.rpc.SessionSkillsListResult;
import com.github.copilot.sdk.generated.rpc.SessionSkillsReloadParams;
import com.github.copilot.sdk.generated.rpc.SessionSkillsReloadResult;
import com.github.copilot.sdk.generated.rpc.SessionToolsHandlePendingToolCallParams;
import com.github.copilot.sdk.generated.rpc.SessionToolsHandlePendingToolCallResult;
import com.github.copilot.sdk.generated.rpc.SessionUiElicitationParams;
import com.github.copilot.sdk.generated.rpc.SessionUiElicitationResult;
import com.github.copilot.sdk.generated.rpc.SessionUiHandlePendingElicitationParams;
import com.github.copilot.sdk.generated.rpc.SessionUiHandlePendingElicitationResult;
import com.github.copilot.sdk.generated.rpc.SessionUsageGetMetricsParams;
import com.github.copilot.sdk.generated.rpc.SessionUsageGetMetricsResult;
import com.github.copilot.sdk.generated.rpc.SessionWorkspaceCreateFileParams;
import com.github.copilot.sdk.generated.rpc.SessionWorkspaceCreateFileResult;
import com.github.copilot.sdk.generated.rpc.SessionWorkspaceListFilesParams;
import com.github.copilot.sdk.generated.rpc.SessionWorkspaceListFilesResult;
import com.github.copilot.sdk.generated.rpc.SessionWorkspaceReadFileParams;
import com.github.copilot.sdk.generated.rpc.SessionWorkspaceReadFileResult;
import com.github.copilot.sdk.generated.rpc.SessionsForkParams;
import com.github.copilot.sdk.generated.rpc.SessionsForkResult;
import com.github.copilot.sdk.generated.rpc.ToolsListParams;
import com.github.copilot.sdk.generated.rpc.ToolsListResult;

/**
 * Tests for generated RPC param and result record types. Exercises
 * constructors, field accessors, and enum variants to provide JaCoCo coverage
 * of the generated code without requiring network access.
 */
class GeneratedRpcRecordsCoverageTest {

    private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();

    // ── Params records ─────────────────────────────────────────────────────

    @Test
    void pingParams_record() {
        var params = new PingParams("hello");
        assertEquals("hello", params.message());
        assertNull(new PingParams(null).message());
    }

    @Test
    void pingResult_record() {
        var result = new PingResult("pong", 1234.0, 2.0);
        assertEquals("pong", result.message());
        assertEquals(1234.0, result.timestamp());
        assertEquals(2.0, result.protocolVersion());
    }

    @Test
    void mcpDiscoverParams_record() {
        var params = new McpDiscoverParams("/workspace");
        assertEquals("/workspace", params.workingDirectory());
        assertNull(new McpDiscoverParams(null).workingDirectory());
    }

    @Test
    void mcpConfigRemoveParams_record() {
        var params = new McpConfigRemoveParams("old-server");
        assertEquals("old-server", params.name());
    }

    @Test
    void mcpConfigUpdateParams_record() {
        var params = new McpConfigUpdateParams("my-server", Map.of("key", "val"));
        assertEquals("my-server", params.name());
        assertNotNull(params.config());
    }

    @Test
    void toolsListParams_record() {
        var params = new ToolsListParams("gpt-5");
        assertEquals("gpt-5", params.model());
        assertNull(new ToolsListParams(null).model());
    }

    @Test
    void sessionsForkParams_record() {
        var params = new SessionsForkParams("sess-1", "event-abc");
        assertEquals("sess-1", params.sessionId());
        assertEquals("event-abc", params.toEventId());
    }

    @Test
    void sessionAgentDeselectParams_record() {
        var params = new SessionAgentDeselectParams("sess-1");
        assertEquals("sess-1", params.sessionId());
    }

    @Test
    void sessionAgentGetCurrentParams_record() {
        var params = new SessionAgentGetCurrentParams("sess-2");
        assertEquals("sess-2", params.sessionId());
    }

    @Test
    void sessionAgentListParams_record() {
        var params = new SessionAgentListParams("sess-3");
        assertEquals("sess-3", params.sessionId());
    }

    @Test
    void sessionAgentReloadParams_record() {
        var params = new SessionAgentReloadParams("sess-4");
        assertEquals("sess-4", params.sessionId());
    }

    @Test
    void sessionAgentSelectParams_record() {
        var params = new SessionAgentSelectParams("sess-5", "my-agent");
        assertEquals("sess-5", params.sessionId());
        assertEquals("my-agent", params.name());
    }

    @Test
    void sessionCommandsHandlePendingCommandParams_record() {
        var params = new SessionCommandsHandlePendingCommandParams("sess-6", "req-cmd", "error msg");
        assertEquals("sess-6", params.sessionId());
        assertEquals("req-cmd", params.requestId());
        assertEquals("error msg", params.error());
    }

    @Test
    void sessionExtensionsDisableParams_record() {
        var params = new SessionExtensionsDisableParams("sess-7", "ext-id-1");
        assertEquals("sess-7", params.sessionId());
        assertEquals("ext-id-1", params.id());
    }

    @Test
    void sessionExtensionsEnableParams_record() {
        var params = new SessionExtensionsEnableParams("sess-8", "ext-id-2");
        assertEquals("sess-8", params.sessionId());
        assertEquals("ext-id-2", params.id());
    }

    @Test
    void sessionExtensionsListParams_record() {
        var params = new SessionExtensionsListParams("sess-9");
        assertEquals("sess-9", params.sessionId());
    }

    @Test
    void sessionExtensionsReloadParams_record() {
        var params = new SessionExtensionsReloadParams("sess-10");
        assertEquals("sess-10", params.sessionId());
    }

    @Test
    void sessionFleetStartParams_record() {
        var params = new SessionFleetStartParams("sess-11", "fix all bugs");
        assertEquals("sess-11", params.sessionId());
        assertEquals("fix all bugs", params.prompt());
    }

    @Test
    void sessionFsAppendFileParams_record() {
        var params = new SessionFsAppendFileParams("sess-12", "/tmp/log.txt", "new line\n", null);
        assertEquals("sess-12", params.sessionId());
        assertEquals("/tmp/log.txt", params.path());
        assertEquals("new line\n", params.content());
        assertNull(params.mode());
    }

    @Test
    void sessionFsExistsParams_record() {
        var params = new SessionFsExistsParams("sess-13", "/tmp/file.txt");
        assertEquals("sess-13", params.sessionId());
        assertEquals("/tmp/file.txt", params.path());
    }

    @Test
    void sessionFsMkdirParams_record() {
        var params = new SessionFsMkdirParams("sess-14", "/tmp/newdir", true, null);
        assertEquals("sess-14", params.sessionId());
        assertEquals("/tmp/newdir", params.path());
        assertTrue(params.recursive());
        assertNull(params.mode());
    }

    @Test
    void sessionFsReadFileParams_record() {
        var params = new SessionFsReadFileParams("sess-15", "/src/Main.java");
        assertEquals("sess-15", params.sessionId());
        assertEquals("/src/Main.java", params.path());
    }

    @Test
    void sessionFsReaddirParams_record() {
        var params = new SessionFsReaddirParams("sess-16", "/src");
        assertEquals("sess-16", params.sessionId());
        assertEquals("/src", params.path());
    }

    @Test
    void sessionFsReaddirWithTypesParams_record() {
        var params = new SessionFsReaddirWithTypesParams("sess-17", "/src");
        assertEquals("sess-17", params.sessionId());
        assertEquals("/src", params.path());
    }

    @Test
    void sessionFsRenameParams_record() {
        var params = new SessionFsRenameParams("sess-18", "/old.txt", "/new.txt");
        assertEquals("sess-18", params.sessionId());
        assertEquals("/old.txt", params.src());
        assertEquals("/new.txt", params.dest());
    }

    @Test
    void sessionFsRmParams_record() {
        var params = new SessionFsRmParams("sess-19", "/tmp/file.txt", false, true);
        assertEquals("sess-19", params.sessionId());
        assertEquals("/tmp/file.txt", params.path());
        assertFalse(params.recursive());
        assertTrue(params.force());
    }

    @Test
    void sessionFsSetProviderParams_conventions_enum() {
        assertEquals("windows", SessionFsSetProviderParams.SessionFsSetProviderParamsConventions.WINDOWS.getValue());
        assertEquals("posix", SessionFsSetProviderParams.SessionFsSetProviderParamsConventions.POSIX.getValue());
        assertEquals(SessionFsSetProviderParams.SessionFsSetProviderParamsConventions.POSIX,
                SessionFsSetProviderParams.SessionFsSetProviderParamsConventions.fromValue("posix"));
        assertThrows(IllegalArgumentException.class,
                () -> SessionFsSetProviderParams.SessionFsSetProviderParamsConventions.fromValue("unknown"));
    }

    @Test
    void sessionFsStatParams_record() {
        var params = new SessionFsStatParams("sess-20", "/etc/hosts");
        assertEquals("sess-20", params.sessionId());
        assertEquals("/etc/hosts", params.path());
    }

    @Test
    void sessionFsWriteFileParams_record() {
        var params = new SessionFsWriteFileParams("sess-21", "/tmp/out.txt", "content here", 0644.0);
        assertEquals("sess-21", params.sessionId());
        assertEquals("/tmp/out.txt", params.path());
        assertEquals("content here", params.content());
        assertEquals(0644.0, params.mode());
    }

    @Test
    void sessionHistoryCompactParams_record() {
        var params = new SessionHistoryCompactParams("sess-22");
        assertEquals("sess-22", params.sessionId());
    }

    @Test
    void sessionHistoryTruncateParams_record() {
        var params = new SessionHistoryTruncateParams("sess-23", "event-id-xyz");
        assertEquals("sess-23", params.sessionId());
        assertEquals("event-id-xyz", params.eventId());
    }

    @Test
    void sessionLogParams_record() {
        var params = new SessionLogParams("sess-24", "test message", SessionLogParams.SessionLogParamsLevel.INFO, false,
                null);
        assertEquals("sess-24", params.sessionId());
        assertEquals("test message", params.message());
        assertEquals(SessionLogParams.SessionLogParamsLevel.INFO, params.level());
        assertFalse(params.ephemeral());
        assertNull(params.url());
    }

    @Test
    void sessionLogParams_level_enum_all_values() {
        for (var level : SessionLogParams.SessionLogParamsLevel.values()) {
            assertNotNull(level.getValue());
            assertEquals(level, SessionLogParams.SessionLogParamsLevel.fromValue(level.getValue()));
        }
    }

    @Test
    void sessionMcpDisableParams_record() {
        var params = new SessionMcpDisableParams("sess-25", "mcp-server-1");
        assertEquals("sess-25", params.sessionId());
        assertEquals("mcp-server-1", params.serverName());
    }

    @Test
    void sessionMcpEnableParams_record() {
        var params = new SessionMcpEnableParams("sess-26", "mcp-server-2");
        assertEquals("sess-26", params.sessionId());
        assertEquals("mcp-server-2", params.serverName());
    }

    @Test
    void sessionMcpListParams_record() {
        var params = new SessionMcpListParams("sess-27");
        assertEquals("sess-27", params.sessionId());
    }

    @Test
    void sessionMcpReloadParams_record() {
        var params = new SessionMcpReloadParams("sess-28");
        assertEquals("sess-28", params.sessionId());
    }

    @Test
    void sessionModeGetParams_record() {
        var params = new SessionModeGetParams("sess-29");
        assertEquals("sess-29", params.sessionId());
    }

    @Test
    void sessionModeSetParams_record() {
        var params = new SessionModeSetParams("sess-30", SessionModeSetParams.SessionModeSetParamsMode.PLAN);
        assertEquals("sess-30", params.sessionId());
        assertEquals(SessionModeSetParams.SessionModeSetParamsMode.PLAN, params.mode());
    }

    @Test
    void sessionModeSetParams_mode_enum() {
        assertEquals("interactive", SessionModeSetParams.SessionModeSetParamsMode.INTERACTIVE.getValue());
        assertEquals("plan", SessionModeSetParams.SessionModeSetParamsMode.PLAN.getValue());
        assertEquals("autopilot", SessionModeSetParams.SessionModeSetParamsMode.AUTOPILOT.getValue());
        for (var mode : SessionModeSetParams.SessionModeSetParamsMode.values()) {
            assertEquals(mode, SessionModeSetParams.SessionModeSetParamsMode.fromValue(mode.getValue()));
        }
        assertThrows(IllegalArgumentException.class,
                () -> SessionModeSetParams.SessionModeSetParamsMode.fromValue("unknown-mode"));
    }

    @Test
    void sessionModelGetCurrentParams_record() {
        var params = new SessionModelGetCurrentParams("sess-31");
        assertEquals("sess-31", params.sessionId());
    }

    @Test
    void sessionModelSwitchToParams_record() {
        var params = new SessionModelSwitchToParams("sess-32", "claude-sonnet-4.5", "high", null);
        assertEquals("sess-32", params.sessionId());
        assertEquals("claude-sonnet-4.5", params.modelId());
        assertEquals("high", params.reasoningEffort());
        assertNull(params.modelCapabilities());
    }

    @Test
    void sessionPermissionsHandlePendingPermissionRequestParams_record() {
        var params = new SessionPermissionsHandlePendingPermissionRequestParams("sess-33", "req-1", "allow");
        assertEquals("sess-33", params.sessionId());
        assertEquals("req-1", params.requestId());
        assertEquals("allow", params.result());
    }

    @Test
    void sessionPlanDeleteParams_record() {
        var params = new SessionPlanDeleteParams("sess-34");
        assertEquals("sess-34", params.sessionId());
    }

    @Test
    void sessionPlanReadParams_record() {
        var params = new SessionPlanReadParams("sess-35");
        assertEquals("sess-35", params.sessionId());
    }

    @Test
    void sessionPlanUpdateParams_record() {
        var params = new SessionPlanUpdateParams("sess-36", "# My Plan\n1. Do stuff");
        assertEquals("sess-36", params.sessionId());
        assertEquals("# My Plan\n1. Do stuff", params.content());
    }

    @Test
    void sessionPluginsListParams_record() {
        var params = new SessionPluginsListParams("sess-37");
        assertEquals("sess-37", params.sessionId());
    }

    @Test
    void sessionShellExecParams_record() {
        var params = new SessionShellExecParams("sess-38", "ls -la", "/workspace", 5000.0);
        assertEquals("sess-38", params.sessionId());
        assertEquals("ls -la", params.command());
        assertEquals("/workspace", params.cwd());
        assertEquals(5000.0, params.timeout());
    }

    @Test
    void sessionShellKillParams_record() {
        var params = new SessionShellKillParams("sess-39", "proc-abc",
                SessionShellKillParams.SessionShellKillParamsSignal.SIGTERM);
        assertEquals("sess-39", params.sessionId());
        assertEquals("proc-abc", params.processId());
        assertEquals(SessionShellKillParams.SessionShellKillParamsSignal.SIGTERM, params.signal());
    }

    @Test
    void sessionShellKillParams_signal_enum() {
        assertEquals("SIGTERM", SessionShellKillParams.SessionShellKillParamsSignal.SIGTERM.getValue());
        assertEquals("SIGKILL", SessionShellKillParams.SessionShellKillParamsSignal.SIGKILL.getValue());
        assertEquals("SIGINT", SessionShellKillParams.SessionShellKillParamsSignal.SIGINT.getValue());
        for (var sig : SessionShellKillParams.SessionShellKillParamsSignal.values()) {
            assertEquals(sig, SessionShellKillParams.SessionShellKillParamsSignal.fromValue(sig.getValue()));
        }
        assertThrows(IllegalArgumentException.class,
                () -> SessionShellKillParams.SessionShellKillParamsSignal.fromValue("SIGHUP"));
    }

    @Test
    void sessionSkillsDisableParams_record() {
        var params = new SessionSkillsDisableParams("sess-40", "my-skill");
        assertEquals("sess-40", params.sessionId());
        assertEquals("my-skill", params.name());
    }

    @Test
    void sessionSkillsEnableParams_record() {
        var params = new SessionSkillsEnableParams("sess-41", "another-skill");
        assertEquals("sess-41", params.sessionId());
        assertEquals("another-skill", params.name());
    }

    @Test
    void sessionSkillsListParams_record() {
        var params = new SessionSkillsListParams("sess-42");
        assertEquals("sess-42", params.sessionId());
    }

    @Test
    void sessionSkillsReloadParams_record() {
        var params = new SessionSkillsReloadParams("sess-43");
        assertEquals("sess-43", params.sessionId());
    }

    @Test
    void sessionToolsHandlePendingToolCallParams_record() {
        var params = new SessionToolsHandlePendingToolCallParams("sess-44", "req-tool-1", "result data", null);
        assertEquals("sess-44", params.sessionId());
        assertEquals("req-tool-1", params.requestId());
        assertEquals("result data", params.result());
        assertNull(params.error());
    }

    @Test
    void sessionUiElicitationParams_record() {
        var params = new SessionUiElicitationParams("sess-45", "What is your name?", null);
        assertEquals("sess-45", params.sessionId());
        assertEquals("What is your name?", params.message());
        assertNull(params.requestedSchema());
    }

    @Test
    void sessionUiHandlePendingElicitationParams_record() {
        var params = new SessionUiHandlePendingElicitationParams("sess-46", "req-elicit-1", null);
        assertEquals("sess-46", params.sessionId());
        assertEquals("req-elicit-1", params.requestId());
        assertNull(params.result());
    }

    @Test
    void sessionUsageGetMetricsParams_record() {
        var params = new SessionUsageGetMetricsParams("sess-47");
        assertEquals("sess-47", params.sessionId());
    }

    @Test
    void sessionWorkspaceCreateFileParams_record() {
        var params = new SessionWorkspaceCreateFileParams("sess-48", "README.md", "# Hello");
        assertEquals("sess-48", params.sessionId());
        assertEquals("README.md", params.path());
        assertEquals("# Hello", params.content());
    }

    @Test
    void sessionWorkspaceListFilesParams_record() {
        var params = new SessionWorkspaceListFilesParams("sess-49");
        assertEquals("sess-49", params.sessionId());
    }

    @Test
    void sessionWorkspaceReadFileParams_record() {
        var params = new SessionWorkspaceReadFileParams("sess-50", "src/Main.java");
        assertEquals("sess-50", params.sessionId());
        assertEquals("src/Main.java", params.path());
    }

    // ── Result records ─────────────────────────────────────────────────────

    @Test
    void pingResult_fields() {
        var result = new PingResult("pong", 9999.0, 1.0);
        assertEquals("pong", result.message());
        assertEquals(9999.0, result.timestamp());
        assertEquals(1.0, result.protocolVersion());
    }

    @Test
    void sessionAgentDeselectResult_empty() {
        assertNotNull(new SessionAgentDeselectResult());
    }

    @Test
    void sessionAgentListResult_with_items() {
        var item = new SessionAgentListResult.SessionAgentListResultAgentsItem("name1", "Name One", "Desc 1");
        var result = new SessionAgentListResult(List.of(item));
        assertEquals(1, result.agents().size());
        assertEquals("name1", result.agents().get(0).name());
        assertEquals("Name One", result.agents().get(0).displayName());
        assertEquals("Desc 1", result.agents().get(0).description());
    }

    @Test
    void sessionAgentGetCurrentResult_nested() {
        var agent = new SessionAgentGetCurrentResult.SessionAgentGetCurrentResultAgent("agent-1", "Agent One",
                "Does things");
        var result = new SessionAgentGetCurrentResult(agent);
        assertEquals("agent-1", result.agent().name());
        assertEquals("Agent One", result.agent().displayName());
        assertEquals("Does things", result.agent().description());
    }

    @Test
    void sessionAgentGetCurrentResult_null_agent() {
        var result = new SessionAgentGetCurrentResult(null);
        assertNull(result.agent());
    }

    @Test
    void sessionAgentReloadResult_with_items() {
        var item = new SessionAgentReloadResult.SessionAgentReloadResultAgentsItem("a", "A", "Desc");
        var result = new SessionAgentReloadResult(List.of(item));
        assertEquals(1, result.agents().size());
        assertEquals("a", result.agents().get(0).name());
    }

    @Test
    void sessionAgentSelectResult_nested() {
        var agent = new SessionAgentSelectResult.SessionAgentSelectResultAgent("selected", "Selected",
                "The selected agent");
        var result = new SessionAgentSelectResult(agent);
        assertEquals("selected", result.agent().name());
    }

    @Test
    void sessionCommandsHandlePendingCommandResult_record() {
        var result = new SessionCommandsHandlePendingCommandResult(true);
        assertTrue(result.success());
        assertFalse(new SessionCommandsHandlePendingCommandResult(false).success());
    }

    @Test
    void sessionExtensionsDisableResult_empty() {
        assertNotNull(new SessionExtensionsDisableResult());
    }

    @Test
    void sessionExtensionsEnableResult_empty() {
        assertNotNull(new SessionExtensionsEnableResult());
    }

    @Test
    void sessionExtensionsListResult_nested() {
        var ext = new SessionExtensionsListResult.SessionExtensionsListResultExtensionsItem("ext-1", "My Extension",
                SessionExtensionsListResult.SessionExtensionsListResultExtensionsItem.SessionExtensionsListResultExtensionsItemSource.PROJECT,
                SessionExtensionsListResult.SessionExtensionsListResultExtensionsItem.SessionExtensionsListResultExtensionsItemStatus.RUNNING,
                1234L);
        var result = new SessionExtensionsListResult(List.of(ext));
        assertEquals(1, result.extensions().size());
        assertEquals("ext-1", result.extensions().get(0).id());
        assertEquals("My Extension", result.extensions().get(0).name());
        assertEquals(
                SessionExtensionsListResult.SessionExtensionsListResultExtensionsItem.SessionExtensionsListResultExtensionsItemSource.PROJECT,
                result.extensions().get(0).source());
        assertEquals(
                SessionExtensionsListResult.SessionExtensionsListResultExtensionsItem.SessionExtensionsListResultExtensionsItemStatus.RUNNING,
                result.extensions().get(0).status());
        assertEquals(1234L, result.extensions().get(0).pid());
    }

    @Test
    void sessionExtensionsListResult_enums() {
        for (var src : SessionExtensionsListResult.SessionExtensionsListResultExtensionsItem.SessionExtensionsListResultExtensionsItemSource
                .values()) {
            assertNotNull(src.getValue());
            assertEquals(src,
                    SessionExtensionsListResult.SessionExtensionsListResultExtensionsItem.SessionExtensionsListResultExtensionsItemSource
                            .fromValue(src.getValue()));
        }
        for (var status : SessionExtensionsListResult.SessionExtensionsListResultExtensionsItem.SessionExtensionsListResultExtensionsItemStatus
                .values()) {
            assertNotNull(status.getValue());
            assertEquals(status,
                    SessionExtensionsListResult.SessionExtensionsListResultExtensionsItem.SessionExtensionsListResultExtensionsItemStatus
                            .fromValue(status.getValue()));
        }
        assertThrows(IllegalArgumentException.class,
                () -> SessionExtensionsListResult.SessionExtensionsListResultExtensionsItem.SessionExtensionsListResultExtensionsItemSource
                        .fromValue("unknown"));
        assertThrows(IllegalArgumentException.class,
                () -> SessionExtensionsListResult.SessionExtensionsListResultExtensionsItem.SessionExtensionsListResultExtensionsItemStatus
                        .fromValue("unknown"));
    }

    @Test
    void sessionExtensionsReloadResult_empty() {
        assertNotNull(new SessionExtensionsReloadResult());
    }

    @Test
    void sessionFleetStartResult_record() {
        var result = new SessionFleetStartResult(true);
        assertTrue(result.started());
        assertFalse(new SessionFleetStartResult(false).started());
    }

    @Test
    void sessionFsExistsResult_record() {
        var result = new SessionFsExistsResult(true);
        assertTrue(result.exists());
        assertFalse(new SessionFsExistsResult(false).exists());
    }

    @Test
    void sessionFsReadFileResult_record() {
        var result = new SessionFsReadFileResult("file content here");
        assertEquals("file content here", result.content());
    }

    @Test
    void sessionFsReaddirResult_record() {
        var result = new SessionFsReaddirResult(List.of("file1.txt", "file2.txt"));
        assertEquals(2, result.entries().size());
        assertEquals("file1.txt", result.entries().get(0));
    }

    @Test
    void sessionFsReaddirWithTypesResult_nested() {
        var entry = new SessionFsReaddirWithTypesResult.SessionFsReaddirWithTypesResultEntriesItem("myfile.txt",
                SessionFsReaddirWithTypesResult.SessionFsReaddirWithTypesResultEntriesItem.SessionFsReaddirWithTypesResultEntriesItemType.FILE);
        var result = new SessionFsReaddirWithTypesResult(List.of(entry));
        assertEquals(1, result.entries().size());
        assertEquals("myfile.txt", result.entries().get(0).name());
        assertEquals(
                SessionFsReaddirWithTypesResult.SessionFsReaddirWithTypesResultEntriesItem.SessionFsReaddirWithTypesResultEntriesItemType.FILE,
                result.entries().get(0).type());
        assertEquals("file", result.entries().get(0).type().getValue());
    }

    @Test
    void sessionFsReaddirWithTypesResult_type_enum() {
        for (var t : SessionFsReaddirWithTypesResult.SessionFsReaddirWithTypesResultEntriesItem.SessionFsReaddirWithTypesResultEntriesItemType
                .values()) {
            assertNotNull(t.getValue());
            assertEquals(t,
                    SessionFsReaddirWithTypesResult.SessionFsReaddirWithTypesResultEntriesItem.SessionFsReaddirWithTypesResultEntriesItemType
                            .fromValue(t.getValue()));
        }
        assertThrows(IllegalArgumentException.class,
                () -> SessionFsReaddirWithTypesResult.SessionFsReaddirWithTypesResultEntriesItem.SessionFsReaddirWithTypesResultEntriesItemType
                        .fromValue("symlink"));
    }

    @Test
    void sessionFsSetProviderResult_record() {
        var result = new SessionFsSetProviderResult(true);
        assertTrue(result.success());
        assertFalse(new SessionFsSetProviderResult(false).success());
    }

    @Test
    void sessionFsStatResult_record() {
        var result = new SessionFsStatResult(true, false, 1024.0, "2026-01-01T00:00:00Z", "2025-12-01T00:00:00Z");
        assertTrue(result.isFile());
        assertFalse(result.isDirectory());
        assertEquals(1024.0, result.size());
        assertEquals("2026-01-01T00:00:00Z", result.mtime());
        assertEquals("2025-12-01T00:00:00Z", result.birthtime());
    }

    @Test
    void sessionHistoryCompactResult_nested() {
        var ctx = new SessionHistoryCompactResult.SessionHistoryCompactResultContextWindow(100000.0, 5000.0, 20.0,
                1000.0, 3000.0, 500.0);
        var result = new SessionHistoryCompactResult(true, 2000.0, 5.0, ctx);
        assertTrue(result.success());
        assertEquals(2000.0, result.tokensRemoved());
        assertEquals(5.0, result.messagesRemoved());
        assertNotNull(result.contextWindow());
        assertEquals(100000.0, result.contextWindow().tokenLimit());
        assertEquals(5000.0, result.contextWindow().currentTokens());
    }

    @Test
    void sessionHistoryTruncateResult_record() {
        var result = new SessionHistoryTruncateResult(3.0);
        assertEquals(3.0, result.eventsRemoved());
    }

    @Test
    void sessionLogResult_record() {
        var id = UUID.randomUUID();
        var result = new SessionLogResult(id);
        assertEquals(id, result.eventId());
    }

    @Test
    void sessionMcpDisableResult_empty() {
        assertNotNull(new SessionMcpDisableResult());
    }

    @Test
    void sessionMcpEnableResult_empty() {
        assertNotNull(new SessionMcpEnableResult());
    }

    @Test
    void sessionMcpListResult_nested() {
        var server = new SessionMcpListResult.SessionMcpListResultServersItem("my-mcp",
                SessionMcpListResult.SessionMcpListResultServersItem.SessionMcpListResultServersItemStatus.CONNECTED,
                "user", null);
        var result = new SessionMcpListResult(List.of(server));
        assertEquals(1, result.servers().size());
        assertEquals("my-mcp", result.servers().get(0).name());
        assertEquals(
                SessionMcpListResult.SessionMcpListResultServersItem.SessionMcpListResultServersItemStatus.CONNECTED,
                result.servers().get(0).status());
        assertEquals("user", result.servers().get(0).source());
    }

    @Test
    void sessionMcpListResult_status_enum_all_values() {
        for (var status : SessionMcpListResult.SessionMcpListResultServersItem.SessionMcpListResultServersItemStatus
                .values()) {
            assertNotNull(status.getValue());
            assertEquals(status,
                    SessionMcpListResult.SessionMcpListResultServersItem.SessionMcpListResultServersItemStatus
                            .fromValue(status.getValue()));
        }
        assertThrows(IllegalArgumentException.class,
                () -> SessionMcpListResult.SessionMcpListResultServersItem.SessionMcpListResultServersItemStatus
                        .fromValue("unknown-status"));
    }

    @Test
    void sessionMcpReloadResult_empty() {
        assertNotNull(new SessionMcpReloadResult());
    }

    @Test
    void sessionModeGetResult_enum() {
        var result = new SessionModeGetResult(SessionModeGetResult.SessionModeGetResultMode.INTERACTIVE);
        assertEquals(SessionModeGetResult.SessionModeGetResultMode.INTERACTIVE, result.mode());
        assertEquals("interactive", result.mode().getValue());
        for (var mode : SessionModeGetResult.SessionModeGetResultMode.values()) {
            assertEquals(mode, SessionModeGetResult.SessionModeGetResultMode.fromValue(mode.getValue()));
        }
        assertThrows(IllegalArgumentException.class,
                () -> SessionModeGetResult.SessionModeGetResultMode.fromValue("unknown"));
    }

    @Test
    void sessionModeSetResult_enum() {
        var result = new SessionModeSetResult(SessionModeSetResult.SessionModeSetResultMode.AUTOPILOT);
        assertEquals(SessionModeSetResult.SessionModeSetResultMode.AUTOPILOT, result.mode());
        assertEquals("autopilot", result.mode().getValue());
    }

    @Test
    void sessionModelGetCurrentResult_record() {
        var result = new SessionModelGetCurrentResult("claude-sonnet-4.5");
        assertEquals("claude-sonnet-4.5", result.modelId());
    }

    @Test
    void sessionModelSwitchToResult_record() {
        var result = new SessionModelSwitchToResult("gpt-5");
        assertEquals("gpt-5", result.modelId());
    }

    @Test
    void sessionPermissionsHandlePendingPermissionRequestResult_record() {
        var result = new SessionPermissionsHandlePendingPermissionRequestResult(true);
        assertTrue(result.success());
        assertFalse(new SessionPermissionsHandlePendingPermissionRequestResult(false).success());
    }

    @Test
    void sessionPlanDeleteResult_empty() {
        assertNotNull(new SessionPlanDeleteResult());
    }

    @Test
    void sessionPlanReadResult_record() {
        var result = new SessionPlanReadResult(true, "# Plan\n1. Do stuff", "/workspace/.plan");
        assertTrue(result.exists());
        assertEquals("# Plan\n1. Do stuff", result.content());
        assertEquals("/workspace/.plan", result.path());
    }

    @Test
    void sessionPlanUpdateResult_empty() {
        assertNotNull(new SessionPlanUpdateResult());
    }

    @Test
    void sessionPluginsListResult_nested() {
        var plugin = new SessionPluginsListResult.SessionPluginsListResultPluginsItem("my-plugin", "marketplace-x",
                "1.2.3", true);
        var result = new SessionPluginsListResult(List.of(plugin));
        assertEquals(1, result.plugins().size());
        assertEquals("my-plugin", result.plugins().get(0).name());
        assertEquals("marketplace-x", result.plugins().get(0).marketplace());
        assertEquals("1.2.3", result.plugins().get(0).version());
        assertTrue(result.plugins().get(0).enabled());
    }

    @Test
    void sessionShellExecResult_record() {
        var result = new SessionShellExecResult("proc-id-123");
        assertEquals("proc-id-123", result.processId());
    }

    @Test
    void sessionShellKillResult_record() {
        var result = new SessionShellKillResult(true);
        assertTrue(result.killed());
        assertFalse(new SessionShellKillResult(false).killed());
    }

    @Test
    void sessionSkillsDisableResult_empty() {
        assertNotNull(new SessionSkillsDisableResult());
    }

    @Test
    void sessionSkillsEnableResult_empty() {
        assertNotNull(new SessionSkillsEnableResult());
    }

    @Test
    void sessionSkillsListResult_nested() {
        var item = new SessionSkillsListResult.SessionSkillsListResultSkillsItem("deploy", "Deploy the app", "project",
                true, true, "/skills/deploy.md");
        var result = new SessionSkillsListResult(List.of(item));
        assertEquals(1, result.skills().size());
        assertEquals("deploy", result.skills().get(0).name());
        assertEquals("project", result.skills().get(0).source());
        assertTrue(result.skills().get(0).enabled());
    }

    @Test
    void sessionSkillsReloadResult_empty() {
        assertNotNull(new SessionSkillsReloadResult());
    }

    @Test
    void sessionToolsHandlePendingToolCallResult_record() {
        var result = new SessionToolsHandlePendingToolCallResult(true);
        assertTrue(result.success());
        assertFalse(new SessionToolsHandlePendingToolCallResult(false).success());
    }

    @Test
    void sessionUiElicitationResult_accept() {
        var result = new SessionUiElicitationResult(SessionUiElicitationResult.SessionUiElicitationResultAction.ACCEPT,
                Map.of("name", "Alice"));
        assertEquals(SessionUiElicitationResult.SessionUiElicitationResultAction.ACCEPT, result.action());
        assertEquals("Alice", result.content().get("name"));
    }

    @Test
    void sessionUiElicitationResult_action_enum() {
        assertEquals("accept", SessionUiElicitationResult.SessionUiElicitationResultAction.ACCEPT.getValue());
        assertEquals("decline", SessionUiElicitationResult.SessionUiElicitationResultAction.DECLINE.getValue());
        assertEquals("cancel", SessionUiElicitationResult.SessionUiElicitationResultAction.CANCEL.getValue());
        for (var a : SessionUiElicitationResult.SessionUiElicitationResultAction.values()) {
            assertEquals(a, SessionUiElicitationResult.SessionUiElicitationResultAction.fromValue(a.getValue()));
        }
        assertThrows(IllegalArgumentException.class,
                () -> SessionUiElicitationResult.SessionUiElicitationResultAction.fromValue("unknown"));
    }

    @Test
    void sessionUiHandlePendingElicitationResult_record() {
        var result = new SessionUiHandlePendingElicitationResult(true);
        assertTrue(result.success());
    }

    @Test
    void sessionUsageGetMetricsResult_nested() {
        var changes = new SessionUsageGetMetricsResult.SessionUsageGetMetricsResultCodeChanges(100L, 50L, 5L);
        var result = new SessionUsageGetMetricsResult(0.5, 10L, 2000.0, 1700000000000L, changes, null, "gpt-5", 1000L,
                500L);
        assertEquals(0.5, result.totalPremiumRequestCost());
        assertEquals(10L, result.totalUserRequests());
        assertNotNull(result.codeChanges());
        assertEquals(100L, result.codeChanges().linesAdded());
        assertEquals(50L, result.codeChanges().linesRemoved());
        assertEquals(5L, result.codeChanges().filesModifiedCount());
        assertEquals("gpt-5", result.currentModel());
    }

    @Test
    void sessionWorkspaceCreateFileResult_empty() {
        assertNotNull(new SessionWorkspaceCreateFileResult());
    }

    @Test
    void sessionWorkspaceListFilesResult_record() {
        var result = new SessionWorkspaceListFilesResult(List.of("src/Main.java", "README.md"));
        assertEquals(2, result.files().size());
        assertEquals("src/Main.java", result.files().get(0));
    }

    @Test
    void sessionWorkspaceReadFileResult_record() {
        var result = new SessionWorkspaceReadFileResult("public class Main {}");
        assertEquals("public class Main {}", result.content());
    }

    @Test
    void sessionsForkResult_record() {
        var result = new SessionsForkResult("forked-sess-id");
        assertEquals("forked-sess-id", result.sessionId());
    }

    // ── Complex nested result records ──────────────────────────────────────

    @Test
    void accountGetQuotaResult_nested() {
        var snapshot = new AccountGetQuotaResult.AccountGetQuotaResultQuotaSnapshotsValue(100.0, 40.0, 60.0, 5.0, true,
                "2026-05-01");
        var result = new AccountGetQuotaResult(Map.of("chat", snapshot));
        assertEquals(1, result.quotaSnapshots().size());
        var s = result.quotaSnapshots().get("chat");
        assertEquals(100.0, s.entitlementRequests());
        assertEquals(40.0, s.usedRequests());
        assertEquals(60.0, s.remainingPercentage());
        assertEquals(5.0, s.overage());
        assertTrue(s.overageAllowedWithExhaustedQuota());
        assertEquals("2026-05-01", s.resetDate());
    }

    @Test
    void mcpConfigListResult_record() {
        var result = new McpConfigListResult(Map.of("server1", "config1"));
        assertEquals(1, result.servers().size());
        assertEquals("config1", result.servers().get("server1"));
    }

    @Test
    void mcpDiscoverResult_nested() {
        var server = new McpDiscoverResult.McpDiscoverResultServersItem("discovered-server", "local",
                McpDiscoverResult.McpDiscoverResultServersItem.McpDiscoverResultServersItemSource.USER, true);
        var result = new McpDiscoverResult(List.of(server));
        assertEquals(1, result.servers().size());
        assertEquals("discovered-server", result.servers().get(0).name());
        assertEquals("local", result.servers().get(0).type());
        assertEquals(McpDiscoverResult.McpDiscoverResultServersItem.McpDiscoverResultServersItemSource.USER,
                result.servers().get(0).source());
        assertTrue(result.servers().get(0).enabled());
    }

    @Test
    void mcpDiscoverResult_source_enum_all_values() {
        for (var src : McpDiscoverResult.McpDiscoverResultServersItem.McpDiscoverResultServersItemSource.values()) {
            assertNotNull(src.getValue());
            assertEquals(src, McpDiscoverResult.McpDiscoverResultServersItem.McpDiscoverResultServersItemSource
                    .fromValue(src.getValue()));
        }
        assertThrows(IllegalArgumentException.class,
                () -> McpDiscoverResult.McpDiscoverResultServersItem.McpDiscoverResultServersItemSource
                        .fromValue("unknown-source"));
    }

    @Test
    void modelsListResult_nested() {
        var supports = new ModelsListResult.ModelsListResultModelsItem.ModelsListResultModelsItemCapabilities.ModelsListResultModelsItemCapabilitiesSupports(
                true, false);
        var limits = new ModelsListResult.ModelsListResultModelsItem.ModelsListResultModelsItemCapabilities.ModelsListResultModelsItemCapabilitiesLimits(
                100000.0, 8192.0, 128000.0, null);
        var capabilities = new ModelsListResult.ModelsListResultModelsItem.ModelsListResultModelsItemCapabilities(
                supports, limits);
        var policy = new ModelsListResult.ModelsListResultModelsItem.ModelsListResultModelsItemPolicy("active", null);
        var billing = new ModelsListResult.ModelsListResultModelsItem.ModelsListResultModelsItemBilling(1.0);
        var modelItem = new ModelsListResult.ModelsListResultModelsItem("gpt-5", "GPT-5", capabilities, policy, billing,
                null, null);
        var result = new ModelsListResult(List.of(modelItem));

        assertEquals(1, result.models().size());
        assertEquals("gpt-5", result.models().get(0).id());
        assertEquals("GPT-5", result.models().get(0).name());
        assertTrue(result.models().get(0).capabilities().supports().vision());
        assertFalse(result.models().get(0).capabilities().supports().reasoningEffort());
        assertEquals(100000.0, result.models().get(0).capabilities().limits().maxPromptTokens());
        assertEquals("active", result.models().get(0).policy().state());
        assertEquals(1.0, result.models().get(0).billing().multiplier());
    }

    @Test
    void toolsListResult_nested() {
        var tool = new ToolsListResult.ToolsListResultToolsItem("bash", "bash", "Run shell commands",
                Map.of("type", "object"), "Use for shell commands");
        var result = new ToolsListResult(List.of(tool));
        assertEquals(1, result.tools().size());
        assertEquals("bash", result.tools().get(0).name());
        assertEquals("bash", result.tools().get(0).namespacedName());
        assertEquals("Run shell commands", result.tools().get(0).description());
        assertEquals("Use for shell commands", result.tools().get(0).instructions());
    }

    // ── SessionModelSwitchToParams nested records ──────────────────────────

    @Test
    void sessionModelSwitchToParams_nested_records() {
        var limitsVision = new SessionModelSwitchToParams.SessionModelSwitchToParamsModelCapabilities.SessionModelSwitchToParamsModelCapabilitiesLimits.SessionModelSwitchToParamsModelCapabilitiesLimitsVision(
                List.of("image/png", "image/jpeg"), 10.0, 5000000.0);
        var limits = new SessionModelSwitchToParams.SessionModelSwitchToParamsModelCapabilities.SessionModelSwitchToParamsModelCapabilitiesLimits(
                100000.0, 8192.0, 128000.0, limitsVision);
        var supports = new SessionModelSwitchToParams.SessionModelSwitchToParamsModelCapabilities.SessionModelSwitchToParamsModelCapabilitiesSupports(
                true, true);
        var capabilities = new SessionModelSwitchToParams.SessionModelSwitchToParamsModelCapabilities(supports, limits);
        var params = new SessionModelSwitchToParams("sess-m", "gpt-5", null, capabilities);

        assertEquals("gpt-5", params.modelId());
        assertNotNull(params.modelCapabilities());
        assertTrue(params.modelCapabilities().supports().vision());
        assertTrue(params.modelCapabilities().supports().reasoningEffort());
        assertEquals(100000.0, params.modelCapabilities().limits().maxPromptTokens());
        assertEquals(2, params.modelCapabilities().limits().vision().supportedMediaTypes().size());
    }

    // ── SessionUiElicitationParams nested record ───────────────────────────

    @Test
    void sessionUiElicitationParams_nested_schema() {
        var schema = new SessionUiElicitationParams.SessionUiElicitationParamsRequestedSchema("object",
                Map.of("name", Map.of("type", "string")), List.of("name"));
        var params = new SessionUiElicitationParams("sess-elicit", "Please fill form", schema);
        assertEquals("sess-elicit", params.sessionId());
        assertEquals("object", params.requestedSchema().type());
        assertTrue(params.requestedSchema().required().contains("name"));
    }

    // ── SessionUiHandlePendingElicitationParams nested enum ────────────────

    @Test
    void sessionUiHandlePendingElicitationParamsResult_action_enum() {
        for (var action : SessionUiHandlePendingElicitationParams.SessionUiHandlePendingElicitationParamsResult.SessionUiHandlePendingElicitationParamsResultAction
                .values()) {
            assertNotNull(action.getValue());
            assertEquals(action,
                    SessionUiHandlePendingElicitationParams.SessionUiHandlePendingElicitationParamsResult.SessionUiHandlePendingElicitationParamsResultAction
                            .fromValue(action.getValue()));
        }
        assertThrows(IllegalArgumentException.class,
                () -> SessionUiHandlePendingElicitationParams.SessionUiHandlePendingElicitationParamsResult.SessionUiHandlePendingElicitationParamsResultAction
                        .fromValue("unknown"));
    }
}
