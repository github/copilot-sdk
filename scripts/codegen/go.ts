/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Go code generator for session-events types.
 */

import { execFile } from "child_process";
import fs from "fs/promises";
import { promisify } from "util";
import type { JSONSchema7 } from "json-schema";
import { FetchingJSONSchemaStore, InputData, JSONSchemaInput, quicktype } from "quicktype-core";
import { getSessionEventsSchemaPath, postProcessSchema, writeGeneratedFile } from "./utils.js";

const execFileAsync = promisify(execFile);

async function formatGoFile(filePath: string): Promise<void> {
    try {
        await execFileAsync("go", ["fmt", filePath]);
        console.log(`  ✓ Formatted with go fmt`);
    } catch {
        // go fmt not available, skip
    }
}

export async function generate(schemaPath?: string): Promise<void> {
    console.log("Go: generating session-events...");

    const resolvedPath = schemaPath ?? (await getSessionEventsSchemaPath());
    const schema = JSON.parse(await fs.readFile(resolvedPath, "utf-8")) as JSONSchema7;
    const resolvedSchema = (schema.definitions?.SessionEvent as JSONSchema7) || schema;
    const processed = postProcessSchema(resolvedSchema);

    const schemaInput = new JSONSchemaInput(new FetchingJSONSchemaStore());
    await schemaInput.addSource({ name: "SessionEvent", schema: JSON.stringify(processed) });

    const inputData = new InputData();
    inputData.addInput(schemaInput);

    const result = await quicktype({
        inputData,
        lang: "go",
        rendererOptions: { package: "copilot" },
    });

    const banner = `// AUTO-GENERATED FILE - DO NOT EDIT
// Generated from: session-events.schema.json

`;

    const outPath = await writeGeneratedFile("go/generated_session_events.go", banner + result.lines.join("\n"));
    console.log(`  ✓ ${outPath}`);

    await formatGoFile(outPath);
}

generate(process.argv[2]).catch((err) => {
    console.error("Go generation failed:", err);
    process.exit(1);
});
