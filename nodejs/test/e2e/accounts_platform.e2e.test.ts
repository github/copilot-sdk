/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { readFile, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Account provider platform support", async () => {
    const { copilotClient: client } = await createSdkTestContext({
        useStdio: true,
        copilotClientOptions: {
            env: {
                COPILOT_ENTRA_AUTH_AUD: "api://platform-gate-override",
                COPILOT_DEBUG_ENTRA_BROKER_URL: "http://127.0.0.1:1/unused-broker",
            },
        },
    });

    it("keeps ordinary providers available and rejects unsupported Entra login", async () => {
        const session = await client.createSession({ onPermissionRequest: approveAll });
        expect(
            await session.rpc.accounts.enumerate({ query: { kind: "providers" } })
        ).toMatchObject({
            kind: "providers",
            items: expect.arrayContaining([
                { kind: "githubDotCom", label: "GitHub.com", available: true },
                { kind: "proxima", label: "GitHub Enterprise Cloud (*.ghe.com)", available: true },
            ]),
        });
        const flow = await session.rpc.accounts.login.begin({ kind: "proxima" });
        try {
            expect(flow.step.kind).toBe("input-required");
        } finally {
            await session.rpc.accounts.login.cancel({ flowId: flow.flowId });
        }
        if (process.platform === "linux") {
            expect(
                await session.rpc.accounts.enumerate({
                    query: { kind: "providers", brokerAvailable: true },
                })
            ).toMatchObject({
                kind: "providers",
                items: expect.arrayContaining([
                    expect.objectContaining({ kind: "entra", available: false }),
                ]),
            });
            await expect(session.rpc.accounts.login.begin({ kind: "entra" })).rejects.toThrow(
                "only on Windows and macOS"
            );
        }
    });
});

describe("Saved account platform support", async () => {
    const { createClient, env, openAiEndpoint } = await createSdkTestContext({
        useStdio: true,
        copilotClientOptions: {
            gitHubToken: undefined,
            useLoggedInUser: true,
            env: {
                COPILOT_GITHUB_TOKEN: "",
                GH_TOKEN: "",
                GITHUB_TOKEN: "",
                GITHUB_ASKPASS: "",
                COPILOT_GH_HOST: "github.com",
                GH_HOST: "github.com",
                COPILOT_HMAC_KEY: "",
                CAPI_HMAC_KEY: "",
                GITHUB_COPILOT_API_TOKEN: "",
                COPILOT_ENABLE_ALT_PROVIDERS: "false",
                COPILOT_DISABLE_KEYTAR: "1",
            },
        },
    });
    it.each(["entraEmu", "githubDotCom"] as const)(
        "restores %s only where supported",
        async (kind) => {
            const path = join(env.COPILOT_HOME, "config.json");
            const authTokens = {
                "https://github.com:emu:entra": {
                    token: "ghu_platform_emu",
                    expiresAt: "2099-01-01T00:00:00Z",
                    accountType: "entra",
                },
                "https://github.com:entra:github": { token: "ghu_platform_github" },
            };
            const login = kind === "entraEmu" ? "emu" : "entra";
            const profile = {
                storeTokenPlaintext: true,
                loggedInUsers: [
                    {
                        host: "https://github.com",
                        login: "emu",
                        kind: "entraEmu",
                        derivedFrom: "base@tenant",
                    },
                    { host: "https://github.com", login: "entra", kind: "githubDotCom" },
                ],
                lastLoggedInUser: { host: "https://github.com", login },
                authTokens,
            };
            await writeFile(path, JSON.stringify(profile));
            await openAiEndpoint.setCopilotUserByToken("ghu_platform_emu", {
                login: "emu",
                copilot_plan: "individual",
            });
            await openAiEndpoint.setCopilotUserByToken("ghu_platform_github", {
                login: "entra",
                copilot_plan: "individual",
            });
            const client = createClient({ gitHubToken: undefined, useLoggedInUser: true });
            try {
                await client.start();
                expect(await client.getAuthStatus()).toMatchObject({
                    isAuthenticated: kind === "githubDotCom" || process.platform !== "linux",
                });
            } finally {
                await client.stop();
            }
            // The runtime adds a comment header to this JSON fixture.
            const saved = (await readFile(path, "utf8")).replace(/^\/\/.*$/gm, "");
            expect(JSON.parse(saved).authTokens).toEqual(authTokens);
        }
    );

    it("checks platform support before setCredentials replaces the selected account", async () => {
        const path = join(env.COPILOT_HOME, "config.json");
        const authTokens = {
            "https://github.com:emu:entra": { token: "ghu_platform_emu" },
            "https://github.com:ordinary:github": { token: "ghu_platform_github" },
        };
        await writeFile(
            path,
            JSON.stringify({
                storeTokenPlaintext: true,
                loggedInUsers: [
                    { host: "https://github.com", login: "emu" },
                    { host: "https://github.com", login: "ordinary", kind: "githubDotCom" },
                ],
                lastLoggedInUser: { host: "https://github.com", login: "ordinary" },
                authTokens,
            })
        );
        await openAiEndpoint.setCopilotUserByToken("ghu_platform_github", {
            login: "ordinary",
            copilot_plan: "individual",
        });
        const client = createClient({ gitHubToken: undefined, useLoggedInUser: true });
        try {
            const session = await client.createSession({ onPermissionRequest: approveAll });
            const original = await session.rpc.gitHubAuth.getStatus();
            expect(original).toMatchObject({ isAuthenticated: true, login: "ordinary" });
            const update = session.rpc.gitHubAuth.setCredentials({
                credentials: {
                    type: "user",
                    host: "https://github.com",
                    login: "emu",
                    copilotUser: { login: "emu" },
                },
            });
            if (process.platform === "linux") {
                await expect(update).rejects.toThrow("only on Windows and macOS");
                expect(await session.rpc.gitHubAuth.getStatus()).toEqual(original);
            } else {
                expect(await update).toMatchObject({ success: true });
                expect(await session.rpc.gitHubAuth.getStatus()).toMatchObject({
                    isAuthenticated: true,
                    login: "emu",
                });
            }
        } finally {
            await client.stop();
        }
        const saved = (await readFile(path, "utf8")).replace(/^\/\/.*$/gm, "");
        expect(JSON.parse(saved).authTokens).toEqual(authTokens);
    });
});
