/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.generated.rpc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class SandboxConfigSerializationTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void allowBypassRoundTripsAndIsOmittedWhenAbsent() throws Exception {
        var configured = MAPPER.readValue("""
                {"enabled":true,"allowBypass":true}
                """, SandboxConfig.class);

        assertEquals(Boolean.TRUE, configured.allowBypass());
        var configuredJson = MAPPER.readTree(MAPPER.writeValueAsString(configured));
        assertTrue(configuredJson.path("allowBypass").asBoolean());

        var omitted = MAPPER.readValue("""
                {"enabled":true}
                """, SandboxConfig.class);
        var omittedJson = MAPPER.readTree(MAPPER.writeValueAsString(omitted));
        assertTrue(omittedJson.path("allowBypass").isMissingNode());
    }
}
