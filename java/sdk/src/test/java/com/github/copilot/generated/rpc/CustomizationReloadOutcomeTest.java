/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.generated.rpc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class CustomizationReloadOutcomeTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void futureReloadValuesRoundTripWithoutChangingKnownValues() throws Exception {
        var wire = MAPPER.readTree("""
                {"status":"newStatus","subsystem":"newSubsystem","detail":"new component"}
                """);
        var outcome = MAPPER.treeToValue(wire, CustomizationReloadOutcome.class);

        assertEquals("newStatus", outcome.status().getValue());
        assertEquals("newSubsystem", outcome.subsystem().getValue());
        assertEquals("new component", outcome.detail());
        assertEquals(wire, MAPPER.valueToTree(outcome));
        assertEquals(CustomizationReloadStatus.RELOADED, CustomizationReloadStatus.fromValue("reloaded"));
        assertEquals(CustomizationReloadSubsystem.SKILLS, CustomizationReloadSubsystem.fromValue("skills"));
        assertEquals("\"reloaded\"", MAPPER.writeValueAsString(CustomizationReloadStatus.RELOADED));
        assertEquals("\"skills\"", MAPPER.writeValueAsString(CustomizationReloadSubsystem.SKILLS));
    }
}
