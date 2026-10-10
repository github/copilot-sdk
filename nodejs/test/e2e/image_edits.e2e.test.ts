/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { createHash, randomUUID } from "node:crypto";
import { readFile } from "node:fs/promises";
import { join } from "node:path";
import { deflateSync } from "node:zlib";
import { describe, expect, it } from "vitest";
import {
    approveAll,
    CopilotRequestHandler,
    type CopilotClient,
    type CopilotRequestContext,
    type SessionEvent,
    type UserPromptTransformedHookInput,
} from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

const MODEL = "gpt-image-2.5-sunburst";
const FINAL = "IMAGE_EDIT_SDK_FINISHED";

function png(red: number, width = 1): string {
    function chunk(name: string, data: Buffer): Buffer {
        const payload = Buffer.concat([Buffer.from(name), data]);
        let crc = 0xffffffff;
        for (const byte of payload) {
            crc ^= byte;
            for (let bit = 0; bit < 8; bit++) {
                crc = (crc >>> 1) ^ (crc & 1 ? 0xedb88320 : 0);
            }
        }
        const length = Buffer.alloc(4);
        length.writeUInt32BE(data.length);
        const checksum = Buffer.alloc(4);
        checksum.writeUInt32BE((crc ^ 0xffffffff) >>> 0);
        return Buffer.concat([length, payload, checksum]);
    }
    const header = Buffer.alloc(13);
    header.writeUInt32BE(width, 0);
    header.writeUInt32BE(1, 4);
    header[8] = 8;
    header[9] = 2;
    const pixels = Buffer.alloc(1 + width * 3);
    for (let column = 0; column < width; column++) {
        pixels[1 + column * 3] = red;
    }
    return Buffer.concat([
        Buffer.from("89504e470d0a1a0a", "hex"),
        chunk("IHDR", header),
        chunk("IDAT", deflateSync(pixels)),
        chunk("IEND", Buffer.alloc(0)),
    ]).toString("base64");
}

function imageId(data: string): string {
    return `sha256:${createHash("sha256").update(Buffer.from(data, "base64")).digest("hex")}`;
}

const SOURCE = png(20);
const EDITED = png(200);

class ImageHandler extends CopilotRequestHandler {
    readonly edits: Array<Record<string, unknown>> = [];
    readonly generations: Array<Record<string, unknown>> = [];
    readonly chats: Array<Record<string, unknown>> = [];
    outputImage = EDITED;
    supportedEndpoints = ["/v1/images/generations", "/v1/images/edits"];
    nextArguments: Record<string, unknown> | undefined;
    private calls = 0;

    protected override async sendRequest(
        request: Request,
        _context: CopilotRequestContext
    ): Promise<Response> {
        const path = new URL(request.url).pathname;
        const body: Record<string, unknown> = request.body ? await request.json() : {};
        if (path.endsWith("/models")) {
            return Response.json({
                data: [
                    {
                        id: MODEL,
                        name: "Image test",
                        capabilities: { type: "image" },
                        supported_endpoints: this.supportedEndpoints,
                        model_picker_enabled: false,
                        policy: { state: "enabled" },
                    },
                ],
            });
        }
        if (path.endsWith("/models/session") || path.includes("/policy")) {
            return Response.json({ state: "enabled" });
        }
        if (path === "/v1/images/edits" || path === "/v1/images/generations") {
            (path.endsWith("/edits") ? this.edits : this.generations).push(body);
            return Response.json({
                created: 1,
                data: [{ b64_json: this.outputImage }],
                output_format: "png",
            });
        }
        if (path.endsWith("/chat/completions")) {
            this.chats.push(body);
            const args = this.nextArguments;
            this.nextArguments = undefined;
            const toolCalls = args
                ? [
                      {
                          id: `image-edit-${++this.calls}`,
                          type: "function",
                          function: { name: "image_generation", arguments: JSON.stringify(args) },
                      },
                  ]
                : undefined;
            const message = {
                role: "assistant",
                content: args ? null : FINAL,
                tool_calls: toolCalls,
            };
            const finish = args ? "tool_calls" : "stop";
            if (body.stream === true) {
                const delta = {
                    ...message,
                    tool_calls: toolCalls?.map((call, index) => ({ index, ...call })),
                };
                return new Response(
                    [
                        `data: ${JSON.stringify({ id: "image-chat", object: "chat.completion.chunk", choices: [{ index: 0, delta, finish_reason: null }] })}\n\n`,
                        `data: ${JSON.stringify({ id: "image-chat", object: "chat.completion.chunk", choices: [{ index: 0, delta: {}, finish_reason: finish }], usage: { prompt_tokens: 1, completion_tokens: 1, total_tokens: 2 } })}\n\n`,
                        "data: [DONE]\n\n",
                    ].join(""),
                    { headers: { "content-type": "text/event-stream" } }
                );
            }
            return Response.json({
                id: "image-chat",
                object: "chat.completion",
                model: "image-edit-chat",
                choices: [{ index: 0, message, finish_reason: finish }],
                usage: { prompt_tokens: 1, completion_tokens: 1, total_tokens: 2 },
            });
        }
        throw new Error(`Unexpected image test request: ${request.method} ${path}`);
    }
}

