/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.copilot.generated.UserMessageDelivery;
import com.github.copilot.generated.UserMessageEvent.UserMessageEventData;
import com.github.copilot.generated.rpc.RpcCaller;
import com.github.copilot.generated.rpc.SessionRpc;
import com.github.copilot.generated.rpc.SessionSendMessagesParams;
import com.github.copilot.generated.rpc.SessionSendMessagesRequest;
import com.github.copilot.generated.rpc.SessionSendParams;
import com.github.copilot.generated.rpc.SessionSendRequest;

class ClientTurnIdCompatibilityTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void releasedUserMessageConstructorKeepsArgumentMeaning() {
        var data = new UserMessageEventData(null, "hi", null, "message-1", null, null, null, null, "user",
                UserMessageDelivery.STEERING, null, null, null, "turn-1", null);

        assertEquals("message-1", data.messageId());
        assertEquals(UserMessageDelivery.STEERING, data.delivery());
        assertEquals("turn-1", data.turnId());
        assertNull(data.clientTurnId());
        assertFalse(MAPPER.valueToTree(data).has("clientTurnId"));
    }

    @Test
    void userMessageConstructorWithClientTurnIdKeepsArgumentMeaning() {
        var data = new UserMessageEventData(null, "hi", null, "message-1", "host-turn-1", null, null, null, null,
                "user", UserMessageDelivery.STEERING, null, null, null, "turn-1", null);

        assertEquals("message-1", data.messageId());
        assertEquals("host-turn-1", data.clientTurnId());
        assertEquals(UserMessageDelivery.STEERING, data.delivery());
        assertEquals("turn-1", data.turnId());
        assertNull(data.hasSubmittedTask());
        assertFalse(MAPPER.valueToTree(data).has("hasSubmittedTask"));
    }

    @Test
    void userMessageClientTurnIdRoundTripsThroughTheCanonicalRecord() throws Exception {
        JsonNode wire = MAPPER.readTree("{\"content\":\"hi\",\"clientTurnId\":\"host-turn-1\"}");
        var data = MAPPER.treeToValue(wire, UserMessageEventData.class);

        assertEquals("host-turn-1", data.clientTurnId());
        assertEquals(wire, MAPPER.valueToTree(data));
    }

    @Test
    void releasedSendParamsKeepTheirShapeAndRequestsCarryClientTurnId() {
        var sent = new ArrayList<ObjectNode>();
        var session = new SessionRpc(capturing(sent), "sess-1");

        session.send(new SessionSendParams("sess-1", "hi", null, null, null, null, true, null, null, null, null, null,
                null, null, null));
        session.send(new SessionSendRequest("hi").setClientTurnId("host-turn-1"));

        assertEquals(MAPPER.createObjectNode().put("sessionId", "sess-1").put("prompt", "hi").put("billable", true),
                sent.get(0));
        assertEquals(MAPPER.createObjectNode().put("prompt", "hi").put("clientTurnId", "host-turn-1").put("sessionId",
                "sess-1"), sent.get(1));
    }

    @Test
    void releasedSendMessagesParamsKeepTheirShapeAndRequestsCarryClientTurnId() {
        var sent = new ArrayList<ObjectNode>();
        var session = new SessionRpc(capturing(sent), "sess-1");

        session.sendMessages(
                new SessionSendMessagesParams("sess-1", List.of(), null, true, null, null, null, null, null, null));
        session.sendMessages(new SessionSendMessagesRequest(List.of()).setClientTurnId("host-turn-1"));

        var legacy = MAPPER.createObjectNode().put("sessionId", "sess-1");
        legacy.putArray("messages");
        legacy.put("prepend", true);
        var extended = MAPPER.createObjectNode();
        extended.putArray("messages");
        extended.put("clientTurnId", "host-turn-1").put("sessionId", "sess-1");
        assertEquals(legacy, sent.get(0));
        assertEquals(extended, sent.get(1));
    }

    private static RpcCaller capturing(List<ObjectNode> sent) {
        return new RpcCaller() {
            @Override
            public <T> CompletableFuture<T> invoke(String method, Object params, Class<T> resultType) {
                sent.add((ObjectNode) params);
                return CompletableFuture.completedFuture(null);
            }
        };
    }
}
