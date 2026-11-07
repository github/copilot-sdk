/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.SessionInfoEvent.SessionInfoEventData;

class SessionInfoConstructorCompatibilityTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void releasedConstructorPreservesUrlAndTip() {
        var info = new SessionInfoEventData("notification", "message", "https://example.com", "tip");

        assertEquals("https://example.com", info.url());
        assertEquals("tip", info.tip());
        assertNull(info.responsesReasoning());
        assertFalse(MAPPER.valueToTree(info).has("responsesReasoning"));
    }

    @Test
    void internalMetadataRoundTripsThroughTheCanonicalRecord() throws Exception {
        JsonNode wire = MAPPER.readTree("""
                {"infoType":"reasoning_effort","message":"","responsesReasoning":{
                  "model":"model","initialEffort":"low","effort":"high"
                }}
                """);
        var info = MAPPER.treeToValue(wire, SessionInfoEventData.class);

        assertEquals("high", info.responsesReasoning().effort());
        assertEquals(wire, MAPPER.valueToTree(info));
    }
}