describe("Native image editing", async () => {
    const { createClient, env, openAiEndpoint } = await createSdkTestContext({
        copilotClientOptions: { env: { IMAGE_GENERATION_TOOL: "true" } },
    });
    async function readPersistedAssets(client: CopilotClient, sessionId: string) {
        // Binary assets are internal persistence records, not public SDK events.
        await client.rpc.sessions.save({ sessionId });
        const log = await readFile(
            join(env.COPILOT_HOME, "session-state", sessionId, "events.jsonl"),
            "utf8"
        );
        return log
            .trim()
            .split("\n")
            .map((line): SessionEvent => JSON.parse(line))
            .filter((event) => event.type === "session.binary_asset");
    }
    const options = {
        model: "image-edit-chat",
        imageGeneration: { enabled: true },
        provider: {
            type: "openai" as const,
            wireApi: "completions" as const,
            baseUrl: "https://image-edit-test.invalid/v1",
            apiKey: "synthetic-image-test",
        },
        onPermissionRequest: approveAll,
    };

    it("edits an explicit attachment and can reuse the generated result after resume", async () => {
        const handler = new ImageHandler();
        const client = createClient({ requestHandler: handler });
        try {
            await client.start();
            const session = await client.createSession(options);
            const events: SessionEvent[] = [];
            session.on((event) => events.push(event));
            handler.nextArguments = {
                prompt: "Use this reference",
                input_images: [{ image_id: imageId(SOURCE) }],
            };
            const reply = await session.sendAndWait({
                prompt: "Use the attached image as a reference for a new image.",
                attachments: [{ type: "blob", mimeType: "image/png", data: SOURCE }],
            });
            expect(reply?.data.content).toBe(FINAL);
            const completion = events.find((event) => event.type === "tool.execution_complete");
            expect(completion?.data.error).toBeUndefined();
            expect(completion?.data.success).toBe(true);
            expect(handler.generations).toHaveLength(0);
            expect(handler.edits).toEqual([
                {
                    model: MODEL,
                    prompt: "Use this reference",
                    images: [{ image_url: `data:image/png;base64,${SOURCE}` }],
                    n: 1,
                    output_format: "png",
                    stream: false,
                },
            ]);
            const sessionId = session.sessionId;
            await session.disconnect();
            await client.stop();
            await client.start();
            const resumed = await client.resumeSession(sessionId, options);
            handler.nextArguments = {
                prompt: "Make the result brighter",
                input_images: [{ image_id: imageId(EDITED) }],
            };
            expect(
                (await resumed.sendAndWait({ prompt: "Make the generated image brighter." }))?.data
                    .content
            ).toBe(FINAL);
            expect(handler.edits).toHaveLength(2);
            expect(handler.edits[1].images).toEqual([
                { image_url: `data:image/png;base64,${EDITED}` },
            ]);
            expect(handler.generations).toHaveLength(0);
            await resumed.disconnect();
        } finally {
            await client.stop();
        }
    });

    it("does not upload an unrelated attachment when the tool selects text-only generation", async () => {
        const handler = new ImageHandler();
        const client = createClient({ requestHandler: handler });
        try {
            await client.start();
            const session = await client.createSession(options);
            handler.nextArguments = { prompt: "A fresh landscape" };
            expect(
                (
                    await session.sendAndWait({
                        prompt: "Ignore the attachment and generate a fresh landscape.",
                        attachments: [{ type: "blob", mimeType: "image/png", data: SOURCE }],
                    })
                )?.data.content
            ).toBe(FINAL);
            expect(handler.edits).toHaveLength(0);
            expect(handler.generations).toHaveLength(1);
            expect(handler.generations[0]).not.toHaveProperty("images");
            await session.disconnect();
        } finally {
            await client.stop();
        }
    });

    it("rejects image references outside the session instead of generating a substitute", async () => {
        const handler = new ImageHandler();
        const client = createClient({ requestHandler: handler });
        try {
            await client.start();
            const session = await client.createSession(options);
            const events: SessionEvent[] = [];
            session.on((event) => events.push(event));
            handler.nextArguments = {
                prompt: "Edit",
                input_images: [{ image_id: imageId(SOURCE) }],
            };
            expect(
                (await session.sendAndWait({ prompt: "Edit the selected image." }))?.data.content
            ).toBe(FINAL);
            expect(handler.edits).toHaveLength(0);
            expect(handler.generations).toHaveLength(0);
            const completion = events.find((event) => event.type === "tool.execution_complete");
            expect(completion?.data.error?.message).toMatch(/^image_generation_input_unavailable:/);
            expect(completion?.data.success).toBe(false);
            await session.disconnect();
        } finally {
            await client.stop();
        }
    });

    it("keeps the transformed hook final for batched image attachments", async () => {
        const handler = new ImageHandler();
        const client = createClient({ requestHandler: handler });
        const preceding = "Inspect the first batch image.";
        const replacement = "IMAGE_BATCH_HOOK_REPLACEMENT";
        const inputs: UserPromptTransformedHookInput[] = [];
        try {
            await client.start();
            const session = await client.createSession({
                ...options,
                hooks: {
                    onUserPromptTransformed: async (input) => {
                        inputs.push(input);
                        return { modifiedTransformedPrompt: replacement };
                    },
                },
            });
            const events: SessionEvent[] = [];
            session.on((event) => events.push(event));
            await session.rpc.sendMessages({
                messages: [
                    {
                        prompt: preceding,
                        attachments: [{ type: "blob", mimeType: "image/png", data: SOURCE }],
                    },
                    { prompt: "Finish the batch." },
                ],
                wait: true,
            });
            expect(inputs.find((input) => input.prompt === preceding)?.transformedPrompt).toContain(
                imageId(SOURCE)
            );
            const messages = events.filter((event) => event.type === "user.message");
            expect(messages).toHaveLength(2);
            for (const message of messages) {
                expect(message.data.transformedContent).toBe(replacement);
            }
            expect(JSON.stringify(handler.chats.at(-1)?.messages)).not.toContain(
                "Image input references"
            );
            // Validate the final assistant response arrived (guards against truncated captures).
            expect(
                events.findLast((event) => event.type === "assistant.message")?.data.content
            ).toBe(FINAL);
            await session.disconnect();
        } finally {
            await client.stop();
        }
    });

    it("emits the preceding image message when its transformed hook throws", async () => {
        const handler = new ImageHandler();
        const client = createClient({ requestHandler: handler });
        const preceding = "Inspect the first batch image.";
        const hooked: string[] = [];
        try {
            await client.start();
            const session = await client.createSession({
                ...options,
                hooks: {
                    onUserPromptTransformed: async (input) => {
                        hooked.push(input.prompt);
                        if (input.prompt === preceding) {
                            throw new Error("IMAGE_TRANSFORM_EXPECTED_FAILURE");
                        }
                    },
                },
            });
            const events: SessionEvent[] = [];
            session.on((event) => events.push(event));
            await session.rpc.sendMessages({
                messages: [
                    {
                        prompt: preceding,
                        attachments: [{ type: "blob", mimeType: "image/png", data: SOURCE }],
                    },
                    { prompt: "Finish the batch." },
                ],
                wait: true,
            });
            expect(hooked).toEqual([preceding, "Finish the batch."]);
            const messages = events.filter((event) => event.type === "user.message");
            expect(messages.map((message) => message.data.content)).toEqual(hooked);
            expect(messages[0].data.attachments).toContainEqual(
                expect.objectContaining({ type: "blob", mimeType: "image/png", data: SOURCE })
            );
            expect(
                (await readPersistedAssets(client, session.sessionId)).map(
                    (asset) => asset.data.assetId
                )
            ).toEqual([imageId(SOURCE)]);
            // Validate the final assistant response arrived (guards against truncated captures).
            expect(
                events.findLast((event) => event.type === "assistant.message")?.data.content
            ).toBe(FINAL);
            await session.disconnect();
        } finally {
            await client.stop();
        }
    });

    it("adds a persisted preview when reattaching a generated image", async () => {
        const handler = new ImageHandler();
        handler.outputImage = png(200, 2200);
        const client = createClient({ requestHandler: handler });
        try {
            await client.start();
            const session = await client.createSession(options);
            handler.nextArguments = { prompt: "Generate a wide image." };
            expect(
                (await session.sendAndWait({ prompt: "Generate a wide image." }))?.data.content
            ).toBe(FINAL);
            expect(handler.generations).toHaveLength(1);
            expect(
                (
                    await session.sendAndWait({
                        prompt: "Inspect the reattached image.",
                        attachments: [
                            { type: "blob", mimeType: "image/png", data: handler.outputImage },
                        ],
                    })
                )?.data.content
            ).toBe(FINAL);
            const assets = await readPersistedAssets(client, session.sessionId);
            const update = assets.findLast(
                (event) => event.data.assetId === `image-edit:${imageId(handler.outputImage)}`
            );
            expect(update?.data.metadata?.imageEditSource).toEqual({
                data: handler.outputImage,
                mimeType: "image/png",
            });
            const preview = update?.data.data;
            if (typeof preview !== "string") {
                throw new Error("The reattached image has no persisted chat preview");
            }
            expect(preview).not.toBe(handler.outputImage);
            expect(Buffer.from(preview, "base64").length).toBeLessThanOrEqual(3 * 1024 * 1024);
            const imageMessage = expect.objectContaining({
                role: "user",
                content: expect.arrayContaining([
                    expect.objectContaining({
                        type: "image_url",
                        image_url: expect.objectContaining({
                            url: `data:image/png;base64,${preview}`,
                        }),
                    }),
                ]),
            });
            expect(handler.chats.at(-1)?.messages).toContainEqual(imageMessage);

            const sessionId = session.sessionId;
            await session.disconnect();
            await client.stop();
            await client.start();
            const resumed = await client.resumeSession(sessionId, options);
            expect((await resumed.sendAndWait({ prompt: "Inspect it again." }))?.data.content).toBe(
                FINAL
            );
            expect(handler.chats.at(-1)?.messages).toContainEqual(imageMessage);
            handler.nextArguments = {
                prompt: "Edit the original wide image.",
                input_images: [{ image_id: imageId(handler.outputImage) }],
            };
            // Validate the final assistant response arrived (guards against truncated captures).
            expect(
                (await resumed.sendAndWait({ prompt: "Edit the reattached image." }))?.data.content
            ).toBe(FINAL);
            expect(handler.edits.at(-1)?.images).toEqual([
                { image_url: `data:image/png;base64,${handler.outputImage}` },
            ]);
            await resumed.disconnect();
        } finally {
            await client.stop();
        }
    });

    it.each(["generations-only", "filtered-out"] as const)(
        "does not retain originals or advertise image inputs when %s",
        async (availability) => {
            const handler = new ImageHandler();
            if (availability === "generations-only") {
                handler.supportedEndpoints = ["/v1/images/generations"];
            }
            // In-process clients share the model catalog cache, keyed by endpoint and auth.
            const token = `image-input-${randomUUID()}`;
            await openAiEndpoint.setCopilotUserByToken(token, {
                login: "image-input-test",
                copilot_plan: "individual_pro",
                endpoints: { api: env.COPILOT_API_URL },
                analytics_tracking_id: token,
            });
            const client = createClient({ requestHandler: handler, gitHubToken: token });
            const inputs: UserPromptTransformedHookInput[] = [];
            try {
                await client.start();
                const session = await client.createSession({
                    ...options,
                    excludedTools: availability === "filtered-out" ? ["image_generation"] : [],
                    hooks: {
                        onUserPromptTransformed: async (input) => {
                            inputs.push(input);
                        },
                    },
                });
                const original = png(20, 2200);
                // Validate the final assistant response arrived (guards against truncated captures).
                expect(
                    (
                        await session.sendAndWait({
                            prompt: "Inspect this attachment.",
                            attachments: [{ type: "blob", mimeType: "image/png", data: original }],
                        })
                    )?.data.content
                ).toBe(FINAL);
                expect(inputs).toHaveLength(1);
                expect(inputs[0].transformedPrompt).not.toContain("Image input references");
                const assets = await readPersistedAssets(client, session.sessionId);
                expect(assets).toHaveLength(1);
                expect(assets[0].data.data).not.toBe(original);
                expect(assets[0].data.metadata?.imageEditSource).toBeUndefined();
                await session.disconnect();
            } finally {
                await client.stop();
            }
        }
    );
});
