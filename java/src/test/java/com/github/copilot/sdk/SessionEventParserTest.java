/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.github.copilot.sdk.events.*;

/**
 * Tests for session event parsing.
 * <p>
 * These are unit tests that verify JSON deserialization works correctly for all
 * event types supported by the SDK.
 * </p>
 */
public class SessionEventParserTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Helper to convert a JSON string to a JsonNode and parse via
     * {@link SessionEventParser#parse(JsonNode)}.
     */
    private static AbstractSessionEvent parseJson(String json) throws Exception {
        JsonNode node = MAPPER.readTree(json);
        return SessionEventParser.parse(node);
    }

    // =========================================================================
    // Session Events
    // =========================================================================

    @Test
    void testParseSessionStartEvent() throws Exception {
        String json = """
                {
                    "type": "session.start",
                    "data": {
                        "sessionId": "sess-123",
                        "model": "gpt-4"
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(SessionStartEvent.class, event);
        assertEquals("session.start", event.getType());

        var startEvent = (SessionStartEvent) event;
        assertEquals("sess-123", startEvent.getData().sessionId());
    }

    @Test
    void testParseSessionResumeEvent() throws Exception {
        String json = """
                {
                    "type": "session.resume",
                    "data": {
                        "sessionId": "sess-456"
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(SessionResumeEvent.class, event);
        assertEquals("session.resume", event.getType());
    }

    @Test
    void testParseSessionErrorEvent() throws Exception {
        String json = """
                {
                    "type": "session.error",
                    "data": {
                        "errorType": "RateLimitError",
                        "message": "Rate limit exceeded",
                        "stack": "Error: Rate limit exceeded\\n    at processRequest"
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(SessionErrorEvent.class, event);
        assertEquals("session.error", event.getType());

        var errorEvent = (SessionErrorEvent) event;
        assertEquals("RateLimitError", errorEvent.getData().errorType());
        assertEquals("Rate limit exceeded", errorEvent.getData().message());
        assertNotNull(errorEvent.getData().stack());
    }

    @Test
    void testParseSessionIdleEvent() throws Exception {
        String json = """
                {
                    "type": "session.idle",
                    "data": {}
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(SessionIdleEvent.class, event);
        assertEquals("session.idle", event.getType());
    }

    @Test
    void testParseSessionInfoEvent() throws Exception {
        String json = """
                {
                    "type": "session.info",
                    "data": {
                        "infoType": "status",
                        "message": "Processing request"
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(SessionInfoEvent.class, event);
        assertEquals("session.info", event.getType());

        var infoEvent = (SessionInfoEvent) event;
        assertEquals("status", infoEvent.getData().infoType());
        assertEquals("Processing request", infoEvent.getData().message());
    }

    @Test
    void testParseSessionModelChangeEvent() throws Exception {
        String json = """
                {
                    "type": "session.model_change",
                    "data": {
                        "previousModel": "gpt-4",
                        "newModel": "gpt-4-turbo"
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(SessionModelChangeEvent.class, event);
        assertEquals("session.model_change", event.getType());
    }

    @Test
    void testParseSessionHandoffEvent() throws Exception {
        String json = """
                {
                    "type": "session.handoff",
                    "data": {
                        "targetAgent": "code-review-agent"
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(SessionHandoffEvent.class, event);
        assertEquals("session.handoff", event.getType());
    }

    @Test
    void testParseSessionTruncationEvent() throws Exception {
        String json = """
                {
                    "type": "session.truncation",
                    "data": {
                        "reason": "context_limit"
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(SessionTruncationEvent.class, event);
        assertEquals("session.truncation", event.getType());
    }

    @Test
    void testParseSessionSnapshotRewindEvent() throws Exception {
        String json = """
                {
                    "type": "session.snapshot_rewind",
                    "data": {
                        "snapshotId": "snap-123"
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(SessionSnapshotRewindEvent.class, event);
        assertEquals("session.snapshot_rewind", event.getType());
    }

    @Test
    void testParseSessionUsageInfoEvent() throws Exception {
        String json = """
                {
                    "type": "session.usage_info",
                    "data": {
                        "tokenCount": 1500
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(SessionUsageInfoEvent.class, event);
        assertEquals("session.usage_info", event.getType());
    }

    @Test
    void testParseSessionCompactionStartEvent() throws Exception {
        String json = """
                {
                    "type": "session.compaction_start",
                    "data": {}
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(SessionCompactionStartEvent.class, event);
        assertEquals("session.compaction_start", event.getType());
    }

    @Test
    void testParseSessionCompactionCompleteEvent() throws Exception {
        String json = """
                {
                    "type": "session.compaction_complete",
                    "data": {}
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(SessionCompactionCompleteEvent.class, event);
        assertEquals("session.compaction_complete", event.getType());
    }

    // =========================================================================
    // User Events
    // =========================================================================

    @Test
    void testParseUserMessageEvent() throws Exception {
        String json = """
                {
                    "type": "user.message",
                    "data": {
                        "messageId": "msg-123",
                        "content": "Hello, Copilot!"
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(UserMessageEvent.class, event);
        assertEquals("user.message", event.getType());
    }

    @Test
    void testParsePendingMessagesModifiedEvent() throws Exception {
        String json = """
                {
                    "type": "pending_messages.modified",
                    "data": {
                        "count": 3
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(PendingMessagesModifiedEvent.class, event);
        assertEquals("pending_messages.modified", event.getType());
    }

    // =========================================================================
    // Assistant Events
    // =========================================================================

    @Test
    void testParseAssistantTurnStartEvent() throws Exception {
        String json = """
                {
                    "type": "assistant.turn_start",
                    "data": {
                        "turnId": "turn-123"
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(AssistantTurnStartEvent.class, event);
        assertEquals("assistant.turn_start", event.getType());

        var turnEvent = (AssistantTurnStartEvent) event;
        assertEquals("turn-123", turnEvent.getData().turnId());
    }

    @Test
    void testParseAssistantIntentEvent() throws Exception {
        String json = """
                {
                    "type": "assistant.intent",
                    "data": {
                        "intent": "code_generation"
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(AssistantIntentEvent.class, event);
        assertEquals("assistant.intent", event.getType());
    }

    @Test
    void testParseAssistantReasoningEvent() throws Exception {
        String json = """
                {
                    "type": "assistant.reasoning",
                    "data": {
                        "reasoningId": "reason-123",
                        "content": "Analyzing the code structure..."
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(AssistantReasoningEvent.class, event);
        assertEquals("assistant.reasoning", event.getType());

        var reasoningEvent = (AssistantReasoningEvent) event;
        assertEquals("reason-123", reasoningEvent.getData().reasoningId());
        assertEquals("Analyzing the code structure...", reasoningEvent.getData().content());
    }

    @Test
    void testParseAssistantReasoningDeltaEvent() throws Exception {
        String json = """
                {
                    "type": "assistant.reasoning_delta",
                    "data": {
                        "reasoningId": "reason-123",
                        "delta": "Considering options..."
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(AssistantReasoningDeltaEvent.class, event);
        assertEquals("assistant.reasoning_delta", event.getType());
    }

    @Test
    void testParseAssistantMessageEvent() throws Exception {
        String json = """
                {
                    "type": "assistant.message",
                    "data": {
                        "messageId": "msg-456",
                        "content": "Here is the code you requested."
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(AssistantMessageEvent.class, event);
        assertEquals("assistant.message", event.getType());

        var msgEvent = (AssistantMessageEvent) event;
        assertEquals("Here is the code you requested.", msgEvent.getData().content());
    }

    @Test
    void testParseAssistantMessageDeltaEvent() throws Exception {
        String json = """
                {
                    "type": "assistant.message_delta",
                    "data": {
                        "messageId": "msg-456",
                        "delta": "Here is"
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(AssistantMessageDeltaEvent.class, event);
        assertEquals("assistant.message_delta", event.getType());
    }

    @Test
    void testParseAssistantTurnEndEvent() throws Exception {
        String json = """
                {
                    "type": "assistant.turn_end",
                    "data": {
                        "turnId": "turn-123"
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(AssistantTurnEndEvent.class, event);
        assertEquals("assistant.turn_end", event.getType());
    }

    @Test
    void testParseAssistantUsageEvent() throws Exception {
        String json = """
                {
                    "type": "assistant.usage",
                    "data": {
                        "promptTokens": 100,
                        "completionTokens": 50,
                        "totalTokens": 150
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(AssistantUsageEvent.class, event);
        assertEquals("assistant.usage", event.getType());
    }

    // =========================================================================
    // Tool Events
    // =========================================================================

    @Test
    void testParseToolUserRequestedEvent() throws Exception {
        String json = """
                {
                    "type": "tool.user_requested",
                    "data": {
                        "toolName": "read_file",
                        "userRequest": "Please read the config file"
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(ToolUserRequestedEvent.class, event);
        assertEquals("tool.user_requested", event.getType());
    }

    @Test
    void testParseToolExecutionStartEvent() throws Exception {
        String json = """
                {
                    "type": "tool.execution_start",
                    "data": {
                        "toolCallId": "call-123",
                        "toolName": "read_file"
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(ToolExecutionStartEvent.class, event);
        assertEquals("tool.execution_start", event.getType());
    }

    @Test
    void testParseToolExecutionPartialResultEvent() throws Exception {
        String json = """
                {
                    "type": "tool.execution_partial_result",
                    "data": {
                        "toolCallId": "call-123",
                        "partialResult": "Reading file..."
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(ToolExecutionPartialResultEvent.class, event);
        assertEquals("tool.execution_partial_result", event.getType());
    }

    @Test
    void testParseToolExecutionProgressEvent() throws Exception {
        String json = """
                {
                    "type": "tool.execution_progress",
                    "data": {
                        "toolCallId": "call-123",
                        "progress": 50
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(ToolExecutionProgressEvent.class, event);
        assertEquals("tool.execution_progress", event.getType());
    }

    @Test
    void testParseToolExecutionCompleteEvent() throws Exception {
        String json = """
                {
                    "type": "tool.execution_complete",
                    "data": {
                        "toolCallId": "call-123",
                        "success": true,
                        "result": {
                            "type": "text",
                            "content": "File contents here"
                        }
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(ToolExecutionCompleteEvent.class, event);
        assertEquals("tool.execution_complete", event.getType());

        var completeEvent = (ToolExecutionCompleteEvent) event;
        assertTrue(completeEvent.getData().success());
    }

    // =========================================================================
    // Subagent Events
    // =========================================================================

    @Test
    void testParseSubagentStartedEvent() throws Exception {
        String json = """
                {
                    "type": "subagent.started",
                    "data": {
                        "toolCallId": "call-789",
                        "agentName": "code-review",
                        "agentDisplayName": "Code Review Agent",
                        "agentDescription": "Reviews code for best practices"
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(SubagentStartedEvent.class, event);
        assertEquals("subagent.started", event.getType());

        var startedEvent = (SubagentStartedEvent) event;
        assertEquals("code-review", startedEvent.getData().agentName());
        assertEquals("Code Review Agent", startedEvent.getData().agentDisplayName());
    }

    @Test
    void testParseSubagentCompletedEvent() throws Exception {
        String json = """
                {
                    "type": "subagent.completed",
                    "data": {
                        "toolCallId": "call-789",
                        "result": "Review completed successfully"
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(SubagentCompletedEvent.class, event);
        assertEquals("subagent.completed", event.getType());
    }

    @Test
    void testParseSubagentFailedEvent() throws Exception {
        String json = """
                {
                    "type": "subagent.failed",
                    "data": {
                        "toolCallId": "call-789",
                        "error": "Agent timeout"
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(SubagentFailedEvent.class, event);
        assertEquals("subagent.failed", event.getType());
    }

    @Test
    void testParseSubagentSelectedEvent() throws Exception {
        String json = """
                {
                    "type": "subagent.selected",
                    "data": {
                        "agentName": "documentation-agent"
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(SubagentSelectedEvent.class, event);
        assertEquals("subagent.selected", event.getType());
    }

    // =========================================================================
    // Hook Events
    // =========================================================================

    @Test
    void testParseHookStartEvent() throws Exception {
        String json = """
                {
                    "type": "hook.start",
                    "data": {
                        "hookInvocationId": "hook-123",
                        "hookType": "preToolUse",
                        "input": {"toolName": "read_file"}
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(HookStartEvent.class, event);
        assertEquals("hook.start", event.getType());

        var hookEvent = (HookStartEvent) event;
        assertEquals("hook-123", hookEvent.getData().hookInvocationId());
        assertEquals("preToolUse", hookEvent.getData().hookType());
    }

    @Test
    void testParseHookEndEvent() throws Exception {
        String json = """
                {
                    "type": "hook.end",
                    "data": {
                        "hookInvocationId": "hook-123",
                        "success": true
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(HookEndEvent.class, event);
        assertEquals("hook.end", event.getType());
    }

    // =========================================================================
    // Other Events
    // =========================================================================

    @Test
    void testParseAbortEvent() throws Exception {
        String json = """
                {
                    "type": "abort",
                    "data": {
                        "reason": "user_requested"
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(AbortEvent.class, event);
        assertEquals("abort", event.getType());
    }

    @Test
    void testParseSystemMessageEvent() throws Exception {
        String json = """
                {
                    "type": "system.message",
                    "data": {
                        "content": "System is ready"
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(SystemMessageEvent.class, event);
        assertEquals("system.message", event.getType());
    }

    @Test
    void testParseSessionShutdownEvent() throws Exception {
        String json = """
                {
                    "type": "session.shutdown",
                    "data": {
                        "shutdownType": "routine",
                        "totalPremiumRequests": 5,
                        "totalApiDurationMs": 1234.5,
                        "sessionStartTime": 1612345678000,
                        "codeChanges": {
                            "linesAdded": 10,
                            "linesRemoved": 3,
                            "filesModified": ["file1.java", "file2.java"]
                        },
                        "modelMetrics": {},
                        "currentModel": "gpt-4"
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(SessionShutdownEvent.class, event);
        assertEquals("session.shutdown", event.getType());

        var shutdownEvent = (SessionShutdownEvent) event;
        assertEquals(SessionShutdownEvent.ShutdownType.ROUTINE, shutdownEvent.getData().shutdownType());
        assertEquals(5, shutdownEvent.getData().totalPremiumRequests());
        assertEquals("gpt-4", shutdownEvent.getData().currentModel());
        assertNotNull(shutdownEvent.getData().codeChanges());
        assertEquals(10, shutdownEvent.getData().codeChanges().linesAdded());
    }

    @Test
    void testParseSkillInvokedEvent() throws Exception {
        String json = """
                {
                    "type": "skill.invoked",
                    "data": {
                        "name": "code-review",
                        "path": "/path/to/skill",
                        "content": "Skill instructions here",
                        "allowedTools": ["view", "edit", "grep"]
                    }
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(SkillInvokedEvent.class, event);
        assertEquals("skill.invoked", event.getType());

        var skillEvent = (SkillInvokedEvent) event;
        assertEquals("code-review", skillEvent.getData().name());
        assertEquals("/path/to/skill", skillEvent.getData().path());
        assertEquals("Skill instructions here", skillEvent.getData().content());
        assertNotNull(skillEvent.getData().allowedTools());
        assertEquals(3, skillEvent.getData().allowedTools().size());
    }

    // =========================================================================
    // Edge Cases
    // =========================================================================

    @Test
    void testParseUnknownEventType() throws Exception {
        // Unknown types log at FINE level, no need to suppress
        String json = """
                {
                    "type": "unknown.event.type",
                    "data": {}
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNull(event, "Unknown event types should return null");
    }

    @Test
    void testParseMissingTypeField() throws Exception {
        // Suppress logging for this test since missing type logs a WARNING
        Logger parserLogger = Logger.getLogger(SessionEventParser.class.getName());
        Level originalLevel = parserLogger.getLevel();
        parserLogger.setLevel(Level.OFF);

        try {
            String json = """
                    {
                        "data": {
                            "content": "Hello"
                        }
                    }
                    """;

            AbstractSessionEvent event = parseJson(json);
            assertNull(event, "Events without type field should return null");
        } finally {
            parserLogger.setLevel(originalLevel);
        }
    }

    @Test
    void testParseEventWithUnknownFields() throws Exception {
        // Should not fail when there are extra unknown fields
        String json = """
                {
                    "type": "session.idle",
                    "data": {
                        "unknownField": "value",
                        "anotherUnknown": 123
                    },
                    "extraTopLevel": true
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event, "Events with unknown fields should still parse");
        assertInstanceOf(SessionIdleEvent.class, event);
    }

    @Test
    void testParseEmptyJson() throws Exception {
        // Suppress logging for this test since empty JSON logs a WARNING
        Logger parserLogger = Logger.getLogger(SessionEventParser.class.getName());
        Level originalLevel = parserLogger.getLevel();
        parserLogger.setLevel(Level.OFF);

        try {
            String json = "{}";

            AbstractSessionEvent event = parseJson(json);
            assertNull(event, "Empty JSON should return null due to missing type");
        } finally {
            parserLogger.setLevel(originalLevel);
        }
    }

    // =========================================================================
    // All event types in one test
    // =========================================================================

    @Test
    void testParseAllEventTypes() throws Exception {
        String[] types = {"session.start", "session.resume", "session.error", "session.idle", "session.info",
                "session.model_change", "session.handoff", "session.truncation", "session.snapshot_rewind",
                "session.usage_info", "session.compaction_start", "session.compaction_complete", "user.message",
                "pending_messages.modified", "assistant.turn_start", "assistant.intent", "assistant.reasoning",
                "assistant.reasoning_delta", "assistant.message", "assistant.message_delta", "assistant.turn_end",
                "assistant.usage", "abort", "tool.user_requested", "tool.execution_start",
                "tool.execution_partial_result", "tool.execution_progress", "tool.execution_complete",
                "subagent.started", "subagent.completed", "subagent.failed", "subagent.selected", "hook.start",
                "hook.end", "system.message", "session.shutdown", "skill.invoked"};

        for (String type : types) {
            String json = """
                    {
                        "type": "%s",
                        "data": {}
                    }
                    """.formatted(type);
            AbstractSessionEvent event = parseJson(json);
            assertNotNull(event, "Event type '%s' should parse".formatted(type));
            assertEquals(type, event.getType(), "Parsed type should match for '%s'".formatted(type));
        }
    }

    // =========================================================================
    // AbstractSessionEvent base fields
    // =========================================================================

    @Test
    void testParseBaseFieldsId() throws Exception {
        String uuid = "550e8400-e29b-41d4-a716-446655440000";
        String json = """
                {
                    "type": "session.idle",
                    "id": "%s",
                    "data": {}
                }
                """.formatted(uuid);

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertEquals(UUID.fromString(uuid), event.getId());
    }

    @Test
    void testParseBaseFieldsParentId() throws Exception {
        String parentUuid = "660e8400-e29b-41d4-a716-446655440001";
        String json = """
                {
                    "type": "session.idle",
                    "parentId": "%s",
                    "data": {}
                }
                """.formatted(parentUuid);

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertEquals(UUID.fromString(parentUuid), event.getParentId());
    }

    @Test
    void testParseBaseFieldsEphemeral() throws Exception {
        String json = """
                {
                    "type": "session.idle",
                    "ephemeral": true,
                    "data": {}
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertTrue(event.getEphemeral());
    }

    @Test
    void testParseBaseFieldsTimestamp() throws Exception {
        String json = """
                {
                    "type": "session.idle",
                    "timestamp": "2025-01-15T10:30:00Z",
                    "data": {}
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertNotNull(event.getTimestamp());
    }

    @Test
    void testParseBaseFieldsAllTogether() throws Exception {
        String uuid = "550e8400-e29b-41d4-a716-446655440000";
        String parentUuid = "660e8400-e29b-41d4-a716-446655440001";
        String json = """
                {
                    "type": "assistant.message",
                    "id": "%s",
                    "parentId": "%s",
                    "ephemeral": false,
                    "timestamp": "2025-06-15T12:00:00+02:00",
                    "data": {
                        "content": "Hello"
                    }
                }
                """.formatted(uuid, parentUuid);

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertEquals(UUID.fromString(uuid), event.getId());
        assertEquals(UUID.fromString(parentUuid), event.getParentId());
        assertFalse(event.getEphemeral());
        assertNotNull(event.getTimestamp());
        assertInstanceOf(AssistantMessageEvent.class, event);
        assertEquals("Hello", ((AssistantMessageEvent) event).getData().content());
    }

    @Test
    void testParseBaseFieldsNullWhenAbsent() throws Exception {
        String json = """
                {
                    "type": "session.idle",
                    "data": {}
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertNull(event.getId());
        assertNull(event.getParentId());
        assertNull(event.getEphemeral());
        assertNull(event.getTimestamp());
    }

    // =========================================================================
    // Rich data field assertions
    // =========================================================================

    @Test
    void testSessionStartEventAllFields() throws Exception {
        String json = """
                {
                    "type": "session.start",
                    "data": {
                        "sessionId": "sess-full",
                        "version": 2.0,
                        "producer": "copilot-cli",
                        "copilotVersion": "1.2.3",
                        "startTime": "2025-03-01T08:00:00Z",
                        "selectedModel": "gpt-4-turbo"
                    }
                }
                """;

        var event = (SessionStartEvent) parseJson(json);
        assertNotNull(event);
        var data = event.getData();
        assertEquals("sess-full", data.sessionId());
        assertEquals(2.0, data.version());
        assertEquals("copilot-cli", data.producer());
        assertEquals("1.2.3", data.copilotVersion());
        assertNotNull(data.startTime());
        assertEquals("gpt-4-turbo", data.selectedModel());
    }

    @Test
    void testSessionResumeEventAllFields() throws Exception {
        String json = """
                {
                    "type": "session.resume",
                    "data": {
                        "resumeTime": "2025-04-10T09:30:00Z",
                        "eventCount": 42
                    }
                }
                """;

        var event = (SessionResumeEvent) parseJson(json);
        assertNotNull(event);
        var data = event.getData();
        assertNotNull(data.resumeTime());
        assertEquals(42.0, data.eventCount());
    }

    @Test
    void testSessionErrorEventAllFields() throws Exception {
        String json = """
                {
                    "type": "session.error",
                    "data": {
                        "errorType": "InternalError",
                        "message": "Something went wrong",
                        "stack": "at line 42",
                        "statusCode": 500,
                        "providerCallId": "prov-err-1"
                    }
                }
                """;

        var event = (SessionErrorEvent) parseJson(json);
        assertNotNull(event);
        var data = event.getData();
        assertEquals("InternalError", data.errorType());
        assertEquals("Something went wrong", data.message());
        assertEquals("at line 42", data.stack());
        assertEquals(500, data.statusCode());
        assertEquals("prov-err-1", data.providerCallId());
    }

    @Test
    void testSessionModelChangeEventAllFields() throws Exception {
        String json = """
                {
                    "type": "session.model_change",
                    "data": {
                        "previousModel": "gpt-4",
                        "newModel": "gpt-4o"
                    }
                }
                """;

        var event = (SessionModelChangeEvent) parseJson(json);
        assertNotNull(event);
        assertEquals("gpt-4", event.getData().previousModel());
        assertEquals("gpt-4o", event.getData().newModel());
    }

    @Test
    void testSessionHandoffEventAllFields() throws Exception {
        String json = """
                {
                    "type": "session.handoff",
                    "data": {
                        "handoffTime": "2025-05-01T10:00:00Z",
                        "sourceType": "cli",
                        "repository": {
                            "owner": "my-org",
                            "name": "my-repo",
                            "branch": "main"
                        },
                        "context": "additional context",
                        "summary": "handoff summary",
                        "remoteSessionId": "remote-sess-1"
                    }
                }
                """;

        var event = (SessionHandoffEvent) parseJson(json);
        assertNotNull(event);
        var data = event.getData();
        assertNotNull(data.handoffTime());
        assertEquals("cli", data.sourceType());
        assertEquals("additional context", data.context());
        assertEquals("handoff summary", data.summary());
        assertEquals("remote-sess-1", data.remoteSessionId());
        assertNotNull(data.repository());
        assertEquals("my-org", data.repository().owner());
        assertEquals("my-repo", data.repository().name());
        assertEquals("main", data.repository().branch());
    }

    @Test
    void testSessionTruncationEventAllFields() throws Exception {
        String json = """
                {
                    "type": "session.truncation",
                    "data": {
                        "tokenLimit": 128000,
                        "preTruncationTokensInMessages": 150000,
                        "preTruncationMessagesLength": 100,
                        "postTruncationTokensInMessages": 120000,
                        "postTruncationMessagesLength": 80,
                        "tokensRemovedDuringTruncation": 30000,
                        "messagesRemovedDuringTruncation": 20,
                        "performedBy": "system"
                    }
                }
                """;

        var event = (SessionTruncationEvent) parseJson(json);
        assertNotNull(event);
        var data = event.getData();
        assertEquals(128000.0, data.tokenLimit());
        assertEquals(150000.0, data.preTruncationTokensInMessages());
        assertEquals(100.0, data.preTruncationMessagesLength());
        assertEquals(120000.0, data.postTruncationTokensInMessages());
        assertEquals(80.0, data.postTruncationMessagesLength());
        assertEquals(30000.0, data.tokensRemovedDuringTruncation());
        assertEquals(20.0, data.messagesRemovedDuringTruncation());
        assertEquals("system", data.performedBy());
    }

    @Test
    void testSessionUsageInfoEventAllFields() throws Exception {
        String json = """
                {
                    "type": "session.usage_info",
                    "data": {
                        "tokenLimit": 128000,
                        "currentTokens": 50000,
                        "messagesLength": 25
                    }
                }
                """;

        var event = (SessionUsageInfoEvent) parseJson(json);
        assertNotNull(event);
        var data = event.getData();
        assertEquals(128000.0, data.tokenLimit());
        assertEquals(50000.0, data.currentTokens());
        assertEquals(25.0, data.messagesLength());
    }

    @Test
    void testSessionCompactionCompleteEventAllFields() throws Exception {
        String json = """
                {
                    "type": "session.compaction_complete",
                    "data": {
                        "success": true,
                        "error": null,
                        "preCompactionTokens": 150000.0,
                        "postCompactionTokens": 60000.0,
                        "preCompactionMessagesLength": 100.0,
                        "messagesRemoved": 50.0,
                        "tokensRemoved": 90000.0,
                        "summaryContent": "Compacted conversation",
                        "checkpointNumber": 3.0,
                        "checkpointPath": "/checkpoints/3",
                        "compactionTokensUsed": {
                            "input": 1000,
                            "output": 500,
                            "cachedInput": 200
                        },
                        "requestId": "req-compact-1"
                    }
                }
                """;

        var event = (SessionCompactionCompleteEvent) parseJson(json);
        assertNotNull(event);
        var data = event.getData();
        assertTrue(data.success());
        assertNull(data.error());
        assertEquals(150000.0, data.preCompactionTokens());
        assertEquals(60000.0, data.postCompactionTokens());
        assertEquals(100.0, data.preCompactionMessagesLength());
        assertEquals(50.0, data.messagesRemoved());
        assertEquals(90000.0, data.tokensRemoved());
        assertEquals("Compacted conversation", data.summaryContent());
        assertEquals(3.0, data.checkpointNumber());
        assertEquals("/checkpoints/3", data.checkpointPath());
        assertEquals("req-compact-1", data.requestId());

        var tokens = data.compactionTokensUsed();
        assertNotNull(tokens);
        assertEquals(1000.0, tokens.input());
        assertEquals(500.0, tokens.output());
        assertEquals(200.0, tokens.cachedInput());
    }

    @Test
    void testSessionShutdownEventAllFields() throws Exception {
        String json = """
                {
                    "type": "session.shutdown",
                    "data": {
                        "shutdownType": "error",
                        "errorReason": "OOM",
                        "totalPremiumRequests": 10,
                        "totalApiDurationMs": 5000.5,
                        "sessionStartTime": 1700000000000,
                        "codeChanges": {
                            "linesAdded": 50,
                            "linesRemoved": 20,
                            "filesModified": ["a.java", "b.java", "c.java"]
                        },
                        "modelMetrics": {
                            "avgLatency": 200
                        },
                        "currentModel": "gpt-4-turbo"
                    }
                }
                """;

        var event = (SessionShutdownEvent) parseJson(json);
        assertNotNull(event);
        var data = event.getData();
        assertEquals(SessionShutdownEvent.ShutdownType.ERROR, data.shutdownType());
        assertEquals("OOM", data.errorReason());
        assertEquals(10.0, data.totalPremiumRequests());
        assertEquals(5000.5, data.totalApiDurationMs());
        assertEquals(1700000000000.0, data.sessionStartTime());
        assertEquals("gpt-4-turbo", data.currentModel());
        assertNotNull(data.modelMetrics());

        var changes = data.codeChanges();
        assertNotNull(changes);
        assertEquals(50.0, changes.linesAdded());
        assertEquals(20.0, changes.linesRemoved());
        assertNotNull(changes.filesModified());
        assertEquals(3, changes.filesModified().size());
        assertEquals("a.java", changes.filesModified().get(0));
    }

    // =========================================================================
    // Assistant events - rich field assertions
    // =========================================================================

    @Test
    void testAssistantMessageEventAllFields() throws Exception {
        String json = """
                {
                    "type": "assistant.message",
                    "data": {
                        "messageId": "msg-rich",
                        "content": "Full response",
                        "toolRequests": [
                            {
                                "toolCallId": "tc-1",
                                "name": "read_file",
                                "arguments": {"path": "/tmp/file.txt"}
                            },
                            {
                                "toolCallId": "tc-2",
                                "name": "write_file",
                                "arguments": {"path": "/tmp/out.txt", "content": "hello"}
                            }
                        ],
                        "parentToolCallId": "parent-tc",
                        "reasoningOpaque": "opaque-data",
                        "reasoningText": "My reasoning",
                        "encryptedContent": "enc123"
                    }
                }
                """;

        var event = (AssistantMessageEvent) parseJson(json);
        assertNotNull(event);
        var data = event.getData();
        assertEquals("msg-rich", data.messageId());
        assertEquals("Full response", data.content());
        assertEquals("parent-tc", data.parentToolCallId());
        assertEquals("opaque-data", data.reasoningOpaque());
        assertEquals("My reasoning", data.reasoningText());
        assertEquals("enc123", data.encryptedContent());

        assertNotNull(data.toolRequests());
        assertEquals(2, data.toolRequests().size());
        assertEquals("tc-1", data.toolRequests().get(0).toolCallId());
        assertEquals("read_file", data.toolRequests().get(0).name());
        assertNotNull(data.toolRequests().get(0).arguments());
        assertEquals("tc-2", data.toolRequests().get(1).toolCallId());
        assertEquals("write_file", data.toolRequests().get(1).name());
    }

    @Test
    void testAssistantMessageDeltaEventAllFields() throws Exception {
        String json = """
                {
                    "type": "assistant.message_delta",
                    "data": {
                        "messageId": "msg-delta-1",
                        "deltaContent": "partial text",
                        "totalResponseSizeBytes": 4096.0,
                        "parentToolCallId": "ptc-1"
                    }
                }
                """;

        var event = (AssistantMessageDeltaEvent) parseJson(json);
        assertNotNull(event);
        var data = event.getData();
        assertEquals("msg-delta-1", data.messageId());
        assertEquals("partial text", data.deltaContent());
        assertEquals(4096.0, data.totalResponseSizeBytes());
        assertEquals("ptc-1", data.parentToolCallId());
    }

    @Test
    void testAssistantUsageEventAllFields() throws Exception {
        String json = """
                {
                    "type": "assistant.usage",
                    "data": {
                        "model": "gpt-4-turbo",
                        "inputTokens": 500,
                        "outputTokens": 200,
                        "cacheReadTokens": 50,
                        "cacheWriteTokens": 150,
                        "cost": 0.05,
                        "duration": 1234.5,
                        "initiator": "user",
                        "apiCallId": "api-1",
                        "providerCallId": "prov-1",
                        "parentToolCallId": "ptc-usage",
                        "quotaSnapshots": {
                            "premium": 100,
                            "standard": 500
                        }
                    }
                }
                """;

        var event = (AssistantUsageEvent) parseJson(json);
        assertNotNull(event);
        var data = event.getData();
        assertEquals("gpt-4-turbo", data.model());
        assertEquals(500.0, data.inputTokens());
        assertEquals(200.0, data.outputTokens());
        assertEquals(50.0, data.cacheReadTokens());
        assertEquals(150.0, data.cacheWriteTokens());
        assertEquals(0.05, data.cost());
        assertEquals(1234.5, data.duration());
        assertEquals("user", data.initiator());
        assertEquals("api-1", data.apiCallId());
        assertEquals("prov-1", data.providerCallId());
        assertEquals("ptc-usage", data.parentToolCallId());
        assertNotNull(data.quotaSnapshots());
        assertEquals(2, data.quotaSnapshots().size());
    }

    @Test
    void testAssistantReasoningDeltaEventAllFields() throws Exception {
        String json = """
                {
                    "type": "assistant.reasoning_delta",
                    "data": {
                        "reasoningId": "r-delta-1",
                        "deltaContent": "thinking about..."
                    }
                }
                """;

        var event = (AssistantReasoningDeltaEvent) parseJson(json);
        assertNotNull(event);
        assertEquals("r-delta-1", event.getData().reasoningId());
        assertEquals("thinking about...", event.getData().deltaContent());
    }

    @Test
    void testAssistantIntentEventAllFields() throws Exception {
        String json = """
                {
                    "type": "assistant.intent",
                    "data": {
                        "intent": "refactor_code"
                    }
                }
                """;

        var event = (AssistantIntentEvent) parseJson(json);
        assertNotNull(event);
        assertEquals("refactor_code", event.getData().intent());
    }

    @Test
    void testAssistantTurnEndEventAllFields() throws Exception {
        String json = """
                {
                    "type": "assistant.turn_end",
                    "data": {
                        "turnId": "turn-end-1"
                    }
                }
                """;

        var event = (AssistantTurnEndEvent) parseJson(json);
        assertNotNull(event);
        assertEquals("turn-end-1", event.getData().turnId());
    }

    // =========================================================================
    // Tool events - rich field assertions
    // =========================================================================

    @Test
    void testToolExecutionStartEventAllFields() throws Exception {
        String json = """
                {
                    "type": "tool.execution_start",
                    "data": {
                        "toolCallId": "tc-start-1",
                        "toolName": "mcp_read_file",
                        "arguments": {"path": "/tmp/x.txt"},
                        "mcpServerName": "filesystem",
                        "mcpToolName": "read_file",
                        "parentToolCallId": "ptc-exec"
                    }
                }
                """;

        var event = (ToolExecutionStartEvent) parseJson(json);
        assertNotNull(event);
        var data = event.getData();
        assertEquals("tc-start-1", data.toolCallId());
        assertEquals("mcp_read_file", data.toolName());
        assertNotNull(data.arguments());
        assertEquals("filesystem", data.mcpServerName());
        assertEquals("read_file", data.mcpToolName());
        assertEquals("ptc-exec", data.parentToolCallId());
    }

    @Test
    void testToolExecutionCompleteEventWithError() throws Exception {
        String json = """
                {
                    "type": "tool.execution_complete",
                    "data": {
                        "toolCallId": "tc-err-1",
                        "success": false,
                        "isUserRequested": true,
                        "error": {
                            "message": "File not found",
                            "code": "ENOENT"
                        },
                        "toolTelemetry": {
                            "duration": 50,
                            "retries": 0
                        },
                        "parentToolCallId": "ptc-complete"
                    }
                }
                """;

        var event = (ToolExecutionCompleteEvent) parseJson(json);
        assertNotNull(event);
        var data = event.getData();
        assertEquals("tc-err-1", data.toolCallId());
        assertFalse(data.success());
        assertTrue(data.isUserRequested());
        assertEquals("ptc-complete", data.parentToolCallId());

        assertNotNull(data.error());
        assertEquals("File not found", data.error().message());
        assertEquals("ENOENT", data.error().code());

        assertNotNull(data.toolTelemetry());
        assertEquals(2, data.toolTelemetry().size());
    }

    @Test
    void testToolExecutionCompleteEventWithResult() throws Exception {
        String json = """
                {
                    "type": "tool.execution_complete",
                    "data": {
                        "toolCallId": "tc-res-1",
                        "success": true,
                        "result": {
                            "content": "file contents",
                            "detailedContent": "full detailed contents"
                        }
                    }
                }
                """;

        var event = (ToolExecutionCompleteEvent) parseJson(json);
        assertNotNull(event);
        var data = event.getData();
        assertTrue(data.success());
        assertNotNull(data.result());
        assertEquals("file contents", data.result().content());
        assertEquals("full detailed contents", data.result().detailedContent());
        assertNull(data.error());
    }

    @Test
    void testToolExecutionPartialResultEventAllFields() throws Exception {
        String json = """
                {
                    "type": "tool.execution_partial_result",
                    "data": {
                        "toolCallId": "tc-partial-1",
                        "partialOutput": "partial output data"
                    }
                }
                """;

        var event = (ToolExecutionPartialResultEvent) parseJson(json);
        assertNotNull(event);
        assertEquals("tc-partial-1", event.getData().toolCallId());
        assertEquals("partial output data", event.getData().partialOutput());
    }

    @Test
    void testToolExecutionProgressEventAllFields() throws Exception {
        String json = """
                {
                    "type": "tool.execution_progress",
                    "data": {
                        "toolCallId": "tc-prog-1",
                        "progressMessage": "50% done"
                    }
                }
                """;

        var event = (ToolExecutionProgressEvent) parseJson(json);
        assertNotNull(event);
        assertEquals("tc-prog-1", event.getData().toolCallId());
        assertEquals("50% done", event.getData().progressMessage());
    }

    @Test
    void testToolUserRequestedEventAllFields() throws Exception {
        String json = """
                {
                    "type": "tool.user_requested",
                    "data": {
                        "toolCallId": "tc-ur-1",
                        "toolName": "search_files",
                        "arguments": {"query": "TODO"}
                    }
                }
                """;

        var event = (ToolUserRequestedEvent) parseJson(json);
        assertNotNull(event);
        assertEquals("tc-ur-1", event.getData().toolCallId());
        assertEquals("search_files", event.getData().toolName());
        assertNotNull(event.getData().arguments());
    }

    // =========================================================================
    // User events - rich field assertions
    // =========================================================================

    @Test
    void testUserMessageEventAllFieldsWithAttachments() throws Exception {
        String json = """
                {
                    "type": "user.message",
                    "data": {
                        "content": "Please review this file",
                        "transformedContent": "Transformed: Please review this file",
                        "source": "editor",
                        "attachments": [
                            {
                                "type": "file",
                                "path": "/src/Main.java",
                                "filePath": "/full/src/Main.java",
                                "displayName": "Main.java",
                                "text": "public class Main {}",
                                "selection": {
                                    "start": { "line": 1, "character": 0 },
                                    "end": { "line": 5, "character": 10 }
                                }
                            }
                        ]
                    }
                }
                """;

        var event = (UserMessageEvent) parseJson(json);
        assertNotNull(event);
        var data = event.getData();
        assertEquals("Please review this file", data.content());
        assertEquals("Transformed: Please review this file", data.transformedContent());
        assertEquals("editor", data.source());

        assertNotNull(data.attachments());
        assertEquals(1, data.attachments().size());

        var att = data.attachments().get(0);
        assertEquals("file", att.type());
        assertEquals("/src/Main.java", att.path());
        assertEquals("/full/src/Main.java", att.filePath());
        assertEquals("Main.java", att.displayName());
        assertEquals("public class Main {}", att.text());

        assertNotNull(att.selection());
        assertNotNull(att.selection().start());
        assertNotNull(att.selection().end());
        assertEquals(1, att.selection().start().line());
        assertEquals(0, att.selection().start().character());
        assertEquals(5, att.selection().end().line());
        assertEquals(10, att.selection().end().character());
    }

    @Test
    void testUserMessageEventNoAttachments() throws Exception {
        String json = """
                {
                    "type": "user.message",
                    "data": {
                        "content": "Simple message"
                    }
                }
                """;

        var event = (UserMessageEvent) parseJson(json);
        assertNotNull(event);
        assertEquals("Simple message", event.getData().content());
        assertNull(event.getData().attachments());
    }

    // =========================================================================
    // Subagent events - rich field assertions
    // =========================================================================

    @Test
    void testSubagentStartedEventAllFields() throws Exception {
        String json = """
                {
                    "type": "subagent.started",
                    "data": {
                        "toolCallId": "tc-sub-1",
                        "agentName": "test-agent",
                        "agentDisplayName": "Test Agent",
                        "agentDescription": "A test subagent"
                    }
                }
                """;

        var event = (SubagentStartedEvent) parseJson(json);
        assertNotNull(event);
        var data = event.getData();
        assertEquals("tc-sub-1", data.toolCallId());
        assertEquals("test-agent", data.agentName());
        assertEquals("Test Agent", data.agentDisplayName());
        assertEquals("A test subagent", data.agentDescription());
    }

    @Test
    void testSubagentCompletedEventAllFields() throws Exception {
        String json = """
                {
                    "type": "subagent.completed",
                    "data": {
                        "toolCallId": "tc-sub-2",
                        "agentName": "reviewer"
                    }
                }
                """;

        var event = (SubagentCompletedEvent) parseJson(json);
        assertNotNull(event);
        assertEquals("tc-sub-2", event.getData().toolCallId());
        assertEquals("reviewer", event.getData().agentName());
    }

    @Test
    void testSubagentFailedEventAllFields() throws Exception {
        String json = """
                {
                    "type": "subagent.failed",
                    "data": {
                        "toolCallId": "tc-sub-3",
                        "agentName": "broken-agent",
                        "error": "Connection timeout"
                    }
                }
                """;

        var event = (SubagentFailedEvent) parseJson(json);
        assertNotNull(event);
        assertEquals("tc-sub-3", event.getData().toolCallId());
        assertEquals("broken-agent", event.getData().agentName());
        assertEquals("Connection timeout", event.getData().error());
    }

    @Test
    void testSubagentSelectedEventAllFields() throws Exception {
        String json = """
                {
                    "type": "subagent.selected",
                    "data": {
                        "agentName": "best-agent",
                        "agentDisplayName": "Best Agent",
                        "tools": ["read", "write", "search"]
                    }
                }
                """;

        var event = (SubagentSelectedEvent) parseJson(json);
        assertNotNull(event);
        var data = event.getData();
        assertEquals("best-agent", data.agentName());
        assertEquals("Best Agent", data.agentDisplayName());
        assertNotNull(data.tools());
        assertEquals(3, data.tools().length);
        assertEquals("read", data.tools()[0]);
        assertEquals("write", data.tools()[1]);
        assertEquals("search", data.tools()[2]);
    }

    // =========================================================================
    // Hook events - rich field assertions
    // =========================================================================

    @Test
    void testHookStartEventAllFields() throws Exception {
        String json = """
                {
                    "type": "hook.start",
                    "data": {
                        "hookInvocationId": "hook-full-1",
                        "hookType": "postToolUse",
                        "input": {"toolName": "write_file", "result": "ok"}
                    }
                }
                """;

        var event = (HookStartEvent) parseJson(json);
        assertNotNull(event);
        assertEquals("hook-full-1", event.getData().hookInvocationId());
        assertEquals("postToolUse", event.getData().hookType());
        assertNotNull(event.getData().input());
    }

    @Test
    void testHookEndEventWithError() throws Exception {
        String json = """
                {
                    "type": "hook.end",
                    "data": {
                        "hookInvocationId": "hook-err-1",
                        "hookType": "preToolUse",
                        "output": null,
                        "success": false,
                        "error": {
                            "message": "Hook validation failed",
                            "stack": "at HookValidator.validate(line 10)"
                        }
                    }
                }
                """;

        var event = (HookEndEvent) parseJson(json);
        assertNotNull(event);
        var data = event.getData();
        assertEquals("hook-err-1", data.hookInvocationId());
        assertEquals("preToolUse", data.hookType());
        assertFalse(data.success());
        assertNotNull(data.error());
        assertEquals("Hook validation failed", data.error().message());
        assertEquals("at HookValidator.validate(line 10)", data.error().stack());
    }

    @Test
    void testHookEndEventSuccess() throws Exception {
        String json = """
                {
                    "type": "hook.end",
                    "data": {
                        "hookInvocationId": "hook-ok-1",
                        "hookType": "preToolUse",
                        "output": "approved",
                        "success": true
                    }
                }
                """;

        var event = (HookEndEvent) parseJson(json);
        assertNotNull(event);
        assertTrue(event.getData().success());
        assertNull(event.getData().error());
    }

    // =========================================================================
    // Other events - rich field assertions
    // =========================================================================

    @Test
    void testAbortEventAllFields() throws Exception {
        String json = """
                {
                    "type": "abort",
                    "data": {
                        "reason": "user_cancelled"
                    }
                }
                """;

        var event = (AbortEvent) parseJson(json);
        assertNotNull(event);
        assertEquals("user_cancelled", event.getData().reason());
    }

    @Test
    void testSystemMessageEventAllFields() throws Exception {
        String json = """
                {
                    "type": "system.message",
                    "data": {
                        "content": "System notification",
                        "type": "warning",
                        "metadata": {
                            "severity": "high",
                            "source": "rate-limiter"
                        }
                    }
                }
                """;

        var event = (SystemMessageEvent) parseJson(json);
        assertNotNull(event);
        var data = event.getData();
        assertEquals("System notification", data.content());
        assertEquals("warning", data.type());
        assertNotNull(data.metadata());
        assertEquals(2, data.metadata().size());
    }

    @Test
    void testSessionInfoEventAllFields() throws Exception {
        String json = """
                {
                    "type": "session.info",
                    "data": {
                        "infoType": "model_selection",
                        "message": "Using gpt-4-turbo for this task"
                    }
                }
                """;

        var event = (SessionInfoEvent) parseJson(json);
        assertNotNull(event);
        assertEquals("model_selection", event.getData().infoType());
        assertEquals("Using gpt-4-turbo for this task", event.getData().message());
    }

    // =========================================================================
    // Null / missing data scenarios
    // =========================================================================

    @Test
    void testParseEventWithNullData() throws Exception {
        String json = """
                {
                    "type": "session.idle",
                    "data": null
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(SessionIdleEvent.class, event);
    }

    @Test
    void testParseEventWithMissingData() throws Exception {
        String json = """
                {
                    "type": "session.idle"
                }
                """;

        AbstractSessionEvent event = parseJson(json);
        assertNotNull(event);
        assertInstanceOf(SessionIdleEvent.class, event);
    }

    @Test
    void testParseNullJsonNode() throws Exception {
        Logger parserLogger = Logger.getLogger(SessionEventParser.class.getName());
        Level originalLevel = parserLogger.getLevel();
        parserLogger.setLevel(Level.OFF);

        try {
            AbstractSessionEvent event = SessionEventParser.parse((JsonNode) null);
            assertNull(event, "Null JsonNode should return null");
        } finally {
            parserLogger.setLevel(originalLevel);
        }
    }

    // =========================================================================
    // Additional data assertion tests
    // =========================================================================

    @Test
    void testParseJsonNodeAssistantMessageWithFields() throws Exception {
        String json = """
                {
                    "type": "assistant.message",
                    "id": "550e8400-e29b-41d4-a716-446655440000",
                    "ephemeral": true,
                    "data": {
                        "messageId": "msg-jn-1",
                        "content": "Hello from JsonNode",
                        "toolRequests": [
                            { "toolCallId": "tc-jn", "name": "grep", "arguments": {} }
                        ]
                    }
                }
                """;

        var event = (AssistantMessageEvent) parseJson(json);
        assertNotNull(event);
        assertEquals(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"), event.getId());
        assertTrue(event.getEphemeral());
        assertEquals("msg-jn-1", event.getData().messageId());
        assertEquals("Hello from JsonNode", event.getData().content());
        assertEquals(1, event.getData().toolRequests().size());
        assertEquals("tc-jn", event.getData().toolRequests().get(0).toolCallId());
    }

    @Test
    void testParseJsonNodeToolExecutionCompleteWithNestedTypes() throws Exception {
        String json = """
                {
                    "type": "tool.execution_complete",
                    "data": {
                        "toolCallId": "tc-jn-comp",
                        "success": false,
                        "error": {
                            "message": "Permission denied",
                            "code": "EPERM"
                        }
                    }
                }
                """;

        var event = (ToolExecutionCompleteEvent) parseJson(json);
        assertNotNull(event);
        assertFalse(event.getData().success());
        assertEquals("Permission denied", event.getData().error().message());
        assertEquals("EPERM", event.getData().error().code());
    }

    @Test
    void testParseJsonNodeSessionShutdownWithCodeChanges() throws Exception {
        String json = """
                {
                    "type": "session.shutdown",
                    "data": {
                        "shutdownType": "routine",
                        "totalPremiumRequests": 3,
                        "totalApiDurationMs": 999.9,
                        "codeChanges": {
                            "linesAdded": 100,
                            "linesRemoved": 50,
                            "filesModified": ["x.java"]
                        },
                        "currentModel": "claude-4"
                    }
                }
                """;

        var event = (SessionShutdownEvent) parseJson(json);
        assertNotNull(event);
        assertEquals(SessionShutdownEvent.ShutdownType.ROUTINE, event.getData().shutdownType());
        assertEquals(100.0, event.getData().codeChanges().linesAdded());
        assertEquals(1, event.getData().codeChanges().filesModified().size());
    }

    @Test
    void testParseJsonNodeUserMessageWithAttachment() throws Exception {
        String json = """
                {
                    "type": "user.message",
                    "data": {
                        "content": "Check this",
                        "attachments": [
                            {
                                "type": "code",
                                "displayName": "snippet.py",
                                "text": "print('hello')",
                                "selection": {
                                    "start": { "line": 0, "character": 0 },
                                    "end": { "line": 0, "character": 14 }
                                }
                            }
                        ]
                    }
                }
                """;

        var event = (UserMessageEvent) parseJson(json);
        assertNotNull(event);
        assertEquals(1, event.getData().attachments().size());
        var att = event.getData().attachments().get(0);
        assertEquals("code", att.type());
        assertEquals("snippet.py", att.displayName());
        assertEquals(0, att.selection().start().line());
        assertEquals(14, att.selection().end().character());
    }
}
