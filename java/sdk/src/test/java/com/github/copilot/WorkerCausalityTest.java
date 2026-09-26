/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.copilot.generated.SessionEvent;
import com.github.copilot.generated.rpc.SessionTasksSendMessageResult;

class WorkerCausalityTest {
    private static final ObjectMapper MAPPER = JsonRpcClient.getObjectMapper();

    private static JsonNode corpus() throws Exception {
        return MAPPER.readTree(Path.of(System.getProperty("copilot.tests.dir"), "worker-causality.json").toFile());
    }

    private static String decode(JsonNode value, boolean event) throws Exception {
        return event
                ? MAPPER.writeValueAsString(MAPPER.treeToValue(value, SessionEvent.class))
                : MAPPER.writeValueAsString(MAPPER.treeToValue(value, SessionTasksSendMessageResult.class));
    }

    private static ObjectNode payload(JsonNode value, boolean event) {
        return (ObjectNode) (event ? value.get("data") : value);
    }

    @Test
    void publicReadersPreserveSourceAndHistoricalProductBytes() throws Exception {
        JsonNode corpus = corpus();
        for (JsonNode test : corpus.get("valid")) {
            boolean event = test.has("event");
            JsonNode wire = test.get(event ? "event" : "result").deepCopy();
            JsonNode observed = MAPPER.readTree(decode(wire, event));
            assertEquals(payload(wire, event).get("workerCausality"), payload(observed, event).get("workerCausality"),
                    test.get("name").asText());

            payload(wire, event).remove("workerCausality");
            String baseline = decode(wire, event);
            for (JsonNode invalid : corpus.get("invalid")) {
                payload(wire, event).set("workerCausality", invalid.get("value"));
                assertEquals(baseline, decode(wire, event), test.get("name") + ": " + invalid.get("name"));
            }
        }
    }

    @Test
    void exactUtf8BudgetAndUnknownFields() throws Exception {
        JsonNode corpus = corpus();
        for (JsonNode test : corpus.get("boundaries")) {
            JsonNode wire = corpus.get("valid").get(0).get("event").deepCopy();
            payload(wire, true).set("workerCausality", test.get("value"));
            ObjectNode observed = payload(MAPPER.readTree(decode(wire, true)), true);
            assertEquals(test.get("accepted").asBoolean(), observed.has("workerCausality"), test.get("name").asText());
            assertEquals(payload(wire, true).get("content"), observed.get("content"));
        }
    }
}
