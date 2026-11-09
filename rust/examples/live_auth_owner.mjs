/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

// This Node example exercises the Rust SDK through cli-native, not the Node SDK.
import assert from "node:assert/strict";
import { createHmac } from "node:crypto";
import { existsSync } from "node:fs";
import { copyFile, mkdir, realpath, writeFile } from "node:fs/promises";
import { createServer } from "node:http";
import { createRequire } from "node:module";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { isDeepStrictEqual, parseArgs } from "node:util";

const { values } = parseArgs({
    options: {
        runtime: { type: "string" },
        "cli-native": { type: "string" },
        root: { type: "string" },
        scenario: { type: "string", default: "live" },
        help: { type: "boolean" },
    },
});

if (values.help || !values.root) {
    console.log(
        "Usage: node src/sdk/rust/examples/live_auth_owner.mjs --root <isolated-directory> " +
            "[--runtime <runtime.node>] [--cli-native <cli-native.node>] [--scenario live|recovery|session|hmac|api-token|cycle|isolation|subscriptions|utility|background|catalog|server-catalog|independent-utility]\n" +
            "Use a dedicated directory. This offline example uses synthetic credentials only.",
    );
} else {
    try {
        await main();
        // Native addons can retain Node handles after their owned runtime stops.
        process.stdout.write("", () => process.exit(0));
    } catch (error) {
        console.error(error);
        process.stderr.write("", () => process.exit(1));
    }
}

function deepFreeze(value) {
    for (const child of Object.values(value)) {
        if (child !== null && typeof child === "object") deepFreeze(child);
    }
    return Object.freeze(value);
}

