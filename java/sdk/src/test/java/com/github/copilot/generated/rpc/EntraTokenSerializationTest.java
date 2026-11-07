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
        var identity = EntraTokenAcquireResultIdentity.builder().accountId("host-account").build();
        var token = EntraTokenAcquireResultToken.builder().accountId("host-account").accessToken("host-token").build();
        for (var result : List.<EntraTokenAcquireResult>of(identity, token,
                EntraTokenAcquireResultUnavailable.builder().build(),
                EntraTokenAcquireResultCancelled.builder().build())) {
            var wire = mapper.writeValueAsString(Map.of("jsonrpc", "2.0", "id", 1, "result", result));
            var serialized = mapper.readTree(wire).get("result");
            assertEquals(result.getKind(), serialized.get("kind").asText());
            assertEquals(result.getClass(), mapper.treeToValue(serialized, EntraTokenAcquireResult.class).getClass());
        }
    }
}
