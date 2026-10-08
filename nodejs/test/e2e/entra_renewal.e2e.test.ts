/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { randomUUID } from "node:crypto";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { expect, it, onTestFinished } from "vitest";
import { approveAll, CopilotClient, RuntimeConnection } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { ConnectProxy } from "../../../test/harness/connectProxy.js";
import { createE2eRequestHandler } from "../../../test/harness/mockHandlers.js";

const { env, workDir, openAiEndpoint } = await createSdkTestContext({
    useStdio: true,
    replayOnly: true,
    copilotClientOptions: { useLoggedInUser: false, gitHubToken: undefined },
});

it("requires linked sign-in, adopts same-session re-login, and persists silent renewal", async () => {
    const profile = join(workDir, `entra-renewal-${randomUUID()}`);
    await mkdir(profile);
    await writeFile(join(profile, "settings.json"), '{"storeTokenPlaintext":true}');
    const host = "https://github.com";
    const login = "renewal_emu";
    const slot = `${host}:${login}`;
    const oid = randomUUID();
    const tid = randomUUID();
    const baseId = `entra:${oid}@${tid}`;
    const legacyToken = `ghu_legacy_${randomUUID()}`;
    await writeFile(
        join(profile, "config.json"),
        JSON.stringify({
            loggedInUsers: [{ host, login, kind: "entraEmu" }],
            authTokens: {
                [slot]: {
                    token: legacyToken,
                    expiresAt: "2099-01-01T00:00:00Z",
                    accountType: "entra",
                },
            },
        })
    );
    let signIns = 0;
    let grants = 0;
    let exchanges = 0;
    let refreshToken = "";
    let expectedGeneration = 1;
    let phase = "legacy account";
    const modelRequests: number[] = [];
    const tokenFor = (generation: number) => `ghu_renewal_${oid}_${generation}`;
    const fallback = createE2eRequestHandler({ capiProxyUrl: openAiEndpoint.url });
    const proxy = new ConnectProxy(
        async (request, response, domain) => {
            const path = request.url ?? "/";
            const reply = (status: number, body: unknown) => {
                response.writeHead(status, { "content-type": "application/json" });
                response.end(JSON.stringify(body));
                return true;
            };
            if (domain === "login.microsoftonline.com" && path.endsWith("/token")) {
                let body = "";
                for await (const chunk of request) body += chunk.toString();
                const grant = new URLSearchParams(body);
                if (grant.get("grant_type") === "authorization_code") {
                    signIns++;
                    expect(grant.get("scope")).toBe("openid profile offline_access");
                    refreshToken = `identity-refresh-${signIns}`;
                    return reply(200, {
                        id_token: `e30.${Buffer.from(JSON.stringify({ oid, tid })).toString("base64url")}.fixture`,
                        refresh_token: refreshToken,
                    });
                }
                expect(grant.get("grant_type")).toBe("refresh_token");
                expect(grant.get("refresh_token")).toBe(refreshToken);
                expect(grant.get("scope")).toContain(
                    "12f6db80-0741-4a7e-b9c5-b85d737b3a31/user_impersonation"
                );
                grants++;
                refreshToken = `rotated-refresh-${grants}`;
                return reply(200, {
                    access_token: "emu-resource-token",
                    refresh_token: refreshToken,
                    expires_in: 3600,
                });
            }
            if (domain === "github.com" && path === "/login/oauth/access_token") {
                let body = "";
                for await (const chunk of request) body += chunk.toString();
                expect(new URLSearchParams(body).get("subject_token")).toBe("emu-resource-token");
                exchanges++;
                return reply(200, {
                    access_token: tokenFor(exchanges),
                    token_type: "bearer",
                    expires_in: 3600,
                });
            }
            if (domain === "api.github.com" && path.startsWith("/copilot_internal/user")) {
                const issued = [
                    legacyToken,
                    ...Array.from({ length: exchanges }, (_, i) => tokenFor(i + 1)),
                ];
                expect(issued).toContain(request.headers.authorization?.split(" ").at(-1));
                return reply(200, {
                    login,
                    copilot_plan: "business",
                    is_staff: false,
                    is_mcp_enabled: true,
                    endpoints: {
                        api: "https://api.githubcopilot.com",
                        telemetry: "https://localhost:1",
                    },
                });
            }
            if (domain === "api.githubcopilot.com" && path === "/models") {
                const generation = Array.from({ length: exchanges }, (_, i) => i + 1).find(
                    (value) => request.headers.authorization?.split(" ").at(-1) === tokenFor(value)
                );
                modelRequests.push(generation ?? 0);
                if (generation !== expectedGeneration) {
                    return reply(401, {
                        error: {
                            message: `fixture rejected generation ${generation ?? 0} during ${phase}; expected ${expectedGeneration}, sign-ins ${signIns}, grants ${grants}, exchanges ${exchanges}`,
                        },
                    });
                }
                return reply(200, {
                    data: [
                        {
                            id: "gpt-5.2",
                            name: `Renewed catalog ${generation}`,
                            capabilities: { supports: { tool_calls: true } },
                            model_picker_enabled: true,
                        },
                    ],
                });
            }
            return fallback(request, response, domain);
        },
        {
            interceptDomains: [
                "login.microsoftonline.com",
                "github.com",
                "api.github.com",
                "api.githubcopilot.com",
                "api.mcp.github.com",
            ],
        }
    );
    let client: CopilotClient | undefined;
    const starting = proxy.start();
    onTestFinished(async () => {
        try {
            await client?.stop();
        } finally {
            await starting;
            await proxy.stop();
        }
    });
    await starting;
    const clientOptions = {
        connection: RuntimeConnection.forStdio({ path: process.env.COPILOT_CLI_PATH }),
        workingDirectory: workDir,
        useLoggedInUser: true,
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
            COPILOT_ENABLE_ALT_PROVIDERS: "",
            COPILOT_DEBUG_ENTRA_BROKER_URL: "",
            COPILOT_API_URL: "https://api.githubcopilot.com",
            COPILOT_DEBUG_GITHUB_API_URL: "https://api.github.com",
            HTTPS_PROXY: proxy.proxyUrl,
            HTTP_PROXY: proxy.proxyUrl,
            https_proxy: proxy.proxyUrl,
            http_proxy: proxy.proxyUrl,
            NODE_EXTRA_CA_CERTS: proxy.caFilePath,
            SSL_CERT_FILE: proxy.caFilePath,
        },
    };
    const sessionOptions = { configDirectory: profile, onPermissionRequest: approveAll };
    client = new CopilotClient(clientOptions);
    let session = await client.createSession(sessionOptions);
    expect(await session.rpc.accounts.get({ query: { kind: "activeAccount" } })).toEqual({
        kind: "activeAccount",
        account: null,
    });
    await expect(session.rpc.model.list({ skipCache: true })).rejects.toThrow(
        /Not authenticated|Sign in to Microsoft Entra again/
    );
    expect(modelRequests).toEqual([]);
    expect(grants).toBe(0);

    const signIn = async () => {
        const { flowId } = await session.rpc.accounts.login.begin({ kind: "entra" });
        const step = await session.rpc.accounts.login.advance({ flowId });
        if (step.kind !== "open-url") throw new Error(`Expected web sign-in, got ${step.kind}`);
        const authorize = new URL(step.url);
        const callback = new URL(authorize.searchParams.get("redirect_uri")!);
        callback.searchParams.set("code", "renewal-code");
        callback.searchParams.set("state", authorize.searchParams.get("state")!);
        const completing = session.rpc.accounts.login.advance({ flowId });
        expect((await fetch(callback)).ok).toBe(true);
        expect(await completing).toMatchObject({
            kind: "completed",
            result: { status: "completed" },
        });
        expect(await session.rpc.accounts.get({ query: { kind: "activeAccount" } })).toMatchObject({
            account: { kind: "entraEmu", derivedFrom: baseId },
        });
    };
    const assertCatalog = async () => {
        const before = modelRequests.length;
        const catalog = await session.rpc.model.list({ skipCache: true });
        expect(catalog).toMatchObject({
            list: expect.arrayContaining([expect.objectContaining({ id: "gpt-5.2" })]),
        });
        expect(modelRequests.slice(before)).toContain(expectedGeneration);
        expect(modelRequests.slice(before).every((value) => value === expectedGeneration)).toBe(
            true
        );
    };
    phase = "initial sign-in";
    await signIn();
    await assertCatalog();

    expectedGeneration = 2;
    phase = "same-session rejection";
    const beforeRejection = modelRequests.length;
    await expect(session.rpc.model.list({ skipCache: true })).rejects.toThrow();
    expect(modelRequests.slice(beforeRejection)).toContain(1);
    phase = "same-session re-login";
    await signIn();
    await assertCatalog();
    expect(signIns).toBe(2);

    expect(await client.stop()).toEqual([]);
    const readState = async () =>
        JSON.parse(
            (await readFile(join(profile, "config.json"), "utf8"))
                .split("\n")
                .filter((line) => !line.trimStart().startsWith("//"))
                .join("\n")
        );
    const saved = await readState();
    // Seed the early-refresh boundary only while the runtime is stopped.
    const nearExpiry = new Date(Date.now() + 60_000).toISOString().replace(/\.\d{3}Z$/, "Z");
    saved.authTokens[`${slot}:entra`].expiresAt = nearExpiry;
    if (saved.authTokens[slot]) saved.authTokens[slot].expiresAt = nearExpiry;
    await writeFile(join(profile, "config.json"), JSON.stringify(saved));
    expectedGeneration = 3;
    phase = "renewal after restart";
    const grantsBeforeRestart = grants;
    client = new CopilotClient(clientOptions);
    session = await client.createSession(sessionOptions);
    await assertCatalog();
    expect(grants).toBe(grantsBeforeRestart + 1);
    expect(signIns).toBe(2);
    const renewed = await readState();
    expect(renewed.authTokens[`${slot}:entra`].token).toBe(tokenFor(3));
    expect(JSON.parse(renewed.authTokens[`entra:${baseId}:entra`].token).web.refresh_token).toBe(
        refreshToken
    );

    expect(await client.stop()).toEqual([]);
    phase = "persisted restart";
    client = new CopilotClient(clientOptions);
    session = await client.createSession(sessionOptions);
    await assertCatalog();
    expect(grants).toBe(grantsBeforeRestart + 1);
    expect(signIns).toBe(2);
});
