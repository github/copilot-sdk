/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/
package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
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
                .setDisableBypassPermissionsMode(DisableBypassPermissionsModes.DISABLE)
                .setDisableAssistedPermissionsMode(true).setDeny(List.of("Shell(rm *)"))
                .setAsk(List.of("Domain(publish.example)")).setAllow(List.of("Read(**)"))
                .setLimitTo(List.of("Domain(github.com)"));
        var managedSettings = new ManagedSettings().setPermissions(permissions);

        var create = SessionRequestBuilder.buildCreateRequest(new SessionConfig().setEnableManagedSettings(true)
                .setExcludedManagedSettings(List.of("sandbox", "allowedMcpServers"))
                .setEnforceManagedModelDefaults(true).setInteractiveUser(true).setManagedSettings(managedSettings),
                "managed-create");
        var resume = SessionRequestBuilder.buildResumeRequest("managed-resume",
                new ResumeSessionConfig().setEnableManagedSettings(true).setEnforceManagedModelDefaults(true)
                        .setExcludedManagedSettings(List.of("deniedMcpServers")).setInteractiveUser(true)
                        .setManagedSettings(managedSettings));

        assertEquals(managedSettings, create.getManagedSettings());
        assertEquals(managedSettings, resume.getManagedSettings());
        assertEquals(List.of("sandbox", "allowedMcpServers"), create.getExcludedManagedSettings());
        assertEquals(List.of("deniedMcpServers"), resume.getExcludedManagedSettings());
        assertTrue(create.getEnforceManagedModelDefaults());
        assertTrue(resume.getEnforceManagedModelDefaults());
        assertEquals(Boolean.TRUE, create.getInteractiveUser());
        assertEquals(Boolean.TRUE, resume.getInteractiveUser());
        var mapper = new ObjectMapper();
        var json = mapper.writeValueAsString(create);
        var resumeJson = mapper.readTree(mapper.writeValueAsString(resume));
        assertEquals(mapper.valueToTree(List.of("deniedMcpServers")), resumeJson.get("excludedManagedSettings"));
        assertTrue(json.contains("\"enableManagedSettings\":true"));
        assertTrue(json.contains("\"excludedManagedSettings\":[\"sandbox\",\"allowedMcpServers\"]"));
        assertTrue(json.contains("\"enforceManagedModelDefaults\":true"));
        assertTrue(json.contains("\"interactiveUser\":true"));
        assertTrue(json.contains("\"managedSettings\":{\"permissions\""));
        assertTrue(json.contains("\"disableBypassPermissionsMode\":\"disable\""));
        assertTrue(json.contains("\"limitTo\":[\"Domain(github.com)\"]"));
        assertTrue(json.contains("\"disableAssistedPermissionsMode\":true"));
    }

    @Test
    void omitsExcludedManagedSettingsWhenUnset() throws Exception {
        var create = SessionRequestBuilder.buildCreateRequest(new SessionConfig(), "default-create");
        var resume = SessionRequestBuilder.buildResumeRequest("default-resume", new ResumeSessionConfig());
        var mapper = new ObjectMapper();

        for (var request : List.of(create, resume)) {
            var json = mapper.readTree(mapper.writeValueAsString(request));
            assertFalse(json.has("excludedManagedSettings"), json.toString());
        }
    }

    @Test
    void preservesExplicitEmptyExcludedManagedSettings() throws Exception {
        var create = SessionRequestBuilder.buildCreateRequest(new SessionConfig().setExcludedManagedSettings(List.of()),
                "empty-exclusions-create");
        var resume = SessionRequestBuilder.buildResumeRequest("empty-exclusions-resume",
                new ResumeSessionConfig().setExcludedManagedSettings(List.of()));
        var mapper = new ObjectMapper();

        for (var request : List.of(create, resume)) {
            var json = mapper.readTree(mapper.writeValueAsString(request));
            assertEquals(mapper.readTree("[]"), json.get("excludedManagedSettings"), json.toString());
        }
    }

    @Test
    void omitsInteractiveUserWhenUnset() throws Exception {
        var create = SessionRequestBuilder.buildCreateRequest(new SessionConfig(), "default-create");
        var resume = SessionRequestBuilder.buildResumeRequest("default-resume", new ResumeSessionConfig());
        var mapper = new ObjectMapper();

        assertFalse(mapper.writeValueAsString(create).contains("interactiveUser"));
        assertFalse(mapper.writeValueAsString(resume).contains("interactiveUser"));
    }

    @Test
    void acceptsFutureBypassPermissionsModes() throws Exception {
        var permissions = new ManagedSettingsPermissions().setDisableBypassPermissionsMode("future-fail-closed-mode");
        var json = new ObjectMapper().writeValueAsString(permissions);

        assertTrue(json.contains("\"disableBypassPermissionsMode\":\"future-fail-closed-mode\""));
    }

    @Test
    void preservesExplicitEmptyPermissionArrays() throws Exception {
        // Security-critical: a present empty allow list admits nothing, while an
        // absent (null) list imposes no such restriction. Jackson NON_NULL must
        // emit an explicit empty array as `[]` and omit null fields, so the two
        // remain distinguishable on the wire.
        var permissions = new ManagedSettingsPermissions().setDeny(List.of()).setAsk(List.of()).setAllow(List.of())
                .setLimitTo(List.of());
        var managedSettings = new ManagedSettings().setPermissions(permissions);
        var create = SessionRequestBuilder.buildCreateRequest(new SessionConfig().setManagedSettings(managedSettings),
                "managed-empty");

        var json = new ObjectMapper().writeValueAsString(create);
        assertTrue(json.contains("\"deny\":[]"), json);
        assertTrue(json.contains("\"ask\":[]"), json);
        assertTrue(json.contains("\"allow\":[]"), json);
        assertTrue(json.contains("\"limitTo\":[]"), json);
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
