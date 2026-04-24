/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Per-session GitHub auth", async () => {
    const { copilotClient: client, openAiEndpoint, env } = await createSdkTestContext();

    // Redirect GitHub API calls (e.g., fetchCopilotUser) to the proxy
    // so per-session auth token resolution can be tested
    env.COPILOT_DEBUG_GITHUB_API_URL = env.COPILOT_API_URL;

    // Configure per-token responses on the proxy.
    // endpoints.api points back to the proxy so subsequent CAPI calls are also intercepted.
    const proxyUrl = env.COPILOT_API_URL;
    await openAiEndpoint.setCopilotUserByToken("token-alice", {
        login: "alice",
        copilot_plan: "individual_pro",
        endpoints: {
            api: proxyUrl,
            telemetry: "https://localhost:1/telemetry",
        },
        analytics_tracking_id: "alice-tracking-id",
    });

    await openAiEndpoint.setCopilotUserByToken("token-bob", {
        login: "bob",
        copilot_plan: "business",
        endpoints: {
            api: proxyUrl,
            telemetry: "https://localhost:1/telemetry",
        },
        analytics_tracking_id: "bob-tracking-id",
    });

    it("should create session with gitHubToken and check auth status", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            gitHubToken: "token-alice",
        });

        const authStatus = await session.rpc.auth.getStatus();
        expect(authStatus.isAuthenticated).toBe(true);
        expect(authStatus.login).toBe("alice");
        expect(authStatus.copilotPlan).toBe("individual_pro");

        await session.disconnect();
    });

    it("should isolate auth between sessions with different tokens", async () => {
        const sessionA = await client.createSession({
            onPermissionRequest: approveAll,
            gitHubToken: "token-alice",
        });
        const sessionB = await client.createSession({
            onPermissionRequest: approveAll,
            gitHubToken: "token-bob",
        });

        const statusA = await sessionA.rpc.auth.getStatus();
        const statusB = await sessionB.rpc.auth.getStatus();

        expect(statusA.isAuthenticated).toBe(true);
        expect(statusA.login).toBe("alice");
        expect(statusA.copilotPlan).toBe("individual_pro");

        expect(statusB.isAuthenticated).toBe(true);
        expect(statusB.login).toBe("bob");
        expect(statusB.copilotPlan).toBe("business");

        await sessionA.disconnect();
        await sessionB.disconnect();
    });

    it("should return unauthenticated when no token is provided", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
        });

        const authStatus = await session.rpc.auth.getStatus();
        // Without a per-session GitHub token, there is no per-session identity.
        // In CI the process-level fake token may still authenticate globally,
        // so we check login rather than isAuthenticated.
        expect(authStatus.login).toBeFalsy();

        await session.disconnect();
    });

    it("should error when creating session with invalid token", async () => {
        await expect(
            client.createSession({
                onPermissionRequest: approveAll,
                gitHubToken: "invalid-token-12345",
            })
        ).rejects.toThrow();
    });
});
