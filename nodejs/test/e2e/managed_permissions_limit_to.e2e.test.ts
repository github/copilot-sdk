/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { writeFile } from "node:fs/promises";
import { createServer, type Server } from "node:http";
import { join } from "node:path";
import { describe, expect, it } from "vitest";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

function closeServer(server: Server): Promise<void> {
    server.closeAllConnections();
    return new Promise((resolve, reject) => {
        server.close((error) => (error ? reject(error) : resolve()));
    });
}

describe("Managed permissions limitTo", async () => {
    const { copilotClient: client, workDir } = await createSdkTestContext();

    it("evaluates retained managed network policy without invoking the permission handler", async () => {
        let permissionRequests = 0;
        const session = await client.createSession({
            managedSettings: {
                permissions: {
                    limitTo: ["Domain(*.example.com)"],
                    deny: ["Domain(blocked.example.com)"],
                    ask: ["Domain(prompt.example.com)"],
                    allow: ["Domain(allowed.example.com)"],
                },
            },
            onPermissionRequest: () => {
                permissionRequests++;
                return { kind: "approve-once" };
            },
        });
        try {
            const urls = [
                "https://allowed.example.com/guide",
                "https://prompt.example.com/",
                "https://blocked.example.com/",
                "https://outside.test/",
                "http://allowed.example.com:8080/redirect",
            ];
            const snapshot = await session.rpc.managedSettings.get();
            expect(snapshot.permissionsContext).toBeDefined();
            const context = snapshot.permissionsContext!;
            const operations = urls.map((url) => ({ kind: "url" as const, url }));
            const result = await client.rpc.managedSettings.permissions.evaluate({
                context,
                operations,
            });
            expect(result).toEqual({
                failClosed: false,
                results: operations.map((operation, index) => ({
                    operation,
                    verdict: ["allow", "ask", "deny", "deny", "ask"][index],
                })),
            });
            expect(permissionRequests).toBe(0);
            await expect(
                client.rpc.managedSettings.permissions.evaluate({
                    context,
                    operations: [{ kind: "url", url: "file:///secret" }],
                })
            ).rejects.toMatchObject({ code: -32602 });
            expect(
                await client.rpc.managedSettings.permissions.evaluate({ context, operations: [] })
            ).toEqual({
                failClosed: false,
                results: [],
            });
            expect(snapshot.settings).not.toHaveProperty("sandbox");
        } finally {
            await session.disconnect();
        }
    });

    it("evaluates an in-memory permissions context without creating a session", async () => {
        await client.start();
        const operations = [
            { kind: "url" as const, url: "https://api.example.com/guide" },
            { kind: "url" as const, url: "https://outside.test/" },
        ];
        const context = {
            permissions: { limitTo: ["Domain(api.example.com)"] },
            failClosed: false,
        };
        const result = await client.rpc.managedSettings.permissions.evaluate({
            context,
            operations,
        });
        expect(result).toEqual({
            failClosed: false,
            results: [
                { operation: operations[0], verdict: "unmanaged" },
                { operation: operations[1], verdict: "deny" },
            ],
        });
        expect(
            await client.rpc.managedSettings.permissions.evaluate({
                context: { ...context, failClosed: true },
                operations,
            })
        ).toEqual({
            failClosed: true,
            results: operations.map((operation) => ({ operation, verdict: "deny" })),
        });
    });

    it("denies every URL for an empty managed boundary", async () => {
        const session = await client.createSession({
            managedSettings: { permissions: { limitTo: [] } },
        });
        try {
            const snapshot = await session.rpc.managedSettings.get();
            expect(snapshot.permissionsContext).toBeDefined();
            expect(
                await client.rpc.managedSettings.permissions.evaluate({
                    context: snapshot.permissionsContext!,
                    operations: [
                        { kind: "url", url: "https://example.com/" },
                        { kind: "url", url: "https://api.github.com/" },
                    ],
                })
            ).toEqual({
                failClosed: false,
                results: [
                    { operation: { kind: "url", url: "https://example.com/" }, verdict: "deny" },
                    { operation: { kind: "url", url: "https://api.github.com/" }, verdict: "deny" },
                ],
            });
        } finally {
            await session.disconnect();
        }
    });

    it.skipIf(process.platform !== "darwin")(
        "admits only the configured dynamic loopback destination",
        async () => {
            const requestedHosts: string[] = [];
            const server = createServer((request, response) => {
                requestedHosts.push(request.headers.host?.split(":")[0] ?? "");
                response.end("LIMIT_TO_SERVER_OK");
            });
            await new Promise<void>((resolve, reject) => {
                server.once("error", reject);
                server.listen(0, "0.0.0.0", () => {
                    server.off("error", reject);
                    resolve();
                });
            });
            const address = server.address();
            expect(address && typeof address !== "string").toBe(true);
            const port = typeof address === "object" && address ? address.port : 0;

            await writeFile(join(workDir, "limit-to-blocked-marker.txt"), "LIMIT_TO_BLOCKED_OK");
            try {
                const session = await client.createSession({
                    managedSettings: {
                        permissions: {
                            limitTo: ["Domain(127.0.0.1)"],
                        },
                    },
                    onPermissionRequest: () => ({ kind: "approve-once" }),
                });
                try {
                    const update = await session.rpc.options.update({
                        sandboxConfig: {
                            enabled: true,
                            failIfUnavailable: true,
                            allowBypass: false,
                            allowDevToolAccess: false,
                            learningMode: "deny",
                            sandboxMcpServers: true,
                            sandboxLspServers: true,
                            userPolicy: {
                                network: {
                                    allowLocalNetwork: true,
                                    allowedHosts: ["127.0.0.1"],
                                    blockedHosts: [],
                                },
                            },
                        },
                    });
                    expect(update.success).toBe(true);
                    await session.rpc.tools.initializeAndValidate();
                    const admitted = await session.rpc.tools.execute({
                        name: "bash",
                        arguments: {
                            command:
                                `host=127.0.0.1; curl --disable --silent --show-error --fail --max-time 5 ` +
                                `--proxy "$HTTP_PROXY" "http://$host:${port}/probe"`,
                            description: "Probe the admitted dynamic loopback destination",
                        },
                        toolCallId: "limit-to-admitted",
                    });
                    expect(admitted).toMatchObject({
                        resultType: "success",
                        textResultForLlm: expect.stringContaining("LIMIT_TO_SERVER_OK"),
                    });

                    const blocked = await session.rpc.tools.execute({
                        name: "bash",
                        arguments: {
                            command:
                                `host=127.0.0.2; curl --disable --silent --show-error --fail --max-time 5 ` +
                                `--noproxy "*" "http://$host:${port}/probe" || ` +
                                "cat limit-to-blocked-marker.txt",
                            description: "Probe a blocked dynamic loopback destination",
                        },
                        toolCallId: "limit-to-blocked",
                    });
                    expect(blocked).toMatchObject({
                        resultType: "success",
                        textResultForLlm: expect.stringContaining("LIMIT_TO_BLOCKED_OK"),
                    });
                    expect(requestedHosts).toEqual(["127.0.0.1"]);
                } finally {
                    await session.disconnect();
                }
            } finally {
                await closeServer(server);
            }
        }
    );
});
