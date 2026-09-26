/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { randomUUID } from "node:crypto";
import { readFileSync } from "node:fs";
import { writeFile } from "node:fs/promises";
import { join } from "node:path";
import { PassThrough } from "node:stream";
import {
    createMessageConnection,
    StreamMessageReader,
    StreamMessageWriter,
} from "vscode-jsonrpc/node.js";
import { describe, expect, it, onTestFinished } from "vitest";
import { approveAll, CopilotSession } from "../src/index.js";
import type { SessionEvent } from "../src/index.js";
import { createServerRpc } from "../src/generated/rpc.js";
import { withWorkerCausalityEvents } from "../src/workerCausality.js";
import { createSdkTestContext } from "./e2e/harness/sdkTestContext.js";

type Wire = Record<string, unknown>;
type HistoryRead = { name: string; events: SessionEvent[] };
const corpus = JSON.parse(
    readFileSync(new URL("../../test/worker-causality.json", import.meta.url), "utf8")
) as {
    valid: { name: string; event?: Wire; result?: Wire }[];
    invalid: { name: string; value: unknown }[];
    invalidRaw: { name: string; json: string }[];
    boundaries: { name: string; value: unknown; accepted: boolean }[];
    workflowCompleted: { event: Wire };
    legacyWorkflowCompleted: { persistedEvent: Wire };
};

function payload(value: Wire, event: boolean): Wire {
    return event ? (value.data as Wire) : value;
}

function expectWorkflowCompleted(events: SessionEvent[], eventId: string, reader: string): void {
    const event = events.find((candidate) => candidate.id === eventId);
    expect(event?.type, reader).toBe("system.notification");
    if (event?.type !== "system.notification") {
        throw new Error(`${reader} did not return the workflow completion event`);
    }
    expect(event.data.kind.type, reader).toBe("workflow_completed");
    if (event.data.kind.type !== "workflow_completed") {
        throw new Error(`${reader} did not canonicalize the workflow completion event`);
    }
    expect(event.data.kind.workflowName, reader).toBe("fix-ci");
    expect(event.data.kind, reader).not.toHaveProperty("factoryName");
}

function readers() {
    const requests = new PassThrough();
    const responses = new PassThrough();
    const client = createMessageConnection(
        new StreamMessageReader(responses),
        new StreamMessageWriter(requests)
    );
    const server = createMessageConnection(
        new StreamMessageReader(requests),
        new StreamMessageWriter(responses)
    );
    let reply: unknown;
    server.onRequest(() => reply);
    client.listen();
    server.listen();
    const session = new CopilotSession("codec-session", client);
    const serverRpc = createServerRpc(client);
    const observed: SessionEvent[] = [];
    const unsubscribe = session.on((event) => observed.push(event));
    onTestFinished(() => {
        unsubscribe();
        client.dispose();
        server.dispose();
        requests.destroy();
        responses.destroy();
    });
    return {
        async read(wire: Wire, event: boolean): Promise<Wire> {
            if (event) {
                session._dispatchEvent(wire as unknown as SessionEvent);
                return observed.at(-1) as unknown as Wire;
            }
            reply = wire;
            return (await session.rpc.tasks.sendMessage({
                id: "worker",
                message: "test",
            })) as unknown as Wire;
        },
        async history(wire: Wire): Promise<HistoryRead[]> {
            reply = { events: [wire], cursor: "cursor", hasMore: false, cursorStatus: "ok" };
            return [
                { name: "session.getMessages", events: await session.getEvents() },
                {
                    name: "session.eventLog.read",
                    events: (await session.rpc.eventLog.read({})).events,
                },
                {
                    name: "sessions.readPersistedEvents",
                    events: (
                        await serverRpc.sessions.readPersistedEvents({
                            sessionId: "codec-session",
                        })
                    ).events,
                },
            ];
        },
    };
}

