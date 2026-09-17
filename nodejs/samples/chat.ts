/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { CopilotClient, approveAll } from "../src/index.js";
import * as readline from "node:readline";
import { dirname, join, resolve } from "node:path";
import { fileURLToPath, pathToFileURL } from "node:url";
import { randomUUID } from "node:crypto";
import { createWriteStream, type WriteStream } from "node:fs";
import { mkdir } from "node:fs/promises";
import { once } from "node:events";
import { finished } from "node:stream/promises";
import { parseArgs } from "node:util";
import { createChatEventFormatter } from "./chatEventFormatting.js";

export async function runChat(
    input: NodeJS.ReadableStream = process.stdin,
    output: NodeJS.WritableStream = process.stdout,
    eventsFile?: string,
    enableHydraFusion = false
) {
    const logPath = resolve(
        eventsFile ??
            join(
                dirname(fileURLToPath(import.meta.url)),
                "..",
                "logs",
                `chat-${new Date().toISOString().replaceAll(":", "-")}-${randomUUID()}.jsonl`
            )
    );
    let eventLog: WriteStream | undefined;
    let logFinished: Promise<void> | undefined;
    let loggingFailed = false;
    const startedAt = performance.now();
    let sequence = 0;
    let promptVisible = false;
    const formatEvent = createChatEventFormatter();
    const color =
        "isTTY" in output &&
        output.isTTY === true &&
        process.env.NO_COLOR === undefined &&
        process.env.TERM !== "dumb";
    const write = (text: string) => output.write(text);
    const logEvent = (source: string, event: unknown) => {
        if (!eventLog) throw new Error("SDK event log is not open");
        const now = performance.now();
        const receivedAt = new Date().toISOString();
        eventLog.write(`${JSON.stringify({ receivedAt, source, event })}\n`);
        const display = formatEvent(source, event, {
            receivedAt,
            elapsedMs: now - startedAt,
            sequence: ++sequence,
            color,
        });
        if (display) {
            if (promptVisible) write("\n");
            promptVisible = false;
            write(display);
        }
    };
    const client = new CopilotClient({
        // Session featureFlags alone do not reach every runtime admission gate.
        env: enableHydraFusion
            ? { ...process.env, HYDRAFUSION: "true", HYDRAFUSION_ROLLOUT: "true" }
            : undefined,
        onGitHubTelemetry: (event) => logEvent("sdk.telemetry", event),
    });
    const unsubscribe = client.onLifecycle((event) => logEvent("sdk.lifecycle", event));
    const rl = readline.createInterface({ input, output });
    // Attach immediately so input pasted while the runtime starts is not lost.
    const lines = rl[Symbol.asyncIterator]();
    const prompt = async (question: string) => {
        write(question);
        promptVisible = true;
        const line = await lines.next();
        return line.done ? undefined : line.value;
    };
    const errors: unknown[] = [];

    try {
        await mkdir(dirname(logPath), { recursive: true });
        eventLog = createWriteStream(logPath, { flags: "wx", mode: 0o600 });
        logFinished = finished(eventLog, { cleanup: true }).catch((error: unknown) => {
            loggingFailed = true;
            errors.push(error);
            write(
                `\nSDK event log failed (${logPath}): ${error instanceof Error ? error.message : String(error)}\n`
            );
            rl.close();
        });
        await once(eventLog, "open");
        write(`SDK event log: ${logPath}\n`);
        write(
            "Timeline: Fusion, tool calls, messages, and turn boundaries; full events stay in JSONL.\n" +
                "Message previews are limited to 180 characters. (*streaming*) marks observed streaming output.\n" +
                "Times are UTC; event numbers match JSONL lines (gaps are hidden housekeeping events).\n"
        );
        if (enableHydraFusion) {
            write(
                "HydraFusion development opt-in: experimental mode and local rollout overrides enabled.\n"
            );
        }
        await client.start();
        const models = await client.listModels();
        const pickModel = async (current?: string, selection?: string) => {
            write("\nAvailable models:\n");
            models.forEach((model, index) => {
                write(`  ${index + 1}. ${model.name} (${model.id})\n`);
            });
            write("You can also enter an unlisted model ID (for example, hydrafusion).\n");
            while (!loggingFailed) {
                const answer =
                    selection ??
                    (
                        await prompt(`Model number or ID [${current ?? "runtime default"}]: `)
                    )?.trim();
                selection = undefined;
                if (answer === undefined) return null;
                if (answer === "") return current;
                const model =
                    models.find((model) => model.id === answer) ??
                    (/^[1-9]\d*$/.test(answer) ? models[Number(answer) - 1] : undefined);
                if (model) return model.id;
                if (!/^\d+$/.test(answer)) return answer;
                write(`Unknown model: ${answer}. Choose a listed number or enter a model ID.\n`);
            }
            return null;
        };

        let model = await pickModel();
        if (model === null) return;
        let fusionCompleted = false;
        const session = await client.createSession({
            model,
            enableExperimentalMode: enableHydraFusion ? true : undefined,
            streaming: true,
            includeSubAgentStreamingEvents: true,
            onPermissionRequest: approveAll,
            onEvent: (event) => {
                if (event.type === "session.fusion_completed" && !event.agentId) {
                    fusionCompleted = true;
                }
                logEvent("sdk.session", event);
            },
        });

        write(`\nChat with Copilot - model: ${model ?? "runtime default"}\n`);
        write("Commands: /model [number or ID], /exit. Ctrl+C also exits.\n");

        while (!loggingFailed) {
            const message = await prompt("You: ");
            const command = message?.trim();
            if (message === undefined || command === "/exit") break;
            if (!command) continue;
            if (command === "/model" || command.startsWith("/model ")) {
                const selected = await pickModel(
                    model,
                    command.slice("/model".length).trim() || undefined
                );
                if (selected === null) break;
                if (selected !== undefined && selected !== model) {
                    await session.setModel(selected);
                    model = selected;
                }
                write(`Model: ${model ?? "runtime default"}\n`);
                continue;
            }

            fusionCompleted = false;
            await session.sendAndWait({ prompt: message });
            if (model === "hydrafusion" && !fusionCompleted) {
                logEvent("chat.diagnostic", {
                    type: "fusion.not_executed",
                    requestedModel: model,
                    message:
                        "HydraFusion was requested but no session.fusion_completed event was received for this turn. " +
                        "An assistant reply alone does not prove Fusion ran; the runtime may have selected a concrete fallback. " +
                        (enableHydraFusion
                            ? "Inspect model_resolution_info and session errors for admission or constituent availability failures."
                            : "Restart with --enable-hydrafusion to enable the local development gates."),
                });
            }
        }
    } catch (error) {
        if (!errors.includes(error)) errors.push(error);
    } finally {
        rl.close();
        try {
            errors.push(...(await client.stop()));
        } catch (error) {
            errors.push(error);
        }
        unsubscribe();
        eventLog?.end();
        await logFinished;
        if (errors.length > 0) throw new AggregateError(errors, "Chat failed");
    }
}

async function main() {
    const { values } = parseArgs({
        options: {
            "events-file": { type: "string" },
            "enable-hydrafusion": { type: "boolean" },
            help: { type: "boolean", short: "h" },
        },
    });
    if (values.help) {
        console.log(
            "Usage: npx tsx chat.ts [--events-file <new-file.jsonl>] [--enable-hydrafusion]\n" +
                "Defaults to a unique file in nodejs\\logs. Existing files are never overwritten.\n" +
                "Each line contains receivedAt, source, and the complete event payload.\n" +
                "--enable-hydrafusion enables experimental mode and Fusion rollout overrides for the spawned runtime."
        );
        return;
    }
    await runChat(
        process.stdin,
        process.stdout,
        values["events-file"],
        values["enable-hydrafusion"]
    );
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
    main().catch((error) => {
        console.error(error);
        process.exitCode = 1;
    });
}
