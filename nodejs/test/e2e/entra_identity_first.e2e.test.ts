/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { randomUUID } from "node:crypto";
import { mkdir, readFile, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { describe, expect, it, onTestFinished } from "vitest";
import { approveAll, CopilotClient, RuntimeConnection } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { ConnectProxy } from "../../../test/harness/connectProxy.js";
import { createE2eRequestHandler } from "../../../test/harness/mockHandlers.js";

const { env, workDir, openAiEndpoint } = await createSdkTestContext({
    useStdio: true,
    copilotClientOptions: { useLoggedInUser: false, gitHubToken: undefined },
});
const EMU_SCOPE = "12f6db80-0741-4a7e-b9c5-b85d737b3a31/user_impersonation";
const M365_SCOPE = "42c8f7bd-c7a8-4ba9-bc1c-91edfb716247/.default";
const LOKI_HOST = "loki.identity-first.example";
const VERIFICATION_UNAVAILABLE =
    "Microsoft sign-in succeeded, but GitHub account verification is temporarily unavailable. Please try again later. Your active account has not changed.";
const scenarios = [
    { name: "both", emuGrant: true, staff: true, capi: 200, loki: 200, expected: "choices" },
    { name: "emu only", emuGrant: true, staff: true, capi: 200, loki: 403, expected: "entraEmu" },
    {
        name: "hidden CAPI models still establish service access",
        emuGrant: true,
        staff: true,
        capi: 200,
        loki: 403,
        hiddenCapi: true,
        expected: "entraEmu",
    },
    {
        name: "selection-restricted CAPI models still establish service access",
        emuGrant: true,
        staff: false,
        capi: 200,
        loki: 403,
        restrictedCapi: true,
        expected: "entraEmu",
    },
    {
        name: "loki only",
        emuGrant: true,
        staff: true,
        capi: 200,
        loki: 200,
        expected: "loki",
        emptyCapi: true,
    },
    { name: "CAPI failed", emuGrant: true, staff: true, capi: 403, loki: 200, expected: "loki" },
    {
        name: "neither",
        emuGrant: true,
        staff: true,
        capi: 200,
        loki: 403,
        expected: "failure",
        emptyCapi: true,
    },
    {
        name: "EMU grant denied",
        emuGrant: false,
        staff: true,
        capi: 200,
        loki: 200,
        expected: "failure",
    },
    {
        name: "denied EMU exchange preserves the rotated web identity",
        emuGrant: true,
        exchangeDenied: true,
        staff: true,
        capi: 200,
        loki: 200,
        expected: "failure",
    },
    {
        name: "initial EMU user lookup outage suggests trying again later",
        emuGrant: true,
        userLookupFailure: 503,
        staff: true,
        capi: 200,
        loki: 200,
        expected: "failure",
    },
    {
        name: "initial EMU network failure suggests trying again later",
        emuGrant: true,
        userLookupFailure: "network",
        staff: true,
        capi: 200,
        loki: 200,
        expected: "failure",
    },
    {
        name: "initial EMU lookup outage survives plaintext consent",
        emuGrant: true,
        userLookupFailure: 503,
        plaintextConsent: true,
        staff: true,
        capi: 200,
        loki: 200,
        expected: "failure",
    },
    {
        name: "initial EMU access denial is not presented as a temporary outage",
        emuGrant: true,
        userLookupFailure: 403,
        staff: true,
        capi: 200,
        loki: 200,
        expected: "failure",
    },
    {
        name: "initial EMU lookup outage does not block an available linked Loki account",
        emuGrant: true,
        userLookupFailure: 503,
        savedLoki: true,
        staff: true,
        capi: 200,
        loki: 200,
        expected: "loki",
    },
    {
        name: "linked nonstaff",
        emuGrant: true,
        staff: false,
        capi: 200,
        loki: 200,
        expected: "failure",
        emptyCapi: true,
    },
    {
        name: "MOS model denial preserves only EMU",
        emuGrant: true,
        staff: true,
        capi: 200,
        loki: 403,
        mosDenied: "models",
        expected: "entraEmu",
    },
    {
        name: "MOS quota denial overrides a successful Loki catalog",
        emuGrant: true,
        staff: true,
        capi: 200,
        loki: 200,
        mosDenied: "quota",
        emptyCapi: true,
        expected: "failure",
    },
] as const;

describe("Entra identity-first resource discovery", () => {
    for (const scenario of scenarios) {
        it(scenario.name, async () => {
            const profile = join(workDir, `identity-first-${randomUUID()}`);
            await mkdir(profile, { recursive: true });
            await writeFile(
                join(profile, "settings.json"),
                JSON.stringify({ storeTokenPlaintext: !("plaintextConsent" in scenario) })
            );
            const oid = randomUUID();
            const tid = randomUUID();
            const baseId = `entra:${oid}@${tid}`;
            const emuToken = `ghu_identity_first_${randomUUID()}`;
            const savedEmuToken = `ghu_saved_${randomUUID()}`;
            if ("savedLoki" in scenario) {
                await writeFile(
                    join(profile, "config.json"),
                    JSON.stringify({
                        loggedInUsers: [
                            {
                                host: "https://github.com",
                                login: "saved_emu",
                                kind: "entraEmu",
                                derivedFrom: baseId,
                            },
                            {
                                host: `https://${LOKI_HOST}/api`,
                                login: "identity@example.test",
                                kind: "loki",
                                derivedFrom: baseId,
                            },
                        ],
                        authTokens: {
                            "https://github.com:saved_emu:entra": {
                                token: savedEmuToken,
                                accountType: "entra",
                                expiresAt: "2099-01-01T00:00:00Z",
                            },
                        },
                    })
                );
            }
            const grants: URLSearchParams[] = [];
            const catalogs: string[] = [];
            let latestRefresh = "identity-refresh";
            const fallback = createE2eRequestHandler({ capiProxyUrl: openAiEndpoint.url });
            const proxy = new ConnectProxy(
                async (request, response, host) => {
                    const path = request.url ?? "/";
                    const reply = (status: number, body: unknown) => {
                        response.writeHead(status, { "content-type": "application/json" });
                        response.end(JSON.stringify(body));
                        return true;
                    };
                    if (host === "login.microsoftonline.com" && path.endsWith("/token")) {
                        let body = "";
                        for await (const chunk of request) body += chunk.toString();
                        const grant = new URLSearchParams(body);
                        grants.push(grant);
                        if (grant.get("grant_type") === "authorization_code") {
                            expect(grants).toHaveLength(1);
                            expect(grant.get("scope")).toBe("openid profile offline_access");
                            return reply(200, {
                                id_token: `e30.${Buffer.from(JSON.stringify({ oid, tid, preferred_username: "identity@example.test" })).toString("base64url")}.fixture`,
                                refresh_token: latestRefresh,
                            });
                        }
                        expect(grant.get("grant_type")).toBe("refresh_token");
                        expect(grant.get("refresh_token")).toBe(latestRefresh);
                        const scope = grant.get("scope") ?? "";
                        if (scope.includes(EMU_SCOPE)) {
                            if (!scenario.emuGrant) return reply(400, { error: "access_denied" });
                            latestRefresh = "rotated-after-emu";
                            return reply(200, {
                                access_token: "emu-resource-token",
                                refresh_token: latestRefresh,
                                expires_in: 3600,
                            });
                        }
                        expect(scope).toContain(M365_SCOPE);
                        latestRefresh = "rotated-after-m365";
                        return reply(200, {
                            access_token: "m365-resource-token",
                            refresh_token: latestRefresh,
                            expires_in: 3600,
                        });
                    }
                    if (host === "github.com" && path === "/login/oauth/access_token") {
                        let body = "";
                        for await (const chunk of request) body += chunk.toString();
                        expect(new URLSearchParams(body).get("subject_token")).toBe(
                            "emu-resource-token"
                        );
                        if ("exchangeDenied" in scenario) {
                            return reply(403, { error: "access_denied" });
                        }
                        return reply(200, {
                            access_token: emuToken,
                            token_type: "bearer",
                            expires_in: 3600,
                        });
                    }
                    if (host === "api.github.com" && path.startsWith("/copilot_internal/user")) {
                        if (
                            "savedLoki" in scenario &&
                            request.headers.authorization?.endsWith(savedEmuToken)
                        ) {
                            return reply(200, {
                                login: "saved_emu",
                                copilot_plan: "business",
                                is_staff: true,
                            });
                        }
                        if ("userLookupFailure" in scenario) {
                            if (scenario.userLookupFailure === "network") {
                                response.destroy();
                                return true;
                            }
                            return reply(scenario.userLookupFailure, {
                                message: "Fixture user lookup failure",
                            });
                        }
                        expect(request.headers.authorization).toBe(`Bearer ${emuToken}`);
                        return reply(200, {
                            login: "identity_emu",
                            copilot_plan: "business",
                            is_staff: scenario.staff,
                            is_mcp_enabled: true,
                            endpoints: {
                                api: "https://api.githubcopilot.com",
                                telemetry: "https://localhost:1",
                            },
                        });
                    }
                    if (host === "api.githubcopilot.com" && path === "/models") {
                        expect(request.headers.authorization).toBe(`Bearer ${emuToken}`);
                        catalogs.push("capi");
                        return reply(scenario.capi, {
                            data:
                                "emptyCapi" in scenario && scenario.emptyCapi
                                    ? []
                                    : [
                                          {
                                              id: "gpt-5.2",
                                              name: "Candidate CAPI model",
                                              capabilities: { supports: { tool_calls: true } },
                                              model_picker_enabled: !(
                                                  "hiddenCapi" in scenario && scenario.hiddenCapi
                                              ),
                                              ...("restrictedCapi" in scenario
                                                  ? { billing: { restricted_to: ["enterprise"] } }
                                                  : {}),
                                          },
                                      ],
                        });
                    }
                    if (host === LOKI_HOST && (path === "/api/models" || path === "/api/quota")) {
                        expect(request.headers.authorization).toBe("Bearer m365-resource-token");
                        if ("mosDenied" in scenario && path === `/api/${scenario.mosDenied}`) {
                            if (scenario.mosDenied === "models") catalogs.push("loki");
                            return reply(403, {
                                error: {
                                    message:
                                        "Copilot CLI Entra access is not enabled for this user.",
                                    type: "invalid_request_error",
                                    code: "copilot_cli_entra_access_disabled",
                                },
                            });
                        }
                        if (path === "/api/quota") return reply(200, {});
                        catalogs.push("loki");
                        return reply(scenario.loki, {
                            data: [
                                {
                                    id: "loki-test",
                                    name: "Candidate M365 model",
                                    capabilities: { supports: { tool_calls: true } },
                                    model_picker_enabled: true,
                                },
                            ],
                        });
                    }
                    return fallback(request, response, host);
                },
                {
                    interceptDomains: [
                        "login.microsoftonline.com",
                        "github.com",
                        "api.github.com",
                        "api.githubcopilot.com",
                        "api.mcp.github.com",
                        LOKI_HOST,
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
                useLoggedInUser: false,
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
                    COPILOT_LOKI_BASE_URL: `https://${LOKI_HOST}/api`,
                    HTTPS_PROXY: proxy.proxyUrl,
                    HTTP_PROXY: proxy.proxyUrl,
                    https_proxy: proxy.proxyUrl,
                    http_proxy: proxy.proxyUrl,
                    NODE_EXTRA_CA_CERTS: proxy.caFilePath,
                    SSL_CERT_FILE: proxy.caFilePath,
                },
            };
            client = new CopilotClient(clientOptions);
            const session = await client.createSession({
                configDirectory: profile,
                onPermissionRequest: approveAll,
            });
            const { flowId } = await session.rpc.accounts.login.begin({ kind: "entra" });
            const step = await session.rpc.accounts.login.advance({ flowId });
            expect(step.kind).toBe("open-url");
            if (step.kind !== "open-url")
                throw new Error(`Expected web identity flow, got ${step.kind}`);
            const authorize = new URL(step.url);
            expect(authorize.searchParams.get("scope")).toBe("openid profile offline_access");
            expect(grants).toHaveLength(0);
            const callback = new URL(authorize.searchParams.get("redirect_uri")!);
            callback.searchParams.set("code", "identity-code");
            callback.searchParams.set("state", authorize.searchParams.get("state")!);
            const completing = session.rpc.accounts.login.advance({ flowId }).then((step) => {
                if ("plaintextConsent" in scenario) {
                    expect(step).toMatchObject({
                        kind: "completed",
                        result: { status: "needs-plaintext-consent" },
                    });
                    return session.rpc.accounts.login.advance({ flowId, input: "allow" });
                }
                return step;
            });
            // Attach the failure assertion before the callback can complete the request.
            const completed =
                scenario.expected === "failure"
                    ? expect(completing).rejects.toThrow(
                          "userLookupFailure" in scenario && scenario.userLookupFailure !== 403
                              ? VERIFICATION_UNAVAILABLE
                              : "No usable inference backend"
                      )
                    : completing;
            expect((await fetch(callback)).ok).toBe(true);
            const result = await completed;
            const saved = JSON.parse(
                (await readFile(join(profile, "config.json"), "utf8"))
                    .split("\n")
                    .filter((line) => !line.trimStart().startsWith("//"))
                    .join("\n")
            );
            const base = JSON.parse(saved.authTokens[`entra:${baseId}:entra`].token);
            expect(base.web.refresh_token).toBe(latestRefresh);
            if (
                "exchangeDenied" in scenario ||
                ("userLookupFailure" in scenario && !("savedLoki" in scenario))
            ) {
                expect(base.web.refresh_token).toBe("rotated-after-emu");
            }
            if (scenario.expected === "failure") {
                expect(saved.lastLoggedInUser).toBeUndefined();
                expect(
                    await session.rpc.accounts.get({ query: { kind: "activeAccount" } })
                ).toEqual({
                    kind: "activeAccount",
                    account: null,
                });
                await client.stop();
                client = new CopilotClient({ ...clientOptions, useLoggedInUser: true });
                const restarted = await client.createSession({
                    configDirectory: profile,
                    onPermissionRequest: approveAll,
                });
                expect(
                    await restarted.rpc.accounts.get({ query: { kind: "activeAccount" } })
                ).toEqual({ kind: "activeAccount", account: null });
            } else {
                let expected = scenario.expected;
                if (expected === "choices") {
                    expect(result).toMatchObject({
                        kind: "completed",
                        result: {
                            status: "needs-account-selection",
                            accounts: expect.arrayContaining([
                                expect.objectContaining({ kind: "entraEmu" }),
                                expect.objectContaining({ kind: "loki" }),
                            ]),
                        },
                    });
                    expect(saved.lastLoggedInUser).toBeUndefined();
                    if (!result || result.kind !== "completed")
                        throw new Error("Expected account choices");
                    const selected = result.result.accounts?.find(
                        (account) => account.kind === "loki"
                    );
                    expect(selected).toBeDefined();
                    await session.rpc.accounts.login.advance({
                        flowId,
                        input: selected!.selectionId,
                    });
                    expected = "loki";
                } else {
                    expect(result).toMatchObject({
                        kind: "completed",
                        result: { status: "completed" },
                    });
                }
                expect(
                    await session.rpc.accounts.get({ query: { kind: "activeAccount" } })
                ).toMatchObject({
                    kind: "activeAccount",
                    account: { kind: expected, active: true, derivedFrom: baseId },
                });
            }
            const emuVerified =
                scenario.emuGrant &&
                !("exchangeDenied" in scenario) &&
                !("userLookupFailure" in scenario);
            if (emuVerified) expect(catalogs).toContain("capi");
            else expect(catalogs).not.toContain("capi");
            if ((scenario.staff && emuVerified) || "savedLoki" in scenario) {
                expect(catalogs).toContain("loki");
                expect(grants.map((grant) => grant.get("scope"))).toEqual([
                    "openid profile offline_access",
                    `${EMU_SCOPE} openid profile offline_access`,
                    `${M365_SCOPE} openid profile offline_access`,
                ]);
            } else {
                expect(catalogs).not.toContain("loki");
                expect(grants).toHaveLength(2);
            }
        });
    }
});
