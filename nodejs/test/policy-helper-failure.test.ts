import { ResponseError, type MessageConnection } from "vscode-jsonrpc/node.js";
import { describe, expect, it, vi } from "vitest";
import { createServerRpc, createSessionRpc } from "../src/generated/rpc.js";
import type { ManagedSettingsResolvedData, PolicyHelperFailure } from "../src/index.js";

const policyHelperFailure = {
    authority: "future-authority",
    category: "future-category",
    code: "helper.future-code",
    details: [
        {
            path: "allowedRepositories",
            message: "Expected an array.",
        },
    ],
    diagnostic: "bounded diagnostic",
    failureMode: "future-mode",
    helpLink: {
        url: "https://docs.example.com/policy-helper",
        label: "Review configuration",
    },
    message: "The Policy Helper rejected the current configuration.",
    previousPolicyRetained: true,
    reason: "future-reason",
    remediation: "Update the configuration and recheck managed policy.",
    severity: "future-severity",
} satisfies PolicyHelperFailure;

const managedSettingsSnapshot = {
    bypassPermissionsDisabled: true,
    deviceManaged: false,
    failClosed: true,
    managedKeys: ["permissions"],
    policyHelperCapabilities: {
        recheck: true,
        structuredFailure: true,
    },
    policyHelperFailure,
    policyHelperManaged: true,
    serverManaged: true,
    source: "policyHelper",
} satisfies ManagedSettingsResolvedData;

describe("Policy Helper failure contracts", () => {
    it("round trips the complete DTO and preserves future string values", () => {
        const roundTripped = JSON.parse(
            JSON.stringify(managedSettingsSnapshot)
        ) as ManagedSettingsResolvedData;

        expect(roundTripped.policyHelperCapabilities).toEqual({
            recheck: true,
            structuredFailure: true,
        });
        expect(roundTripped.policyHelperFailure).toEqual(policyHelperFailure);
        expect(roundTripped.policyHelperFailure?.category).toBe("future-category");
        expect(roundTripped.policyHelperFailure?.failureMode).toBe("future-mode");
        expect(roundTripped.policyHelperFailure?.severity).toBe("future-severity");
        expect(roundTripped.policyHelperFailure?.reason).toBe("future-reason");
        expect(roundTripped.policyHelperFailure?.authority).toBe("future-authority");
        expect(roundTripped.policyHelperFailure?.code).toBe("helper.future-code");
    });

    it("keeps older runtime snapshots compatible", () => {
        const legacySnapshot: ManagedSettingsResolvedData = {
            bypassPermissionsDisabled: false,
            deviceManaged: false,
            failClosed: false,
            managedKeys: [],
            serverManaged: false,
            source: "none",
        };

        expect(legacySnapshot.policyHelperCapabilities).toBeUndefined();
        expect(legacySnapshot.policyHelperFailure).toBeUndefined();
    });

    it("binds the session-scoped recheck RPC and returns its authoritative snapshot", async () => {
        const sendRequest = vi.fn().mockResolvedValue(managedSettingsSnapshot);
        const rpc = createSessionRpc(
            { sendRequest } as unknown as MessageConnection,
            "policy-session"
        );

        const result = await rpc.managedSettings.recheckPolicyHelper();

        expect(sendRequest).toHaveBeenCalledWith("session.managedSettings.recheckPolicyHelper", {
            sessionId: "policy-session",
        });
        expect(result.policyHelperCapabilities?.recheck).toBe(true);
        expect(result.policyHelperFailure).toEqual(policyHelperFailure);
    });

    it("preserves Policy Helper context on policy-dependent activation errors", async () => {
        const errorData = {
            code: "plugin_activation_failed",
            policyHelperFailure,
        };
        const responseError = new ResponseError(-32001, "plugin activation failed", errorData);
        const sendRequest = vi.fn().mockRejectedValue(responseError);
        const rpc = createServerRpc({ sendRequest } as unknown as MessageConnection);

        const error = await rpc.managedSettings
            .resolve({
                workingDirectory: "/workspace",
                policyHelperReason: "pluginActivation",
            })
            .then(
                () => undefined,
                (reason: unknown) => reason
            );

        expect(sendRequest).toHaveBeenCalledWith("managedSettings.resolve", {
            workingDirectory: "/workspace",
            policyHelperReason: "pluginActivation",
        });
        expect(error).toBe(responseError);
        expect(responseError.code).toBe(-32001);
        expect(responseError.message).toBe("plugin activation failed");
        expect(responseError.data).toBe(errorData);
        expect(responseError.data.policyHelperFailure).toBe(policyHelperFailure);
    });
});