describe("worker causality public readers", () => {
    it("preserves unrelated event payloads that reuse the diagnostic field name", () => {
        const result = {
            events: [
                {
                    type: "session.start",
                    data: { workerCausality: { extensionPayload: true } },
                },
            ],
        };
        expect(withWorkerCausalityEvents(result)).toBe(result);
        expect(result.events[0].data.workerCausality).toEqual({ extensionPayload: true });
    });

    it.each([
        ["absent events", { method: "session.eventLog.read", state: { phase: "covered" } }],
        ["non-array events", { events: { state: "covered" } }],
    ])("leaves RPC results with $0 unchanged", (_name, result) => {
        expect(withWorkerCausalityEvents(result)).toBe(result);
    });

    it.each(corpus.valid)(
        "$name preserves exact observations and historical product bytes",
        async (test) => {
            const reader = readers();
            const event = test.event !== undefined;
            const wire = structuredClone((test.event ?? test.result) as Wire);
            const original = JSON.stringify(wire);
            expect(payload(await reader.read(wire, event), event).workerCausality).toEqual(
                payload(wire, event).workerCausality
            );
            expect(JSON.stringify(wire)).toBe(original);
            if (event) {
                for (const history of await reader.history(wire)) {
                    expect(
                        payload(history.events[0] as unknown as Wire, true).workerCausality,
                        history.name
                    ).toEqual(payload(wire, true).workerCausality);
                    expect(JSON.stringify(history.events[0]), history.name).toBe(original);
                }
            }

            delete payload(wire, event).workerCausality;
            const baseline = JSON.stringify(await reader.read(wire, event));
            if (event) {
                for (const history of await reader.history(wire)) {
                    expect(JSON.stringify(history.events[0]), `${history.name} absent`).toBe(
                        baseline
                    );
                }
            }
            for (const invalid of corpus.invalid) {
                const candidate = structuredClone(wire);
                payload(candidate, event).workerCausality = invalid.value;
                expect(JSON.stringify(await reader.read(candidate, event)), invalid.name).toBe(
                    baseline
                );
                if (event) {
                    for (const history of await reader.history(candidate)) {
                        expect(
                            JSON.stringify(history.events[0]),
                            `${history.name} ${invalid.name}`
                        ).toBe(baseline);
                    }
                }
            }
        }
    );

    it.each(corpus.boundaries)(
        "$name enforces compact UTF-8 bytes and tolerates unknown fields",
        async (test) => {
            const reader = readers();
            const wire = structuredClone(corpus.valid[0].event as Wire);
            payload(wire, true).workerCausality = test.value;
            const data = payload(await reader.read(wire, true), true);
            expect("workerCausality" in data).toBe(test.accepted);
            expect(data.content).toBe(payload(wire, true).content);
        }
    );

    it("decodes the canonical workflow completion notification with typed fields", async () => {
        const reader = readers();
        const eventId = corpus.workflowCompleted.event.id;
        if (typeof eventId !== "string") {
            throw new Error("workflow completion fixture must have an event ID");
        }
        for (const history of await reader.history(corpus.workflowCompleted.event)) {
            expectWorkflowCompleted(history.events, eventId, history.name);
        }
    });

    it.each(corpus.invalidRaw)("$name rejects invalid Unicode scalar identities", async (test) => {
        const reader = readers();
        const result = {
            sent: true,
            workerCausality: JSON.parse(test.json),
        };
        expect(await reader.read(result, false)).toEqual({ sent: true });
    });
});

describe.skipIf(process.env.COPILOT_RUNTIME_SOURCE !== "checkout")(
    "worker causality persisted history",
    async () => {
        const { copilotClient: client, env } = await createSdkTestContext();

        it("canonicalizes legacy workflow completion through every history reader", async () => {
            const session = await client.createSession({ onPermissionRequest: approveAll });
            const sessionId = session.sessionId;
            await client.rpc.sessions.save({ sessionId });
            await session.disconnect();

            const startId = randomUUID();
            const timestamp = new Date().toISOString();
            const persistedEvent = {
                ...structuredClone(corpus.legacyWorkflowCompleted.persistedEvent),
                parentId: startId,
            };
            const eventId = persistedEvent.id;
            if (typeof eventId !== "string") {
                throw new Error("legacy workflow fixture must have an event ID");
            }
            const copilotHome = env.COPILOT_HOME;
            if (copilotHome === undefined) {
                throw new Error("SDK test context must define COPILOT_HOME");
            }
            await writeFile(
                join(copilotHome, "session-state", sessionId, "events.jsonl"),
                `${JSON.stringify({
                    type: "session.start",
                    data: {
                        sessionId,
                        version: 1,
                        producer: "node-sdk-worker-history-test",
                        copilotVersion: "test",
                        startTime: timestamp,
                    },
                    id: startId,
                    timestamp,
                    parentId: null,
                })}\n${JSON.stringify(persistedEvent)}\n`
            );

            const persisted = await client.rpc.sessions.readPersistedEvents({
                sessionId,
                max: 1000,
            });
            expectWorkflowCompleted(persisted.events, eventId, "sessions.readPersistedEvents");

            const resumed = await client.resumeSession(sessionId, {
                onPermissionRequest: approveAll,
            });
            try {
                expectWorkflowCompleted(await resumed.getEvents(), eventId, "session.getMessages");
                const active = await resumed.rpc.eventLog.read({ max: 1000, waitMs: 0 });
                expectWorkflowCompleted(active.events, eventId, "session.eventLog.read");
            } finally {
                await resumed.disconnect();
            }
        });
    }
);