async function main() {
    assert(["live", "recovery", "session", "hmac", "api-token", "cycle", "isolation", "subscriptions", "utility", "background", "catalog", "server-catalog", "independent-utility"].includes(values.scenario), "Unknown scenario");
    const repository = resolve(dirname(fileURLToPath(import.meta.url)), "../../../..");
    const libc =
        process.platform === "linux" && !process.report.getReport().header.glibcVersionRuntime ? "musl" : "gnu";
    const suffix = process.platform === "linux" ? `-${libc}` : process.platform === "win32" ? "-msvc" : "";
    const triple = `${process.platform}-${process.arch}${suffix}`;
    const runtimePath = resolve(values.runtime ?? join(repository, "src/native/runtime", `runtime.${triple}.node`));
    const cliNativePath = resolve(
        values["cli-native"] ?? join(repository, "src/native/cli", `cli-native.${triple}.node`),
    );
    assert(existsSync(runtimePath), `Missing native runtime: ${runtimePath}. Build the native addons first.`);
    assert(existsSync(cliNativePath), `Missing cli-native addon: ${cliNativePath}. Build the native addons first.`);
    const runtimeImage = await realpath(runtimePath);
    const entrypoint = await realpath(fileURLToPath(import.meta.url));
    await mkdir(resolve(values.root), { recursive: true });
    const root = await realpath(resolve(values.root));
    const home = join(root, "home");
    const configDir = join(root, "config");
    await mkdir(home, { recursive: true });
    await mkdir(configDir, { recursive: true });

    // Clear inherited credentials before either native addon captures its environment.
    for (const key of Object.keys(process.env)) {
        if (/^(COPILOT_|GITHUB_|GH_|AGENT_|WS1_|XDG_|AZURE_|ENTRA_|MSAL_)/i.test(key)) {
            delete process.env[key];
        }
    }
    Object.assign(process.env, {
        HOME: home,
        USERPROFILE: home,
        XDG_CONFIG_HOME: join(home, ".config"),
        XDG_CACHE_HOME: join(home, ".cache"),
        XDG_STATE_HOME: join(home, ".state"),
        COPILOT_HOME: configDir,
        COPILOT_CACHE_HOME: join(root, "cache"),
        COPILOT_OFFLINE: "true",
        COPILOT_DISABLE_KEYTAR: "1",
        COPILOT_CLI_PATH: entrypoint,
        GH_CONFIG_DIR: join(home, "gh"),
        GIT_CONFIG_GLOBAL: join(home, ".gitconfig"),
        GIT_CONFIG_NOSYSTEM: "1",
    });
    process.chdir(root);

    const require = createRequire(import.meta.url);
    // The N-API owner and the Rust SDK must load this exact runtime image.
    const runtime = require(runtimeImage);
    const cliNative = require(await realpath(cliNativePath));
    const sessionFixture = ["session", "hmac", "api-token", "cycle", "isolation", "subscriptions", "utility", "background", "catalog", "server-catalog", "independent-utility"].includes(values.scenario)
        ? await createSessionFixture(root)
        : undefined;
    if (sessionFixture) {
        process.env.COPILOT_API_URL = sessionFixture.url;
        process.env.COPILOT_GITHUB_TELEMETRY_DISABLED = "1";
    }
    const authOptions = deepFreeze({
        authTokenEnvVar: "AGENT_GITHUB_TOKEN",
        configDir,
        disableAutoLogin: !sessionFixture,
        startupEnvironment: {
            ...(["utility", "background"].includes(values.scenario) ? {
                UTILITY_EXPLICIT_TOKEN: "ghs_synthetic_utility_explicit",
                UTILITY_SECOND_TOKEN: "ghs_synthetic_utility_second",
            } : {}),
            ...(values.scenario === "hmac"
                ? { CAPI_HMAC_KEY: "sdk-owner-hmac-key", COPILOT_API_URL: sessionFixture.url }
                : values.scenario === "api-token"
                  ? {
                        AGENT_GITHUB_TOKEN: "ghs_synthetic_original_owner",
                        GITHUB_COPILOT_API_TOKEN: "sdk-owner-api-token",
                        COPILOT_API_URL: sessionFixture.url,
                    }
                  : sessionFixture
                    ? {
                          GITHUB_ASKPASS: join(
                              repository,
                              "src/sdk/rust/examples",
                              process.platform === "win32"
                                  ? "live_auth_owner_askpass.cmd"
                                  : "live_auth_owner_askpass.mjs",
                          ),
                          COPILOT_AUTH_OWNER_TOKEN_FILE: sessionFixture.tokenFile,
                          PATH: process.env.PATH ?? "",
                          ...(process.platform === "win32" ? { SystemRoot: process.env.SystemRoot ?? "" } : {}),
                          COPILOT_API_URL: sessionFixture.url,
                      }
                    : { AGENT_GITHUB_TOKEN: "ghs_synthetic_original_owner" }),
            COPILOT_GH_HOST: "https://owner.example",
            COPILOT_DISABLE_KEYTAR: "1",
            COPILOT_OFFLINE: "false",
            COPILOT_HOME: configDir,
            HOME: home,
            USERPROFILE: home,
            GH_CONFIG_DIR: join(home, "gh"),
        },
    });
    const options = deepFreeze({
        configDir,
        disableAutoLogin: true,
        runtimeLibraryPath: runtimeImage,
    });
    const originalAuthOptions = structuredClone(authOptions);
    const originalOptions = structuredClone(options);
    const errors = [];
    let owner;
    let revoked = false;
    let sessionId;
    let inheritedSessionId;
    let secondary;
    let secondaryOwner;
    let secondarySessionId;
    try {
        if (values.scenario === "recovery") {
            const retired = new runtime.ProcessAuthHandle(authOptions);
            retired.dispose();
            await assert.rejects(
                async () => cliNative.cliRuntimeStart(entrypoint, options, retired),
                /auth owner.*(?:not active|no longer available)/i,
                "A revoked owner must reject SDK-owned startup",
            );
            assert(isDeepStrictEqual(options, originalOptions), "Failed startup changed caller options");
            console.log("Revoked startup: rejected");
        }

        owner = new runtime.ProcessAuthHandle(authOptions);
        const originalConfig = structuredClone(owner.managerConfig());
        const original = await owner.current();
        if (values.scenario === "hmac") {
            assert.equal(original?.authInfo.type, "hmac", "The original owner must select HMAC without OAuth");
            console.log("Original owner: HMAC without OAuth");
        } else {
            assert.equal(original?.authInfo.host, "https://owner.example", "The original owner lost its host");
            console.log(`Original owner host: ${original.authInfo.host}`);
        }

        // There is no serverId: the Rust SDK owns startup and admits this original owner.
        await cliNative.cliRuntimeStart(entrypoint, options, owner);
        const current = JSON.parse(await cliNative.cliRuntimeCall("account.getCurrentAuth", "{}"));
        assert.equal(
            values.scenario === "hmac" ? current.authInfo?.type : current.authInfo?.host,
            values.scenario === "hmac" ? "hmac" : "https://owner.example",
            "SDK-owned startup must preserve the original auth owner host",
        );
        console.log(values.scenario === "hmac" ? "SDK-owned auth: HMAC" : `SDK-owned host: ${current.authInfo.host}`);
        assert(isDeepStrictEqual(owner.managerConfig(), originalConfig), "Startup changed the original manager config");
        assert(isDeepStrictEqual(authOptions, originalAuthOptions), "Startup changed caller auth config");
        assert(isDeepStrictEqual(options, originalOptions), "Startup changed caller options");

        if (values.scenario === "isolation") {
            await verifyCredentialIsolation(cliNative, root, sessionFixture);
        } else if (values.scenario === "subscriptions") {
            await verifyDependentSubscription(cliNative, runtime, owner, root, authOptions);
        } else if (values.scenario === "catalog") {
            await verifyCatalogSelection(cliNative, owner, root, sessionFixture);
        } else if (values.scenario === "server-catalog") {
            sessionFixture.beforeCatalog(() => {
                owner.dispose();
                revoked = true;
            });
            await assert.rejects(
                async () => cliNative.cliRuntimeCall("models.list", "{}"),
                /auth owner.*(?:not active|no longer available)|authentication selection changed/i,
                "The server catalog must reject a result after its owner is revoked",
            );
            assert(revoked, "The server catalog must reach the controlled response before revocation");
            sessionFixture.assertCredential("ghs_synthetic_original_owner");
            console.log("Server catalog: revoked result rejected");
            const count = sessionFixture.requestCount();
            await assert.rejects(
                async () => cliNative.cliRuntimeCall("models.list", "{}"),
                /auth owner.*(?:not active|no longer available)/i,
            );
            assert.equal(sessionFixture.requestCount(), count, "Revocation must reject before another catalog request");
            console.log("Server catalog: revoked request rejected before dispatch");
        } else if (values.scenario === "independent-utility") {
            sessionId = await createExampleSession(cliNative, root, {
                gitHubToken: "ghs_synthetic_independent_utility",
            });
            owner.dispose();
            revoked = true;
            for (const manual of [false, true]) {
                const outcome = await runtime.modelGenerateSessionName({
                    host: { sessionId, copilotUrl: sessionFixture.url, cwd: root },
                    userMessage: "Show independent session utility authentication",
                    manual,
                });
                assert.equal(outcome.value, "Independent Utility Authentication", JSON.stringify(outcome));
                sessionFixture.assertCredential("ghs_synthetic_independent_utility");
                console.log(`${manual ? "Manual" : "Automatic"} session utility: independent after owner revocation`);
            }
        } else if (values.scenario === "background") {
            sessionId = await createExampleSession(cliNative, root);
            sessionFixture.beforeCatalog(() => {
                owner.dispose();
                revoked = true;
            });
            const outcome = await runtime.modelGenerateSessionName({
                host: { sessionId, copilotUrl: sessionFixture.url, cwd: root },
                userMessage: "Show inherited utility authentication",
            });
            assert(revoked, "The inherited utility must request its catalog before revocation");
            assert.equal(outcome.kind, "unavailable");
            assert.equal(sessionFixture.completionCount(), 0, "A revoked owner must prevent background HTTP dispatch");
            console.log("Inherited utility: revocation prevented completion dispatch");
            const explicit = await runtime.modelGenerateSessionName({
                host: {
                    authInfo: { type: "env", host: "https://owner.example", envVar: "UTILITY_EXPLICIT_TOKEN" },
                    sessionId,
                    copilotUrl: sessionFixture.url,
                    cwd: root,
                },
                userMessage: "Show independent utility authentication",
            });
            assert.equal(explicit.value, "Independent Utility Authentication", JSON.stringify({
                outcome: explicit,
                requests: sessionFixture.requestCount(),
                paths: sessionFixture.requestPaths(),
                completions: sessionFixture.completionCount(),
            }));
            assert(sessionFixture.completionCount() > 0, "An independent credential must still dispatch");
            console.log("Explicit utility: independent completion succeeded");
        } else if (values.scenario === "utility") {
            sessionId = await createExampleSession(cliNative, root);
            const input = {
                host: {
                    authInfo: {
                        type: "env",
                        host: "https://owner.example",
                        envVar: "UTILITY_EXPLICIT_TOKEN",
                        copilotUser: { login: "utility-explicit" },
                    },
                    copilotUrl: sessionFixture.url,
                    sessionId,
                    cwd: root,
                },
                userMessage: "Show explicit utility authentication",
            };
            const before = sessionFixture.requestCount();
            await runtime.modelGenerateSessionName(input);
            assert(sessionFixture.requestCount() > before, "The utility must request its model catalog");
            sessionFixture.assertCredential("ghs_synthetic_utility_explicit");
            console.log("Utility catalog: explicit credential");
            owner.dispose();
            revoked = true;
            input.host.authInfo.envVar = "UTILITY_SECOND_TOKEN";
            const after = sessionFixture.requestCount();
            await runtime.modelGenerateSessionName({ ...input, manual: true });
            assert(sessionFixture.requestCount() > after, "Explicit utility auth must survive owner revocation");
            sessionFixture.assertCredential("ghs_synthetic_utility_second");
            console.log("Utility catalog: independent after owner revocation");
        } else {
        if (sessionFixture) {
            sessionId = await createExampleSession(cliNative, root);
            assert.equal(typeof sessionId, "string", "SDK create must return a session identity");
            console.log("SDK session: created");
            const list = async () =>
                JSON.parse(
                    await cliNative.cliRuntimeCall(
                        "session.model.list",
                        JSON.stringify({ sessionId, skipCache: true }),
                    ),
                );
            const initial = await list();
            if (values.scenario === "hmac") sessionFixture.assertHmac();
            else sessionFixture.assertCredential("ghs_synthetic_original_owner");
            assert(
                initial.list.some((model) => model.id === "gpt-5.5"),
                `The session must read the local model catalog: ${JSON.stringify(initial)}`,
            );
            console.log(
                values.scenario === "hmac"
                    ? "Session request: signed HMAC without OAuth"
                    : "Session request: original credential",
            );
            if (values.scenario !== "hmac") {
                inheritedSessionId = await createExampleSession(cliNative, root);
            }
            owner.bindSession(sessionId);
            owner.bindSession(sessionId);
            console.log("Foreground binding: repeated without recursion");
            if (values.scenario === "api-token") {
                await cliNative.cliRuntimeCall(
                    "session.gitHubAuth.setCredentials",
                    JSON.stringify({
                        sessionId,
                        credentials: { type: "copilot-api-token", host: "https://owner.example" },
                    }),
                );
                await list();
                sessionFixture.assertCredential("sdk-owner-api-token");
                console.log("Explicit API-token selection: environment credential");
            } else if (values.scenario !== "hmac") {
                await sessionFixture.renew();
                await list();
                sessionFixture.assertCredential("ghs_synthetic_renewed_owner");
                console.log("Session renewal: refreshed credential");
                const replacement = {
                    type: "env",
                    host: "https://owner.example",
                    envVar: "GH_TOKEN",
                    token: "ghs_synthetic_explicit_replacement",
                    copilotUser: { login: "sdk-owner-example" },
                };
                const setCredentials = (credentials) =>
                    cliNative.cliRuntimeCall(
                        "session.gitHubAuth.setCredentials",
                        JSON.stringify({ sessionId, credentials }),
                    );
                await setCredentials(replacement);
                await list();
                sessionFixture.assertCredential(replacement.token);
                console.log("Explicit credential update: supplied token");
                await setCredentials(null);
                const status = JSON.parse(
                    await cliNative.cliRuntimeCall("session.gitHubAuth.getStatus", JSON.stringify({ sessionId })),
                );
                assert.equal(status.isAuthenticated, false, "An explicit clear must report no authentication");
                const count = sessionFixture.requestCount();
                await assert.rejects(list, "An explicit clear must reject authenticated model discovery");
                assert.equal(
                    sessionFixture.requestCount(),
                    count,
                    "An explicit clear must reject before HTTP dispatch",
                );
                console.log("Explicit credential clear: rejected before dispatch");
                await setCredentials(replacement);

                // Each addon image owns its SDK client; both clients load the same runtime image.
                const secondaryPath = join(root, "cli-native-secondary.node");
                await copyFile(cliNativePath, secondaryPath);
                secondary = require(secondaryPath);
                const secondaryConfig = join(root, "secondary-config");
                await mkdir(secondaryConfig, { recursive: true });
                secondaryOwner = new runtime.ProcessAuthHandle(
                    deepFreeze({
                        ...authOptions,
                        configDir: secondaryConfig,
                        startupEnvironment: {
                            ...authOptions.startupEnvironment,
                            AGENT_GITHUB_TOKEN: "ghs_synthetic_independent_owner",
                        },
                    }),
                );
                await secondary.cliRuntimeStart(
                    entrypoint,
                    deepFreeze({ ...options, configDir: secondaryConfig }),
                    secondaryOwner,
                );
                secondaryOwner.bindSession(inheritedSessionId);
                secondarySessionId = await createExampleSession(secondary, root);
                await secondary.cliRuntimeCall(
                    "session.model.list",
                    JSON.stringify({ sessionId: secondarySessionId, skipCache: true }),
                );
                sessionFixture.assertCredential(replacement.token);
                console.log("Cross-owner SDK request: original live authority");
                if (values.scenario === "cycle") {
                    let rejected = false;
                    try {
                        owner.bindSession(secondarySessionId);
                    } catch (error) {
                        assert.match(error.message, /cyclic.*auth|auth.*cycle/i);
                        rejected = true;
                    }
                    // Exercise the published graph even if an old addon accepts the cycle.
                    await cliNative.cliRuntimeCall("account.getCurrentAuth", "{}");
                    assert(rejected, "A reciprocal foreground binding must reject a resolver cycle");
                    console.log("Cyclic foreground binding: rejected");
                    secondaryOwner.bindSession();
                    await list();
                    sessionFixture.assertCredential(replacement.token);
                    await secondary.cliRuntimeCall(
                        "session.model.list",
                        JSON.stringify({ sessionId: secondarySessionId, skipCache: true }),
                    );
                    sessionFixture.assertCredential("ghs_synthetic_independent_owner");
                    secondaryOwner.bindSession(inheritedSessionId);
                    await secondary.cliRuntimeCall(
                        "session.model.list",
                        JSON.stringify({ sessionId: secondarySessionId, skipCache: true }),
                    );
                    sessionFixture.assertCredential(replacement.token);
                    console.log("Rejected cycle: original binding and fresh requests preserved");
                }
            }
        }

        owner.dispose();
        revoked = true;
        // A live ambient credential must not replace the revoked original owner.
        process.env.AGENT_GITHUB_TOKEN = "ghs_synthetic_ambient_replacement";
        process.env.COPILOT_GITHUB_TOKEN = "ghs_synthetic_ambient_replacement";
        process.env.GH_TOKEN = "ghs_synthetic_ambient_replacement";
        process.env.COPILOT_GH_HOST = "https://ambient.example";
        await assert.rejects(
            async () => cliNative.cliRuntimeCall("account.getCurrentAuth", "{}"),
            /auth owner.*(?:not active|no longer available)/i,
            "A revoked owner must reject SDK auth instead of using ambient credentials",
        );
        console.log("Revoked SDK auth: rejected");
        if (sessionFixture) {
            if (inheritedSessionId) {
                await cliNative.cliRuntimeCall("session.model.list", JSON.stringify({ sessionId, skipCache: true }));
                sessionFixture.assertCredential(
                    values.scenario === "api-token" ? "sdk-owner-api-token" : "ghs_synthetic_explicit_replacement",
                );
                console.log("Explicit session override: independent after owner revocation");
            }
            const count = sessionFixture.requestCount();
            await assert.rejects(
                async () =>
                    cliNative.cliRuntimeCall("session.model.list", JSON.stringify({
                        sessionId: inheritedSessionId ?? sessionId,
                        skipCache: true,
                    })),
                /auth owner.*(?:not active|no longer available)/i,
                "A revoked owner must not authorize a session request",
            );
            assert.equal(sessionFixture.requestCount(), count, "Revocation must stop the request before HTTP dispatch");
            console.log("Revoked session request: rejected");
            if (secondarySessionId) {
                await assert.rejects(
                    async () =>
                        secondary.cliRuntimeCall(
                            "session.model.list",
                            JSON.stringify({ sessionId: secondarySessionId, skipCache: true }),
                        ),
                    /auth owner.*(?:not active|no longer available)/i,
                    "A different foreground owner must not bypass the original owner's revocation",
                );
                assert.equal(sessionFixture.requestCount(), count, "Cross-owner revocation must reject before HTTP");
                console.log("Cross-owner revocation: rejected before dispatch");
            }
            await cliNative.cliRuntimeCall("session.shutdown", JSON.stringify({ sessionId }));
            sessionId = undefined;
            if (values.scenario !== "hmac") {
                const independent = JSON.parse(
                    await cliNative.cliRuntimeCall(
                        "session.create",
                        JSON.stringify({
                            gitHubToken: "ghs_synthetic_explicit_session",
                            workingDirectory: root,
                            enableHostGitOperations: false,
                            enableMcp: false,
                            enableSkills: false,
                            enableHooks: false,
                            enableConfigDiscovery: false,
                        }),
                    ),
                );
                sessionId = independent.sessionId;
                await cliNative.cliRuntimeCall("session.model.list", JSON.stringify({ sessionId, skipCache: true }));
                sessionFixture.assertCredential("ghs_synthetic_explicit_session");
                console.log("Explicit session request: independent credential");
            }
        }
        }
    } catch (error) {
        errors.push(error);
    } finally {
        if (inheritedSessionId) {
            try {
                await cliNative.cliRuntimeCall("session.shutdown", JSON.stringify({ sessionId: inheritedSessionId }));
            } catch (error) {
                errors.push(error);
            }
        }
        if (secondarySessionId) {
            try {
                await secondary.cliRuntimeCall("session.shutdown", JSON.stringify({ sessionId: secondarySessionId }));
            } catch (error) {
                errors.push(error);
            }
        }
        if (secondary) {
            try {
                await secondary.cliRuntimeStop();
                assert.equal(
                    secondary.cliRuntimeConnectionId(),
                    null,
                    "The second SDK client must release its connection",
                );
            } catch (error) {
                errors.push(error);
            }
        }
        try {
            secondaryOwner?.dispose();
        } catch (error) {
            errors.push(error);
        }
        if (sessionId) {
            try {
                await cliNative.cliRuntimeCall("session.shutdown", JSON.stringify({ sessionId }));
                console.log("SDK session: closed");
            } catch (error) {
                errors.push(error);
            }
        }
        if (owner && !revoked) {
            try {
                owner.dispose();
            } catch (error) {
                errors.push(error);
            }
        }
        try {
            await cliNative.cliRuntimeStop();
            assert.equal(cliNative.cliRuntimeConnectionId(), null, "SDK stop must release its native connection");
        } catch (error) {
            errors.push(error);
        }
        if (sessionFixture) {
            try {
                await sessionFixture.close();
            } catch (error) {
                errors.push(error);
            }
        }
        try {
            assert(isDeepStrictEqual(authOptions, originalAuthOptions), "Use changed caller auth config");
            assert(isDeepStrictEqual(options, originalOptions), "Use changed caller options");
        } catch (error) {
            errors.push(error);
        }
    }
    if (errors.length === 1) throw errors[0];
    if (errors.length > 1) throw new AggregateError(errors, "Auth owner example and teardown failed");
    console.log("Caller config: unchanged");
    console.log("SDK stop: clean");
}

