/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.github.copilot.rpc.MessageOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;
import com.github.copilot.rpc.ToolDefinition;

class StringSchemaToolsIT {
    @Test
    void stringSchemaApplyPatchOverrideBindsPatchInput() throws Exception {
        try (E2ETestContext ctx = E2ETestContext.create()) {
            ctx.configureForTest("tools", "string_schema_apply_patch_override_binds_patch_input");
            var receivedPatch = new CompletableFuture<String>();
            var tool = ToolDefinition.create("apply_patch", "Apply a patch", Map.of("type", "string"), invocation -> {
                receivedPatch.complete(invocation.getArgumentsAs(String.class));
                return CompletableFuture.completedFuture("HOST_PATCH_HANDLED");
            }).overridesBuiltInTool(true);
            try (CopilotClient client = ctx.createClient();
                    CopilotSession session = client.createSession(new SessionConfig()
                            .setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setTools(List.of(tool)))
                            .get(30, TimeUnit.SECONDS)) {
                var response = session
                        .sendAndWait(new MessageOptions().setPrompt("Use apply_patch to apply the supplied patch."))
                        .get(60, TimeUnit.SECONDS);

                assertTrue(receivedPatch.isDone(), "Typed string handler was not invoked");
                assertEquals("*** Begin Patch\n*** Add File: override-marker.txt\n+from-native\n*** End Patch",
                        receivedPatch.get(10, TimeUnit.SECONDS));
                assertNotNull(response);
                assertEquals("Host override completed.", response.getData().content());
                assertFalse(Files.exists(ctx.getWorkDir().resolve("override-marker.txt")));
            }
        }
    }
}
