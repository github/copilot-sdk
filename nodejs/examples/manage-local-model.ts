/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { readFile } from "node:fs/promises";
import { pathToFileURL } from "node:url";
import {
    CopilotClient,
    RuntimeConnection,
    type AzureMlAcquireFamilyRequest,
    type AzureMlRegistrationResult,
    type ModelProviderSaveConfigurationRequest,
} from "../src/index.js";

/** A desktop model manager can save or restore a model without retaining its source session. */
export async function withLocalModelRegistration<T>(
    client: CopilotClient,
    family: AzureMlAcquireFamilyRequest,
    entries: ModelProviderSaveConfigurationRequest,
    activate: (saved: AzureMlRegistrationResult) => Promise<T>
): Promise<T> {
    const lease = await client.rpc.azureMl.acquireFamily(family);
    const request = { ...family, leaseId: lease.leaseId };
    const errors: unknown[] = [];
    let saved: AzureMlRegistrationResult | undefined;
    let result!: T;
    try {
        saved = await client.rpc.azureMl.registration({ ...request, operation: "upsert", entries });
        if (saved.kind !== "written") {
            throw new Error(saved.detail ?? `Registration ${saved.kind}`);
        }
        result = await activate(saved);
    } catch (error) {
        errors.push(error);
        if (saved?.kind === "written" && saved.before != null) {
            try {
                const restored = await client.rpc.azureMl.registration({
                    ...request,
                    operation: "restore",
                    before: saved.before,
                });
                if (restored.kind !== "written") {
                    throw new Error(restored.detail ?? `Restoration ${restored.kind}`);
                }
            } catch (restorationError) {
                errors.push(restorationError);
            }
        }
    }
    try {
        await client.rpc.azureMl.releaseFamily(request);
    } catch (releaseError) {
        errors.push(releaseError);
    }
    if (errors.length === 1) throw errors[0];
    if (errors.length > 1) throw new AggregateError(errors, "The model transaction failed.");
    return result;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
    const [, , command, root, name, file] = process.argv;
    if (!root || (command !== "inspect" && (command !== "save" || !name || !file))) {
        console.log("Usage: manage-local-model.ts inspect <config-directory>");
        console.log(
            "       manage-local-model.ts save <config-directory> <azureml-model-name> <entries.json>"
        );
    } else {
        const client = new CopilotClient({
            connection: RuntimeConnection.forStdio(),
            baseDirectory: root,
        });
        try {
            await client.start();
            if (command === "inspect") {
                console.log(JSON.stringify(await client.rpc.azureMl.hardware(), null, 2));
            } else {
                const entries: ModelProviderSaveConfigurationRequest = JSON.parse(
                    await readFile(file!, "utf8")
                );
                const output = await withLocalModelRegistration(
                    client,
                    { registry: "azureml", name: name! },
                    entries,
                    async (saved) => JSON.stringify(saved, null, 2)
                );
                console.log(output);
            }
        } finally {
            await client.stop();
        }
    }
}
