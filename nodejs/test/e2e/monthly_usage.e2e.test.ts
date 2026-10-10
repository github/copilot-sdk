/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { createServer } from "node:http";
import { mkdir, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { expect, it, onTestFinished } from "vitest";
import { approveAll, CopilotClient, RuntimeConnection } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

const { env, workDir, openAiEndpoint } = await createSdkTestContext({
    useStdio: true,
    replayOnly: true,
    copilotClientOptions: { useLoggedInUser: false, gitHubToken: undefined },
});

it("preserves monthly Microsoft 365 usage without treating it as session spend", async ({
    signal,
}) => {
    const profile = join(workDir, "monthly-usage-profile");
    await mkdir(profile);
    let usage: Record<string, unknown> = {};
    let quotaStatus = 200;
    let invalidQuota = false;
    const requests: string[] = [];
    const server = createServer((request, response) => {
        requests.push(`${request.method} ${request.url}`);
        expect(request.headers.authorization).toBe("Bearer monthly-usage-token");
        response.setHeader("Content-Type", "application/json");
        if (request.url === "/models") {
            response.end(
                JSON.stringify({
                    data: [{ id: "gpt-5-mini", name: "GPT-5 mini", model_picker_enabled: true }],
                })
            );
        } else if (request.url === "/quota") {
            if (quotaStatus !== 200) {
                response
                    .writeHead(quotaStatus)
                    .end(JSON.stringify({ error: { code: "quota_read_failed" } }));
                return;
            }
            if (invalidQuota) {
                response.end("not JSON");
                return;
            }
            response.end(
                JSON.stringify({
                    quotaSnapshots: {
                        enterprise: {
                            source: "enterprise",
                            service: "GitHubCopilotApp",
                            accessState: "allowed",
                            capacityState: "available",
                            hasQuota: true,
                            entitledQuantity: 100,
                            availableQuantity: 13,
                            ...usage,
                        },
                    },
                })
            );
        } else {
            response.writeHead(404).end();
        }
    });
    let client: CopilotClient | undefined;
    const listening = new Promise<void>((resolve, reject) => {
        server.once("error", reject);
        server.listen(0, "127.0.0.1", resolve);
    });
    onTestFinished(async () => {
        try {
            await client?.stop();
        } finally {
            await listening;
            server.closeAllConnections();
            await new Promise<void>((resolve, reject) =>
                server.close((error) => (error ? reject(error) : resolve()))
            );
        }
    });
    await listening;
    signal.throwIfAborted();
    const address = server.address();
    if (!address || typeof address === "string") throw new Error("Expected a TCP quota fixture");
    const host = `http://127.0.0.1:${address.port}`;
    const baseId = "monthly-usage-base";
    const emuToken = "ghu_monthly_usage_emu";
    const account = { host, login: "monthly-user", kind: "loki", derivedFrom: baseId };
    await openAiEndpoint.setCopilotUserByToken(emuToken, {
        login: "monthly_emu",
        is_staff: true,
        copilot_plan: "business",
        endpoints: { api: env.COPILOT_API_URL, telemetry: "https://localhost:1/telemetry" },
    });
    await writeFile(join(profile, "settings.json"), JSON.stringify({ storeTokenPlaintext: true }));
    await writeFile(
        join(profile, "config.json"),
        JSON.stringify({
            loggedInUsers: [
                account,
                {
                    host: "https://github.com",
                    login: "monthly_emu",
                    kind: "entraEmu",
                    derivedFrom: baseId,
                },
            ],
            lastLoggedInUser: account,
            authTokens: {
                [`entra:${baseId}:entra`]: {
                    token: JSON.stringify({ hostOwned: { account_id: baseId } }),
                },
                [`loki:${baseId}:loki`]: {
                    token: "monthly-usage-token",
                    expiresAt: "2099-01-01T00:00:00Z",
                    accountType: "entra",
                },
                "https://github.com:monthly_emu:entra": {
                    token: emuToken,
                    expiresAt: "2099-01-01T00:00:00Z",
                    accountType: "entra",
                },
            },
        })
    );
    client = new CopilotClient({
        workingDirectory: workDir,
        useLoggedInUser: true,
        connection: RuntimeConnection.forStdio({ path: process.env.COPILOT_CLI_PATH }),
        env: {
            ...env,
            COPILOT_HOME: profile,
            COPILOT_DISABLE_KEYTAR: "1",
            GH_TOKEN: "",
            GITHUB_TOKEN: "",
            COPILOT_GITHUB_TOKEN: "",
            COPILOT_SDK_AUTH_TOKEN: "",
            GITHUB_COPILOT_API_TOKEN: "",
            COPILOT_HMAC_KEY: "",
            CAPI_HMAC_KEY: "",
        },
    });
    await using session = await client.createSession({
        configDirectory: profile,
        onPermissionRequest: approveAll,
    });
    const before = await session.rpc.usage.getMetrics();
    if (process.platform === "linux") {
        expect(await session.rpc.gitHubAuth.getStatus()).toMatchObject({ isAuthenticated: false });
        await expect(session.rpc.quota.refresh()).rejects.toThrow("Not authenticated");
        expect((await session.rpc.quota.get()).providerQuotas).toBeUndefined();
        expect((await session.rpc.usage.getMetrics()).totalNanoAiu).toBe(before.totalNanoAiu);
        expect(requests).toEqual([]);
        return;
    }
    const initial = await session.rpc.quota.refresh();
    expect(
        initial.providerQuotas?.find((state) => state.provider.kind === "loki")?.monthlyUsage
    ).toBeUndefined();

    for (const consumedQuantity of [0, 125.375]) {
        usage = {
            usageState: "available",
            usageUnit: "ai_credits",
            usageScope: "user",
            consumedQuantity,
            usageQueriedAt: "2026-10-08T12:00:00Z",
            usageCycleStart: "2026-10-01T00:00:00Z",
            usageResetOn: "2026-11-01T00:00:00Z",
        };
        const quota = await session.rpc.quota.refresh();
        const state = quota.providerQuotas?.find((state) => state.provider.kind === "loki");
        expect(state).toMatchObject({
            accessState: "allowed",
            capacityState: "available",
            availableQuantity: 13,
            quantityKind: "advisory_balance",
            monthlyUsage: {
                state: "available",
                unit: "ai_credits",
                scope: "user",
                consumedQuantity,
                queriedAt: "2026-10-08T12:00:00Z",
                cycleStart: "2026-10-01T00:00:00Z",
                resetOn: "2026-11-01T00:00:00Z",
            },
        });
        expect((await session.rpc.quota.get()).providerQuotas).toEqual(quota.providerQuotas);
        expect(await session.rpc.quota.takeWarnings()).toEqual([]);
    }
    for (const usageState of ["no_policy", "unavailable"]) {
        usage = { usageState, usageUnit: "ai_credits", usageScope: "user" };
        const quota = await session.rpc.quota.refresh();
        expect(
            quota.providerQuotas?.find((state) => state.provider.kind === "loki")?.monthlyUsage
        ).toEqual({
            state: usageState,
            unit: "ai_credits",
            scope: "user",
        });
    }
    for (const status of [401, 403, 404, 429, 503]) {
        quotaStatus = status;
        const quota = await session.rpc.quota.refresh();
        const state = quota.providerQuotas?.find((state) => state.provider.kind === "loki");
        expect(state).toMatchObject({
            httpStatus: status,
            acquisitionStatus: status === 404 ? "unavailable" : "failed",
        });
        expect(state?.monthlyUsage).toBeUndefined();
    }
    quotaStatus = 200;
    invalidQuota = true;
    const invalid = await session.rpc.quota.refresh();
    const invalidState = invalid.providerQuotas?.find((state) => state.provider.kind === "loki");
    expect(invalidState).toMatchObject({
        acquisitionStatus: "failed",
        acquisitionError: "invalid_json",
    });
    expect(invalidState?.monthlyUsage).toBeUndefined();
    expect((await session.rpc.usage.getMetrics()).totalNanoAiu).toBe(before.totalNanoAiu);
    expect(requests.length).toBeGreaterThan(0);
    expect(requests.every((request) => request === "GET /models" || request === "GET /quota")).toBe(
        true
    );
});
