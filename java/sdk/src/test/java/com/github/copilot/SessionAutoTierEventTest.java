/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.github.copilot.generated.AutoTier;
import com.github.copilot.generated.SessionEvent;
import com.github.copilot.generated.SessionResumeEvent;
import com.github.copilot.generated.SessionStartEvent;

/**
 * Verifies auto routing preferences on generated session lifecycle events.
 */
class SessionAutoTierEventTest {

    private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();

    @ParameterizedTest
    @CsvSource({"session.start,EFFICIENCY,efficiency", "session.start,BALANCE,balance",
            "session.start,INTELLIGENCE,intelligence", "session.resume,EFFICIENCY,efficiency",
            "session.resume,BALANCE,balance", "session.resume,INTELLIGENCE,intelligence"})
    void canonicalAutoTierRoundTrips(String type, AutoTier tier, String value) throws Exception {
        String json = """
                {"type":"%s","data":{"selectedModel":"auto","autoTier":"%s"}}
                """.formatted(type, value);

        var event = MAPPER.readValue(json, SessionEvent.class);
        assertEquals(tier, autoTier(event, type));
        String serialized = MAPPER.writeValueAsString(event);
        assertEquals(value, MAPPER.readTree(serialized).path("data").path("autoTier").asText());
        assertEquals(tier, autoTier(MAPPER.readValue(serialized, SessionEvent.class), type));
    }

    @ParameterizedTest
    @ValueSource(strings = {"session.start", "session.resume"})
    void missingOrNullAutoTierRemainsOptional(String type) throws Exception {
        for (String data : new String[]{"{}", "{\"autoTier\":null}"}) {
            String json = """
                    {"type":"%s","data":%s}
                    """.formatted(type, data);

            var event = MAPPER.readValue(json, SessionEvent.class);
            assertNull(autoTier(event, type));
            var serialized = MAPPER.readTree(MAPPER.writeValueAsString(event));
            assertFalse(serialized.path("data").has("autoTier"));
        }
    }

    private static AutoTier autoTier(SessionEvent event, String type) {
        if ("session.start".equals(type)) {
            return assertInstanceOf(SessionStartEvent.class, event).getData().autoTier();
        }
        return assertInstanceOf(SessionResumeEvent.class, event).getData().autoTier();
    }
}
