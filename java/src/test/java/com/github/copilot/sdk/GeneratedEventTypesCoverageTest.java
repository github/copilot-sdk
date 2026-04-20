/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.github.copilot.sdk.generated.*;

/**
 * Deserialization tests for generated session event types that are not covered
 * in {@link SessionEventDeserializationTest}. Verifies that each event
 * deserializes correctly from JSON and that the {@code type} discriminator and
 * {@code data} fields are accessible.
 */
public class GeneratedEventTypesCoverageTest {

    private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();

    private static SessionEvent parse(String json) throws Exception {
        return MAPPER.readValue(json, SessionEvent.class);
    }

    // ── AssistantStreamingDeltaEvent ───────────────────────────────────────

    @Test
    void testParseAssistantStreamingDeltaEvent() throws Exception {
        var event = parse("""
                {"type":"assistant.streaming_delta","data":{"totalResponseSizeBytes":1024.0}}
                """);
        assertInstanceOf(AssistantStreamingDeltaEvent.class, event);
        assertEquals("assistant.streaming_delta", event.getType());
        var typed = (AssistantStreamingDeltaEvent) event;
        assertEquals(1024.0, typed.getData().totalResponseSizeBytes());
    }

    // ── CapabilitiesChangedEvent ───────────────────────────────────────────

    @Test
    void testParseCapabilitiesChangedEvent() throws Exception {
        var event = parse("""
                {"type":"capabilities.changed","data":{"ui":{"elicitation":true}}}
                """);
        assertInstanceOf(CapabilitiesChangedEvent.class, event);
        assertEquals("capabilities.changed", event.getType());
        var typed = (CapabilitiesChangedEvent) event;
        assertNotNull(typed.getData());
        assertTrue(typed.getData().ui().elicitation());
    }

    @Test
    void testParseCapabilitiesChangedEventNoData() throws Exception {
        var event = parse("""
                {"type":"capabilities.changed"}
                """);
        assertInstanceOf(CapabilitiesChangedEvent.class, event);
    }

    // ── CommandQueuedEvent ─────────────────────────────────────────────────

    @Test
    void testParseCommandQueuedEvent() throws Exception {
        var event = parse("""
                {"type":"command.queued","data":{"requestId":"req-001","command":"/deploy"}}
                """);
        assertInstanceOf(CommandQueuedEvent.class, event);
        assertEquals("command.queued", event.getType());
        var typed = (CommandQueuedEvent) event;
        assertEquals("req-001", typed.getData().requestId());
        assertEquals("/deploy", typed.getData().command());
    }

    // ── CommandExecuteEvent ────────────────────────────────────────────────

    @Test
    void testParseCommandExecuteEvent() throws Exception {
        var event = parse(
                """
                        {"type":"command.execute","data":{"requestId":"req-002","command":"/help me","commandName":"help","args":"me"}}
                        """);
        assertInstanceOf(CommandExecuteEvent.class, event);
        assertEquals("command.execute", event.getType());
        var typed = (CommandExecuteEvent) event;
        assertEquals("req-002", typed.getData().requestId());
        assertEquals("help", typed.getData().commandName());
        assertEquals("me", typed.getData().args());
    }

    // ── CommandCompletedEvent ──────────────────────────────────────────────

    @Test
    void testParseCommandCompletedEvent() throws Exception {
        var event = parse("""
                {"type":"command.completed","data":{"requestId":"req-003"}}
                """);
        assertInstanceOf(CommandCompletedEvent.class, event);
        assertEquals("command.completed", event.getType());
        var typed = (CommandCompletedEvent) event;
        assertEquals("req-003", typed.getData().requestId());
    }

    // ── CommandsChangedEvent ───────────────────────────────────────────────