async function verifyCredentialIsolation(cli, root, fixture) {
    const sessions = [];
    try {
        for (let index = 0; index < 2; index++) {
            sessions.push(await createExampleSession(cli, root));
        }
        const list = (sessionId) => cli.cliRuntimeCall(
            "session.model.list",
            JSON.stringify({ sessionId, skipCache: true }),
        );
        await list(sessions[1]);
        fixture.assertCredential("ghs_synthetic_original_owner");
        await cli.cliRuntimeCall("session.gitHubAuth.setCredentials", JSON.stringify({
            sessionId: sessions[0],
            credentials: {
                type: "env",
                host: "https://owner.example",
                envVar: "GH_TOKEN",
                token: "ghs_synthetic_session_override",
                copilotUser: { login: "sdk-owner-example" },
            },
        }));
        await list(sessions[0]);
        fixture.assertCredential("ghs_synthetic_session_override");
        console.log("Explicit session override: supplied credential");
        await list(sessions[1]);
        fixture.assertCredential("ghs_synthetic_original_owner");
        console.log("Sibling session: original live owner credential");
    } finally {
        for (const sessionId of sessions) {
            await cli.cliRuntimeCall("session.shutdown", JSON.stringify({ sessionId }));
        }
    }
}

async function createExampleSession(cli, root, options = {}) {
    const created = JSON.parse(await cli.cliRuntimeCall("session.create", JSON.stringify({
        workingDirectory: root,
        enableHostGitOperations: false,
        enableMcp: false,
        enableSkills: false,
        enableHooks: false,
        enableConfigDiscovery: false,
        ...options,
    })));
    return created.sessionId;
}

