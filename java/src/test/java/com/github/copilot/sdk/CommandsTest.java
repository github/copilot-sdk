/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

package com.github.copilot.sdk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import com.github.copilot.sdk.json.CommandContext;
import com.github.copilot.sdk.json.CommandDefinition;
import com.github.copilot.sdk.json.CommandHandler;
import com.github.copilot.sdk.json.CommandWireDefinition;
import com.github.copilot.sdk.json.PermissionHandler;
import com.github.copilot.sdk.json.ResumeSessionConfig;
import com.github.copilot.sdk.json.SessionConfig;

/**
 * Unit tests for the Commands feature (CommandDefinition, CommandContext,
 * SessionConfig.commands, ResumeSessionConfig.commands, and the wire
 * representation).
 *
 * <p>
 * Ported from {@code CommandsTests.cs} in the reference implementation dotnet
 * SDK.
 * </p>
 */
class CommandsTest {

    @Test
    void commandDefinitionHasRequiredProperties() {
        CommandHandler handler = context -> CompletableFuture.completedFuture(null);
        var cmd = new CommandDefinition().setName("deploy").setDescription("Deploy the app").setHandler(handler);

        assertEquals("deploy", cmd.getName());
        assertEquals("Deploy the app", cmd.getDescription());
        assertNotNull(cmd.getHandler());
    }

    @Test
    void commandContextHasAllProperties() {
        var ctx = new CommandContext().setSessionId("session-1").setCommand("/deploy production")
                .setCommandName("deploy").setArgs("production");

        assertEquals("session-1", ctx.getSessionId());
        assertEquals("/deploy production", ctx.getCommand());
        assertEquals("deploy", ctx.getCommandName());
        assertEquals("production", ctx.getArgs());
    }

    @Test
    void sessionConfigCommandsAreCloned() {
        CommandHandler handler = ctx -> CompletableFuture.completedFuture(null);
        var config = new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                .setCommands(List.of(new CommandDefinition().setName("deploy").setHandler(handler)));

        var clone = config.clone();

        assertNotNull(clone.getCommands());
        assertEquals(1, clone.getCommands().size());
        assertEquals("deploy", clone.getCommands().get(0).getName());

        // Collections should be independent — clone list is a copy
        assertNotSame(config.getCommands(), clone.getCommands());
    }

    @Test
    void resumeConfigCommandsAreCloned() {
        CommandHandler handler = ctx -> CompletableFuture.completedFuture(null);
        var config = new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL)
                .setCommands(List.of(new CommandDefinition().setName("deploy").setHandler(handler)));

        var clone = config.clone();

        assertNotNull(clone.getCommands());
        assertEquals(1, clone.getCommands().size());
        assertEquals("deploy", clone.getCommands().get(0).getName());
    }

    @Test
    void buildCreateRequestIncludesCommandWireDefinitions() {
        CommandHandler handler = ctx -> CompletableFuture.completedFuture(null);
        var config = new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setCommands(
                List.of(new CommandDefinition().setName("deploy").setDescription("Deploy").setHandler(handler),
                        new CommandDefinition().setName("rollback").setHandler(handler)));

        var request = SessionRequestBuilder.buildCreateRequest(config);

        assertNotNull(request.getCommands());
        assertEquals(2, request.getCommands().size());
        assertEquals("deploy", request.getCommands().get(0).getName());
        assertEquals("Deploy", request.getCommands().get(0).getDescription());
        assertEquals("rollback", request.getCommands().get(1).getName());
        assertNull(request.getCommands().get(1).getDescription());
    }

    @Test
    void buildResumeRequestIncludesCommandWireDefinitions() {
        CommandHandler handler = ctx -> CompletableFuture.completedFuture(null);
        var config = new ResumeSessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL).setCommands(
                List.of(new CommandDefinition().setName("deploy").setDescription("Deploy").setHandler(handler)));

        var request = SessionRequestBuilder.buildResumeRequest("session-1", config);

        assertNotNull(request.getCommands());
        assertEquals(1, request.getCommands().size());
        assertEquals("deploy", request.getCommands().get(0).getName());
        assertEquals("Deploy", request.getCommands().get(0).getDescription());
    }

    @Test
    void buildCreateRequestWithNoCommandsHasNullCommandsList() {
        var config = new SessionConfig().setOnPermissionRequest(PermissionHandler.APPROVE_ALL);

        var request = SessionRequestBuilder.buildCreateRequest(config);

        assertNull(request.getCommands());
    }

    @Test
    void commandWireDefinitionHasNameAndDescription() {
        var wire = new CommandWireDefinition("deploy", "Deploy the app");

        assertEquals("deploy", wire.getName());
        assertEquals("Deploy the app", wire.getDescription());
    }

    @Test
    void commandWireDefinitionNullDescriptionAllowed() {
        var wire = new CommandWireDefinition("rollback", null);

        assertEquals("rollback", wire.getName());
        assertNull(wire.getDescription());
    }

    @Test
    void commandWireDefinitionFluentSetters() {
        var wire = new CommandWireDefinition();
        wire.setName("status");
        wire.setDescription("Show deployment status");

        assertEquals("status", wire.getName());
        assertEquals("Show deployment status", wire.getDescription());
    }

    @Test
    void commandWireDefinitionFluentSettersChaining() {
        var wire = new CommandWireDefinition().setName("logs").setDescription("View application logs");

        assertEquals("logs", wire.getName());
        assertEquals("View application logs", wire.getDescription());
    }
}
