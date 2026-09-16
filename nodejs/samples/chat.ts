/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { CopilotClient, approveAll } from "../src/index.js";
import * as readline from "node:readline";
import { resolve } from "node:path";
import { pathToFileURL } from "node:url";

export async function runChat(
    input: NodeJS.ReadableStream = process.stdin,
    output: NodeJS.WritableStream = process.stdout
) {
    const write = (text: string) => output.write(text);
    const logEvent = (source: string, event: unknown) => {
        write(`\n[${source}]\n${JSON.stringify(event, null, 2)}\n`);
    };
    const client = new CopilotClient({
        onGitHubTelemetry: (event) => logEvent("sdk.telemetry", event),
    });
    const unsubscribe = client.onLifecycle((event) => logEvent("sdk.lifecycle", event));
    const rl = readline.createInterface({ input, output });
    // Attach immediately so input pasted while the runtime starts is not lost.
    const lines = rl[Symbol.asyncIterator]();
    const prompt = async (question: string) => {
        write(question);
        const line = await lines.next();
        return line.done ? undefined : line.value;
    };
    const errors: unknown[] = [];

    try {
        write(
            "Full event payloads are printed, including potentially sensitive tool and telemetry data.\n"
        );
        await client.start();
        const models = await client.listModels();
        const pickModel = async (current?: string, selection?: string) => {
            write("\nAvailable models:\n");
            models.forEach((model, index) => {
                write(`  ${index + 1}. ${model.name} (${model.id})\n`);
            });
            write("You can also enter an unlisted model ID (for example, hydrafusion).\n");
            while (true) {
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
        };

        let model = await pickModel();
        if (model === null) return;
        const session = await client.createSession({
            model,
            streaming: true,
            includeSubAgentStreamingEvents: true,
            onPermissionRequest: approveAll,
            onEvent: (event) => logEvent("sdk.session", event),
        });

        write(`\nChat with Copilot - model: ${model ?? "runtime default"}\n`);
        write("Commands: /model [number or ID], /exit. Ctrl+C also exits.\n");

        while (true) {
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

            const reply = await session.sendAndWait({ prompt: message });
            if (reply) write(`\nAssistant: ${reply.data.content}\n\n`);
        }
    } catch (error) {
        errors.push(error);
    } finally {
        rl.close();
        try {
            errors.push(...(await client.stop()));
        } catch (error) {
            errors.push(error);
        }
        unsubscribe();
        if (errors.length > 0) throw new AggregateError(errors, "Chat failed");
    }
}

if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
    runChat().catch((error) => {
        console.error(error);
        process.exitCode = 1;
    });
}
