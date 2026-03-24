/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.sdk.events.AbstractSessionEvent;
import com.github.copilot.sdk.events.SessionEventParser;
import com.github.copilot.sdk.events.UnknownSessionEvent;
import com.github.copilot.sdk.events.UserMessageEvent;

/**
 * Unit tests for forward-compatible handling of unknown session event types.
 * <p>
 * Verifies that the SDK gracefully handles event types introduced by newer CLI
 * versions without crashing.
 */
public class ForwardCompatibilityTest {

    @Test
    void parse_knownEventType_returnsTypedEvent() {
        String json = """
                {
                    "id": "00000000-0000-0000-0000-000000000001",
                    "timestamp": "2026-01-01T00:00:00Z",
                    "type": "user.message",
                    "data": { "content": "Hello" }
                }
                """;
        var node = parse(json);
        AbstractSessionEvent result = SessionEventParser.parse(node);

        assertInstanceOf(UserMessageEvent.class, result);
        assertEquals("user.message", result.getType());
    }

    @Test
    void parse_unknownEventType_returnsUnknownSessionEvent() {
        String json = """
                {
                    "id": "12345678-1234-1234-1234-123456789abc",
                    "timestamp": "2026-06-15T10:30:00Z",
                    "type": "future.feature_from_server",
                    "data": { "key": "value" }
                }
                """;
        var node = parse(json);
        AbstractSessionEvent result = SessionEventParser.parse(node);

        assertInstanceOf(UnknownSessionEvent.class, result);
        assertEquals("unknown", result.getType());
    }

    @Test
    void parse_unknownEventType_preservesOriginalType() {
        String json = """
                {
                    "id": "12345678-1234-1234-1234-123456789abc",
                    "timestamp": "2026-06-15T10:30:00Z",
                    "type": "future.feature_from_server",
                    "data": {}
                }
                """;
        var node = parse(json);
        AbstractSessionEvent result = SessionEventParser.parse(node);

        assertInstanceOf(UnknownSessionEvent.class, result);
        assertEquals("future.feature_from_server", ((UnknownSessionEvent) result).getOriginalType());
    }

    @Test
    void parse_unknownEventType_preservesBaseMetadata() {
        String json = """
                {
                    "id": "12345678-1234-1234-1234-123456789abc",
                    "timestamp": "2026-06-15T10:30:00Z",
                    "parentId": "abcdefab-abcd-abcd-abcd-abcdefabcdef",
                    "type": "future.feature_from_server",
                    "data": {}
                }
                """;
        var node = parse(json);
        AbstractSessionEvent result = SessionEventParser.parse(node);

        assertNotNull(result);
        assertEquals(UUID.fromString("12345678-1234-1234-1234-123456789abc"), result.getId());
        assertEquals(UUID.fromString("abcdefab-abcd-abcd-abcd-abcdefabcdef"), result.getParentId());
    }

    @Test
    void unknownSessionEvent_getType_returnsUnknown() {
        var evt = new UnknownSessionEvent("some.future.type");
        assertEquals("unknown", evt.getType());
    }

    @Test
    void unknownSessionEvent_getOriginalType_returnsOriginal() {
        var evt = new UnknownSessionEvent("some.future.type");
        assertEquals("some.future.type", evt.getOriginalType());
    }

    @Test
    void unknownSessionEvent_nullType_usesUnknown() {
        var evt = new UnknownSessionEvent(null);
        assertEquals("unknown", evt.getType());
        assertEquals("unknown", evt.getOriginalType());
    }

    private com.fasterxml.jackson.databind.JsonNode parse(String json) {
        try {
            return new ObjectMapper().readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
