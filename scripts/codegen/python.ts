/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Python code generator for session-events types.
 */

import fs from "fs/promises";
import type { JSONSchema7 } from "json-schema";
import { FetchingJSONSchemaStore, InputData, JSONSchemaInput, quicktype } from "quicktype-core";
import { getSessionEventsSchemaPath, postProcessSchema, writeGeneratedFile } from "./utils.js";

export async function generate(schemaPath?: string): Promise<void> {
    console.log("Python: generating session-events...");

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
        lang: "python",
        rendererOptions: { "python-version": "3.7" },
    });

    let code = result.lines.join("\n");

    // Fix dataclass field ordering (Any fields need defaults)
    code = code.replace(/: Any$/gm, ": Any = None");

    // Add UNKNOWN enum value for forward compatibility
    code = code.replace(
        /^(class SessionEventType\(Enum\):.*?)(^\s*\n@dataclass)/ms,
        `$1    # UNKNOWN is used for forward compatibility
    UNKNOWN = "unknown"

    @classmethod
    def _missing_(cls, value: object) -> "SessionEventType":
        """Handle unknown event types gracefully for forward compatibility."""
        return cls.UNKNOWN

$2`
    );

    const banner = `"""
AUTO-GENERATED FILE - DO NOT EDIT
Generated from: session-events.schema.json
"""

`;

    const outPath = await writeGeneratedFile("python/copilot/generated/session_events.py", banner + code);
    console.log(`  ✓ ${outPath}`);
}

generate(process.argv[2]).catch((err) => {
    console.error("Python generation failed:", err);
    process.exit(1);
});
