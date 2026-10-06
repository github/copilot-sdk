/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { randomUUID } from "node:crypto";
import { createServer } from "node:http";
import type { Server } from "node:http";
import { join } from "node:path";
import { describe, it } from "vitest";
import { z } from "zod";
import { approveAll, defineTool, RuntimeConnection } from "../../src/index.js";
import type { CopilotClient } from "../../src/index.js";
import { createSdkTestContext } from "./harness/sdkTestContext.js";

const attribute = z.object({
    key: z.string(),
    value: z.object({ stringValue: z.string().optional() }).passthrough(),
});
const tracePayload = z.object({
    resourceSpans: z.array(
        z.object({
            scopeSpans: z.array(
                z.object({
                    scope: z.object({ name: z.string() }),
                    spans: z.array(
                        z
                            .object({
                                traceId: z.string(),
                                spanId: z.string(),
                                attributes: z.array(attribute),
                            })
                            .passthrough()
                    ),
                })
            ),
        })
    ),
});
const metricPayload = z.object({
    resourceMetrics: z.array(
        z.object({
            scopeMetrics: z.array(
                z.object({
                    scope: z.object({ name: z.string() }),
                    metrics: z.array(z.object({ name: z.string() }).passthrough()),
                })
            ),
        })
    ),
});
const exportRecord = z.object({ path: z.string(), payload: z.unknown() });
type ExportRecord = z.infer<typeof exportRecord>;

async function closeServer(server: Server): Promise<void> {
    server.closeAllConnections();
    await new Promise<void>((resolve, reject) =>
        server.close((error) => (error ? reject(error) : resolve()))
    );
}

