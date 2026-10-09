/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { describe, expect, it } from "vitest";
import {
    approveAll,
    CopilotRequestHandler,
    RuntimeConnection,
    type CopilotRequestContext,
    type CopilotSession,
} from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";
import { withTestBackend } from "./harness/testBackend.js";

const IMAGE_MODEL = "gpt-image-2.5-sunburst";
const IMAGE_ENDPOINT = "/v1/images/generations";
const PNG =
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAABHNCSVQICAgIfAhkiAAAAAFzUkdCAK7OHOkAAAANSURBVAiZY2BgYPgPAAEEAQB9ssjfAAAAAElFTkSuQmCC";

class ImageService extends CopilotRequestHandler {
    readonly imageRequests: unknown[] = [];
    readonly chatToolNames: string[][] = [];

    constructor(private readonly modelAvailable = true) {
        super();
    }

    protected override async sendRequest(
        request: Request,
        context: CopilotRequestContext
    ): Promise<Response> {
        const pathname = new URL(request.url).pathname;
        if (pathname.endsWith("/chat/completions") && request.method === "POST") {
            const body = (await request.json()) as {
                model: string;
                stream?: boolean;
                tools?: Array<{ function: { name: string } }>;
            };
            this.chatToolNames.push(body.tools?.map((tool) => tool.function.name) ?? []);
            const message = { role: "assistant", content: "IMAGE_CONSENT_TURN_COMPLETE" };
            const response = {
                id: "image-consent-turn",
                created: 123,
                model: body.model,
                usage: { prompt_tokens: 5, completion_tokens: 1, total_tokens: 6 },
            };
            if (!body.stream) {
                return Response.json({
                    ...response,
                    object: "chat.completion",
                    choices: [{ index: 0, message, finish_reason: "stop" }],
                });
            }
            return new Response(
                `data: ${JSON.stringify({
                    ...response,
                    object: "chat.completion.chunk",
                    choices: [{ index: 0, delta: message, finish_reason: "stop" }],
                })}\n\ndata: [DONE]\n\n`,
                { headers: { "content-type": "text/event-stream" } }
            );
        }
        if (pathname === IMAGE_ENDPOINT) {
            this.imageRequests.push(await request.json());
            return Response.json({
                created: 123,
                data: [{ b64_json: PNG }],
                output_format: "png",
            });
        }
        const response = await super.sendRequest(request, context);
        if (pathname !== "/models" || !response.ok) {
            return response;
        }
        const catalog = (await response.json()) as { data: unknown[] };
        if (this.modelAvailable) {
            catalog.data.push({
                id: IMAGE_MODEL,
                name: "SDK test image model",
                capabilities: { type: "image" },
                supported_endpoints: [IMAGE_ENDPOINT],
                model_picker_enabled: false,
                policy: { state: "enabled" },
            });
        }
        return Response.json(catalog);
    }
}

async function imageToolOffered(session: CopilotSession): Promise<boolean> {
    await session.rpc.tools.initializeAndValidate();
    const { tools } = await session.rpc.tools.getCurrentMetadata();
    return tools.some((tool) => tool.name === "image_generation");
}

