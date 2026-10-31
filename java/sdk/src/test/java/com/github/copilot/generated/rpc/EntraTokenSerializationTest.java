/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.generated.rpc;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.AllowCopilotExperimental;

@AllowCopilotExperimental
class EntraTokenSerializationTest {
    @Test
    void callbackResultsHaveExactlyOneDiscriminator() throws Exception {
        var mapper = new ObjectMapper().enable(JsonParser.Feature.STRICT_DUPLICATE_DETECTION);
        var identity = new EntraTokenAcquireResultIdentity();
        identity.setAccountId("host-account");
        var token = new EntraTokenAcquireResultToken();
        token.setAccountId("host-account");
        token.setAccessToken("host-token");
        for (var result : List.<EntraTokenAcquireResult>of(identity, token, new EntraTokenAcquireResultUnavailable(),
                new EntraTokenAcquireResultCancelled())) {
            var wire = mapper.writeValueAsString(Map.of("jsonrpc", "2.0", "id", 1, "result", result));
            var serialized = mapper.readTree(wire).get("result");
            assertEquals(result.getKind(), serialized.get("kind").asText());
            assertEquals(result.getClass(), mapper.treeToValue(serialized, EntraTokenAcquireResult.class).getClass());
        }
    }
}
