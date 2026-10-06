/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import http from "node:http";
import { afterAll, describe, expect, it } from "vitest";
import { approveAll, type SessionEvent } from "../../src/index.js";
import { withTestCleanup } from "../helpers/withTestCleanup.js";
import { createSdkTestContext, DEFAULT_GITHUB_TOKEN } from "./harness/sdkTestContext.js";
import { createCompletedResponse } from "./harness/webSocketResponsesServer.js";

const FINAL_OUTPUT = "SSE_INCOMPLETE_TERMINAL_DONE";
const pollOptions = { timeout: 10_000 };

describe("SSE incomplete terminals", async () => {
    const release = Promise.withResolvers<void>();
    const responses: http.ServerResponse[] = [];
    const errors: unknown[] = [];
    // Native responses_transport tests own the envelope/output matrix. This case
    // proves the held-open HTTP stream crosses the subprocess SDK boundary.
    const server = http.createServer((req, res) => {
        void (async () => {
            if (req.method !== "POST" || req.url !== "/v1/responses") {
                throw new Error(`Unexpected provider request: ${req.method} ${req.url}`);
            }
            req.setEncoding("utf8");
            let body = "";
            for await (const chunk of req) body += chunk;
            const request: unknown = JSON.parse(body);
            if (typeof request !== "object" || request === null) {
                throw new Error("Expected a Responses request object.");
            }
            if (!("stream" in request) || request.stream !== true) {
                res.writeHead(200, {
                    "content-type": "application/json",
                    "x-content-type-options": "nosniff",
                });
                res.end(JSON.stringify(createCompletedResponse("Test session", "resp-title")));
                return;
            }
            responses.push(res);
            res.on("error", (error) => errors.push(error));
            res.writeHead(200, { "content-type": "text/event-stream" });
            res.write(
                `data: ${JSON.stringify({
                    type: "response.created",
                    response: { id: "resp-incomplete", status: "in_progress", output: [] },
                })}\n\n`
            );
            await release.promise;
            res.write(
                `data: ${JSON.stringify({
                    type: "response.completed",
                    response: {
                        ...createCompletedResponse(FINAL_OUTPUT, "resp-incomplete"),
                        status: "incomplete",
                        // Isolate draining from the existing length-continuation policy.
                        incomplete_details: { reason: "content_filter" },
                    },
                })}\n\n`
            );
            // Neither [DONE] nor res.end(): only the runtime can retire this stream.
        })().catch((error: unknown) => {
            errors.push(error);
            res.destroy(error instanceof Error ? error : new Error(String(error)));
        });
    });
    await new Promise<void>((resolve, reject) => {
        server.once("error", reject);
        server.listen(0, "127.0.0.1", resolve);
    });
    afterAll(async () => {
        release.resolve();
        server.closeAllConnections();
        await new Promise<void>((resolve, reject) =>
            server.close((error) => (error ? reject(error) : resolve()))
        );
        if (errors.length > 0) throw new AggregateError(errors, "SSE provider fixture failed.");
    });
    const address = server.address();
    if (address === null || typeof address === "string") {
        throw new Error("Expected a bound TCP address for the SSE provider.");
    }
    const { copilotClient: client } = await createSdkTestContext({
        useStdio: true,
        copilotClientOptions: { gitHubToken: DEFAULT_GITHUB_TOKEN },
    });

    it("finishes a completed-envelope incomplete response without waiting for peer EOF", async () => {
        const session = await client.createSession({
            model: "gpt-5-responses",
            streaming: true,
            provider: {
                type: "openai",
                baseUrl: `http://127.0.0.1:${address.port}/v1`,
                apiKey: "test-provider-key",
                wireApi: "responses",
                transport: "http",
            },
            onPermissionRequest: approveAll,
        });
        const events: SessionEvent[] = [];
        const unsubscribe = session.on((event) => events.push(event));
        await withTestCleanup(
            async () => {
                await session.send({ prompt: "Return the controlled partial answer." });
                await expect
                    .poll(
                        () => ({
                            requests: responses.length,
                            errors: events
                                .filter((event) => event.type === "session.error")
                                .map((event) => event.data.message),
                        }),
                        pollOptions
                    )
                    .toEqual({ requests: 1, errors: [] });
                expect(responses[0].writableEnded).toBe(false);
                expect(events.filter((event) => event.type === "assistant.message")).toEqual([]);
                await session.rpc.mode.get();
                release.resolve();

                await expect
                    .poll(
                        () =>
                            events.filter((event) => event.type === "assistant.message").at(-1)
                                ?.data.content,
                        pollOptions
                    )
                    .toBe(FINAL_OUTPUT);
                await expect
                    .poll(() => {
                        const finalMessage = events.findLastIndex(
                            (event) =>
                                event.type === "assistant.message" &&
                                event.data.content === FINAL_OUTPUT
                        );
                        return (
                            finalMessage !== -1 &&
                            events
                                .slice(finalMessage + 1)
                                .some((event) => event.type === "session.idle")
                        );
                    }, pollOptions)
                    .toBe(true);
                expect(responses).toHaveLength(1);
                expect(responses[0].writableEnded).toBe(false);
                await expect.poll(() => responses[0].destroyed, pollOptions).toBe(true);
                expect(events.filter((event) => event.type === "session.error")).toEqual([]);
            },
            () => release.resolve(),
            unsubscribe,
            () => session.disconnect(),
            async () => {
                const errors = await client.stop();
                if (errors.length > 0) {
                    throw new AggregateError(errors, "SSE client cleanup failed");
                }
            }
        );
    });
});
