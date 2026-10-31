/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.rpc.HostDiagnostics;
import com.github.copilot.rpc.GetStatusResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HostDiagnosticsTest {

    private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();

    @Test
    void acceptsStatusWithoutHostDiagnostics() throws Exception {
        GetStatusResponse response = MAPPER.readValue("{\"version\":\"1.2.3\",\"protocolVersion\":2}",
                GetStatusResponse.class);

        assertEquals("1.2.3", response.getVersion());
        assertEquals(2, response.getProtocolVersion());
        assertNull(response.getHostDiagnostics());
    }

    @ParameterizedTest
    @ValueSource(strings = {"pending", "enabled", "disabled"})
    void preservesHostDiagnosticsAndFluentSetters(String errorReporting) throws Exception {
        String json = """
                {"version":"1.2.3","protocolVersion":2,"hostDiagnostics":{"errorReporting":"%s"}}
                """.formatted(errorReporting);

        GetStatusResponse response = MAPPER.readValue(json, GetStatusResponse.class);

        assertInstanceOf(HostDiagnostics.class, response.getHostDiagnostics());
        assertEquals(errorReporting,
                MAPPER.valueToTree(response).path("hostDiagnostics").path("errorReporting").asText());
        assertSame(response, response.setHostDiagnostics(null));
        assertNull(response.getHostDiagnostics());
    }
}
