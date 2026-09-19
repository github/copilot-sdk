/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

import { createHash } from "node:crypto";
import { fileURLToPath } from "node:url";
import { loadSchemaJson, stableStringify } from "./utils.js";

export interface SchemaRevision {
    replacements: {
        path: string[];
        /** Hash of the released predecessor, or null if the field must be absent. */
        beforeSha256: string | null;
        value: unknown;
    }[];
    insertions: {
        path: string[];
        after: string;
        value: { $ref: string; description: string };
    }[];
}

interface CanvasSchemaRevisions {
    api: SchemaRevision;
    sessionEvents: SchemaRevision;
}

export function schemaFingerprint(value: unknown): string {
    return createHash("sha256").update(stableStringify(value)).digest("hex");
}

export async function loadCanvasSchemaRevisions(): Promise<CanvasSchemaRevisions> {
    return loadSchemaJson<CanvasSchemaRevisions>(
        fileURLToPath(new URL("./experimental/canvas.schema.json", import.meta.url))
    );
}

function isSchemaObject(value: unknown): value is Record<string, unknown> {
    return typeof value === "object" && value !== null && !Array.isArray(value);
}

function schemaSlot(schema: unknown, path: readonly string[]) {
    const key = path.at(-1);
    if (!key || path.some((part) => ["__proto__", "prototype", "constructor"].includes(part))) {
        throw new Error(`Invalid canvas schema path: ${path.join("/")}`);
    }
    let parent = schema;
    for (const part of path.slice(0, -1)) {
        if (!isSchemaObject(parent) || !Object.hasOwn(parent, part)) {
            throw new Error(`Missing canvas schema parent: ${path.join("/")}`);
        }
        parent = parent[part];
    }
    if (!isSchemaObject(parent)) {
        throw new Error(`Invalid canvas schema parent: ${path.join("/")}`);
    }
    return { parent, key };
}

function revisionMismatch(path: readonly string[]): Error {
    return new Error(
        `Canvas schema revision mismatch at ${path.join("/")}. ` +
        "The released contract changed; review or remove experimental/canvas.schema.json instead of overriding it."
    );
}

/** Applies only the reviewed predecessor-to-candidate changes, without mutating the release input. */
export function applySchemaRevision<T>(schema: T, revision: SchemaRevision): T {
    const result = structuredClone(schema);
    for (const replacement of revision.replacements) {
        const { parent, key } = schemaSlot(result, replacement.path);
        const currentHash = Object.hasOwn(parent, key) ? schemaFingerprint(parent[key]) : null;
        if (currentHash === schemaFingerprint(replacement.value)) {
            continue;
        }
        if (currentHash !== replacement.beforeSha256) {
            throw revisionMismatch(replacement.path);
        }
        parent[key] = structuredClone(replacement.value);
    }
    for (const insertion of revision.insertions) {
        const { parent, key } = schemaSlot(result, insertion.path);
        const value = parent[key];
        if (!Array.isArray(value)) {
            throw new Error(`Missing canvas event union: ${insertion.path.join("/")}`);
        }
        const variants: unknown[] = value;
        const existing = variants.filter(
            (variant) => isSchemaObject(variant) && variant.$ref === insertion.value.$ref
        );
        if (existing.length > 0) {
            if (existing.length !== 1 || schemaFingerprint(existing[0]) !== schemaFingerprint(insertion.value)) {
                throw revisionMismatch(insertion.path);
            }
            continue;
        }
        const anchor = variants.findIndex(
            (variant) => isSchemaObject(variant) && variant.$ref === insertion.after
        );
        if (anchor === -1) {
            throw new Error(`Missing canvas event insertion anchor: ${insertion.after}`);
        }
        variants.splice(anchor + 1, 0, structuredClone(insertion.value));
    }
    return result;
}