describe("HTTP telemetry export", async () => {
    // The KDC workflow runs this same public SDK scenario through real Squid
    // authentication. Ordinary SDK CI uses a test-owned loopback collector.
    const proxy = process.env.COPILOT_KDC_PROXY_URL;
    const collectorUrl = process.env.COPILOT_KDC_COLLECTOR_URL;
    const ca = process.env.COPILOT_KDC_CA_PATH;
    if (proxy && (!collectorUrl || !ca)) {
        throw new Error("KDC telemetry E2E requires collector URL and CA path");
    }
    const caseId = randomUUID();
    const harness = await createSdkTestContext({
        copilotClientOptions: {
            connection: RuntimeConnection.forStdio(),
            env: {
                OTEL_EXPORTER_OTLP_TRACES_ENDPOINT: "",
                OTEL_EXPORTER_OTLP_METRICS_ENDPOINT: "",
                OTEL_EXPORTER_OTLP_TRACES_PROTOCOL: "",
                OTEL_EXPORTER_OTLP_METRICS_PROTOCOL: "",
                OTEL_EXPORTER_OTLP_CERTIFICATE: "",
                OTEL_EXPORTER_OTLP_TRACES_CERTIFICATE: "",
                OTEL_EXPORTER_OTLP_METRICS_CERTIFICATE: "",
                OTEL_EXPORTER_OTLP_CLIENT_CERTIFICATE: "",
                OTEL_EXPORTER_OTLP_TRACES_CLIENT_CERTIFICATE: "",
                OTEL_EXPORTER_OTLP_METRICS_CLIENT_CERTIFICATE: "",
                OTEL_EXPORTER_OTLP_CLIENT_KEY: "",
                OTEL_EXPORTER_OTLP_TRACES_CLIENT_KEY: "",
                OTEL_EXPORTER_OTLP_METRICS_CLIENT_KEY: "",
                OTEL_EXPORTER_OTLP_TIMEOUT: "10000",
                OTEL_EXPORTER_OTLP_TRACES_TIMEOUT: "",
                OTEL_EXPORTER_OTLP_METRICS_TIMEOUT: "",
                OTEL_EXPORTER_OTLP_TRACES_HEADERS: `x-test-case=${caseId}`,
                OTEL_EXPORTER_OTLP_METRICS_HEADERS: `x-test-case=${caseId}`,
                OTEL_EXPORTER_OTLP_HEADERS: `x-test-case=${caseId}`,
                NO_PROXY: "127.0.0.1,localhost,::1",
                no_proxy: "127.0.0.1,localhost,::1",
                ...(proxy
                    ? {
                          HTTP_PROXY: proxy,
                          http_proxy: proxy,
                          HTTPS_PROXY: proxy,
                          https_proxy: proxy,
                          NODE_EXTRA_CA_CERTS: ca,
                          SSL_CERT_FILE: ca,
                          CURL_CA_BUNDLE: ca,
                          COPILOT_PROXY_KERBEROS_SPN: "HTTP/proxy.example.test",
                      }
                    : {}),
            },
        },
    });

    it(
        "exports completed SDK turns as OTLP traces and metrics",
        { timeout: 90_000 },
        async ({ expect, onTestFinished }) => {
            const received: ExportRecord[] = [];
            const failures: Error[] = [];
            let client: CopilotClient | undefined;
            let startingServer: Promise<Server> | undefined;
            onTestFinished(async () => {
                try {
                    if (client) {
                        expect(await client.stop()).toEqual([]);
                    }
                } finally {
                    const [startup] = await Promise.allSettled(
                        startingServer ? [startingServer] : []
                    );
                    if (startup?.status === "fulfilled") {
                        await closeServer(startup.value);
                    }
                }
            });
            let endpoint = "https://kerberos-proxy-test.invalid:8443";
            if (!proxy) {
                const server = createServer((request, response) => {
                    void (async () => {
                        expect(request.headers["x-test-case"]).toBe(caseId);
                        const chunks: Buffer[] = [];
                        for await (const chunk of request) {
                            chunks.push(Buffer.isBuffer(chunk) ? chunk : Buffer.from(chunk));
                        }
                        received.push({
                            path: request.url ?? "",
                            payload: JSON.parse(Buffer.concat(chunks).toString("utf8")),
                        });
                        response.setHeader("content-type", "application/json");
                        response.end("{}");
                    })().catch((error: Error) => {
                        failures.push(error);
                        response.writeHead(500).end();
                    });
                });
                startingServer = new Promise<Server>((resolve, reject) => {
                    server.once("error", reject);
                    server.listen(0, "127.0.0.1", () => resolve(server));
                });
                await startingServer;
                const address = server.address();
                if (!address || typeof address === "string") {
                    throw new Error("OTLP collector did not bind a TCP address");
                }
                endpoint = `http://127.0.0.1:${address.port}`;
            }

            // Reuse the existing telemetry turn capture, unchanged: the transport
            // test does not need another model sample or handcrafted response.
            await harness.openAiEndpoint.updateConfig({
                filePath: join(
                    import.meta.dirname,
                    "..",
                    "..",
                    "..",
                    "test",
                    "snapshots",
                    "telemetry",
                    "should_export_file_telemetry_for_sdk_interactions.yaml"
                ),
                workDir: harness.workDir,
            });
            const sourceName = "ts-sdk-telemetry-e2e";
            client = harness.createClient({
                telemetry: {
                    exporterType: "otlp-http",
                    otlpEndpoint: endpoint,
                    otlpProtocol: "http/json",
                    sourceName,
                    captureContent: true,
                },
            });
            const session = await client.createSession({
                onPermissionRequest: approveAll,
                tools: [
                    defineTool("echo_telemetry_marker", {
                        description: "Echoes a marker string for telemetry validation.",
                        parameters: z.object({ value: z.string() }),
                        handler: ({ value }) => value,
                    }),
                ],
            });
            const assistant = await session.sendAndWait(
                {
                    prompt: "Use the echo_telemetry_marker tool with value 'copilot-sdk-telemetry-e2e', then respond with TELEMETRY_E2E_DONE.",
                },
                90_000
            );
            expect(assistant?.data.content).toContain("TELEMETRY_E2E_DONE");
            await session.disconnect();
            expect(await client.stop()).toEqual([]);

            if (proxy) {
                const response = await fetch(`${collectorUrl}/exports/${caseId}`);
                expect(response.ok).toBe(true);
                received.push(...z.array(exportRecord).parse(await response.json()));
            }
            expect(failures).toEqual([]);
            const traces = received
                .filter(({ path }) => path === "/v1/traces")
                .flatMap(({ payload }) => tracePayload.parse(payload).resourceSpans)
                .flatMap(({ scopeSpans }) => scopeSpans);
            const metrics = received
                .filter(({ path }) => path === "/v1/metrics")
                .flatMap(({ payload }) => metricPayload.parse(payload).resourceMetrics)
                .flatMap(({ scopeMetrics }) => scopeMetrics);
            expect(traces.length).toBeGreaterThan(0);
            expect(metrics.length).toBeGreaterThan(0);
            expect(traces.every(({ scope }) => scope.name === sourceName)).toBe(true);
            expect(metrics.every(({ scope }) => scope.name === sourceName)).toBe(true);
            const spans = traces.flatMap(({ spans }) => spans);
            expect(
                spans.some(({ attributes }) =>
                    attributes.some(
                        ({ key, value }) =>
                            key === "gen_ai.conversation.id" &&
                            value.stringValue === session.sessionId
                    )
                )
            ).toBe(true);
            expect(
                spans.some(({ attributes }) =>
                    attributes.some(
                        ({ key, value }) =>
                            key === "gen_ai.output.messages" &&
                            value.stringValue?.includes("TELEMETRY_E2E_DONE")
                    )
                )
            ).toBe(true);
            expect(
                spans.every(
                    ({ traceId, spanId }) =>
                        /^[0-9a-f]{32}$/i.test(traceId) && /^[0-9a-f]{16}$/i.test(spanId)
                )
            ).toBe(true);
            expect(metrics.flatMap(({ metrics }) => metrics).length).toBeGreaterThan(0);
        }
    );
});
