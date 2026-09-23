/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.generated;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.copilot.generated.UserMessageEvent.UserMessageEventData;
import com.github.copilot.generated.rpc.QueuePendingItems;

class MessageIdentitySerializationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void testQueuePendingMessageIdUsesCamelCaseAndIsOptional() throws Exception {
        var item = MAPPER.readValue("""
                {
                    "id": "queue-1",
                    "messageId": "message-1",
                    "kind": "message",
                    "displayText": "hello",
                    "agentMode": "interactive"
                }
                """, QueuePendingItems.class);

        assertEquals("message-1", item.messageId());
        assertEquals("message-1", MAPPER.valueToTree(item).get("messageId").textValue());

        var olderItem = MAPPER.readValue("""
                {
                    "id": "queue-2",
                    "kind": "command",
                    "displayText": "/help",
                    "agentMode": "interactive"
                }
                """, QueuePendingItems.class);

        assertNull(olderItem.messageId());
        assertFalse(MAPPER.<JsonNode>valueToTree(olderItem).has("messageId"));
    }

    @Test
    void testUserMessageIdUsesCamelCaseAndIsOptional() throws Exception {
        var message = MAPPER.readValue("""
                {"content": "hello", "messageId": "message-1"}
                """, UserMessageEventData.class);

        assertEquals("message-1", message.messageId());
        assertEquals("message-1", MAPPER.valueToTree(message).get("messageId").textValue());

        var olderMessage = MAPPER.readValue("""
                {"content": "hello"}
                """, UserMessageEventData.class);

        assertNull(olderMessage.messageId());
        assertFalse(MAPPER.<JsonNode>valueToTree(olderMessage).has("messageId"));
    }

    @Test
    void testToolStartTraceContextIsOptionalAndPreserved() throws Exception {
        for (var context : new String[] {
                "{}",
                """
                        {"traceparent":"00-11111111111111111111111111111111-2222222222222222-01","tracestate":"vendor=value"}
                        """,
                """
                        {"traceparent":"00-11111111111111111111111111111111-2222222222222222-00"}
                        """,
                """
                        {"traceparent":"invalid","tracestate":"invalid"}
                        """,
                """
                        {"traceparent":""}
                        """
        }) {
            var wire = MAPPER.createObjectNode()
                    .put("toolCallId", "tool-call-a")
                    .put("toolName", "client-tool");
            wire.setAll(MAPPER.readValue(context, ObjectNode.class));
            var data = MAPPER.treeToValue(wire, ToolExecutionStartEvent.ToolExecutionStartEventData.class);

            assertEquals(wire.path("traceparent").asText(null), data.traceparent());
            assertEquals(wire.path("tracestate").asText(null), data.tracestate());
            assertEquals(wire, MAPPER.valueToTree(data));
        }
    }
}
