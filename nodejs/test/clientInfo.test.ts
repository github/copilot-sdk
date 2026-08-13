/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it, vi, afterEach } from "vitest";
import { CopilotClient, RuntimeConnection } from "../src/index.js";
import { COPILOT_CLIENT_INFO_ENV_VAR, applyClientInfoEnv } from "../src/clientInfo.js";

afterEach(() => {
    // `vi.spyOn` needs restoring, not `unstubAllGlobals`, or a stubbed
    // `process.platform` leaks into later tests and breaks CLI path resolution.
    vi.restoreAllMocks();
});

/** Pretend to be Windows, where environment variables are case-insensitive. */
function stubPlatform(platform: NodeJS.Platform): void {
    vi.spyOn(process, "platform", "get").mockReturnValue(platform);
}

describe("applyClientInfoEnv", () => {
    it("serializes the identity an application declared", () => {
        const env: Record<string, string | undefined> = {};
        applyClientInfoEnv(env, {
            editorName: "vscode",
            editorVersion: "1.124.2",
            extensionName: "vscode-agent-host",
            extensionVersion: "1.124.2",
        });
        expect(JSON.parse(env[COPILOT_CLIENT_INFO_ENV_VAR]!)).toEqual({
            editorName: "vscode",
            editorVersion: "1.124.2",
            extensionName: "vscode-agent-host",
            extensionVersion: "1.124.2",
        });
    });

    it("omits fields an application left undefined", () => {
        // Every field is optional while integrators onboard, so a partial
        // identity is serialized as what it declared rather than as nulls.
        const env: Record<string, string | undefined> = {};
        applyClientInfoEnv(env, { extensionName: "vscode-agent-host" });
        expect(JSON.parse(env[COPILOT_CLIENT_INFO_ENV_VAR]!)).toEqual({
            extensionName: "vscode-agent-host",
        });
    });

    it("leaves no variable behind when nothing usable was declared", () => {
        for (const clientInfo of [undefined, {}, { extensionName: "" }]) {
            const env: Record<string, string | undefined> = {};
            applyClientInfoEnv(env, clientInfo);
            expect(env).not.toHaveProperty(COPILOT_CLIENT_INFO_ENV_VAR);
        }
    });

    it("drops a raw value supplied through env so the typed option is the only way in", () => {
        // Otherwise an application could bypass the type by writing the string
        // itself, which is exactly the drift this option removes.
        const env: Record<string, string | undefined> = {
            [COPILOT_CLIENT_INFO_ENV_VAR]: JSON.stringify({ extensionName: "hand-rolled" }),
        };
        applyClientInfoEnv(env, { extensionName: "vscode-agent-host" });
        expect(JSON.parse(env[COPILOT_CLIENT_INFO_ENV_VAR]!)).toEqual({
            extensionName: "vscode-agent-host",
        });
    });

    it("clears an inherited value when no identity is declared", () => {
        // An inherited value describes whatever launched this process, so it
        // must not be forwarded as if this client had declared it.
        const env: Record<string, string | undefined> = {
            [COPILOT_CLIENT_INFO_ENV_VAR]: JSON.stringify({ extensionName: "whatever-launched-us" }),
        };
        applyClientInfoEnv(env, undefined);
        expect(env).not.toHaveProperty(COPILOT_CLIENT_INFO_ENV_VAR);
    });

    it("clears every spelling on Windows, and only there", () => {
        stubPlatform("win32");
        const windowsEnv: Record<string, string | undefined> = { Copilot_Client_Info: "stale" };
        applyClientInfoEnv(windowsEnv, { extensionName: "vscode-agent-host" });
        // Windows resolves names case-insensitively, so a differently cased
        // spelling could win de-duplication when the child is spawned.
        expect(windowsEnv).not.toHaveProperty("Copilot_Client_Info");
        expect(JSON.parse(windowsEnv[COPILOT_CLIENT_INFO_ENV_VAR]!)).toEqual({
            extensionName: "vscode-agent-host",
        });

        vi.restoreAllMocks();
        stubPlatform("linux");
        const posixEnv: Record<string, string | undefined> = { Copilot_Client_Info: "unrelated" };
        applyClientInfoEnv(posixEnv, { extensionName: "vscode-agent-host" });
        // Elsewhere it is a genuinely separate variable and not ours to delete.
        expect(posixEnv.Copilot_Client_Info).toBe("unrelated");
    });

    it("leaves unrelated variables alone", () => {
        const env: Record<string, string | undefined> = { PATH: "/usr/bin", COPILOT_HOME: "/tmp/home" };
        applyClientInfoEnv(env, { extensionName: "vscode-agent-host" });
        expect(env.PATH).toBe("/usr/bin");
        expect(env.COPILOT_HOME).toBe("/tmp/home");
    });
});

describe("CopilotClient client identity wiring", () => {
    /** Build the env a spawned runtime would receive for these options. */
    function runtimeEnv(options: ConstructorParameters<typeof CopilotClient>[0]): Record<string, string | undefined> {
        const client = new CopilotClient({
            // Pinned so construction does not depend on a resolvable bundled CLI.
            connection: RuntimeConnection.forStdio({ path: "/nonexistent/copilot" }),
            ...options,
        });
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        return (client as any).buildRuntimeEnv();
    }

    it("passes a declared identity to the spawned runtime", () => {
        // The helper being correct in isolation does not prove the option is
        // reached, so this covers the wiring rather than the serialization.
        const env = runtimeEnv({
            clientInfo: { editorName: "vscode", extensionName: "vscode-agent-host" },
        });
        expect(JSON.parse(env[COPILOT_CLIENT_INFO_ENV_VAR]!)).toEqual({
            editorName: "vscode",
            extensionName: "vscode-agent-host",
        });
    });

    it("declares nothing when an application did not opt in", () => {
        // The option is additive: an existing application must be unaffected.
        expect(runtimeEnv({})).not.toHaveProperty(COPILOT_CLIENT_INFO_ENV_VAR);
    });

    it("ignores a raw value passed through env", () => {
        const env = runtimeEnv({
            env: { ...process.env, [COPILOT_CLIENT_INFO_ENV_VAR]: JSON.stringify({ extensionName: "hand-rolled" }) },
        });
        expect(env).not.toHaveProperty(COPILOT_CLIENT_INFO_ENV_VAR);
    });
});