    @Test
    void testParseCommandsChangedEvent() throws Exception {
        var event = parse("""
                {"type":"commands.changed","data":{"commands":[{"name":"deploy","description":"Deploy to prod"}]}}
                """);
        assertInstanceOf(CommandsChangedEvent.class, event);
        assertEquals("commands.changed", event.getType());
        var typed = (CommandsChangedEvent) event;
        assertNotNull(typed.getData().commands());
        assertEquals(1, typed.getData().commands().size());
        assertEquals("deploy", typed.getData().commands().get(0).name());
        assertEquals("Deploy to prod", typed.getData().commands().get(0).description());
    }

    @Test
    void testParseCommandsChangedEventEmpty() throws Exception {
        var event = parse("""
                {"type":"commands.changed","data":{"commands":[]}}
                """);
        assertInstanceOf(CommandsChangedEvent.class, event);
        var typed = (CommandsChangedEvent) event;
        assertNotNull(typed.getData().commands());
        assertEquals(0, typed.getData().commands().size());
    }

    // ── ElicitationRequestedEvent ──────────────────────────────────────────

    @Test
    void testParseElicitationRequestedEvent() throws Exception {
        var event = parse(
                """
                        {"type":"elicitation.requested","data":{"requestId":"elicit-1","message":"Please enter your name","mode":"form"}}
                        """);
        assertInstanceOf(ElicitationRequestedEvent.class, event);
        assertEquals("elicitation.requested", event.getType());
        var typed = (ElicitationRequestedEvent) event;
        assertEquals("elicit-1", typed.getData().requestId());
        assertEquals("Please enter your name", typed.getData().message());
        assertEquals(ElicitationRequestedEvent.ElicitationRequestedEventData.ElicitationRequestedEventDataMode.FORM,
                typed.getData().mode());
    }

    @Test
    void testParseElicitationRequestedEventUrlMode() throws Exception {
        var event = parse(
                """
                        {"type":"elicitation.requested","data":{"requestId":"elicit-2","message":"Open browser","mode":"url","url":"https://example.com"}}
                        """);
        assertInstanceOf(ElicitationRequestedEvent.class, event);
        var typed = (ElicitationRequestedEvent) event;
        assertEquals(ElicitationRequestedEvent.ElicitationRequestedEventData.ElicitationRequestedEventDataMode.URL,
                typed.getData().mode());
        assertEquals("https://example.com", typed.getData().url());
    }

    // ── ElicitationCompletedEvent ──────────────────────────────────────────

    @Test
    void testParseElicitationCompletedEvent() throws Exception {
        var event = parse(
                """
                        {"type":"elicitation.completed","data":{"requestId":"elicit-1","action":"accept","content":{"name":"Alice"}}}
                        """);
        assertInstanceOf(ElicitationCompletedEvent.class, event);
        assertEquals("elicitation.completed", event.getType());
        var typed = (ElicitationCompletedEvent) event;
        assertEquals("elicit-1", typed.getData().requestId());
        assertEquals(ElicitationCompletedEvent.ElicitationCompletedEventData.ElicitationCompletedEventDataAction.ACCEPT,
                typed.getData().action());
        assertEquals("Alice", typed.getData().content().get("name"));
    }

    @Test
    void testParseElicitationCompletedEventDecline() throws Exception {
        var event = parse("""
                {"type":"elicitation.completed","data":{"requestId":"elicit-2","action":"decline"}}
                """);
        assertInstanceOf(ElicitationCompletedEvent.class, event);
        var typed = (ElicitationCompletedEvent) event;
        assertEquals(
                ElicitationCompletedEvent.ElicitationCompletedEventData.ElicitationCompletedEventDataAction.DECLINE,
                typed.getData().action());
    }

    @Test
    void testParseElicitationCompletedEventCancel() throws Exception {
        var event = parse("""
                {"type":"elicitation.completed","data":{"requestId":"elicit-3","action":"cancel"}}
                """);
        assertInstanceOf(ElicitationCompletedEvent.class, event);
        var typed = (ElicitationCompletedEvent) event;
        assertEquals(ElicitationCompletedEvent.ElicitationCompletedEventData.ElicitationCompletedEventDataAction.CANCEL,
                typed.getData().action());
    }

