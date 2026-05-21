/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.generated.SessionEvent;
import com.github.copilot.sdk.generated.UnknownSessionEvent;
import com.github.copilot.sdk.generated.UserMessageEvent;

/**
 * Unit tests for forward-compatible handling of unknown session event types.
 * <p>
 * Verifies that the SDK gracefully handles event types introduced by newer CLI
 * versions without crashing.
 */
public class ForwardCompatibilityTest {

    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();

    @Test
    void parse_knownEventType_returnsTypedEvent() throws Exception {
        String json = """
                {
                    "id": "00000000-0000-0000-0000-000000000001",
                    "timestamp": "2026-01-01T00:00:00Z",
                    "type": "user.message",
                    "data": { "content": "Hello" }
                }
                """;
        SessionEvent result = MAPPER.readValue(json, SessionEvent.class);

        assertInstanceOf(UserMessageEvent.class, result);
        assertEquals("user.message", result.getType());
    }

    @Test
    void parse_unknownEventType_returnsUnknownSessionEvent() throws Exception {
        String json = """
                {
                    "id": "12345678-1234-1234-1234-123456789abc",
                    "timestamp": "2026-06-15T10:30:00Z",
                    "type": "future.feature_from_server",
                    "data": { "key": "value" }
                }
                """;
        SessionEvent result = MAPPER.readValue(json, SessionEvent.class);

        assertInstanceOf(UnknownSessionEvent.class, result);
        assertEquals("future.feature_from_server", result.getType());
    }

    @Test
    void parse_unknownEventType_preservesOriginalType() throws Exception {
        String json = """
                {
                    "id": "12345678-1234-1234-1234-123456789abc",
                    "timestamp": "2026-06-15T10:30:00Z",
                    "type": "future.feature_from_server",
                    "data": {}
                }
                """;
        SessionEvent result = MAPPER.readValue(json, SessionEvent.class);

        assertInstanceOf(UnknownSessionEvent.class, result);
        assertEquals("future.feature_from_server", result.getType());
    }

    @Test
    void parse_unknownEventType_preservesBaseMetadata() throws Exception {
        String json = """
                {
                    "id": "12345678-1234-1234-1234-123456789abc",
                    "timestamp": "2026-06-15T10:30:00Z",
                    "parentId": "abcdefab-abcd-abcd-abcd-abcdefabcdef",
                    "type": "future.feature_from_server",
                    "data": {}
                }
                """;
        SessionEvent result = MAPPER.readValue(json, SessionEvent.class);

        assertNotNull(result);
        assertEquals(UUID.fromString("12345678-1234-1234-1234-123456789abc"), result.getId());
        assertEquals(UUID.fromString("abcdefab-abcd-abcd-abcd-abcdefabcdef"), result.getParentId());
    }

    @Test
    void unknownSessionEvent_getType_returnsUnknown() {
        var evt = new UnknownSessionEvent();
        assertEquals("unknown", evt.getType());
    }
}
