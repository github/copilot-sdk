/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { readFileSync } from "fs";
import path from "path";
import { describe, expect, test } from "vitest";

/**
 * Validates that the test harness CLI binary is compatible with the SDK
 * protocol. This prevents regressions where the SDK advances its protocol
 * (e.g., new permission kind values or RPC methods) but the harness CLI
 * is not bumped to a version that supports them.
 *
 * See: https://github.com/github/copilot-sdk/issues/1146
 */
describe("Test harness CLI compatibility", () => {
    const cliAppPath = path.resolve(
        import.meta.dirname,
        "node_modules/@github/copilot/app.js",
    );

    const appContent = readFileSync(cliAppPath, "utf-8");

    test("CLI app.js bundle exists", () => {
        expect(appContent.length).toBeGreaterThan(0);
    });

    test("CLI supports new permission decision kinds (approve-once, reject, user-not-available)", () => {
        // The handlePendingPermissionRequest RPC handler must accept the new
        // PermissionDecision kinds introduced alongside the SDK's
        // PermissionRequestResultKind changes (approve-once replaces approved,
        // reject replaces denied-interactively-by-user, user-not-available
        // replaces denied-no-approval-rule-and-could-not-request-from-user).
        expect(appContent).toContain("handlePendingPermissionRequest");
        expect(appContent).toContain("approve-once");
        expect(appContent).toContain("reject");
        expect(appContent).toContain("user-not-available");
    });

    test("CLI supports per-session auth (auth.getStatus)", () => {
        // Per-session GitHub authentication requires the CLI to implement
        // the session.auth.getStatus RPC method and accept gitHubToken in
        // session creation.
        expect(appContent).toContain("auth.getStatus");
        expect(appContent).toContain("gitHubToken");
        expect(appContent).toContain("isAuthenticated");
    });
});