    // ── ExitPlanModeRequestedEvent ─────────────────────────────────────────

    @Test
    void testParseExitPlanModeRequestedEvent() throws Exception {
        var event = parse(
                """
                        {"type":"exit_plan_mode.requested","data":{"requestId":"epm-1","summary":"Implement login","planContent":"# Plan\\n1. Create login","actions":["approve","edit","reject"],"recommendedAction":"approve"}}
                        """);
        assertInstanceOf(ExitPlanModeRequestedEvent.class, event);
        assertEquals("exit_plan_mode.requested", event.getType());
        var typed = (ExitPlanModeRequestedEvent) event;
        assertEquals("epm-1", typed.getData().requestId());
        assertEquals("Implement login", typed.getData().summary());
        assertEquals("approve", typed.getData().recommendedAction());
        assertEquals(3, typed.getData().actions().size());
    }

    // ── ExitPlanModeCompletedEvent ─────────────────────────────────────────

    @Test
    void testParseExitPlanModeCompletedEvent() throws Exception {
        var event = parse("""
                {"type":"exit_plan_mode.completed","data":{"requestId":"epm-1","action":"approve"}}
                """);
        assertInstanceOf(ExitPlanModeCompletedEvent.class, event);
        assertEquals("exit_plan_mode.completed", event.getType());
        var typed = (ExitPlanModeCompletedEvent) event;
        assertNotNull(typed.getData());
    }

    // ── ExternalToolRequestedEvent ─────────────────────────────────────────

    @Test
    void testParseExternalToolRequestedEvent() throws Exception {
        var event = parse(
                """
                        {"type":"external_tool.requested","data":{"requestId":"ext-1","sessionId":"sess-abc","toolCallId":"tc-1","toolName":"myTool","arguments":{"key":"value"}}}
                        """);
        assertInstanceOf(ExternalToolRequestedEvent.class, event);
        assertEquals("external_tool.requested", event.getType());
        var typed = (ExternalToolRequestedEvent) event;
        assertEquals("ext-1", typed.getData().requestId());
        assertEquals("sess-abc", typed.getData().sessionId());
        assertEquals("myTool", typed.getData().toolName());
    }

    // ── ExternalToolCompletedEvent ─────────────────────────────────────────

    @Test
    void testParseExternalToolCompletedEvent() throws Exception {
        var event = parse("""
                {"type":"external_tool.completed","data":{"requestId":"ext-1"}}
                """);
        assertInstanceOf(ExternalToolCompletedEvent.class, event);
        assertEquals("external_tool.completed", event.getType());
        var typed = (ExternalToolCompletedEvent) event;
        assertEquals("ext-1", typed.getData().requestId());
    }

    // ── McpOauthRequiredEvent ──────────────────────────────────────────────

    @Test
    void testParseMcpOauthRequiredEvent() throws Exception {
        var event = parse(
                """
                        {"type":"mcp.oauth_required","data":{"requestId":"mcp-oauth-1","serverName":"my-mcp","serverUrl":"https://mcp.example.com"}}
                        """);
        assertInstanceOf(McpOauthRequiredEvent.class, event);
        assertEquals("mcp.oauth_required", event.getType());
        var typed = (McpOauthRequiredEvent) event;
        assertEquals("mcp-oauth-1", typed.getData().requestId());
        assertEquals("my-mcp", typed.getData().serverName());
        assertEquals("https://mcp.example.com", typed.getData().serverUrl());
    }

    @Test
    void testParseMcpOauthRequiredEventWithStaticConfig() throws Exception {
        var event = parse(
                """
                        {"type":"mcp.oauth_required","data":{"requestId":"mcp-oauth-2","serverName":"s","serverUrl":"https://s.com","staticClientConfig":{"clientId":"cid-123","publicClient":true}}}
                        """);
        assertInstanceOf(McpOauthRequiredEvent.class, event);
        var typed = (McpOauthRequiredEvent) event;
        assertEquals("cid-123", typed.getData().staticClientConfig().clientId());
        assertTrue(typed.getData().staticClientConfig().publicClient());
    }

