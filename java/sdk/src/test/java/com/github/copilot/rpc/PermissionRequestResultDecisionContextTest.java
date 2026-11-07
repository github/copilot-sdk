/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.rpc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.rpc.PermissionDecisionContext;
import com.github.copilot.generated.rpc.PermissionDecisionOutcome;
import com.github.copilot.generated.rpc.PermissionDecisionSource;
import com.github.copilot.generated.rpc.PermissionDecisionSurface;
import com.github.copilot.generated.rpc.SessionPermissionsHandlePendingPermissionRequestParams;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link PermissionRequestResult} carries an optional
 * {@link PermissionDecisionContext} as a sibling of {@code result} — never
 * nested inside the serialized result — when the SDK forwards a permission
 * response to the runtime.
 */
class PermissionRequestResultDecisionContextTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static PermissionDecisionContext sampleContext() {
        return PermissionDecisionContext.builder().outcome(PermissionDecisionOutcome.AUTO_APPROVED)
                .source(PermissionDecisionSource.HOST_POLICY).surface(PermissionDecisionSurface.SDK)
                .responseCapability(null).userFeedback(null).build();
    }

    @Test
    void setDecisionContextForwardsContextAsSiblingOfResult() throws Exception {
        var result = PermissionRequestResult.approveOnce().setDecisionContext(sampleContext());
        var params = SessionPermissionsHandlePendingPermissionRequestParams.builder().sessionId("session-1")
                .requestId("req-1").result(result).decisionContext(result.getDecisionContext()).build();

        JsonNode json = MAPPER.valueToTree(params);

        assertTrue(json.has("decisionContext"), "decisionContext must be a top-level sibling of result");
        assertEquals("host_policy", json.get("decisionContext").get("source").asText());
        assertEquals("auto_approved", json.get("decisionContext").get("outcome").asText());
        assertEquals("sdk", json.get("decisionContext").get("surface").asText());
        assertFalse(json.get("result").has("decisionContext"), "decisionContext must NOT be nested inside result");
    }

    @Test
    void withoutContextOmitsDecisionContextKey() throws Exception {
        var result = PermissionRequestResult.approveOnce();
        assertNull(result.getDecisionContext());

        var params = SessionPermissionsHandlePendingPermissionRequestParams.builder().sessionId("session-1")
                .requestId("req-1").result(result).decisionContext(result.getDecisionContext()).build();

        JsonNode json = MAPPER.valueToTree(params);

        // Generated params record is @JsonInclude(NON_NULL), so a null
        // decisionContext is omitted entirely — byte-identical to legacy behavior.
        assertFalse(json.has("decisionContext"), "decisionContext key must be absent when no context is supplied");
    }

    @Test
    void setDecisionContextTwiceReplacesRatherThanNests() {
        var first = sampleContext();
        var second = PermissionDecisionContext.builder().outcome(PermissionDecisionOutcome.PROMPTED_USER)
                .source(PermissionDecisionSource.HUMAN_RESPONSE).surface(PermissionDecisionSurface.TUI)
                .responseCapability(null).userFeedback(null).build();

        var result = PermissionRequestResult.approveOnce().setDecisionContext(first).setDecisionContext(second);

        assertSame(second, result.getDecisionContext(), "second setDecisionContext must replace the first, not nest");
    }

    @Test
    void serializingResultWithContextDoesNotEmitContextInsideResult() throws Exception {
        var result = PermissionRequestResult.approveOnce().setDecisionContext(sampleContext());

        JsonNode resultJson = MAPPER.valueToTree(result);

        assertFalse(resultJson.has("decisionContext"),
                "@JsonIgnore must keep decisionContext out of the serialized result");
        assertEquals(PermissionRequestResultKind.APPROVED.getValue(), resultJson.get("kind").asText());
    }

    @Test
    void userFeedbackRemainsSeparateFromThePermissionResult() {
        var context = PermissionDecisionContext.builder().outcome(PermissionDecisionOutcome.PROMPTED_USER)
                .source(PermissionDecisionSource.HUMAN_RESPONSE).surface(PermissionDecisionSurface.SDK)
                .responseCapability(null).userFeedback("This action is acceptable.").build();
        var result = PermissionRequestResult.approveOnce().setDecisionContext(context);
        var params = SessionPermissionsHandlePendingPermissionRequestParams.builder().sessionId("session-1")
                .requestId("req-1").result(result).decisionContext(context).build();

        JsonNode json = MAPPER.valueToTree(params);

        assertEquals("This action is acceptable.", json.get("decisionContext").get("userFeedback").asText());
        assertFalse(json.get("result").has("userFeedback"));
        assertFalse(json.get("result").has("feedback"));
    }

    @Test
    void setDecisionContextAcceptsNullAsNoContext() {
        var result = PermissionRequestResult.approveOnce().setDecisionContext(sampleContext());

        result.setDecisionContext(null);

        assertNull(result.getDecisionContext(), "null must clear the context rather than throwing");
    }
}
