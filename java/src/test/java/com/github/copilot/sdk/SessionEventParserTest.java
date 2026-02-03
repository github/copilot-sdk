/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.events.*;

/**
 * Tests for session event parsing.
 * <p>
 * These are unit tests that verify JSON deserialization works correctly for all
 * event types supported by the SDK.
 * </p>
 */
public class SessionEventParserTest {

    // =========================================================================
    // Session Events
    // =========================================================================

    @Test
    void testParseSessionStartEvent() {
        String json = """
                {
                    "type": "session.start",
                    "data": {
                        "sessionId": "sess-123",
                        "model": "gpt-4"
                    }
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(SessionStartEvent.class, event);
        assertEquals("session.start", event.getType());

        SessionStartEvent startEvent = (SessionStartEvent) event;
        assertEquals("sess-123", startEvent.getData().getSessionId());
    }

    @Test
    void testParseSessionResumeEvent() {
        String json = """
                {
                    "type": "session.resume",
                    "data": {
                        "sessionId": "sess-456"
                    }
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(SessionResumeEvent.class, event);
        assertEquals("session.resume", event.getType());
    }

    @Test
    void testParseSessionErrorEvent() {
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

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(SessionErrorEvent.class, event);
        assertEquals("session.error", event.getType());

        SessionErrorEvent errorEvent = (SessionErrorEvent) event;
        assertEquals("RateLimitError", errorEvent.getData().getErrorType());
        assertEquals("Rate limit exceeded", errorEvent.getData().getMessage());
        assertNotNull(errorEvent.getData().getStack());
    }

    @Test
    void testParseSessionIdleEvent() {
        String json = """
                {
                    "type": "session.idle",
                    "data": {}
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(SessionIdleEvent.class, event);
        assertEquals("session.idle", event.getType());
    }

    @Test
    void testParseSessionInfoEvent() {
        String json = """
                {
                    "type": "session.info",
                    "data": {
                        "infoType": "status",
                        "message": "Processing request"
                    }
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(SessionInfoEvent.class, event);
        assertEquals("session.info", event.getType());

        SessionInfoEvent infoEvent = (SessionInfoEvent) event;
        assertEquals("status", infoEvent.getData().getInfoType());
        assertEquals("Processing request", infoEvent.getData().getMessage());
    }

    @Test
    void testParseSessionModelChangeEvent() {
        String json = """
                {
                    "type": "session.model_change",
                    "data": {
                        "previousModel": "gpt-4",
                        "newModel": "gpt-4-turbo"
                    }
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(SessionModelChangeEvent.class, event);
        assertEquals("session.model_change", event.getType());
    }

    @Test
    void testParseSessionHandoffEvent() {
        String json = """
                {
                    "type": "session.handoff",
                    "data": {
                        "targetAgent": "code-review-agent"
                    }
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(SessionHandoffEvent.class, event);
        assertEquals("session.handoff", event.getType());
    }

    @Test
    void testParseSessionTruncationEvent() {
        String json = """
                {
                    "type": "session.truncation",
                    "data": {
                        "reason": "context_limit"
                    }
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(SessionTruncationEvent.class, event);
        assertEquals("session.truncation", event.getType());
    }

    @Test
    void testParseSessionSnapshotRewindEvent() {
        String json = """
                {
                    "type": "session.snapshot_rewind",
                    "data": {
                        "snapshotId": "snap-123"
                    }
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(SessionSnapshotRewindEvent.class, event);
        assertEquals("session.snapshot_rewind", event.getType());
    }

    @Test
    void testParseSessionUsageInfoEvent() {
        String json = """
                {
                    "type": "session.usage_info",
                    "data": {
                        "tokenCount": 1500
                    }
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(SessionUsageInfoEvent.class, event);
        assertEquals("session.usage_info", event.getType());
    }

    @Test
    void testParseSessionCompactionStartEvent() {
        String json = """
                {
                    "type": "session.compaction_start",
                    "data": {}
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(SessionCompactionStartEvent.class, event);
        assertEquals("session.compaction_start", event.getType());
    }

    @Test
    void testParseSessionCompactionCompleteEvent() {
        String json = """
                {
                    "type": "session.compaction_complete",
                    "data": {}
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(SessionCompactionCompleteEvent.class, event);
        assertEquals("session.compaction_complete", event.getType());
    }

    // =========================================================================
    // User Events
    // =========================================================================

    @Test
    void testParseUserMessageEvent() {
        String json = """
                {
                    "type": "user.message",
                    "data": {
                        "messageId": "msg-123",
                        "content": "Hello, Copilot!"
                    }
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(UserMessageEvent.class, event);
        assertEquals("user.message", event.getType());
    }

    @Test
    void testParsePendingMessagesModifiedEvent() {
        String json = """
                {
                    "type": "pending_messages.modified",
                    "data": {
                        "count": 3
                    }
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(PendingMessagesModifiedEvent.class, event);
        assertEquals("pending_messages.modified", event.getType());
    }

    // =========================================================================
    // Assistant Events
    // =========================================================================

    @Test
    void testParseAssistantTurnStartEvent() {
        String json = """
                {
                    "type": "assistant.turn_start",
                    "data": {
                        "turnId": "turn-123"
                    }
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(AssistantTurnStartEvent.class, event);
        assertEquals("assistant.turn_start", event.getType());

        AssistantTurnStartEvent turnEvent = (AssistantTurnStartEvent) event;
        assertEquals("turn-123", turnEvent.getData().getTurnId());
    }

    @Test
    void testParseAssistantIntentEvent() {
        String json = """
                {
                    "type": "assistant.intent",
                    "data": {
                        "intent": "code_generation"
                    }
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(AssistantIntentEvent.class, event);
        assertEquals("assistant.intent", event.getType());
    }

    @Test
    void testParseAssistantReasoningEvent() {
        String json = """
                {
                    "type": "assistant.reasoning",
                    "data": {
                        "reasoningId": "reason-123",
                        "content": "Analyzing the code structure..."
                    }
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(AssistantReasoningEvent.class, event);
        assertEquals("assistant.reasoning", event.getType());

        AssistantReasoningEvent reasoningEvent = (AssistantReasoningEvent) event;
        assertEquals("reason-123", reasoningEvent.getData().getReasoningId());
        assertEquals("Analyzing the code structure...", reasoningEvent.getData().getContent());
    }

    @Test
    void testParseAssistantReasoningDeltaEvent() {
        String json = """
                {
                    "type": "assistant.reasoning_delta",
                    "data": {
                        "reasoningId": "reason-123",
                        "delta": "Considering options..."
                    }
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(AssistantReasoningDeltaEvent.class, event);
        assertEquals("assistant.reasoning_delta", event.getType());
    }

    @Test
    void testParseAssistantMessageEvent() {
        String json = """
                {
                    "type": "assistant.message",
                    "data": {
                        "messageId": "msg-456",
                        "content": "Here is the code you requested."
                    }
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(AssistantMessageEvent.class, event);
        assertEquals("assistant.message", event.getType());

        AssistantMessageEvent msgEvent = (AssistantMessageEvent) event;
        assertEquals("Here is the code you requested.", msgEvent.getData().getContent());
    }

    @Test
    void testParseAssistantMessageDeltaEvent() {
        String json = """
                {
                    "type": "assistant.message_delta",
                    "data": {
                        "messageId": "msg-456",
                        "delta": "Here is"
                    }
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(AssistantMessageDeltaEvent.class, event);
        assertEquals("assistant.message_delta", event.getType());
    }

    @Test
    void testParseAssistantTurnEndEvent() {
        String json = """
                {
                    "type": "assistant.turn_end",
                    "data": {
                        "turnId": "turn-123"
                    }
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(AssistantTurnEndEvent.class, event);
        assertEquals("assistant.turn_end", event.getType());
    }

    @Test
    void testParseAssistantUsageEvent() {
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

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(AssistantUsageEvent.class, event);
        assertEquals("assistant.usage", event.getType());
    }

    // =========================================================================
    // Tool Events
    // =========================================================================

    @Test
    void testParseToolUserRequestedEvent() {
        String json = """
                {
                    "type": "tool.user_requested",
                    "data": {
                        "toolName": "read_file",
                        "userRequest": "Please read the config file"
                    }
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(ToolUserRequestedEvent.class, event);
        assertEquals("tool.user_requested", event.getType());
    }

    @Test
    void testParseToolExecutionStartEvent() {
        String json = """
                {
                    "type": "tool.execution_start",
                    "data": {
                        "toolCallId": "call-123",
                        "toolName": "read_file"
                    }
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(ToolExecutionStartEvent.class, event);
        assertEquals("tool.execution_start", event.getType());
    }

    @Test
    void testParseToolExecutionPartialResultEvent() {
        String json = """
                {
                    "type": "tool.execution_partial_result",
                    "data": {
                        "toolCallId": "call-123",
                        "partialResult": "Reading file..."
                    }
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(ToolExecutionPartialResultEvent.class, event);
        assertEquals("tool.execution_partial_result", event.getType());
    }

    @Test
    void testParseToolExecutionProgressEvent() {
        String json = """
                {
                    "type": "tool.execution_progress",
                    "data": {
                        "toolCallId": "call-123",
                        "progress": 50
                    }
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(ToolExecutionProgressEvent.class, event);
        assertEquals("tool.execution_progress", event.getType());
    }

    @Test
    void testParseToolExecutionCompleteEvent() {
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

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(ToolExecutionCompleteEvent.class, event);
        assertEquals("tool.execution_complete", event.getType());

        ToolExecutionCompleteEvent completeEvent = (ToolExecutionCompleteEvent) event;
        assertTrue(completeEvent.getData().isSuccess());
    }

    // =========================================================================
    // Subagent Events
    // =========================================================================

    @Test
    void testParseSubagentStartedEvent() {
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

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(SubagentStartedEvent.class, event);
        assertEquals("subagent.started", event.getType());

        SubagentStartedEvent startedEvent = (SubagentStartedEvent) event;
        assertEquals("code-review", startedEvent.getData().getAgentName());
        assertEquals("Code Review Agent", startedEvent.getData().getAgentDisplayName());
    }

    @Test
    void testParseSubagentCompletedEvent() {
        String json = """
                {
                    "type": "subagent.completed",
                    "data": {
                        "toolCallId": "call-789",
                        "result": "Review completed successfully"
                    }
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(SubagentCompletedEvent.class, event);
        assertEquals("subagent.completed", event.getType());
    }

    @Test
    void testParseSubagentFailedEvent() {
        String json = """
                {
                    "type": "subagent.failed",
                    "data": {
                        "toolCallId": "call-789",
                        "error": "Agent timeout"
                    }
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(SubagentFailedEvent.class, event);
        assertEquals("subagent.failed", event.getType());
    }

    @Test
    void testParseSubagentSelectedEvent() {
        String json = """
                {
                    "type": "subagent.selected",
                    "data": {
                        "agentName": "documentation-agent"
                    }
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(SubagentSelectedEvent.class, event);
        assertEquals("subagent.selected", event.getType());
    }

    // =========================================================================
    // Hook Events
    // =========================================================================

    @Test
    void testParseHookStartEvent() {
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

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(HookStartEvent.class, event);
        assertEquals("hook.start", event.getType());

        HookStartEvent hookEvent = (HookStartEvent) event;
        assertEquals("hook-123", hookEvent.getData().getHookInvocationId());
        assertEquals("preToolUse", hookEvent.getData().getHookType());
    }

    @Test
    void testParseHookEndEvent() {
        String json = """
                {
                    "type": "hook.end",
                    "data": {
                        "hookInvocationId": "hook-123",
                        "success": true
                    }
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(HookEndEvent.class, event);
        assertEquals("hook.end", event.getType());
    }

    // =========================================================================
    // Other Events
    // =========================================================================

    @Test
    void testParseAbortEvent() {
        String json = """
                {
                    "type": "abort",
                    "data": {
                        "reason": "user_requested"
                    }
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(AbortEvent.class, event);
        assertEquals("abort", event.getType());
    }

    @Test
    void testParseSystemMessageEvent() {
        String json = """
                {
                    "type": "system.message",
                    "data": {
                        "content": "System is ready"
                    }
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event);
        assertInstanceOf(SystemMessageEvent.class, event);
        assertEquals("system.message", event.getType());
    }

    // =========================================================================
    // Edge Cases
    // =========================================================================

    @Test
    void testParseUnknownEventType() {
        // Unknown types log at FINE level, no need to suppress
        String json = """
                {
                    "type": "unknown.event.type",
                    "data": {}
                }
                """;

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNull(event, "Unknown event types should return null");
    }

    @Test
    void testParseMissingTypeField() {
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

            AbstractSessionEvent event = SessionEventParser.parse(json);
            assertNull(event, "Events without type field should return null");
        } finally {
            parserLogger.setLevel(originalLevel);
        }
    }

    @Test
    void testParseEventWithUnknownFields() {
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

        AbstractSessionEvent event = SessionEventParser.parse(json);
        assertNotNull(event, "Events with unknown fields should still parse");
        assertInstanceOf(SessionIdleEvent.class, event);
    }

    @Test
    void testParseInvalidJson() {
        // Suppress logging for this test since invalid JSON logs a SEVERE error
        Logger parserLogger = Logger.getLogger(SessionEventParser.class.getName());
        Level originalLevel = parserLogger.getLevel();
        parserLogger.setLevel(Level.OFF);

        try {
            String json = "{ this is not valid json }";

            AbstractSessionEvent event = SessionEventParser.parse(json);
            assertNull(event, "Invalid JSON should return null");
        } finally {
            parserLogger.setLevel(originalLevel);
        }
    }

    @Test
    void testParseEmptyJson() {
        // Suppress logging for this test since empty JSON logs a WARNING
        Logger parserLogger = Logger.getLogger(SessionEventParser.class.getName());
        Level originalLevel = parserLogger.getLevel();
        parserLogger.setLevel(Level.OFF);

        try {
            String json = "{}";

            AbstractSessionEvent event = SessionEventParser.parse(json);
            assertNull(event, "Empty JSON should return null due to missing type");
        } finally {
            parserLogger.setLevel(originalLevel);
        }
    }
}
