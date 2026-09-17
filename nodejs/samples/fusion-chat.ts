/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { randomUUID } from "node:crypto";
import { once } from "node:events";
import { createWriteStream, type WriteStream } from "node:fs";
import { mkdir } from "node:fs/promises";
import { dirname, join, resolve } from "node:path";
import * as readline from "node:readline";
import { finished } from "node:stream/promises";
import { parseArgs } from "node:util";
import { fileURLToPath, pathToFileURL } from "node:url";
import { approveAll, CopilotClient, type SessionEvent } from "../src/index.js";
import { FusionChatRenderer } from "./fusionChatRenderer.js";

export const DEFAULT_TURN_TIMEOUT_MS = 5 * 60_000;

export function resolveTurnTimeoutMs(value: string | undefined): number {
    if (value === undefined) return DEFAULT_TURN_TIMEOUT_MS;
    const seconds = Number(value);
    if (!Number.isSafeInteger(seconds) || seconds <= 0) {
        throw new Error("--timeout-seconds must be a positive integer");
    }
    return seconds * 1000;
}

export async function runFusionChat(
    input: NodeJS.ReadableStream = process.stdin,
    output: NodeJS.WritableStream = process.stdout,
    eventsFile?: string,
    debug = false,
    turnTimeoutMs = DEFAULT_TURN_TIMEOUT_MS
): Promise<void> {
    const logPath = resolve(
        eventsFile ??
            join(
                dirname(fileURLToPath(import.meta.url)),
                "..",
                "logs",
                `fusion-chat-${new Date().toISOString().replaceAll(":", "-")}-${randomUUID()}.jsonl`
            )
    );
    const color =
        "isTTY" in output &&
        output.isTTY === true &&
        process.env.NO_COLOR === undefined &&
        process.env.TERM !== "dumb";
    const renderer = new FusionChatRenderer(output, {
        color,
        debug,
        interactive: "isTTY" in output && output.isTTY === true,
    });
    const errors: unknown[] = [];
    let eventLog: WriteStream | undefined;
    let logFinished: Promise<void> | undefined;

    const logEvent = (event: SessionEvent) => {
        if (!eventLog) throw new Error("Fusion chat event log is not open");
        eventLog.write(
            `${JSON.stringify({
                receivedAt: new Date().toISOString(),
                source: "sdk.session",
                event,
            })}\n`
        );
        renderer.handle(event);
    };

    const client = new CopilotClient({
        env: { ...process.env, HYDRAFUSION: "true", HYDRAFUSION_ROLLOUT: "true" },
    });
    const rl = readline.createInterface({ input, output });
    const lines = rl[Symbol.asyncIterator]();
    rl.on("SIGINT", () => rl.close());

    try {
        await mkdir(dirname(logPath), { recursive: true });
        eventLog = createWriteStream(logPath, { flags: "wx", mode: 0o600 });
        logFinished = finished(eventLog, { cleanup: true }).catch((error: unknown) => {
            errors.push(error);
            rl.close();
        });
        await once(eventLog, "open");

        output.write("\nHydraFusion SDK chat POC\n");
        output.write("========================\n");
        output.write(`Full event log: ${logPath}\n`);
        output.write("Model: hydrafusion (experimental local opt-in enabled)\n");
        output.write(`Turn timeout: ${turnTimeoutMs / 1000}s\n`);
        if (debug) output.write("Debug details: enabled\n");
        output.write("Commands: /exit. Tool permissions are auto-approved for this local POC.\n\n");

        await client.start();
        const session = await client.createSession({
            model: "hydrafusion",
            enableExperimentalMode: true,
            streaming: true,
            includeSubAgentStreamingEvents: true,
            onPermissionRequest: approveAll,
            onEvent: logEvent,
        });

        while (true) {
            output.write("You > ");
            const line = await lines.next();
            if (line.done || line.value.trim() === "/exit") break;
            if (!line.value.trim()) continue;
            await session.sendAndWait({ prompt: line.value }, turnTimeoutMs);
            renderer.finish();
            output.write("\n");
        }
    } catch (error) {
        errors.push(error);
    } finally {
        renderer.finish();
        rl.close();
        try {
            errors.push(...(await client.stop()));
        } catch (error) {
            errors.push(error);
        }
        eventLog?.end();
        await logFinished;
        if (errors.length > 0) throw new AggregateError(errors, "Fusion chat failed");
    }
}

async function main(): Promise<void> {
    const { values } = parseArgs({
        options: {
            "events-file": { type: "string" },
            debug: { type: "boolean" },
            "timeout-seconds": { type: "string" },
            help: { type: "boolean", short: "h" },
        },
    });
    if (values.help) {
        console.log(
            "Usage: npx tsx samples/fusion-chat.ts [--debug] [--timeout-seconds <seconds>] [--events-file <new-file.jsonl>]\n" +
                "Runs an interactive HydraFusion session against COPILOT_CLI_PATH.\n" +
                "--debug shows the full phase plan, phase metadata, and commit selection.\n" +
                `Turn timeout defaults to ${DEFAULT_TURN_TIMEOUT_MS / 1000} seconds.`
        );
        return;
    }
    await runFusionChat(
        process.stdin,
        process.stdout,
        values["events-file"],
        values.debug,
        resolveTurnTimeoutMs(values["timeout-seconds"])
    );
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
    main().catch((error) => {
        console.error(error);
        process.exitCode = 1;
    });
}
