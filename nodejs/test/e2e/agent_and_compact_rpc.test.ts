/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import type { CustomAgentConfig } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Agent Selection RPC", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    it("should list available custom agents", async () => {
        const customAgents: CustomAgentConfig[] = [
            {
                name: "test-agent",
                displayName: "Test Agent",
                description: "A test agent",
                prompt: "You are a test agent.",
            },
            {
                name: "another-agent",
                displayName: "Another Agent",
                description: "Another test agent",
                prompt: "You are another agent.",
            },
        ];

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            customAgents,
        });

        const result = await session.rpc.agent.list();
        expect(result.agents).toBeDefined();
        expect(Array.isArray(result.agents)).toBe(true);
        expect(result.agents.length).toBe(2);
        expect(result.agents[0].name).toBe("test-agent");
        expect(result.agents[0].displayName).toBe("Test Agent");
        expect(result.agents[0].description).toBe("A test agent");
        expect(result.agents[1].name).toBe("another-agent");

        await session.destroy();
    });

    it("should return null when no agent is selected", async () => {
        const customAgents: CustomAgentConfig[] = [
            {
                name: "test-agent",
                displayName: "Test Agent",
                description: "A test agent",
                prompt: "You are a test agent.",
            },
        ];

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            customAgents,
        });

        const result = await session.rpc.agent.getCurrent();
        expect(result.agent).toBeNull();

        await session.destroy();
    });

    it("should select and get current agent", async () => {
        const customAgents: CustomAgentConfig[] = [
            {
                name: "test-agent",
                displayName: "Test Agent",
                description: "A test agent",
                prompt: "You are a test agent.",
            },
        ];

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            customAgents,
        });

        // Select the agent
        const selectResult = await session.rpc.agent.select({ name: "test-agent" });
        expect(selectResult.agent).toBeDefined();
        expect(selectResult.agent.name).toBe("test-agent");
        expect(selectResult.agent.displayName).toBe("Test Agent");

        // Verify getCurrent returns the selected agent
        const currentResult = await session.rpc.agent.getCurrent();
        expect(currentResult.agent).not.toBeNull();
        expect(currentResult.agent!.name).toBe("test-agent");

        await session.destroy();
    });

    it("should deselect current agent", async () => {
        const customAgents: CustomAgentConfig[] = [
            {
                name: "test-agent",
                displayName: "Test Agent",
                description: "A test agent",
                prompt: "You are a test agent.",
            },
        ];

        const session = await client.createSession({
            onPermissionRequest: approveAll,
            customAgents,
        });

        // Select then deselect
        await session.rpc.agent.select({ name: "test-agent" });
        await session.rpc.agent.deselect();

        // Verify no agent is selected
        const currentResult = await session.rpc.agent.getCurrent();
        expect(currentResult.agent).toBeNull();

        await session.destroy();
    });

    it("should return empty list when no custom agents configured", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        const result = await session.rpc.agent.list();
        expect(result.agents).toEqual([]);

        await session.destroy();
    });
});

describe("Session Compact RPC", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    it("should compact session history after messages", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });

        // Send a message to create some history
        await session.sendAndWait({ prompt: "What is 2+2?" });

        // Compact the session
        const result = await session.rpc.compaction.compact();
        expect(typeof result.success).toBe("boolean");
        expect(typeof result.tokensRemoved).toBe("number");
        expect(typeof result.messagesRemoved).toBe("number");

        await session.destroy();
    }, 60000);
});
