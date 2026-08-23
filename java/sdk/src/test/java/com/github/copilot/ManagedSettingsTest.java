/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/
package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.generated.rpc.DisableBypassPermissionsMode;
import com.github.copilot.rpc.DisableBypassPermissionsModes;
import com.github.copilot.rpc.ManagedSettings;
import com.github.copilot.rpc.ManagedSettingsPermissions;
import com.github.copilot.rpc.PermissionRequestResult;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.SessionConfig;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class ManagedSettingsTest {
    @Test
    void forwardsManagedSettingsOnCreateAndResume() throws Exception {
        var permissions = new ManagedSettingsPermissions()
                .setDisableBypassPermissionsMode(DisableBypassPermissionsMode.DISABLE).setDeny(List.of("Shell(rm *)"))
                .setAsk(List.of("Domain(publish.example)")).setAllow(List.of("Read(**)"));
        var managedSettings = new ManagedSettings().setPermissions(permissions);

        var create = SessionRequestBuilder.buildCreateRequest(
                new SessionConfig().setEnableManagedSettings(true).setManagedSettings(managedSettings),
                "managed-create");
        var resume = SessionRequestBuilder.buildResumeRequest("managed-resume",
                new ResumeSessionConfig().setEnableManagedSettings(true).setManagedSettings(managedSettings));

        assertEquals(managedSettings, create.getManagedSettings());
        assertEquals(managedSettings, resume.getManagedSettings());
        var json = new ObjectMapper().writeValueAsString(create);
        assertTrue(json.contains("\"enableManagedSettings\":true"));
        assertTrue(json.contains("\"managedSettings\":{\"permissions\""));
        assertTrue(json.contains("\"disableBypassPermissionsMode\":\"disable\""));
    }

    @Test
    void serializesKnownBypassPermissionsModes() throws Exception {
        var permissions = new ManagedSettingsPermissions()
                .setDisableBypassPermissionsMode(DisableBypassPermissionsModes.ALLOW_AUTO_ONLY);
        var json = new ObjectMapper().writeValueAsString(permissions);

        assertEquals(DisableBypassPermissionsModes.ALLOW_AUTO_ONLY, permissions.getDisableBypassPermissionsMode());
        assertTrue(json.contains("\"disableBypassPermissionsMode\":\"allow-auto-only\""));
    }

    @Test
    void acceptsFutureBypassPermissionsModes() throws Exception {
        var permissions = new ManagedSettingsPermissions().setDisableBypassPermissionsMode("future-fail-closed-mode");
        var json = new ObjectMapper().writeValueAsString(permissions);

        assertEquals("future-fail-closed-mode", permissions.getDisableBypassPermissionsMode());
        assertTrue(json.contains("\"disableBypassPermissionsMode\":\"future-fail-closed-mode\""));
    }

    @Test
    void preservesExplicitEmptyPermissionArrays() throws Exception {
        // Security-critical: a present empty allow list admits nothing, while an
        // absent (null) list imposes no such restriction. Jackson NON_NULL must
        // emit an explicit empty array as `[]` and omit null fields, so the two
        // remain distinguishable on the wire.
        var permissions = new ManagedSettingsPermissions().setDeny(List.of()).setAsk(List.of()).setAllow(List.of());
        var managedSettings = new ManagedSettings().setPermissions(permissions);
        var create = SessionRequestBuilder.buildCreateRequest(new SessionConfig().setManagedSettings(managedSettings),
                "managed-empty");

        var json = new ObjectMapper().writeValueAsString(create);
        assertTrue(json.contains("\"deny\":[]"), json);
        assertTrue(json.contains("\"ask\":[]"), json);
        assertTrue(json.contains("\"allow\":[]"), json);
    }

    @Test
    void distinguishesExplicitEmptyAllowFromAbsentAllow() throws Exception {
        // Present empty allow admits nothing; the null deny/ask must be omitted.
        var permissions = new ManagedSettingsPermissions().setAllow(List.of());
        var managedSettings = new ManagedSettings().setPermissions(permissions);
        var create = SessionRequestBuilder.buildCreateRequest(new SessionConfig().setManagedSettings(managedSettings),
                "managed-mixed");

        var json = new ObjectMapper().writeValueAsString(create);
        assertTrue(json.contains("\"allow\":[]"), json);
        assertFalse(json.contains("\"deny\""), json);
        assertFalse(json.contains("\"ask\""), json);
    }

    @Test
    void directInjectionEnablesManagedSafeguards() throws Exception {
        var session = new CopilotSession("session-1", null);
        var settings = new ManagedSettings().setPermissions(new ManagedSettingsPermissions());
        var managedSettingsEnabled = new AtomicBoolean();
        var config = new SessionConfig().setManagedSettings(settings).setOnPermissionRequest((request, invocation) -> {
            managedSettingsEnabled.set(invocation.isManagedSettingsEnabled());
            return CompletableFuture.completedFuture(PermissionRequestResult.noResult());
        });

        SessionRequestBuilder.configureSession(session, config);
        session.handlePermissionRequest(new ObjectMapper().readTree("{\"kind\":\"read\"}")).get();

        assertTrue(managedSettingsEnabled.get());
    }
}