describe("Native image generation consent", async () => {
    // Native image inference is CAPI-only, independent of the chat-provider CI matrix.
    const { createClient, openAiEndpoint } = await createSdkTestContext({
        replayOnly: true,
        backend: "capi",
    });

    it("requires SDK consent and CAPI access regardless of the CLI rollout flag", async () => {
        for (const flag of [false, true]) {
            for (const enabled of [undefined, false, true]) {
                const service = new ImageService();
                await using client = createClient({ requestHandler: service });
                await using session = await client.createSession({
                    onPermissionRequest: approveAll,
                    featureFlags: { IMAGE_GENERATION_TOOL: flag },
                    imageGeneration: enabled === undefined ? undefined : { enabled },
                });
                expect(await imageToolOffered(session)).toBe(enabled === true);
                expect(service.imageRequests).toEqual([]);
            }
        }
    });

    it("executes an opted-in CAPI image tool while the rollout flag is off", async () => {
        const service = new ImageService();
        await using client = createClient({ requestHandler: service });
        await using session = await client.createSession({
            onPermissionRequest: approveAll,
            featureFlags: { IMAGE_GENERATION_TOOL: false },
            imageGeneration: { enabled: true },
        });
        expect(await imageToolOffered(session)).toBe(true);
        const result = await session.rpc.tools.execute({
            name: "image_generation",
            arguments: { prompt: "A watercolor fox" },
            toolCallId: "sdk-image",
        });
        expect(result).toMatchObject({
            resultType: "success",
            contents: [{ type: "image", mimeType: "image/png", data: PNG }],
        });
        expect(service.imageRequests).toEqual([
            {
                model: IMAGE_MODEL,
                prompt: "A watercolor fox",
                n: 1,
                output_format: "png",
                stream: false,
            },
        ]);
    });

    it("still requires an image model from CAPI", async () => {
        const service = new ImageService(false);
        await using client = createClient({ requestHandler: service });
        await using session = await client.createSession({
            onPermissionRequest: approveAll,
            imageGeneration: { enabled: true },
            featureFlags: { IMAGE_GENERATION_TOOL: true },
        });
        expect(await imageToolOffered(session)).toBe(false);
        expect(service.imageRequests).toEqual([]);
    });

    it("preserves resident consent but applies an explicit opt-out", async () => {
        await using client = createClient({ requestHandler: new ImageService() });
        await using session = await client.createSession({
            onPermissionRequest: approveAll,
            imageGeneration: { enabled: true },
            featureFlags: { IMAGE_GENERATION_TOOL: false },
        });
        expect(await imageToolOffered(session)).toBe(true);
        await using resumed = await client.resumeSession(session.sessionId, {
            onPermissionRequest: approveAll,
        });
        expect(await imageToolOffered(resumed)).toBe(true);
        await using disabled = await client.resumeSession(session.sessionId, {
            onPermissionRequest: approveAll,
            imageGeneration: { enabled: false },
            featureFlags: { IMAGE_GENERATION_TOOL: true },
        });
        expect(await imageToolOffered(disabled)).toBe(false);
    });

    it("requires renewed consent after a runtime restart", async () => {
        // Separate stdio runtimes prove that consent is not restored from disk.
        const firstService = new ImageService();
        await using first = createClient({
            requestHandler: firstService,
            connection: RuntimeConnection.forStdio(),
        });
        const original = await first.createSession({
            model: "claude-sonnet-5",
            onPermissionRequest: approveAll,
            imageGeneration: { enabled: true },
        });
        expect(await imageToolOffered(original)).toBe(true);
        // A session with no user activity is deliberately not persisted.
        const response = await original.sendAndWait({
            prompt: "Acknowledge this message without using tools.",
        });
        expect(response?.data.content).toBe("IMAGE_CONSENT_TURN_COMPLETE");
        expect(firstService.chatToolNames.some((names) => names.includes("image_generation"))).toBe(
            true
        );
        const sessionId = original.sessionId;
        await first.rpc.sessions.save({ sessionId });
        await original.disconnect();
        await first.stop();

        const secondService = new ImageService();
        await using second = createClient({
            requestHandler: secondService,
            connection: RuntimeConnection.forStdio(),
        });
        await using resumed = await second.resumeSession(sessionId, {
            onPermissionRequest: approveAll,
            featureFlags: { IMAGE_GENERATION_TOOL: true },
        });
        expect(await imageToolOffered(resumed)).toBe(false);
        const resumedResponse = await resumed.sendAndWait({
            prompt: "Acknowledge again without using tools.",
        });
        expect(resumedResponse?.data.content).toBe("IMAGE_CONSENT_TURN_COMPLETE");
        expect(secondService.chatToolNames.length).toBeGreaterThan(0);
        expect(
            secondService.chatToolNames.every((names) => !names.includes("image_generation"))
        ).toBe(true);
        await using optedIn = await second.resumeSession(sessionId, {
            onPermissionRequest: approveAll,
            imageGeneration: { enabled: true },
            featureFlags: { IMAGE_GENERATION_TOOL: false },
        });
        expect(await imageToolOffered(optedIn)).toBe(true);
    });

    it.each(["anthropic-messages", "openai-responses", "openai-completions"] as const)(
        "does not expose image generation for %s without CAPI image-model access",
        async (backend) => {
            const service = new ImageService(false);
            await using client = createClient({ requestHandler: service });
            await using session = await client.createSession(
                withTestBackend(
                    {
                        model: "claude-sonnet-5",
                        onPermissionRequest: approveAll,
                        imageGeneration: { enabled: true },
                        featureFlags: { IMAGE_GENERATION_TOOL: true },
                    },
                    backend,
                    openAiEndpoint.url
                )
            );
            expect(await imageToolOffered(session)).toBe(false);
            expect(service.imageRequests).toEqual([]);
        }
    );
});