    // ── McpOauthCompletedEvent ─────────────────────────────────────────────

    @Test
    void testParseMcpOauthCompletedEvent() throws Exception {
        var event = parse("""
                {"type":"mcp.oauth_completed","data":{"requestId":"mcp-oauth-1"}}
                """);
        assertInstanceOf(McpOauthCompletedEvent.class, event);
        assertEquals("mcp.oauth_completed", event.getType());
        var typed = (McpOauthCompletedEvent) event;
        assertEquals("mcp-oauth-1", typed.getData().requestId());
    }

    // ── PermissionRequestedEvent ───────────────────────────────────────────

    @Test
    void testParsePermissionRequestedEvent() throws Exception {
        var event = parse(
                """
                        {"type":"permission.requested","data":{"requestId":"perm-1","permissionRequest":{"tool":"bash"},"resolvedByHook":false}}
                        """);
        assertInstanceOf(PermissionRequestedEvent.class, event);
        assertEquals("permission.requested", event.getType());
        var typed = (PermissionRequestedEvent) event;
        assertEquals("perm-1", typed.getData().requestId());
        assertNotNull(typed.getData().permissionRequest());
        assertFalse(typed.getData().resolvedByHook());
    }

    @Test
    void testParsePermissionRequestedEventResolvedByHook() throws Exception {
        var event = parse("""
                {"type":"permission.requested","data":{"requestId":"perm-2","resolvedByHook":true}}
                """);
        assertInstanceOf(PermissionRequestedEvent.class, event);
        var typed = (PermissionRequestedEvent) event;
        assertTrue(typed.getData().resolvedByHook());
    }

    // ── PermissionCompletedEvent ───────────────────────────────────────────

    @Test
    void testParsePermissionCompletedEvent() throws Exception {
        var event = parse("""
                {"type":"permission.completed","data":{"requestId":"perm-1","decision":"allow"}}
                """);
        assertInstanceOf(PermissionCompletedEvent.class, event);
        assertEquals("permission.completed", event.getType());
        var typed = (PermissionCompletedEvent) event;
        assertNotNull(typed.getData());
    }

    // ── SamplingRequestedEvent ─────────────────────────────────────────────

    @Test
    void testParseSamplingRequestedEvent() throws Exception {
        var event = parse("""
                {"type":"sampling.requested","data":{"requestId":"samp-1","serverName":"my-mcp","mcpRequestId":42}}
                """);
        assertInstanceOf(SamplingRequestedEvent.class, event);
        assertEquals("sampling.requested", event.getType());
        var typed = (SamplingRequestedEvent) event;
        assertEquals("samp-1", typed.getData().requestId());
        assertEquals("my-mcp", typed.getData().serverName());
        assertNotNull(typed.getData().mcpRequestId());
    }

    // ── SamplingCompletedEvent ─────────────────────────────────────────────

    @Test
    void testParseSamplingCompletedEvent() throws Exception {
        var event = parse("""
                {"type":"sampling.completed","data":{"requestId":"samp-1"}}
                """);
        assertInstanceOf(SamplingCompletedEvent.class, event);
        assertEquals("sampling.completed", event.getType());
        var typed = (SamplingCompletedEvent) event;
        assertNotNull(typed.getData());
    }

    // ── SessionBackgroundTasksChangedEvent ─────────────────────────────────

    @Test
    void testParseSessionBackgroundTasksChangedEvent() throws Exception {
        var event = parse("""
                {"type":"session.background_tasks_changed","data":{}}
                """);
        assertInstanceOf(SessionBackgroundTasksChangedEvent.class, event);
        assertEquals("session.background_tasks_changed", event.getType());
        assertNotNull(((SessionBackgroundTasksChangedEvent) event).getData());
    }

