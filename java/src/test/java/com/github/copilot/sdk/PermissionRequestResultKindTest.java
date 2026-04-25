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
        assertEquals("approve-once", PermissionRequestResultKind.APPROVED.getValue());
        assertEquals("reject", PermissionRequestResultKind.REJECTED.getValue());
        assertEquals("user-not-available", PermissionRequestResultKind.USER_NOT_AVAILABLE.getValue());
        assertEquals("no-result", PermissionRequestResultKind.NO_RESULT.getValue());

        // Deprecated aliases still resolve
        assertEquals(PermissionRequestResultKind.REJECTED, PermissionRequestResultKind.DENIED_INTERACTIVELY_BY_USER);
        assertEquals(PermissionRequestResultKind.USER_NOT_AVAILABLE,
                PermissionRequestResultKind.DENIED_COULD_NOT_REQUEST_FROM_USER);
        assertEquals(PermissionRequestResultKind.USER_NOT_AVAILABLE, PermissionRequestResultKind.DENIED_BY_RULES);
    }

    @Test
    void equals_sameValue_returnsTrue() {
        var a = new PermissionRequestResultKind("approve-once");
        assertEquals(PermissionRequestResultKind.APPROVED, a);
        assertEquals(a, PermissionRequestResultKind.APPROVED);
    }

    @Test
    void equals_differentValue_returnsFalse() {
        assertNotEquals(PermissionRequestResultKind.APPROVED, PermissionRequestResultKind.REJECTED);
    }

    @Test
    void equals_isCaseInsensitive() {
        var upper = new PermissionRequestResultKind("APPROVE-ONCE");
        assertEquals(PermissionRequestResultKind.APPROVED, upper);
    }

    @Test
    void hashCode_isCaseInsensitive() {
        var upper = new PermissionRequestResultKind("APPROVE-ONCE");
        assertEquals(PermissionRequestResultKind.APPROVED.hashCode(), upper.hashCode());
    }

    @Test
    void toString_returnsValue() {
        assertEquals("approve-once", PermissionRequestResultKind.APPROVED.toString());
        assertEquals("reject", PermissionRequestResultKind.REJECTED.toString());
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
        assertNotEquals(PermissionRequestResultKind.APPROVED, "approve-once");
    }

    @Test
    void jsonSerialize_writesStringValue() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var result = new PermissionRequestResult().setKind(PermissionRequestResultKind.APPROVED);
        String json = mapper.writeValueAsString(result);
        assertTrue(json.contains("\"kind\":\"approve-once\""), "Expected kind to be serialized as string: " + json);
    }

    @Test
    void jsonDeserialize_readsStringValue() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = "{\"kind\":\"reject\"}";
        var result = mapper.readValue(json, PermissionRequestResult.class);
        assertEquals("reject", result.getKind());
    }

    @Test
    void permissionRequestResult_setKindWithKindType() {
        var result = new PermissionRequestResult().setKind(PermissionRequestResultKind.APPROVED);
        assertEquals("approve-once", result.getKind());
    }

    @Test
    void permissionRequestResult_setKindWithString_backwardCompatible() {
        var result = new PermissionRequestResult().setKind("approve-once");
        assertEquals("approve-once", result.getKind());
    }

    @Test
    void jsonRoundTrip_allWellKnownKinds() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        PermissionRequestResultKind[] kinds = {PermissionRequestResultKind.APPROVED,
                PermissionRequestResultKind.REJECTED, PermissionRequestResultKind.USER_NOT_AVAILABLE,
                PermissionRequestResultKind.NO_RESULT,};
        for (PermissionRequestResultKind kind : kinds) {
            var result = new PermissionRequestResult().setKind(kind);
            String json = mapper.writeValueAsString(result);
            var deserialized = mapper.readValue(json, PermissionRequestResult.class);
            assertEquals(kind.getValue(), deserialized.getKind());
        }
    }
}