async function verifyCatalogSelection(cli, owner, root, fixture) {
    const sessions = [];
    try {
        for (const plan of ["free", "pro"]) {
            const sessionId = await createExampleSession(cli, root);
            sessions.push(sessionId);
            await cli.cliRuntimeCall("session.gitHubAuth.setCredentials", JSON.stringify({
                sessionId,
                credentials: {
                    type: "env",
                    host: "https://owner.example",
                    envVar: "GH_TOKEN",
                    token: `ghs_synthetic_catalog_${plan}`,
                    copilotUser: {
                        login: `catalog-${plan}`,
                        copilot_plan: plan === "pro" ? "individual" : "free",
                        ...(plan === "free" ? { access_type_sku: "free_limited_copilot" } : {}),
                    },
                },
            }));
        }
        owner.bindSession(sessions[0]);
        const freeAuth = await owner.current();
        assert.equal(freeAuth.authInfo.copilotUser?.copilot_plan, "free", JSON.stringify(freeAuth));
        const sessionId = await createExampleSession(cli, root);
        sessions.push(sessionId);
        const list = async () => JSON.parse(await cli.cliRuntimeCall(
            "session.model.list", JSON.stringify({ sessionId, skipCache: true }),
        ));
        const hasProModel = (result) => result.list.some((model) => model.id === "gpt-5.5");
        const initial = await list();
        assert(!hasProModel(initial), `The free account must not advertise a Pro-only model: ${JSON.stringify(initial)}`);
        owner.bindSession(sessions[1]);
        assert(hasProModel(await list()), "The catalog must use the live Pro account metadata");
        fixture.assertCredential("ghs_synthetic_catalog_pro");
        console.log("Catalog selection: live bearer and plan agree");
        fixture.beforeCatalog(() => owner.bindSession(sessions[0]));
        assert(!hasProModel(await list()), "A rebound request must not publish the retired Pro catalog");
        fixture.assertCredential("ghs_synthetic_catalog_free");
        console.log("Catalog publication: rebound request refreshed and retried");
    } finally {
        owner.bindSession();
        for (const sessionId of sessions.reverse()) {
            await cli.cliRuntimeCall("session.shutdown", JSON.stringify({ sessionId }));
        }
    }
}