    // ── SessionContextChangedEvent ─────────────────────────────────────────

    @Test
    void testParseSessionContextChangedEvent() throws Exception {
        var event = parse(
                """
                        {"type":"session.context_changed","data":{"cwd":"/workspace","gitRoot":"/workspace","repository":"myorg/myrepo","hostType":"github","branch":"main","headCommit":"abc123","baseCommit":"def456"}}
                        """);
        assertInstanceOf(SessionContextChangedEvent.class, event);
        assertEquals("session.context_changed", event.getType());
        var typed = (SessionContextChangedEvent) event;
        assertEquals("/workspace", typed.getData().cwd());
        assertEquals("myorg/myrepo", typed.getData().repository());
        assertEquals(
                SessionContextChangedEvent.SessionContextChangedEventData.SessionContextChangedEventDataHostType.GITHUB,
                typed.getData().hostType());
        assertEquals("main", typed.getData().branch());
    }

    @Test
    void testParseSessionContextChangedEventAdoHostType() throws Exception {
        var event = parse("""
                {"type":"session.context_changed","data":{"hostType":"ado"}}
                """);
        assertInstanceOf(SessionContextChangedEvent.class, event);
        var typed = (SessionContextChangedEvent) event;
        assertEquals(
                SessionContextChangedEvent.SessionContextChangedEventData.SessionContextChangedEventDataHostType.ADO,
                typed.getData().hostType());
    }

    // ── SessionCustomAgentsUpdatedEvent ────────────────────────────────────

    @Test
    void testParseSessionCustomAgentsUpdatedEvent() throws Exception {
        var event = parse(
                """
                        {"type":"session.custom_agents_updated","data":{"agents":[{"name":"my-agent","displayName":"My Agent","description":"Does stuff"}]}}
                        """);
        assertInstanceOf(SessionCustomAgentsUpdatedEvent.class, event);
        assertEquals("session.custom_agents_updated", event.getType());
        var typed = (SessionCustomAgentsUpdatedEvent) event;
        assertNotNull(typed.getData().agents());
        assertEquals(1, typed.getData().agents().size());
        assertEquals("my-agent", typed.getData().agents().get(0).name());
    }

    // ── SessionExtensionsLoadedEvent ───────────────────────────────────────

    @Test
    void testParseSessionExtensionsLoadedEvent() throws Exception {
        var event = parse(
                """
                        {"type":"session.extensions_loaded","data":{"extensions":[{"id":"ext-1","name":"My Extension","enabled":true}]}}
                        """);
        assertInstanceOf(SessionExtensionsLoadedEvent.class, event);
        assertEquals("session.extensions_loaded", event.getType());
        var typed = (SessionExtensionsLoadedEvent) event;
        assertNotNull(typed.getData().extensions());
        assertEquals(1, typed.getData().extensions().size());
        assertEquals("ext-1", typed.getData().extensions().get(0).id());
    }

    @Test
    void testParseSessionExtensionsLoadedEventEmpty() throws Exception {
        var event = parse("""
                {"type":"session.extensions_loaded","data":{"extensions":[]}}
                """);
        assertInstanceOf(SessionExtensionsLoadedEvent.class, event);
    }

    // ── SessionMcpServersLoadedEvent ───────────────────────────────────────

    @Test
    void testParseSessionMcpServersLoadedEvent() throws Exception {
        var event = parse(
                """
                        {"type":"session.mcp_servers_loaded","data":{"servers":[{"name":"mcp1","status":"connected","source":"user"}]}}
                        """);
        assertInstanceOf(SessionMcpServersLoadedEvent.class, event);
        assertEquals("session.mcp_servers_loaded", event.getType());
        var typed = (SessionMcpServersLoadedEvent) event;
        assertNotNull(typed.getData().servers());
        assertEquals(1, typed.getData().servers().size());
        assertEquals("mcp1", typed.getData().servers().get(0).name());
        assertEquals(
                SessionMcpServersLoadedEvent.SessionMcpServersLoadedEventData.SessionMcpServersLoadedEventDataServersItem.SessionMcpServersLoadedEventDataServersItemStatus.CONNECTED,
                typed.getData().servers().get(0).status());
    }

