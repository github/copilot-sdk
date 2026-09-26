/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { readFileSync } from "node:fs";
import { PassThrough } from "node:stream";
import {
    createMessageConnection,
    StreamMessageReader,
    StreamMessageWriter,
} from "vscode-jsonrpc/node.js";
import { describe, expect, it, onTestFinished } from "vitest";
import { CopilotSession } from "../src/session.js";
import type { SessionEvent } from "../src/index.js";

type Wire = Record<string, unknown>;
const corpus = JSON.parse(
    readFileSync(new URL("../../test/worker-causality.json", import.meta.url), "utf8")
) as {
    valid: { name: string; event?: Wire; result?: Wire }[];
    invalid: { name: string; value: unknown }[];
    boundaries: { name: string; value: unknown; accepted: boolean }[];
    workflowCompleted: { event: Wire };
};

function payload(value: Wire, event: boolean): Wire {
    return event ? (value.data as Wire) : value;
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
        async history(wire: Wire): Promise<SessionEvent[][]> {
            reply = { events: [wire], cursor: "cursor", hasMore: false, cursorStatus: "ok" };
            return [await session.getEvents(), (await session.rpc.eventLog.read({})).events];
        },
    };
}

describe("worker causality public readers", () => {
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

            delete payload(wire, event).workerCausality;
            const baseline = JSON.stringify(await reader.read(wire, event));
            for (const invalid of corpus.invalid) {
                const candidate = structuredClone(wire);
                payload(candidate, event).workerCausality = invalid.value;
                expect(JSON.stringify(await reader.read(candidate, event)), invalid.name).toBe(
                    baseline
                );
                if (event) {
                    for (const events of await reader.history(candidate)) {
                        expect(JSON.stringify(events[0]), invalid.name).toBe(baseline);
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
        const event = corpus.workflowCompleted.event as unknown as SessionEvent;
        expect(event.type).toBe("system.notification");
        if (event.type !== "system.notification") throw new Error("expected system.notification");
        expect(event.data.kind.type).toBe("workflow_completed");
        if (event.data.kind.type !== "workflow_completed") {
            throw new Error("expected workflow_completed");
        }
        expect(event.data.kind.workflowName).toBe("fix-ci");
        expect(event.data.kind.runId).toBe("run-1");
        expect(event.data.kind.status).toBe("completed");
        expect(event.data.kind.consumedSubagents).toBe(1);
    });
});