async function verifyDependentSubscription(cli, runtime, owner, root, authOptions) {
    const sessions = [];
    const observer = new runtime.ProcessAuthHandle(authOptions);
    let subscription;
    try {
        sessions.push(await createExampleSession(cli, root));
        sessions.push(await createExampleSession(cli, root));
        observer.bindSession(sessions[0]);
        subscription = observer.subscribe();
        const nextEvent = async () => {
            let timer;
            try {
                return await Promise.race([
                    subscription.next(),
                    new Promise((_, reject) => {
                        timer = setTimeout(() => reject(new Error("Dependent auth subscription did not follow its owner")), 5000);
                    }),
                ]);
            } finally {
                clearTimeout(timer);
            }
        };
        const initial = await nextEvent();
        const setCredentials = (login, sessionId = sessions[1]) => cli.cliRuntimeCall("session.gitHubAuth.setCredentials", JSON.stringify({
            sessionId,
            credentials: {
                type: "env",
                host: "https://owner.example",
                envVar: "GH_TOKEN",
                token: "ghs_synthetic_subscription_override",
                copilotUser: { login },
            },
        }));
        await setCredentials("subscription-rebound");
        owner.bindSession(sessions[1]);
        const current = await observer.current();
        assert.notDeepEqual(current, initial.state, "The live owner must select the other account");
        assert.deepEqual((await nextEvent()).state, current, "The dependent subscription must publish the rebound account");
        console.log("Dependent subscription: rebound account delivered");
        await setCredentials("subscription-updated");
        assert.deepEqual((await nextEvent()).state, await observer.current());
        console.log("Dependent subscription: current-manager update delivered");
        owner.bindSession();
        assert.deepEqual((await nextEvent()).state, await observer.current());
        sessions.push(await createExampleSession(cli, root));
        owner.bindSession(sessions[2]);
        await setCredentials("subscription-detached", sessions[2]);
        assert.deepEqual((await nextEvent()).state, await observer.current());
        console.log("Dependent subscription: replacement authority delivered");
        owner.dispose();
        assert(!(await nextEvent()).state, "Revocation must publish signed-out state");
        console.log("Dependent subscription: revoked authority delivered");
    } finally {
        subscription?.dispose();
        observer.dispose();
        owner.bindSession();
        for (const sessionId of sessions) {
            await cli.cliRuntimeCall("session.shutdown", JSON.stringify({ sessionId }));
        }
    }
}

