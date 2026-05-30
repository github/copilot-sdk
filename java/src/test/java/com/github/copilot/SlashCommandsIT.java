/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.copilot.generated.rpc.SessionCommandsListResult;
import com.github.copilot.generated.rpc.SlashCommandInfo;
import com.github.copilot.rpc.CopilotClientOptions;
import com.github.copilot.rpc.PermissionHandler;
import com.github.copilot.rpc.SessionConfig;

/**
 * Failsafe integration test that exercises slash commands against the live
 * Copilot CLI (not the replay proxy).
 * <p>
 * Requires the CLI to be installed and the user to be signed in.
 */
class SlashCommandsIT {

    private static CopilotClient client;
    private static CopilotSession session;

    @BeforeAll
    static void setup() throws Exception {
        CopilotClientOptions options = new CopilotClientOptions()
                .setUseLoggedInUser(true);
        client = new CopilotClient(options);
        client.start().get(30, TimeUnit.SECONDS);
        session = client.createSession(new SessionConfig()
                .setOnPermissionRequest(PermissionHandler.APPROVE_ALL)).get(30, TimeUnit.SECONDS);
    }

    @AfterAll
    static void teardown() throws Exception {
        if (session != null) {
            session.close();
        }
        if (client != null) {
            client.close();
        }
    }

    @Test
    void listCommandsReturnsAtLeast20() throws Exception {
        SessionCommandsListResult result =
                session.getRpc().commands.list().get(15, TimeUnit.SECONDS);

        assertNotNull(result, "commands.list result must not be null");
        assertNotNull(result.commands(), "commands list must not be null");
        assertTrue(result.commands().size() >= 20,
                "Expected at least 20 commands but got " + result.commands().size());

        Pattern namePattern = Pattern.compile("^[a-z].*$");

        // Print every command so we can pick one for the next iteration
        System.out.println("=== Available slash commands ===");
        for (SlashCommandInfo cmd : result.commands()) {
            System.out.printf("  /%s  kind=%s  desc=%s  aliases=%s%n",
                    cmd.name(), cmd.kind(), cmd.description(), cmd.aliases());
            assertTrue(namePattern.matcher(cmd.name()).matches(),
                    "Command name should match /^[a-z].*$/ but was: " + cmd.name());
        }
        System.out.println("=== Total: " + result.commands().size() + " commands ===");
    }
}
