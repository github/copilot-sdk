/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { pathToFileURL } from "node:url";
import {
    CopilotClient,
    RuntimeConnection,
    type LocalModelsConfigureRequest,
    type LocalModelsSnapshot,
} from "../src/index.js";

/** A desktop host can inspect its runtime manager before it creates a chat session. */
export async function inspectLocalModels(client: CopilotClient): Promise<LocalModelsSnapshot> {
    const preferences: LocalModelsConfigureRequest = {
        enabled: true,
        catalogEnabled: false,
        includeDev: false,
        streamerMode: false,
    };
    return client.rpc.localModels.configure(preferences);
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
    const root = process.argv[2];
    if (!root) throw new Error("Usage: inspect-local-models.ts <config-directory> [--watch]");
    const client = new CopilotClient({
        connection: RuntimeConnection.forStdio(),
        baseDirectory: root,
    });
    try {
        await client.start();
        const snapshot = await inspectLocalModels(client);
        console.log(
            JSON.stringify({
                enabled: snapshot.state.enabled,
                modelsDirectory: snapshot.state.machine.modelsDirectory,
                operations: snapshot.state.operations,
            })
        );
        if (process.argv[3] === "--watch") {
            const cancellation = new AbortController();
            const interrupt = () => cancellation.abort();
            process.once("SIGINT", interrupt);
            let revision = snapshot.revision;
            try {
                while (!cancellation.signal.aborted) {
                    const next = await client.rpc.localModels.watch(
                        { afterRevision: revision },
                        cancellation.signal
                    );
                    revision = next.revision;
                    console.log(JSON.stringify({ revision }));
                }
            } catch (error) {
                if (
                    !(
                        cancellation.signal.aborted &&
                        error instanceof Error &&
                        "code" in error &&
                        error.code === -32800
                    )
                )
                    throw error;
            } finally {
                process.off("SIGINT", interrupt);
            }
            const current = await client.rpc.localModels.inspect();
            console.log(JSON.stringify({ watchStopped: true, enabled: current.state.enabled }));
        }
    } finally {
        await client.stop();
    }
}
