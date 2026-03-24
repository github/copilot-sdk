/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.sdk.json.PermissionRequestResult;
import com.github.copilot.sdk.json.PermissionRequestResultKind;

/**
 * Unit tests for {@link PermissionRequestResultKind}.
 * <p>
 * Covers well-known kind values, equality, hash code, serialization, and
 * backward-compatible {@link PermissionRequestResult} integration.
 */
public class PermissionRequestResultKindTest {

    @Test
    void wellKnownKinds_haveExpectedValues() {
        assertEquals("approved", PermissionRequestResultKind.APPROVED.getValue());
        assertEquals("denied-by-rules", PermissionRequestResultKind.DENIED_BY_RULES.getValue());
        assertEquals("denied-no-approval-rule-and-could-not-request-from-user",
                PermissionRequestResultKind.DENIED_COULD_NOT_REQUEST_FROM_USER.getValue());
        assertEquals("denied-interactively-by-user",
                PermissionRequestResultKind.DENIED_INTERACTIVELY_BY_USER.getValue());
        assertEquals("no-result", PermissionRequestResultKind.NO_RESULT.getValue());
    }

    @Test
    void equals_sameValue_returnsTrue() {
        var a = new PermissionRequestResultKind("approved");
        assertEquals(PermissionRequestResultKind.APPROVED, a);
        assertEquals(a, PermissionRequestResultKind.APPROVED);
    }

    @Test
    void equals_differentValue_returnsFalse() {
        assertNotEquals(PermissionRequestResultKind.APPROVED, PermissionRequestResultKind.DENIED_BY_RULES);
    }

    @Test
    void equals_isCaseInsensitive() {
        var upper = new PermissionRequestResultKind("APPROVED");
        assertEquals(PermissionRequestResultKind.APPROVED, upper);
    }

    @Test
    void hashCode_isCaseInsensitive() {
        var upper = new PermissionRequestResultKind("APPROVED");
        assertEquals(PermissionRequestResultKind.APPROVED.hashCode(), upper.hashCode());
    }

    @Test
    void toString_returnsValue() {
        assertEquals("approved", PermissionRequestResultKind.APPROVED.toString());
        assertEquals("denied-by-rules", PermissionRequestResultKind.DENIED_BY_RULES.toString());
    }

    @Test
    void customValue_isPreserved() {
        var custom = new PermissionRequestResultKind("custom-kind");
        assertEquals("custom-kind", custom.getValue());
        assertEquals("custom-kind", custom.toString());
    }

    @Test
    void constructor_nullValue_treatedAsEmpty() {
        var kind = new PermissionRequestResultKind(null);
        assertEquals("", kind.getValue());
        assertEquals("", kind.toString());
    }

    @Test
    void equals_nonKindObject_returnsFalse() {
        assertNotEquals(PermissionRequestResultKind.APPROVED, "approved");
    }

    @Test
    void jsonSerialize_writesStringValue() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var result = new PermissionRequestResult().setKind(PermissionRequestResultKind.APPROVED);
        String json = mapper.writeValueAsString(result);
        assertTrue(json.contains("\"kind\":\"approved\""), "Expected kind to be serialized as string: " + json);
    }

    @Test
    void jsonDeserialize_readsStringValue() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = "{\"kind\":\"denied-by-rules\"}";
        var result = mapper.readValue(json, PermissionRequestResult.class);
        assertEquals("denied-by-rules", result.getKind());
    }

    @Test
    void permissionRequestResult_setKindWithKindType() {
        var result = new PermissionRequestResult().setKind(PermissionRequestResultKind.APPROVED);
        assertEquals("approved", result.getKind());
    }

    @Test
    void permissionRequestResult_setKindWithString_backwardCompatible() {
        var result = new PermissionRequestResult().setKind("approved");
        assertEquals("approved", result.getKind());
    }

    @Test
    void jsonRoundTrip_allWellKnownKinds() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        PermissionRequestResultKind[] kinds = {PermissionRequestResultKind.APPROVED,
                PermissionRequestResultKind.DENIED_BY_RULES,
                PermissionRequestResultKind.DENIED_COULD_NOT_REQUEST_FROM_USER,
                PermissionRequestResultKind.DENIED_INTERACTIVELY_BY_USER, PermissionRequestResultKind.NO_RESULT,};
        for (PermissionRequestResultKind kind : kinds) {
            var result = new PermissionRequestResult().setKind(kind);
            String json = mapper.writeValueAsString(result);
            var deserialized = mapper.readValue(json, PermissionRequestResult.class);
            assertEquals(kind.getValue(), deserialized.getKind());
        }
    }
}
