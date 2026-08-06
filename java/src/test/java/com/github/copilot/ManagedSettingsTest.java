/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/
package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.copilot.rpc.ManagedSettings;
import com.github.copilot.rpc.ManagedSettingsPermissions;
import com.github.copilot.rpc.ResumeSessionConfig;
import com.github.copilot.rpc.SessionConfig;
import java.util.List;
import org.junit.jupiter.api.Test;

class ManagedSettingsTest {
    @Test
    void forwardsManagedSettingsOnCreateAndResume() throws Exception {
        var permissions = new ManagedSettingsPermissions().setDisableBypassPermissionsMode("disable")
                .setDeny(List.of("Shell(rm *)")).setAsk(List.of()).setAllow(List.of());
        var settings = new ManagedSettings().setPermissions(permissions);

        var create = SessionRequestBuilder.buildCreateRequest(new SessionConfig().setManagedSettings(settings),
                "managed-create");
        var resume = SessionRequestBuilder.buildResumeRequest("managed-resume",
                new ResumeSessionConfig().setManagedSettings(settings));

        assertEquals(settings, create.getManagedSettings());
        assertEquals(settings, resume.getManagedSettings());
        var json = new ObjectMapper().writeValueAsString(create);
        assertTrue(json.contains("\"disableBypassPermissionsMode\":\"disable\""));
        assertTrue(json.contains("\"ask\":[]"));
        assertTrue(json.contains("\"allow\":[]"));
        assertFalse(json.contains("\"enableManagedSettings\""));
    }

    @Test
    void directInjectionEnablesManagedSafeguards() throws Exception {
        var session = new CopilotSession("session-1", null);
        SessionRequestBuilder.configureSession(session, new SessionConfig().setManagedSettings(new ManagedSettings()));

        var field = CopilotSession.class.getDeclaredField("managedSettingsEnabled");
        field.setAccessible(true);
        assertTrue(field.getBoolean(session));
    }

    @Test
    void rejectsUnsupportedBypassValue() {
        var permissions = new ManagedSettingsPermissions();
        assertThrows(IllegalArgumentException.class, () -> permissions.setDisableBypassPermissionsMode("enable"));
    }
}