async function createSessionFixture(root) {
    const tokenFile = join(root, "owner-token");
    await writeFile(tokenFile, "ghs_synthetic_original_owner");
    const requests = [];
    const unknownPaths = [];
    let beforeCatalog;
    const models = {
        data: [
            {
                id: "gpt-5.5",
                name: "GPT 5.5",
                version: "gpt-5.5",
                model_picker_enabled: true,
                supported_endpoints: ["/chat/completions"],
                capabilities: {
                    type: "chat",
                    family: "gpt-5.5",
                    supports: { tool_calls: true },
                    limits: { max_prompt_tokens: 65536, max_output_tokens: 4096, max_context_window_tokens: 65536 },
                },
            },
        ],
    };
    if (["background", "independent-utility"].includes(values.scenario)) models.data[0].billing = { multiplier: 0 };
    if (values.scenario === "catalog") models.data[0].billing = { restricted_to: ["pro"] };
    const server = createServer((request, response) => {
        const path = new URL(request.url, "http://localhost").pathname;
        requests.push({ path, authorization: request.headers.authorization, hmac: request.headers["request-hmac"] });
        response.setHeader("Content-Type", "application/json");
        if (path === "/models" || path === "/meta") {
            const callback = beforeCatalog;
            beforeCatalog = undefined;
            callback?.();
            response.end(JSON.stringify(path === "/meta" ? { models } : models));
        }
        else if (path === "/responses") {
            response.setHeader("Content-Type", "text/event-stream");
            response.end(`data: ${JSON.stringify({
                type: "response.completed",
                response: {
                    id: "local-utility-completion",
                    model: "gpt-5.5",
                    output: [{
                        type: "message",
                        id: "local-utility-message",
                        role: "assistant",
                        status: "completed",
                        content: [{ type: "output_text", text: "<session-title>Independent Utility Authentication</session-title>" }],
                    }],
                    usage: { input_tokens: 4, output_tokens: 4, total_tokens: 8 },
                },
            })}\n\n`);
        }
        else {
            unknownPaths.push(path);
            response.writeHead(404);
            response.end(JSON.stringify({ error: "Unexpected local fixture request" }));
        }
    });
    await new Promise((resolve, reject) => {
        server.once("error", reject);
        server.listen(0, "127.0.0.1", resolve);
    });
    return {
        tokenFile,
        url: `http://127.0.0.1:${server.address().port}`,
        requestCount: () => requests.length,
        requestPaths: () => requests.map((request) => request.path),
        completionCount: () => requests.filter((request) => request.path === "/responses").length,
        beforeCatalog(callback) { beforeCatalog = callback; },
        assertCredential(token) {
            const request = requests.findLast((request) => request.path === "/models" || request.path === "/meta");
            assert(request, "The real session must make an authenticated model request");
            assert.equal(request.authorization, `Bearer ${token}`, "The session must use the live original credential");
            assert.deepEqual(unknownPaths, [], "The example must use only the local scripted endpoints");
        },
        assertHmac() {
            const request = requests.findLast((request) => request.path === "/models" || request.path === "/meta");
            assert(request?.hmac, "The real session must send a signed HMAC request");
            const [timestamp, digest] = request.hmac.split(".");
            assert.match(timestamp, /^\d+$/);
            assert.equal(
                digest,
                createHmac("sha256", "sdk-owner-hmac-key").update(timestamp).digest("hex").toUpperCase(),
            );
            assert(!request.authorization, "The HMAC owner must not require an OAuth bearer");
            assert.deepEqual(unknownPaths, [], "The example must use only the local scripted endpoints");
        },
        renew: () => writeFile(tokenFile, "ghs_synthetic_renewed_owner"),
        async close() {
            await new Promise((resolve, reject) => server.close((error) => (error ? reject(error) : resolve())));
        },
    };
}
