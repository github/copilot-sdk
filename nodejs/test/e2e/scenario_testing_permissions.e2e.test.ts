/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { approveAll } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Scenario permission mode RPC", async () => {
    const { copilotClient: client } = await createSdkTestContext();

    it.each([
        ["assisted", "gpt-5.5"],
        ["allow-all", undefined],
    ] as const)("sets, resets and reads authoritative %s permission mode", async (mode, model) => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            featureFlags: { AUTO_APPROVAL: true },
        });
        try {
            expect((await session.rpc.permissions.getMode()).mode).toBe("manual");
            const set = await session.rpc.permissions.setMode({
                mode,
                assistedApprovalModel: model,
                source: "rpc",
            });
            expect(set).toMatchObject({ success: true, mode });
            expect((await session.rpc.permissions.getMode()).mode).toBe(mode);
            expect(
                await session.rpc.permissions.setMode({ mode: "manual", source: "rpc" })
            ).toMatchObject({ success: true, mode: "manual" });
            expect((await session.rpc.permissions.getMode()).mode).toBe("manual");
        } finally {
            await session.disconnect();
        }
    });

    it("refuses Assisted Permissions when managed policy disables it", async () => {
        const session = await client.createSession({
            onPermissionRequest: approveAll,
            featureFlags: { AUTO_APPROVAL: true },
            managedSettings: {
                permissions: {
                    disableAssistedPermissionsMode: true,
                },
            },
        });
        let enforcement:
            | {
                  escalation?: string;
                  setting: string;
                  failClosed: boolean;
              }
            | undefined;
        session.on((event) => {
            if (event.type === "session.managed_settings_enforced") {
                enforcement = event.data;
            }
        });

        try {
            expect((await session.rpc.permissions.getMode()).mode).toBe("manual");
            await expect(
                session.rpc.permissions.setMode({
                    mode: "assisted",
                    assistedApprovalModel: "gpt-5.5",
                    source: "rpc",
                })
            ).resolves.toMatchObject({ success: false, mode: "manual" });
            expect((await session.rpc.permissions.getMode()).mode).toBe("manual");
            // RPC completion does not drain the independently delivered session events.
            await expect
                .poll(() => enforcement, { timeout: 30_000 })
                .toMatchObject({
                    escalation: "assisted_approval",
                    setting: "permissions.disableAssistedPermissionsMode",
                    failClosed: false,
                });
        } finally {
            await session.disconnect();
        }
    });
});
