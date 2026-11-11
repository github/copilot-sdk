/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.generated.rpc;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.AllowCopilotExperimental;

@AllowCopilotExperimental
class FetchErrorSerializationTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void responseModesMatchRequestModesAndRoundTrip() throws Exception {
        for (var mode : FetchResponseMode.values()) {
            var json = MAPPER.createObjectNode();
            json.put("responseMode", mode.getValue());
            json.put("url", "https://example.com");
            json.put("status", 200);
            json.put("statusText", "OK");
            json.putArray("headers");
            if (mode == FetchResponseMode.TEXT) {
                json.put("bodyText", "hello");
                json.put("endedAtUnverifiedEof", false);
            } else if (mode == FetchResponseMode.FILE) {
                json.put("path", "response.txt");
                json.put("sizeBytes", 5);
                json.put("endedAtUnverifiedEof", false);
            }
            var result = MAPPER.treeToValue(json, FetchSendResult.class);
            assertTrue(result.getResponseMode().equals(mode), mode.getValue());
            assertEquals(json, MAPPER.readTree(MAPPER.writeValueAsString(result)));
        }
    }

    @Test
    void credentialErrorsExposeTheirStableCode() throws Exception {
        var json = """
                {"code":"credential_revoked"}
                """;
        var data = MAPPER.readValue(json, CredentialContextErrorData.class);
        assertEquals(CredentialContextErrorCode.CREDENTIAL_REVOKED, data.code());
        assertEquals(MAPPER.readTree(json), MAPPER.readTree(MAPPER.writeValueAsString(data)));
    }

    @Test
    void transportErrorsPreserveClassificationAndOptionalCode() throws Exception {
        var json = """
                {"kind":"timeout","code":"ETIMEDOUT","permanentRequestFailure":false,"networkError":true}
                """;
        var data = MAPPER.readValue(json, FetchTransportErrorData.class);
        assertEquals(FetchTransportErrorKind.TIMEOUT, data.kind());
        assertEquals("ETIMEDOUT", data.code());
        assertFalse(data.permanentRequestFailure());
        assertTrue(data.networkError());
        assertEquals(MAPPER.readTree(json), MAPPER.readTree(MAPPER.writeValueAsString(data)));

        var withoutCode = MAPPER.readTree(json);
        ((com.fasterxml.jackson.databind.node.ObjectNode) withoutCode).remove("code");
        var omitted = MAPPER.treeToValue(withoutCode, FetchTransportErrorData.class);
        assertNull(omitted.code());
        assertEquals(withoutCode, MAPPER.readTree(MAPPER.writeValueAsString(omitted)));
    }
}
