/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.github.copilot.rpc.PermissionRequestResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Tests for {@link PermissionRequestResult} factory methods and feedback field.
 */
public class PermissionRequestResultTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().serializationInclusion(JsonInclude.Include.NON_NULL)
            .build();

    @Test
    void testApproveOnce() {
        var result = PermissionRequestResult.approveOnce();
        assertEquals("approve-once", result.getKind());
        assertNull(result.getFeedback());
    }

    @Test
    void testRejectWithFeedback() {
        var result = PermissionRequestResult.reject("Not allowed");
        assertEquals("reject", result.getKind());
        assertEquals("Not allowed", result.getFeedback());
    }

    @Test
    void testRejectWithoutFeedback() {
        var result = PermissionRequestResult.reject(null);
        assertEquals("reject", result.getKind());
        assertNull(result.getFeedback());
    }

    @Test
    void testUserNotAvailable() {
        var result = PermissionRequestResult.userNotAvailable();
        assertEquals("user-not-available", result.getKind());
        assertNull(result.getFeedback());
    }

    @Test
    void testNoResult() {
        var result = PermissionRequestResult.noResult();
        assertEquals("no-result", result.getKind());
        assertNull(result.getFeedback());
    }

    @Test
    void testFeedbackSerialized() throws Exception {
        var result = PermissionRequestResult.reject("Unsafe operation");
        var json = MAPPER.writeValueAsString(result);
        assertTrue(json.contains("\"feedback\":\"Unsafe operation\""));
        assertTrue(json.contains("\"kind\":\"reject\""));
    }

    @Test
    void testFeedbackNotSerializedWhenNull() throws Exception {
        var result = PermissionRequestResult.approveOnce();
        var json = MAPPER.writeValueAsString(result);
        assertFalse(json.contains("feedback"));
    }
}