    @Test
    void testParseSessionMcpServersLoadedEventAllStatuses() throws Exception {
        // Verify all enum variants are parseable
        for (var status : new String[]{"connected", "failed", "needs-auth", "pending", "disabled", "not_configured"}) {
            var event = parse(
                    "{\"type\":\"session.mcp_servers_loaded\",\"data\":{\"servers\":[{\"name\":\"s\",\"status\":\""
                            + status + "\"}]}}");
            assertInstanceOf(SessionMcpServersLoadedEvent.class, event);
        }
    }

    // ── SessionMcpServerStatusChangedEvent ─────────────────────────────────

    @Test
    void testParseSessionMcpServerStatusChangedEvent() throws Exception {
        var event = parse("""
                {"type":"session.mcp_server_status_changed","data":{"name":"mcp1","status":"connected"}}
                """);
        assertInstanceOf(SessionMcpServerStatusChangedEvent.class, event);
        assertEquals("session.mcp_server_status_changed", event.getType());
        var typed = (SessionMcpServerStatusChangedEvent) event;
        assertNotNull(typed.getData());
    }

    // ── SessionRemoteSteerableChangedEvent ─────────────────────────────────

    @Test
    void testParseSessionRemoteSteerableChangedEvent() throws Exception {
        var event = parse("""
                {"type":"session.remote_steerable_changed","data":{"remoteSteerable":true}}
                """);
        assertInstanceOf(SessionRemoteSteerableChangedEvent.class, event);
        assertEquals("session.remote_steerable_changed", event.getType());
        var typed = (SessionRemoteSteerableChangedEvent) event;
        assertTrue(typed.getData().remoteSteerable());
    }

    @Test
    void testParseSessionRemoteSteerableChangedEventFalse() throws Exception {
        var event = parse("""
                {"type":"session.remote_steerable_changed","data":{"remoteSteerable":false}}
                """);
        assertInstanceOf(SessionRemoteSteerableChangedEvent.class, event);
        var typed = (SessionRemoteSteerableChangedEvent) event;
        assertFalse(typed.getData().remoteSteerable());
    }

    // ── SessionSkillsLoadedEvent ───────────────────────────────────────────

    @Test
    void testParseSessionSkillsLoadedEvent() throws Exception {
        var event = parse(
                """
                        {"type":"session.skills_loaded","data":{"skills":[{"name":"deploy","description":"Deploy app","source":"project","userInvocable":true,"enabled":true,"path":"/skills/deploy.md"}]}}
                        """);
        assertInstanceOf(SessionSkillsLoadedEvent.class, event);
        assertEquals("session.skills_loaded", event.getType());
        var typed = (SessionSkillsLoadedEvent) event;
        assertNotNull(typed.getData().skills());
        assertEquals(1, typed.getData().skills().size());
        var skill = typed.getData().skills().get(0);
        assertEquals("deploy", skill.name());
        assertEquals("project", skill.source());
        assertTrue(skill.userInvocable());
        assertTrue(skill.enabled());
    }

    // ── SessionTaskCompleteEvent ───────────────────────────────────────────

    @Test
    void testParseSessionTaskCompleteEvent() throws Exception {
        var event = parse("""
                {"type":"session.task_complete","data":{"summary":"All tests pass","success":true}}
                """);
        assertInstanceOf(SessionTaskCompleteEvent.class, event);
        assertEquals("session.task_complete", event.getType());
        var typed = (SessionTaskCompleteEvent) event;
        assertEquals("All tests pass", typed.getData().summary());
        assertTrue(typed.getData().success());
    }

