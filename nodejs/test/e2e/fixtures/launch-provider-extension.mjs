/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { appendFileSync, existsSync, readFileSync } from "node:fs";
import { join } from "node:path";

// When requested by the test, observe native durability before the first write.
const retentionEventsPath = new URL("./retention-events-path", import.meta.url);
if (existsSync(retentionEventsPath)) {
    const events = readFileSync(readFileSync(retentionEventsPath, "utf8"), "utf8")
        .trim()
        .split("\n")
        .map((line) => JSON.parse(line));
    if (!events.some((event) => event.type === "session.retained")) {
        throw new Error("Package startup preceded durable session retention");
    }
    appendFileSync(new URL("./retained-startup-pids", import.meta.url), `${process.pid}\n`);
}

// This marker precedes all SDK code: a denied candidate must not reach it.
appendFileSync(new URL("./startup-pids", import.meta.url), `${process.pid}\n`);

const { createCanvas } = await import("@github/copilot-sdk");
const { joinSession } = await import("@github/copilot-sdk/extension");

await joinSession({
    canvases: [
        createCanvas({
            id: "launch-marker",
            displayName: "Launch Marker",
            description: "Records a local non-chat operation for SDK integration tests.",
            inputSchema: {
                type: "object",
                properties: { fail: { type: "boolean" } },
            },
            open: (context) => {
                appendFileSync(
                    join(process.env.VSCODE_CANVAS_DATA_DIR, "operations"),
                    `${context.instanceId}\n`
                );
                if (context.input?.fail) {
                    throw new Error("Fixture open failed after saving data");
                }
                return { url: "https://example.test/launch-marker", title: "Launch Marker" };
            },
        }),
    ],
});

appendFileSync(new URL("./ready-pids", import.meta.url), `${process.pid}\n`);
