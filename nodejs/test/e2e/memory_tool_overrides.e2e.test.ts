/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import { z } from "zod";
import { defineTool } from "../../src/index.js";
import type { PermissionRequest, SessionConfig, ToolInvocation } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

describe("Memory tool overrides", async () => {
    const { copilotClient: client } = await createSdkTestContext({ replayOnly: true });

    it.each(["store_memory", "vote_memory"])(
        "routes %s to the host with custom-tool permissions on create and resume",
        async (name) => {
            const invocations: ToolInvocation[] = [];
            const permissions: PermissionRequest[] = [];
            let approve = true;
            const config = {
                memory: { enabled: false },
                onPermissionRequest: (request) => {
                    permissions.push(request);
                    return approve ? { kind: "approve-once" } : { kind: "reject" };
                },
                tools: [
                    defineTool(name, {
                        description: "Host-owned memory",
                        parameters: z.object({ fact: z.string() }),
                        overridesBuiltInTool: true,
                        defer: "never",
                        handler: (_args, invocation) => {
                            invocations.push(invocation);
                            return "HOST_MEMORY_HANDLED";
                        },
                    }),
                ],
            } satisfies SessionConfig;
            let session = await client.createSession(config);
            try {
                for (const phase of ["create", "resume"]) {
                    if (phase === "resume") {
                        const sessionId = session.sessionId;
                        await session.disconnect();
                        session = await client.resumeSession(sessionId, config);
                    }
                    await session.rpc.tools.initializeAndValidate();
                    const { tools } = await session.rpc.tools.getCurrentMetadata();
                    expect(tools.filter((tool) => tool.name === name)).toEqual([
                        expect.objectContaining({
                            description: "Host-owned memory",
                            input_schema: expect.objectContaining({ required: ["fact"] }),
                        }),
                    ]);
                    const args = { fact: `Host-owned memory after ${phase}.` };
                    const toolCallId = `memory-override-${phase}`;
                    const result = await session.rpc.tools.execute({
                        name,
                        arguments: args,
                        toolCallId,
                    });

                    expect(result, `${name} after ${phase}`).toMatchObject({
                        resultType: "success",
                        textResultForLlm: "HOST_MEMORY_HANDLED",
                    });
                    expect(invocations.at(-1)).toMatchObject({
                        toolName: name,
                        toolCallId,
                        arguments: args,
                    });
                    expect(permissions.at(-1)).toMatchObject({
                        kind: "custom-tool",
                        toolName: name,
                        toolCallId,
                        args,
                    });
                }
                expect(invocations).toHaveLength(2);
                expect(permissions).toHaveLength(2);

                approve = false;
                const args = { fact: "A rejected memory." };
                const rejected = await session.rpc.tools.execute({
                    name,
                    arguments: args,
                    toolCallId: "memory-override-rejected",
                });
                expect(rejected.resultType).toBe("rejected");
                expect(invocations).toHaveLength(2);
                expect(permissions).toHaveLength(3);
                expect(permissions.at(-1)).toMatchObject({
                    kind: "custom-tool",
                    toolName: name,
                    args,
                });
            } finally {
                await session.disconnect();
            }
        }
    );
});