    @Test
    void testParseSessionTaskCompleteEventFailure() throws Exception {
        var event = parse("""
                {"type":"session.task_complete","data":{"summary":"Build failed","success":false}}
                """);
        assertInstanceOf(SessionTaskCompleteEvent.class, event);
        var typed = (SessionTaskCompleteEvent) event;
        assertFalse(typed.getData().success());
    }

    // ── SessionTitleChangedEvent ───────────────────────────────────────────

    @Test
    void testParseSessionTitleChangedEvent() throws Exception {
        var event = parse("""
                {"type":"session.title_changed","data":{"title":"My new session title"}}
                """);
        assertInstanceOf(SessionTitleChangedEvent.class, event);
        assertEquals("session.title_changed", event.getType());
        var typed = (SessionTitleChangedEvent) event;
        assertEquals("My new session title", typed.getData().title());
    }

    // ── SessionToolsUpdatedEvent ───────────────────────────────────────────

    @Test
    void testParseSessionToolsUpdatedEvent() throws Exception {
        var event = parse("""
                {"type":"session.tools_updated","data":{"model":"gpt-5"}}
                """);
        assertInstanceOf(SessionToolsUpdatedEvent.class, event);
        assertEquals("session.tools_updated", event.getType());
        var typed = (SessionToolsUpdatedEvent) event;
        assertEquals("gpt-5", typed.getData().model());
    }

    // ── SessionWarningEvent ────────────────────────────────────────────────

    @Test
    void testParseSessionWarningEvent() throws Exception {
        var event = parse(
                """
                        {"type":"session.warning","data":{"warningType":"subscription","message":"Quota at 90%","url":"https://github.com/billing"}}
                        """);
        assertInstanceOf(SessionWarningEvent.class, event);
        assertEquals("session.warning", event.getType());
        var typed = (SessionWarningEvent) event;
        assertEquals("subscription", typed.getData().warningType());
        assertEquals("Quota at 90%", typed.getData().message());
        assertEquals("https://github.com/billing", typed.getData().url());
    }

    // ── SubagentDeselectedEvent ────────────────────────────────────────────

    @Test
    void testParseSubagentDeselectedEvent() throws Exception {
        var event = parse("""
                {"type":"subagent.deselected","data":{}}
                """);
        assertInstanceOf(SubagentDeselectedEvent.class, event);
        assertEquals("subagent.deselected", event.getType());
        assertNotNull(((SubagentDeselectedEvent) event).getData());
    }

    // ── SystemNotificationEvent ────────────────────────────────────────────

    @Test
    void testParseSystemNotificationEvent() throws Exception {
        var event = parse("""
                {"type":"system.notification","data":{"message":"Update available","level":"info"}}
                """);
        assertInstanceOf(SystemNotificationEvent.class, event);
        assertEquals("system.notification", event.getType());
        var typed = (SystemNotificationEvent) event;
        assertNotNull(typed.getData());
    }

    // ── UserInputRequestedEvent ────────────────────────────────────────────

    @Test
    void testParseUserInputRequestedEvent() throws Exception {
        var event = parse(
                """
                        {"type":"user_input.requested","data":{"requestId":"ui-1","question":"What is your name?","choices":["Alice","Bob"],"allowFreeform":true,"toolCallId":"tc-ui-1"}}
                        """);
        assertInstanceOf(UserInputRequestedEvent.class, event);
        assertEquals("user_input.requested", event.getType());
        var typed = (UserInputRequestedEvent) event;
        assertEquals("ui-1", typed.getData().requestId());
        assertEquals("What is your name?", typed.getData().question());
        assertEquals(2, typed.getData().choices().size());
        assertTrue(typed.getData().allowFreeform());
        assertEquals("tc-ui-1", typed.getData().toolCallId());
    }

    // ── UserInputCompletedEvent ────────────────────────────────────────────

