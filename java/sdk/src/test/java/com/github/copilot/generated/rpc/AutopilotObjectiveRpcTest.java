/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.generated.rpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

class AutopilotObjectiveRpcTest {

    private static final class StubCaller implements RpcCaller {

        record Call(String method, Object params) {
        }

        final List<Call> calls = new ArrayList<>();

        @Override
        public <T> CompletableFuture<T> invoke(String method, Object params, Class<T> resultType) {
            calls.add(new Call(method, params));
            return CompletableFuture.completedFuture(null);
        }
    }

    @Test
    void getState_injectsSessionId() {
        var stub = new StubCaller();
        var session = new SessionRpc(stub, "session-1");

        session.autopilotObjective.getState();

        assertEquals(1, stub.calls.size());
        assertEquals("session.autopilotObjective.getState", stub.calls.get(0).method());
        assertInstanceOf(Map.class, stub.calls.get(0).params());
        assertEquals("session-1", ((Map<?, ?>) stub.calls.get(0).params()).get("sessionId"));
    }

    @Test
    void state_preservesCanonicalPayloads() throws Exception {
        var noObjective = RpcMapper.INSTANCE.readValue("{\"state\":null}",
                SessionAutopilotObjectiveGetStateResult.class);
        assertNull(noObjective.state());

        var active = state("""
                {
                  "state": {
                    "id": 1,
                    "objective": "Ship the release",
                    "status": "active",
                    "turnCount": 2,
                    "creditCountNanoAiu": "0"
                  }
                }
                """);
        assertEquals(AutopilotObjectiveStatus.ACTIVE, active.status());
        assertNull(active.pauseReason());
        assertNull(active.completionSummary());
        assertNull(active.creditLimit());
        var activeJson = RpcMapper.INSTANCE.valueToTree(active);
        assertFalse(activeJson.has("pauseReason"));
        assertFalse(activeJson.has("completionSummary"));
        assertFalse(activeJson.has("creditLimit"));

        var paused = state("""
                {
                  "state": {
                    "id": 2,
                    "objective": "Wait for approval",
                    "status": "paused",
                    "turnCount": 3,
                    "pauseReason": "Approval required",
                    "creditCountNanoAiu": "9007199254740993",
                    "creditLimit": {
                      "creditsUsed": 9.007199254740993,
                      "creditsUsedNanoAiu": "9007199254740993"
                    }
                  }
                }
                """);
        assertEquals(AutopilotObjectiveStatus.PAUSED, paused.status());
        assertEquals("Approval required", paused.pauseReason());
        assertEquals("9007199254740993", paused.creditCountNanoAiu());
        assertNotNull(paused.creditLimit());
        assertNull(paused.creditLimit().credits());

        var completed = state("""
                {
                  "state": {
                    "id": 3,
                    "objective": "Publish the SDK",
                    "status": "completed",
                    "turnCount": 4,
                    "completionSummary": "Published",
                    "creditCountNanoAiu": "9007199254740994",
                    "creditLimit": {
                      "credits": 2.5,
                      "creditsUsed": 1.25,
                      "creditsUsedNanoAiu": "1250000000"
                    }
                  }
                }
                """);
        assertEquals(AutopilotObjectiveStatus.COMPLETED, completed.status());
        assertEquals("Published", completed.completionSummary());
        assertNotNull(completed.creditLimit());
        assertEquals(2.5, completed.creditLimit().credits());
        assertEquals("1250000000", completed.creditLimit().creditsUsedNanoAiu());
    }

    private static AutopilotObjectiveState state(String json) throws Exception {
        var result = RpcMapper.INSTANCE.readValue(json, SessionAutopilotObjectiveGetStateResult.class);
        assertNotNull(result.state());
        return result.state();
    }
}
