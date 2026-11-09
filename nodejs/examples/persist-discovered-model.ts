/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { pathToFileURL } from "node:url";
import {
    CopilotClient,
    RuntimeConnection,
    toSavedProviderConfiguration,
    type ModelProviderSaveConfigurationRequest,
} from "../src/index.js";

/** Saves a discovered model without replacing an existing provider's credentials. */
export async function persistDiscoveredModel(
    client: CopilotClient,
    configuration: ModelProviderSaveConfigurationRequest
): Promise<string> {
    const outcome = await client.rpc.providers.config.addModel({
        provider: toSavedProviderConfiguration(configuration.provider),
        model: configuration.model,
    });
    return JSON.stringify(outcome, null, 2);
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
    const baseDirectory = process.argv[2];
    if (!baseDirectory) {
        throw new Error("Usage: persist-discovered-model.ts <base-directory> [stdio|inprocess]");
    }
    const client = new CopilotClient({
        baseDirectory,
        connection:
            process.argv[3] === "inprocess"
                ? RuntimeConnection.forInProcess()
                : RuntimeConnection.forStdio(),
    });
    try {
        await client.start();
        const output = await persistDiscoveredModel(client, {
            provider: {
                name: "ollama-local",
                type: "openai",
                baseUrl: "http://127.0.0.1:11434/v1",
            },
            model: { id: "llama3", provider: "ollama-local", name: "Llama 3" },
        });
        console.log(output);
    } finally {
        await client.stop();
    }
}