    @Test
    void testParseUserInputCompletedEvent() throws Exception {
        var event = parse("""
                {"type":"user_input.completed","data":{"requestId":"ui-1","answer":"Alice","wasFreeform":false}}
                """);
        assertInstanceOf(UserInputCompletedEvent.class, event);
        assertEquals("user_input.completed", event.getType());
        var typed = (UserInputCompletedEvent) event;
        assertEquals("ui-1", typed.getData().requestId());
        assertEquals("Alice", typed.getData().answer());
        assertFalse(typed.getData().wasFreeform());
    }

    @Test
    void testParseUserInputCompletedEventFreeform() throws Exception {
        var event = parse(
                """
                        {"type":"user_input.completed","data":{"requestId":"ui-2","answer":"Custom response","wasFreeform":true}}
                        """);
        assertInstanceOf(UserInputCompletedEvent.class, event);
        var typed = (UserInputCompletedEvent) event;
        assertTrue(typed.getData().wasFreeform());
    }

    // ── Enum round-trip tests ──────────────────────────────────────────────

    @Test
    void testElicitationRequestedEventDataModeEnumValues() {
        assertEquals("form",
                ElicitationRequestedEvent.ElicitationRequestedEventData.ElicitationRequestedEventDataMode.FORM
                        .getValue());
        assertEquals("url",
                ElicitationRequestedEvent.ElicitationRequestedEventData.ElicitationRequestedEventDataMode.URL
                        .getValue());
    }

    @Test
    void testElicitationRequestedEventDataModeEnumFromValue() {
        assertEquals(ElicitationRequestedEvent.ElicitationRequestedEventData.ElicitationRequestedEventDataMode.FORM,
                ElicitationRequestedEvent.ElicitationRequestedEventData.ElicitationRequestedEventDataMode
                        .fromValue("form"));
        assertThrows(IllegalArgumentException.class,
                () -> ElicitationRequestedEvent.ElicitationRequestedEventData.ElicitationRequestedEventDataMode
                        .fromValue("unknown"));
    }

    @Test
    void testElicitationCompletedEventActionEnumValues() {
        assertEquals("accept",
                ElicitationCompletedEvent.ElicitationCompletedEventData.ElicitationCompletedEventDataAction.ACCEPT
                        .getValue());
        assertEquals("decline",
                ElicitationCompletedEvent.ElicitationCompletedEventData.ElicitationCompletedEventDataAction.DECLINE
                        .getValue());
        assertEquals("cancel",
                ElicitationCompletedEvent.ElicitationCompletedEventData.ElicitationCompletedEventDataAction.CANCEL
                        .getValue());
    }

    @Test
    void testSessionContextChangedHostTypeEnumFromValue() {
        assertEquals(
                SessionContextChangedEvent.SessionContextChangedEventData.SessionContextChangedEventDataHostType.GITHUB,
                SessionContextChangedEvent.SessionContextChangedEventData.SessionContextChangedEventDataHostType
                        .fromValue("github"));
        assertEquals(
                SessionContextChangedEvent.SessionContextChangedEventData.SessionContextChangedEventDataHostType.ADO,
                SessionContextChangedEvent.SessionContextChangedEventData.SessionContextChangedEventDataHostType
                        .fromValue("ado"));
        assertThrows(IllegalArgumentException.class,
                () -> SessionContextChangedEvent.SessionContextChangedEventData.SessionContextChangedEventDataHostType
                        .fromValue("unknown"));
    }

    @Test
    void testSessionMcpServersLoadedStatusEnumFromValue() {
        var s = SessionMcpServersLoadedEvent.SessionMcpServersLoadedEventData.SessionMcpServersLoadedEventDataServersItem.SessionMcpServersLoadedEventDataServersItemStatus.class;
        assertThrows(IllegalArgumentException.class,
                () -> SessionMcpServersLoadedEvent.SessionMcpServersLoadedEventData.SessionMcpServersLoadedEventDataServersItem.SessionMcpServersLoadedEventDataServersItemStatus
                        .fromValue("unknown"));
    }
}
