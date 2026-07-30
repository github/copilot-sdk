/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.github.copilot.generated.PermissionRequestedEvent;
import com.github.copilot.rpc.PermissionRequestResult;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.PermissionInvocation;
import com.github.copilot.rpc.PermissionRequest;
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

    @Test
    void testPermissionRequestExposesManagedApprovalRequired() throws Exception {
        var request = MAPPER.readValue("""
                {
                    "kind": "read",
                    "path": "/workspace/file.txt",
                    "managedApprovalRequired": true
                }
                """, PermissionRequest.class);

        assertTrue(request.getManagedApprovalRequired());
    }

    @Test
    void testPermissionEventValueConvertsToTypedRequest() {
        var event = MAPPER
                .convertValue(
                        java.util.Map.of("type", "permission.requested", "data",
                                java.util.Map.of("requestId", "permission-1", "permissionRequest", java.util.Map.of(
                                        "kind", "url", "managedApprovalRequired", true, "url", "https://example.com"))),
                        PermissionRequestedEvent.class);
        var request = PermissionRequest.fromJsonValue(event.getData().permissionRequest());

        assertTrue(request.getManagedApprovalRequired());
    }

    @Test
    void testApproveAllFailsWhenManagedSettingsEnabled() {
        var request = new PermissionRequest();
        request.setKind("read");
        request.setManagedApprovalRequired(true);

        var invocation = new PermissionInvocation().setManagedSettingsEnabled(true);
        var error = assertThrows(java.util.concurrent.CompletionException.class,
                () -> PermissionHandler.APPROVE_ALL.handle(request, invocation).join());

        assertTrue(error.getCause() instanceof IllegalStateException);
    }

    @Test
    void testApproveAllApprovesOrdinaryRequest() {
        var request = new PermissionRequest();
        request.setKind("read");

        var result = PermissionHandler.APPROVE_ALL.handle(request, new PermissionInvocation()).join();

        assertEquals("approve-once", result.getKind());
    }

    @Test
    void testApproveAllLeavesManagedRequestPendingWhenSessionFlagIsAbsent() {
        var request = new PermissionRequest();
        request.setKind("read");
        request.setManagedApprovalRequired(true);

        var result = PermissionHandler.APPROVE_ALL.handle(request, new PermissionInvocation()).join();

        assertEquals("no-result", result.getKind());
    }
}
