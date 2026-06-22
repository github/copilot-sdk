/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Shared utilities for code generation - schema loading, file I/O, schema processing.
 */

import { execFile } from "child_process";
import fs from "fs/promises";
import type { JSONSchema7, JSONSchema7Definition } from "json-schema";
import path from "path";
import { fileURLToPath } from "url";
import { promisify } from "util";

export const execFileAsync = promisify(execFile);

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

/** Root of the copilot-sdk repo */
export const REPO_ROOT = path.resolve(__dirname, "../..");

/** Event types to exclude from generation (internal/legacy types) */
export const EXCLUDED_EVENT_TYPES = new Set(["session.import_legacy"]);

export interface DefinitionCollections {
    definitions?: Record<string, JSONSchema7Definition>;
    $defs?: Record<string, JSONSchema7Definition>;
}

export type EnumValueDescriptions = Record<string, string>;

export interface SessionEventEnvelopeProperty {
    name: string;
    schema: JSONSchema7;
    required: boolean;
}

export interface JSONSchema7WithDefs extends JSONSchema7, DefinitionCollections {}

export type SchemaWithSharedDefinitions<T extends JSONSchema7 = JSONSchema7> = T & {
    definitions: Record<string, JSONSchema7Definition>;
    $defs: Record<string, JSONSchema7Definition>;
};
// ── Schema paths ────────────────────────────────────────────────────────────

const SDK_NODE_MODULES = path.join(REPO_ROOT, "nodejs/node_modules");

/**
 * Resolve a JSON schema shipped by the `@github/copilot` CLI package.
 *
 * The CLI package layout changed in 1.0.64-1: the umbrella `@github/copilot`
 * package became a thin loader and its bundled assets (including the JSON
 * schemas) moved into the platform-specific packages installed as optional
 * dependencies, e.g. `@github/copilot-linux-x64` or `@github/copilot-win32-x64`.
 *
 * To support both layouts we look in the umbrella package first (older
 * versions) and then in whichever platform package was installed for the
 * current host.
 */
async function resolveCopilotSchemaPath(nodeModulesDir: string, fileName: string): Promise<string> {
    const candidates = [path.join(nodeModulesDir, "@github/copilot/schemas", fileName)];

    const githubScopeDir = path.join(nodeModulesDir, "@github");
    try {
        for (const entry of await fs.readdir(githubScopeDir)) {
            if (entry.startsWith("copilot-")) {
                candidates.push(path.join(githubScopeDir, entry, "schemas", fileName));
            }
        }
    } catch (err) {
        const code = (err as NodeJS.ErrnoException).code;
        if (code !== "ENOENT" && code !== "ENOTDIR") {
            throw err;
        }
        // @github scope directory may not exist yet; fall through to the error below.
    }

    for (const candidate of candidates) {
        try {
            await fs.access(candidate);
            return candidate;
        } catch {
            // Try the next candidate.
        }
    }

    throw new Error(
        `${fileName} not found under ${githubScopeDir}. Run 'npm ci' in nodejs/ first.`
    );
}

export async function getSessionEventsSchemaPath(): Promise<string> {
    return resolveCopilotSchemaPath(SDK_NODE_MODULES, "session-events.schema.json");
}

export async function getApiSchemaPath(cliArg?: string): Promise<string> {
    if (cliArg) return cliArg;
    return resolveCopilotSchemaPath(SDK_NODE_MODULES, "api.schema.json");
}

// ── Brand casing normalization ──────────────────────────────────────────────

/**
 * Correct the GitHub brand casing in a generated identifier or documentation
 * string. Some schema titles/definition names and value-derived identifiers
 * render the brand as "Github"; the correct casing is "GitHub". Wire/protocol
 * values (e.g. "github", "github_reference") are lowercase and therefore left
 * untouched. The replacement is idempotent: already-correct "GitHub" contains a
 * capital "H" and no "Github" substring, so it is unaffected.
 */
export function fixBrandCasing(value: string): string {
    return value.replace(/Github/g, "GitHub");
}

const BRAND_NORMALIZED_STRING_KEYS = new Set(["title", "description", "markdownDescription"]);

/**
 * Recursively normalize GitHub brand casing within a parsed JSON schema:
 * - keys of `definitions` / `$defs` maps,
 * - `$ref` pointers (definition-name segment only),
 * - documentation strings (`title`, `description`, `markdownDescription`).
 *
 * Wire-level string values (`const`, `enum`, `default`, examples, etc.) are left
 * untouched so protocol values such as "github" remain lowercase. The schema is
 * mutated in place and also returned for convenience.
 */
export function normalizeSchemaBrandCasing<T>(schema: T): T {
    normalizeBrandCasingNode(schema);
    return schema;
}

function normalizeBrandCasingNode(node: unknown): void {
    if (Array.isArray(node)) {
        for (const item of node) normalizeBrandCasingNode(item);
        return;
    }
    if (node === null || typeof node !== "object") return;
    const obj = node as Record<string, unknown>;

    for (const defsKey of ["definitions", "$defs"] as const) {
        const defs = obj[defsKey];
        if (defs && typeof defs === "object" && !Array.isArray(defs)) {
            renameBrandDefinitionKeys(defs as Record<string, unknown>);
        }
    }

    for (const [key, value] of Object.entries(obj)) {
        if (typeof value === "string") {
            if (key === "$ref") {
                obj[key] = fixBrandRef(value);
            } else if (BRAND_NORMALIZED_STRING_KEYS.has(key)) {
                obj[key] = fixBrandCasing(value);
            }
        } else {
            normalizeBrandCasingNode(value);
        }
    }
}

/** Apply brand-casing only to the definition-name segment of a `$ref`. */
function fixBrandRef(ref: string): string {
    const lastSlash = ref.lastIndexOf("/");
    if (lastSlash === -1) return ref;
    const prefix = ref.slice(0, lastSlash + 1);
    const name = ref.slice(lastSlash + 1);
    return `${prefix}${fixBrandCasing(name)}`;
}

function renameBrandDefinitionKeys(defs: Record<string, unknown>): void {
    for (const oldKey of Object.keys(defs)) {
        const newKey = fixBrandCasing(oldKey);
        if (newKey === oldKey) continue;
        if (newKey in defs && stableStringify(defs[newKey]) !== stableStringify(defs[oldKey])) {
            throw new Error(
                `Brand-casing normalization collision: "${oldKey}" -> "${newKey}" but a different definition already exists under "${newKey}".`
            );
        }
        defs[newKey] = defs[oldKey];
        delete defs[oldKey];
    }
}

/** Load a JSON schema file and normalize GitHub brand casing in titles, refs, and definition keys. */
export async function loadSchemaJson<T>(filePath: string): Promise<T> {
    const parsed = JSON.parse(await fs.readFile(filePath, "utf-8")) as T;
    return normalizeSchemaBrandCasing(parsed);
}

// ── Schema processing ───────────────────────────────────────────────────────

/**
 * Post-process JSON Schema for code generators that expect enum-style literals.
 * Converts boolean const values to enum.
 */
export function postProcessSchema(schema: JSONSchema7): JSONSchema7 {
    if (typeof schema !== "object" || schema === null) return schema;

    const processed = { ...schema } as JSONSchema7WithDefs;

    if ("const" in processed && typeof processed.const === "boolean") {
        processed.enum = [processed.const];
        delete processed.const;
    }

    if (processed.properties) {
        const newProps: Record<string, JSONSchema7Definition> = {};
        for (const [key, value] of Object.entries(processed.properties).sort(([a], [b]) => a.localeCompare(b))) {
            newProps[key] = typeof value === "object" ? postProcessSchema(value as JSONSchema7) : value;
        }
        processed.properties = newProps;
    }

    if (processed.items) {
        if (typeof processed.items === "object" && !Array.isArray(processed.items)) {
            processed.items = postProcessSchema(processed.items as JSONSchema7);
        } else if (Array.isArray(processed.items)) {
            processed.items = processed.items.map((item) =>
                typeof item === "object" ? postProcessSchema(item as JSONSchema7) : item
            ) as JSONSchema7Definition[];
        }
    }

    for (const combiner of ["anyOf", "allOf", "oneOf"] as const) {
        if (processed[combiner]) {
            processed[combiner] = processed[combiner]!.map((item) =>
                typeof item === "object" ? postProcessSchema(item as JSONSchema7) : item
            ) as JSONSchema7Definition[];
        }
    }

    const { definitions, $defs } = collectDefinitionCollections(processed as Record<string, unknown>);
    let newDefs: Record<string, JSONSchema7Definition> | undefined;
    if (Object.keys(definitions).length > 0) {
        newDefs = {};
        for (const [key, value] of Object.entries(definitions)) {
            newDefs[key] = typeof value === "object" ? postProcessSchema(value as JSONSchema7) : value;
        }
        processed.definitions = newDefs;
    }
    let newDraftDefs: Record<string, JSONSchema7Definition> | undefined;
    if (Object.keys($defs).length > 0) {
        newDraftDefs = {};
        for (const [key, value] of Object.entries($defs)) {
            newDraftDefs[key] = typeof value === "object" ? postProcessSchema(value as JSONSchema7) : value;
        }
        processed.$defs = newDraftDefs;
    }
    if (processed.definitions && !processed.$defs) {
        processed.$defs = { ...(newDefs ?? processed.definitions) };
    } else if (processed.$defs && !processed.definitions) {
        processed.definitions = { ...processed.$defs };
    }

    if (typeof processed.additionalProperties === "object") {
        processed.additionalProperties = postProcessSchema(processed.additionalProperties as JSONSchema7);
    }

    return processed;
}

/**
 * Strip boolean literal constraints (`const: true/false`, `enum: [true]`, `enum: [false]`)
 * from a schema, recursively. quicktype's Python renderer attempts to derive
 * identifier names from enum values; deriving a name from a boolean throws inside
 * `snakeNameStyle` (TypeError: s.codePointAt is not a function).
 *
 * The literal narrowing isn't expressible in Python anyway, so we drop it and
 * keep just `type: "boolean"`. Other codegen runs on the original schema.
 */
export function stripBooleanLiterals<T>(schema: T): T {
    if (typeof schema !== "object" || schema === null) return schema;
    if (Array.isArray(schema)) {
        return schema.map((item) => stripBooleanLiterals(item)) as unknown as T;
    }
    const result: Record<string, unknown> = {};
    const src = schema as unknown as Record<string, unknown>;
    const isBooleanType = src.type === "boolean";
    for (const [key, value] of Object.entries(src)) {
        if (isBooleanType && key === "const" && typeof value === "boolean") continue;
        if (
            isBooleanType &&
            key === "enum" &&
            Array.isArray(value) &&
            value.every((v) => typeof v === "boolean")
        ) {
            continue;
        }
        result[key] = stripBooleanLiterals(value);
    }
    return result as T;
}

/**
 * Normalize schema defects where a required property with a `$ref` to an object type
 * has a description explicitly mentioning "null" as a valid value.
 *
 * In JSON Schema, `required` only means the key must be present — it doesn't prevent
 * the value from being null. Some schemas mark properties as required but describe them
 * as nullable (e.g., "Currently selected agent, or null if using the default").
 *
 * This function converts such properties from:
 *   `{ "$ref": "#/definitions/Foo", "description": "...null..." }`
 * to:
 *   `{ "anyOf": [{ "$ref": "#/definitions/Foo" }, { "type": "null" }], "description": "...null..." }`
 *
 * This makes all downstream codegen (Go, C#, Python/quicktype, TypeScript) correctly
 * emit nullable/optional types without per-language heuristics.
 */
export function normalizeNullableRequiredRefs(schema: JSONSchema7): JSONSchema7 {
    if (typeof schema !== "object" || schema === null) return schema;

    const processed = { ...schema };

    if (processed.properties && processed.required) {
        const requiredSet = new Set(processed.required);
        const newProps: Record<string, JSONSchema7Definition> = {};
        const newRequired = [...processed.required];

        for (const [key, value] of Object.entries(processed.properties)) {
            if (typeof value !== "object" || value === null) {
                newProps[key] = value;
                continue;
            }
            const prop = value as JSONSchema7;
            if (
                requiredSet.has(key) &&
                prop.$ref &&
                typeof prop.description === "string" &&
                /\bnull\b/i.test(prop.description)
            ) {
                // Convert to anyOf: [$ref, null] and remove from required
                const { $ref, ...rest } = prop;
                newProps[key] = {
                    ...rest,
                    anyOf: [{ $ref }, { type: "null" as const }],
                };
                const idx = newRequired.indexOf(key);
                if (idx !== -1) newRequired.splice(idx, 1);
            } else {
                newProps[key] = normalizeNullableRequiredRefs(prop);
            }
        }

        processed.properties = newProps;
        processed.required = newRequired;
    }

    // Recurse into nested schemas
    if (processed.items) {
        if (typeof processed.items === "object" && !Array.isArray(processed.items)) {
            processed.items = normalizeNullableRequiredRefs(processed.items as JSONSchema7);
        }
    }
    for (const combiner of ["anyOf", "allOf", "oneOf"] as const) {
        if (processed[combiner]) {
            processed[combiner] = processed[combiner]!.map((item) =>
                typeof item === "object" ? normalizeNullableRequiredRefs(item as JSONSchema7) : item
            ) as JSONSchema7Definition[];
        }
    }

    return processed;
}

// ── File output ─────────────────────────────────────────────────────────────

export async function writeGeneratedFile(relativePath: string, content: string): Promise<string> {
    const fullPath = path.join(REPO_ROOT, relativePath);
    await fs.mkdir(path.dirname(fullPath), { recursive: true });
    await fs.writeFile(fullPath, content, "utf-8");
    return fullPath;
}

// ── RPC schema types ────────────────────────────────────────────────────────

export interface RpcMethod {
    rpcMethod: string;
    description?: string;
    params: JSONSchema7 | null;
    result: JSONSchema7 | null;
    stability?: string;
    visibility?: string;
    deprecated?: boolean;
}

export function getRpcSchemaTypeName(schema: JSONSchema7 | null | undefined, fallback: string): string {
    if (typeof schema?.title === "string") return schema.title;
    return fallback;
}

/**
 * Returns true if the schema represents an object with properties (i.e., a type that should
 * be generated as a class/struct/dataclass). Returns false for enums, primitives, arrays,
 * and other non-object schemas.
 */
export function isObjectSchema(schema: JSONSchema7 | null | undefined): boolean {
    if (!schema) return false;
    if (schema.type === "object" && schema.properties) return true;
    return false;
}

/**
 * Returns true if the schema represents a void/null result (type: "null").
 * These carry a title for languages that need a named empty type (e.g., Go)
 * but should be treated as void in other languages.
 */
export function isVoidSchema(schema: JSONSchema7 | null | undefined): boolean {
    if (!schema) return true;
    return schema.type === "null";
}

/**
 * If the schema is a nullable anyOf (anyOf: [nullLike, T] or [T, nullLike]),
 * returns the non-null inner schema. Recognizes both `{ type: "null" }` and
 * `{ not: {} }` (zod-to-json-schema 2019-09 format for undefined).
 * Returns undefined if the schema is not a nullable wrapper.
 */
export function getNullableInner(schema: JSONSchema7): JSONSchema7 | undefined {
    if (!schema.anyOf || !Array.isArray(schema.anyOf) || schema.anyOf.length !== 2) return undefined;
    const [a, b] = schema.anyOf;
    if (isNullLike(a) && !isNullLike(b)) return b as JSONSchema7;
    if (isNullLike(b) && !isNullLike(a)) return a as JSONSchema7;
    return undefined;
}

function isNullLike(s: unknown): boolean {
    if (!s || typeof s !== "object") return false;
    const obj = s as Record<string, unknown>;
    if (obj.type === "null") return true;
    if ("not" in obj && typeof obj.not === "object" && obj.not !== null && Object.keys(obj.not).length === 0) return true;
    return false;
}

export function cloneSchemaForCodegen<T>(value: T): T {
    if (Array.isArray(value)) {
        return value.map((item) => cloneSchemaForCodegen(item)) as T;
    }

    if (value && typeof value === "object") {
        const source = value as Record<string, unknown>;
        const result: Record<string, unknown> = {};

        for (const [key, child] of Object.entries(source)) {
            result[key] = cloneSchemaForCodegen(child);
        }

        return result as T;
    }

    return value;
}

export function getEnumValueDescriptions(schema: JSONSchema7 | null | undefined): EnumValueDescriptions | undefined {
    if (!schema || typeof schema !== "object") return undefined;

    const rawDescriptions = (schema as Record<string, unknown>)["x-enumDescriptions"];
    if (!rawDescriptions || typeof rawDescriptions !== "object" || Array.isArray(rawDescriptions)) return undefined;

    const descriptions: EnumValueDescriptions = {};
    for (const [value, description] of Object.entries(rawDescriptions)) {
        if (typeof description !== "string") continue;

        const trimmedDescription = description.trim();
        if (trimmedDescription.length > 0) {
            descriptions[value] = trimmedDescription;
        }
    }

    return Object.keys(descriptions).length > 0 ? descriptions : undefined;
}

const INT32_MIN = -(2 ** 31);
const INT32_MAX = 2 ** 31 - 1;

function isIntegerValue(value: unknown): value is number {
    return Number.isInteger(value);
}

export function isIntegerSchemaBoundedToInt32(schema: JSONSchema7): boolean {
    return (
        isIntegerValue(schema.minimum) &&
        isIntegerValue(schema.maximum) &&
        schema.minimum >= INT32_MIN &&
        schema.maximum <= INT32_MAX
    );
}

export function stableStringify(value: unknown): string {
    if (Array.isArray(value)) {
        return `[${value.map((item) => stableStringify(item)).join(",")}]`;
    }

    if (value && typeof value === "object") {
        const entries = Object.entries(value as Record<string, unknown>).sort(([a], [b]) => a.localeCompare(b));
        return `{${entries.map(([key, entryValue]) => `${JSON.stringify(key)}:${stableStringify(entryValue)}`).join(",")}}`;
    }

    return JSON.stringify(value) ?? "undefined";
}

export interface ApiSchema {
    definitions?: Record<string, JSONSchema7Definition>;
    $defs?: Record<string, JSONSchema7Definition>;
    server?: Record<string, unknown>;
    session?: Record<string, unknown>;
    clientSession?: Record<string, unknown>;
}

export function isRpcMethod(node: unknown): node is RpcMethod {
    return typeof node === "object" && node !== null && "rpcMethod" in node;
}

/**
 * Apply `normalizeNullableRequiredRefs` to every JSON Schema reachable from the API schema
 * (method params, results, and shared definitions). Call after `cloneSchemaForCodegen` to
 * fix schema defects before any per-language codegen runs.
 */
export function fixNullableRequiredRefsInApiSchema(schema: ApiSchema): ApiSchema {
    function walkApiNode(node: Record<string, unknown> | undefined): Record<string, unknown> | undefined {
        if (!node) return undefined;
        const result: Record<string, unknown> = {};
        for (const [key, value] of Object.entries(node)) {
            if (isRpcMethod(value)) {
                const method = value as RpcMethod;
                result[key] = {
                    ...method,
                    params: method.params ? normalizeNullableRequiredRefs(method.params) : method.params,
                    result: method.result ? normalizeNullableRequiredRefs(method.result) : method.result,
                };
            } else if (typeof value === "object" && value !== null) {
                result[key] = walkApiNode(value as Record<string, unknown>);
            } else {
                result[key] = value;
            }
        }
        return result;
    }

    function normalizeDefs(defs: Record<string, JSONSchema7Definition> | undefined): Record<string, JSONSchema7Definition> | undefined {
        if (!defs) return undefined;
        return Object.fromEntries(
            Object.entries(defs).map(([key, value]) => [
                key,
                typeof value === "object" && value !== null ? normalizeNullableRequiredRefs(value as JSONSchema7) : value,
            ])
        );
    }

    return {
        ...schema,
        definitions: normalizeDefs(schema.definitions),
        $defs: normalizeDefs(schema.$defs),
        server: walkApiNode(schema.server),
        session: walkApiNode(schema.session),
        clientSession: walkApiNode(schema.clientSession),
    };
}

/** Returns true when every leaf RPC method inside `node` is marked experimental. */
export function isNodeFullyExperimental(node: Record<string, unknown>): boolean {
    const methods: RpcMethod[] = [];
    (function collect(n: Record<string, unknown>) {
        for (const value of Object.values(n)) {
            if (isRpcMethod(value)) {
                methods.push(value);
            } else if (typeof value === "object" && value !== null) {
                collect(value as Record<string, unknown>);
            }
        }
    })(node);
    return methods.length > 0 && methods.every(m => m.stability === "experimental");
}

/** Returns true when every leaf RPC method inside `node` is marked deprecated. */
export function isNodeFullyDeprecated(node: Record<string, unknown>): boolean {
    const methods: RpcMethod[] = [];
    (function collect(n: Record<string, unknown>) {
        for (const value of Object.values(n)) {
            if (isRpcMethod(value)) {
                methods.push(value);
            } else if (typeof value === "object" && value !== null) {
                collect(value as Record<string, unknown>);
            }
        }
    })(node);
    return methods.length > 0 && methods.every(m => m.deprecated === true);
}

/**
 * Returns a filtered copy of an API tree containing only methods whose visibility
 * matches `keep`. Sub-groups that end up empty are pruned. Returns null if nothing
 * survives the filter.
 *
 * `"public"` keeps methods without `visibility === "internal"`.
 * `"internal"` keeps methods with `visibility === "internal"`.
 */
export function filterNodeByVisibility(
    node: Record<string, unknown>,
    keep: "public" | "internal",
): Record<string, unknown> | null {
    const result: Record<string, unknown> = {};
    for (const [key, value] of Object.entries(node)) {
        if (isRpcMethod(value)) {
            const isInternal = (value as RpcMethod).visibility === "internal";
            if (keep === "public" && isInternal) continue;
            if (keep === "internal" && !isInternal) continue;
            result[key] = value;
        } else if (typeof value === "object" && value !== null) {
            const sub = filterNodeByVisibility(value as Record<string, unknown>, keep);
            if (sub) result[key] = sub;
        }
    }
    return Object.keys(result).length === 0 ? null : result;
}

/** Returns true when a JSON Schema node is marked as deprecated. */
export function isSchemaDeprecated(schema: JSONSchema7 | null | undefined): boolean {
    return typeof schema === "object" && schema !== null && (schema as Record<string, unknown>).deprecated === true;
}

/** Returns true when a JSON Schema node is marked as experimental. */
export function isSchemaExperimental(schema: JSONSchema7 | null | undefined): boolean {
    return typeof schema === "object" && schema !== null && (schema as Record<string, unknown>).stability === "experimental";
}

/** Returns true when a JSON Schema node is marked as visibility:"internal" (set via `.asInternal()` on the Zod source). */
export function isSchemaInternal(schema: JSONSchema7 | null | undefined): boolean {
    return typeof schema === "object" && schema !== null && (schema as Record<string, unknown>).visibility === "internal";
}

/**
 * Collects the set of definition names marked `visibility: "internal"` and a
 * per-definition set of internal property names. Used by code generators that
 * need to apply `_`-prefix or similar renames consistently across both type
 * declarations and references.
 *
 * Call after `propagateInternalVisibility` so transitively-internal fields are
 * also picked up.
 */
export function collectInternalSymbols(schema: JSONSchema7): {
    typeNames: Set<string>;
    fieldsByType: Map<string, Set<string>>;
} {
    const typeNames = new Set<string>();
    const fieldsByType = new Map<string, Set<string>>();
    const { definitions, $defs } = collectDefinitionCollections(schema as Record<string, unknown>);
    const allDefs: Record<string, JSONSchema7Definition> = { ...definitions, ...$defs };
    for (const [name, def] of Object.entries(allDefs)) {
        if (!def || typeof def !== "object") continue;
        const d = def as Record<string, unknown>;
        if (d.visibility === "internal") typeNames.add(name);
        const props = d.properties;
        if (props && typeof props === "object" && !Array.isArray(props)) {
            for (const [propName, propSchema] of Object.entries(props as Record<string, unknown>)) {
                if (propSchema && typeof propSchema === "object" && (propSchema as Record<string, unknown>).visibility === "internal") {
                    if (!fieldsByType.has(name)) fieldsByType.set(name, new Set());
                    fieldsByType.get(name)!.add(propName);
                }
            }
        }
    }
    return { typeNames, fieldsByType };
}

/**
 * Post-process a Python module so that types marked `visibility: "internal"`
 * carry an underscore prefix on their class identifier.
 *
 * Why: Python has no compiler-enforced visibility, but the leading-underscore
 * convention is universally recognized as "no stability guarantee". Combined
 * with `__all__` exclusion at the module level (handled separately), this is
 * the strongest "internal" signal Python idioms provide and matches the
 * cross-language bar of "we can do breaking changes on these without
 * having to apologize".
 *
 * Field-level visibility is expected to be handled at emission time by each
 * Python emitter (because field names depend on the emitter's PEP 8 normalization
 * and the emitter's class-name conventions may diverge from the schema's
 * definition names, breaking any single-class regex). Type-level renaming is
 * safe to do globally because schema definition names match the emitted class
 * identifiers for the types that carry `visibility: "internal"`.
 */
export function renameInternalPythonSymbols(
    code: string,
    typeNames: Iterable<string>
): string {
    const escapeRegex = (s: string): string => s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    let result = code;
    const sortedTypes = [...typeNames].sort((a, b) => b.length - a.length);
    // Phase 1: rename each identifier globally at word boundaries.
    for (const t of sortedTypes) {
        result = result.replace(
            new RegExp(`(?<![A-Za-z0-9_])${escapeRegex(t)}(?![A-Za-z0-9_])`, "g"),
            `_${t}`
        );
    }
    // Phase 2: restore JSON-key strings that match the rename target. Those
    // strings carry the wire-protocol definition name and must remain untouched
    // regardless of the Python-side rename. Patterns: `obj.get("Foo")` and
    // `result["Foo"] = ...` in quicktype's serialization helpers.
    for (const t of sortedTypes) {
        const escaped = escapeRegex(t);
        result = result.replace(
            new RegExp(`(obj\\.get\\(")_${escaped}("\\))`, "g"),
            `$1${t}$2`
        );
        result = result.replace(
            new RegExp(`(result\\[")_${escaped}("\\])`, "g"),
            `$1${t}$2`
        );
    }
    return result;
}

/**
 * Collects the set of (publicTypeName, internalFieldName[]) pairs from a
 * processed schema. Used by code generators that need to annotate or rename
 * properties whose type carries `visibility: "internal"` but whose containing
 * definition is itself public — e.g. so IDEs/code completion can hint that a
 * field is internal even when the type isn't renamed.
 *
 * Only definitions that are NOT themselves `visibility: "internal"` are included
 * (those are already covered by type-level rename/visibility). The returned map
 * is keyed by JSON Schema definition name; field names are the JSON property
 * names (un-cased).
 */
export function collectInternalFieldsOnPublicTypes(
    schema: JSONSchema7
): Map<string, Set<string>> {
    const out = new Map<string, Set<string>>();
    const { definitions, $defs } = collectDefinitionCollections(schema as Record<string, unknown>);
    const allDefs: Record<string, JSONSchema7Definition> = { ...definitions, ...$defs };
    for (const [name, def] of Object.entries(allDefs)) {
        if (!def || typeof def !== "object") continue;
        const d = def as Record<string, unknown>;
        if (d.visibility === "internal") continue;
        const props = d.properties;
        if (!props || typeof props !== "object" || Array.isArray(props)) continue;
        for (const [propName, propSchema] of Object.entries(props as Record<string, unknown>)) {
            if (propSchema && typeof propSchema === "object" && (propSchema as Record<string, unknown>).visibility === "internal") {
                if (!out.has(name)) out.set(name, new Set());
                out.get(name)!.add(propName);
            }
        }
    }
    return out;
}

/**
 * Annotate quicktype-generated Python field declarations whose schema is marked
 * `visibility: "internal"` with a `# Internal:` comment immediately above the
 * declaration. The comment is visible in IDE hovers/code completion, so
 * consumers see the marker even though the identifier itself is unchanged.
 *
 * This is the field-level fallback for code paths that can't rename the field
 * identifier (quicktype's generated `from_dict`/`to_dict` reference field names
 * in patterns brittle to regex rewriting). For session-events and other
 * hand-rolled emitters, prefer renaming.
 *
 * The `toFieldName` callback maps a JSON property name to its Python attribute
 * name (typically snake_case).
 */
export function annotateInternalPythonFields(
    code: string,
    fieldsByType: Map<string, Set<string>>,
    toFieldName: (jsonName: string) => string
): string {
    const escapeRegex = (s: string): string => s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
    let result = code;
    for (const [typeName, fields] of fieldsByType) {
        // Match the class body up to the next top-level statement. quicktype's
        // generated classes are separated by blank-line boundaries.
        const classRe = new RegExp(
            `(@dataclass\\nclass ${escapeRegex(typeName)}[:(][^]*?)(?=\\n(?:@dataclass\\n)?class \\w|\\n\\nclass |\\n[A-Za-z_]\\w* =|$)`,
            "g"
        );
        result = result.replace(classRe, (block) => {
            for (const jsonField of fields) {
                const pyField = toFieldName(jsonField);
                const escaped = escapeRegex(pyField);
                // Match `    fieldName: type` style declarations (PEP 526). Avoid
                // double-annotating if the comment is already present immediately above.
                block = block.replace(
                    new RegExp(`(^(?!    # Internal:.*$)(?:.*\\n)?)(    )${escaped}(?=\\s*:)`, "gm"),
                    (_match, prefix, indent) => {
                        // Avoid duplicate annotation if the previous line is already an Internal: marker.
                        if (/    # Internal:/.test(prefix)) return `${prefix}${indent}${pyField}`;
                        return `${prefix}${indent}# Internal: this field is an internal SDK API and is not part of the public surface.\n${indent}${pyField}`;
                    }
                );
            }
            return block;
        });
    }
    return result;
}

/**
 * Walks a top-level JSON Schema and marks any property whose referenced type
 * resolves to an internal definition as `visibility: "internal"` itself.
 *
 * Schemas can be authored with an internal-typed reference on a property that
 * isn't itself explicitly marked internal (e.g. `copilotUsage` referencing
 * `AssistantUsageCopilotUsage`). Code generators that map `visibility:
 * "internal"` to hard language-level visibility (C# `internal`, Rust
 * `pub(crate)`) would otherwise produce inconsistent-accessibility errors
 * (CS0053 in C#, E0446 in Rust). This pass closes that gap by promoting
 * referencing properties to internal — matching the language compilers'
 * own transitivity rule.
 *
 * Only references that resolve directly, through arrays, or through dictionary
 * `additionalProperties` are considered. References that flow only through a
 * `oneOf`/`anyOf` of public+internal variants are left alone (the union itself
 * is the carrier of visibility there).
 *
 * Mutates `schema` in place and returns it. Idempotent.
 */
export function propagateInternalVisibility(schema: JSONSchema7): JSONSchema7 {
    if (typeof schema !== "object" || schema === null) return schema;

    const { definitions, $defs } = collectDefinitionCollections(schema as Record<string, unknown>);
    const allDefs: Record<string, JSONSchema7Definition> = { ...definitions, ...$defs };
    const internalTypeNames = new Set<string>();
    for (const [name, def] of Object.entries(allDefs)) {
        if (def && typeof def === "object" && isSchemaInternal(def as JSONSchema7)) {
            internalTypeNames.add(name);
        }
    }
    if (internalTypeNames.size === 0) return schema;

    const refToName = (ref: unknown): string | undefined => {
        if (typeof ref !== "string") return undefined;
        const m = ref.match(/^#\/(?:definitions|\$defs)\/([^/]+)$/);
        return m ? m[1] : undefined;
    };

    /** Returns true when a property's *direct* type carrier is an internal definition. */
    const propertyReferencesInternal = (propSchema: JSONSchema7): boolean => {
        const direct = refToName((propSchema as Record<string, unknown>).$ref);
        if (direct && internalTypeNames.has(direct)) return true;
        const items = (propSchema as Record<string, unknown>).items;
        if (items && typeof items === "object" && !Array.isArray(items)) {
            const itemsRef = refToName((items as Record<string, unknown>).$ref);
            if (itemsRef && internalTypeNames.has(itemsRef)) return true;
        }
        const addl = (propSchema as Record<string, unknown>).additionalProperties;
        if (addl && typeof addl === "object") {
            const addlRef = refToName((addl as Record<string, unknown>).$ref);
            if (addlRef && internalTypeNames.has(addlRef)) return true;
        }
        return false;
    };

    const visit = (node: unknown): void => {
        if (!node || typeof node !== "object") return;
        if (Array.isArray(node)) {
            for (const item of node) visit(item);
            return;
        }
        const record = node as Record<string, unknown>;
        const props = record.properties;
        if (props && typeof props === "object" && !Array.isArray(props)) {
            for (const propSchema of Object.values(props as Record<string, unknown>)) {
                if (!propSchema || typeof propSchema !== "object") continue;
                if (!isSchemaInternal(propSchema as JSONSchema7) && propertyReferencesInternal(propSchema as JSONSchema7)) {
                    (propSchema as Record<string, unknown>).visibility = "internal";
                }
                visit(propSchema);
            }
        }
        for (const key of ["items", "additionalProperties", "anyOf", "allOf", "oneOf"]) {
            if (record[key]) visit(record[key]);
        }
        for (const collectionKey of ["definitions", "$defs"]) {
            const collection = record[collectionKey];
            if (collection && typeof collection === "object" && !Array.isArray(collection)) {
                for (const def of Object.values(collection as Record<string, unknown>)) {
                    if (def && typeof def === "object") visit(def);
                }
            }
        }
    };

    visit(schema);
    return schema;
}

/**
 * Returns true when a JSON Schema node is marked `x-opaque-json: true` (set via
 * `.asOpaqueJson()` on the Zod source). These are the only shapes that legitimately
 * surface as opaque JSON in the SDK; everything else with an underspecified type
 * is rejected by the runtime's schema lint pass.
 */
export function isOpaqueJson(schema: JSONSchema7 | null | undefined): boolean {
    return typeof schema === "object" && schema !== null && (schema as Record<string, unknown>)["x-opaque-json"] === true;
}

/**
 * Removes the `x-opaque-json` marker from a schema node in place. Useful for
 * codegens (e.g. TypeScript) that don't distinguish opaque JSON from any other
 * unconstrained value and would otherwise have the marker confuse downstream
 * tooling. Codegens that *do* care (e.g. C#, which maps opaque JSON to
 * `JsonElement`) should call `isOpaqueJson` *before* this point.
 */
export function stripOpaqueJsonMarker(schema: Record<string, unknown>): void {
    delete schema["x-opaque-json"];
}

/**
 * Append `@internal` and/or `@experimental` JSDoc-style tags to the `description`
 * of every property that carries `visibility: "internal"` or `stability: "experimental"`
 * inline. Used by codegens whose output mechanism (e.g. `json-schema-to-typescript`)
 * renders `description` verbatim as JSDoc; downstream tooling then picks the tags
 * up automatically.
 *
 * Mutates `schema` in place and returns it. Callers that don't want their input
 * mutated should clone first.
 */
export function appendPropertyMarkerTagsToDescriptions(schema: JSONSchema7): JSONSchema7 {
    const seen = new WeakSet<object>();
    const visit = (node: unknown): void => {
        if (!node || typeof node !== "object") return;
        if (seen.has(node)) return;
        seen.add(node);

        if (Array.isArray(node)) {
            for (const item of node) visit(item);
            return;
        }

        const record = node as Record<string, unknown>;
        const props = record.properties;
        if (props && typeof props === "object" && !Array.isArray(props)) {
            for (const propSchema of Object.values(props as Record<string, unknown>)) {
                if (!propSchema || typeof propSchema !== "object") continue;
                const tags: string[] = [];
                if (isSchemaInternal(propSchema as JSONSchema7)) tags.push("@internal");
                if (isSchemaExperimental(propSchema as JSONSchema7)) tags.push("@experimental");
                if (tags.length === 0) continue;
                const propRecord = propSchema as Record<string, unknown>;
                const existing = typeof propRecord.description === "string" ? propRecord.description : "";
                const suffix = tags.join("\n");
                propRecord.description = existing.length > 0 ? `${existing}\n\n${suffix}` : suffix;

                // json-schema-to-typescript drops the description on properties whose
                // schema is a bare `$ref`. Rewriting to `allOf: [{$ref}]` keeps the
                // referenced type while preserving the description (and our appended
                // JSDoc tags) on the property declaration. Other generators don't see
                // this wrapper because they consume the schema before this pass.
                if (typeof propRecord.$ref === "string" && !propRecord.allOf) {
                    const refValue = propRecord.$ref;
                    delete propRecord.$ref;
                    propRecord.allOf = [{ $ref: refValue } as JSONSchema7Definition];
                }
            }
        }

        for (const value of Object.values(record)) {
            if (value && typeof value === "object") visit(value);
        }
    };
    visit(schema);
    return schema;
}

// ── $ref resolution ─────────────────────────────────────────────────────────

/** Extract the generated type name from a `$ref` path (e.g. "#/definitions/Model" → "Model"). */
export function refTypeName(ref: string, definitions?: DefinitionCollections): string {
    const baseName = ref.split("/").pop()!;
    const match = ref.match(/^#\/(definitions|\$defs)\/(.+)$/);
    if (!match || match[1] !== "$defs" || !definitions) return baseName;

    const key = match[2];
    const legacyDefinition = definitions.definitions?.[key];
    const draftDefinition = definitions.$defs?.[key];
    if (
        legacyDefinition !== undefined &&
        draftDefinition !== undefined &&
        stableStringify(legacyDefinition) !== stableStringify(draftDefinition)
    ) {
        return `Draft${baseName}`;
    }

    return baseName;
}

export function parseExternalSchemaRef(ref: string): { schemaFile: string; definitionName: string } | undefined {
    const match = ref.match(/^([^#]+)#\/(?:definitions|\$defs)\/(.+)$/);
    return match ? { schemaFile: match[1], definitionName: match[2] } : undefined;
}

export function collectExternalSchemaRefNames(schema: unknown): Map<string, Set<string>> {
    const refs = new Map<string, Set<string>>();

    const visit = (value: unknown): void => {
        if (Array.isArray(value)) {
            for (const item of value) visit(item);
            return;
        }

        if (!value || typeof value !== "object") return;

        const node = value as Record<string, unknown>;
        if (typeof node.$ref === "string") {
            const externalRef = parseExternalSchemaRef(node.$ref);
            if (externalRef) {
                let bucket = refs.get(externalRef.schemaFile);
                if (!bucket) {
                    bucket = new Set<string>();
                    refs.set(externalRef.schemaFile, bucket);
                }
                bucket.add(externalRef.definitionName);
            }
        }

        for (const child of Object.values(node)) visit(child);
    };

    visit(schema);
    return refs;
}

/** Resolve a `$ref` path against a definitions map, returning the referenced schema. */
export function resolveRef(
    ref: string,
    definitions: DefinitionCollections | undefined
): JSONSchema7 | undefined {
    const match = ref.match(/^#\/(definitions|\$defs)\/(.+)$/);
    if (!match || !definitions) return undefined;
    const [, namespace, key] = match;
    const primary = namespace === "$defs" ? definitions.$defs : definitions.definitions;
    const fallback = namespace === "$defs" ? definitions.definitions : definitions.$defs;
    const def = primary?.[key] ?? fallback?.[key];
    return typeof def === "object" ? (def as JSONSchema7) : undefined;
}

export function resolveSchema(
    schema: JSONSchema7 | null | undefined,
    definitions: DefinitionCollections | undefined
): JSONSchema7 | undefined {
    let current = schema ?? undefined;
    const seenRefs = new Set<string>();
    while (current?.$ref) {
        if (seenRefs.has(current.$ref)) break;
        seenRefs.add(current.$ref);
        const resolved = resolveRef(current.$ref, definitions);
        if (!resolved) break;
        current = resolved;
    }
    return current;
}

function hasObjectShape(schema: JSONSchema7): boolean {
    return !!(schema.properties || schema.additionalProperties || schema.type === "object");
}

function isEmptyNotSchema(schema: JSONSchema7): boolean {
    return !!schema.not && typeof schema.not === "object" && Object.keys(schema.not).length === 0;
}

function mergeObjectSchemas(schemas: JSONSchema7[]): JSONSchema7 | undefined {
    const mergedProperties: Record<string, JSONSchema7Definition> = {};
    const mergedRequired = new Set<string>();
    const merged: JSONSchema7 = {
        type: "object",
    };
    let hasShape = false;

    for (const objectSchema of schemas) {
        if (!merged.title && objectSchema.title) {
            merged.title = objectSchema.title;
        }
        if (!merged.description && objectSchema.description) {
            merged.description = objectSchema.description;
        }
        if (objectSchema.properties) {
            Object.assign(mergedProperties, objectSchema.properties);
            hasShape = true;
        }
        if (objectSchema.required) {
            for (const name of objectSchema.required) {
                mergedRequired.add(name);
            }
        }
        if (objectSchema.additionalProperties !== undefined) {
            merged.additionalProperties = objectSchema.additionalProperties;
            hasShape = true;
        }
    }

    if (!hasShape) return undefined;
    if (Object.keys(mergedProperties).length > 0) {
        merged.properties = mergedProperties;
    }
    if (mergedRequired.size > 0) {
        merged.required = [...mergedRequired];
    }
    return merged;
}

export function resolveObjectSchema(
    schema: JSONSchema7 | null | undefined,
    definitions: DefinitionCollections | undefined
): JSONSchema7 | undefined {
    const resolved = resolveSchema(schema, definitions) ?? schema ?? undefined;
    if (!resolved) return undefined;
    const resolvedHasObjectShape = hasObjectShape(resolved);

    if (resolved.allOf) {
        const objectSchemas: JSONSchema7[] = [];
        if (resolvedHasObjectShape) {
            objectSchemas.push(resolved);
        }

        for (const item of resolved.allOf) {
            if (typeof item !== "object") continue;
            const objectSchema = resolveObjectSchema(item as JSONSchema7, definitions);
            if (!objectSchema) continue;
            objectSchemas.push(objectSchema);
        }

        return mergeObjectSchemas(objectSchemas) ?? resolved;
    }

    const singleBranch = (resolved.anyOf ?? resolved.oneOf)
        ?.filter((item): item is JSONSchema7 => {
            if (!item || typeof item !== "object") return false;
            const s = item as JSONSchema7;
            // Filter out null types and `{ not: {} }` (Zod's representation of "nothing" in optional anyOf)
            if (s.type === "null") return false;
            if (isEmptyNotSchema(s)) return false;
            return true;
        });
    if (singleBranch && singleBranch.length === 1) {
        const objectSchema = resolveObjectSchema(singleBranch[0], definitions);
        if (!objectSchema) return resolved;
        if (resolvedHasObjectShape) {
            return mergeObjectSchemas([resolved, objectSchema]) ?? objectSchema;
        }
        return objectSchema;
    }

    if (resolvedHasObjectShape) return resolved;

    return resolved;
}

export function getSessionEventVariantSchemas(
    schema: JSONSchema7,
    definitionCollections: DefinitionCollections = collectDefinitionCollections(schema as Record<string, unknown>)
): JSONSchema7[] {
    const sessionEvent =
        resolveSchema({ $ref: "#/definitions/SessionEvent" }, definitionCollections) ??
        resolveSchema({ $ref: "#/$defs/SessionEvent" }, definitionCollections);
    if (!sessionEvent?.anyOf) throw new Error("Schema must have SessionEvent definition with anyOf");

    return (sessionEvent.anyOf as JSONSchema7[]).map((variant) => {
        const resolvedVariant =
            resolveObjectSchema(variant, definitionCollections) ??
            resolveSchema(variant, definitionCollections) ??
            variant;
        if (typeof resolvedVariant !== "object" || !resolvedVariant.properties) throw new Error("Invalid event variant");
        return resolvedVariant;
    });
}

export function getSharedSessionEventEnvelopeProperties(
    schema: JSONSchema7,
    definitionCollections: DefinitionCollections = collectDefinitionCollections(schema as Record<string, unknown>)
): SessionEventEnvelopeProperty[] {
    const variants = getSessionEventVariantSchemas(schema, definitionCollections);
    const firstVariant = variants[0];
    const firstProperties = firstVariant.properties ?? {};

    return Object.entries(firstProperties)
        .filter(([name]) => name !== "type" && name !== "data")
        .map(([name]) => {
            const propertySchemas = variants
                .map((variant) => variant.properties?.[name])
                .filter((propSchema): propSchema is JSONSchema7 => typeof propSchema === "object" && propSchema !== null);

            if (propertySchemas.length !== variants.length) return undefined;

            return {
                name,
                schema: selectSessionEventEnvelopePropertySchema(propertySchemas),
                required: variants.every((variant) => (variant.required ?? []).includes(name)),
            };
        })
        .filter((property): property is SessionEventEnvelopeProperty => property !== undefined);
}

function selectSessionEventEnvelopePropertySchema(propertySchemas: JSONSchema7[]): JSONSchema7 {
    // Some variants further constrain a shared envelope property, e.g. ephemeral const true.
    // Generate the base property from the least restrictive schema that has useful metadata.
    return (
        propertySchemas.find((schema) => !isConstOrEnumSchema(schema) && schema.description) ??
        propertySchemas.find((schema) => !isConstOrEnumSchema(schema)) ??
        propertySchemas.find((schema) => schema.description) ??
        propertySchemas[0]
    );
}

function isConstOrEnumSchema(schema: JSONSchema7): boolean {
    return "const" in schema || (Array.isArray(schema.enum) && schema.enum.length > 0);
}

export function hasSchemaPayload(schema: JSONSchema7 | null | undefined): boolean {
    if (!schema) return false;
    if (schema.properties) return Object.keys(schema.properties).length > 0;
    if (schema.additionalProperties) return true;
    if (schema.items) return true;
    if (schema.anyOf || schema.oneOf || schema.allOf) return true;
    if (schema.enum && schema.enum.length > 0) return true;
    if (schema.const !== undefined) return true;
    if (schema.$ref) return true;
    if (Array.isArray(schema.type)) return schema.type.length > 0 && !(schema.type.length === 1 && schema.type[0] === "object");
    return schema.type !== undefined && schema.type !== "object";
}

export function collectDefinitionCollections(
    schema: Record<string, unknown>
): Required<DefinitionCollections> {
    return {
        definitions: { ...((schema.definitions ?? {}) as Record<string, JSONSchema7Definition>) },
        $defs: { ...((schema.$defs ?? {}) as Record<string, JSONSchema7Definition>) },
    };
}

/** Collect the shared definitions from a schema (handles both `definitions` and `$defs`). */
export function collectDefinitions(
    schema: Record<string, unknown>
): Record<string, JSONSchema7Definition> {
    const { definitions, $defs } = collectDefinitionCollections(schema);
    return { ...$defs, ...definitions };
}

export function findSharedSchemaDefinitions(
    sourceSchema: Record<string, unknown>,
    canonicalSchema: Record<string, unknown>
): Set<string> {
    const sourceDefinitions = collectDefinitions(sourceSchema);
    const canonicalDefinitions = collectDefinitions(canonicalSchema);
    const shared = new Set<string>();

    for (const [name, sourceDefinition] of Object.entries(sourceDefinitions)) {
        const canonicalDefinition = canonicalDefinitions[name];
        if (
            canonicalDefinition !== undefined &&
            stableStringify(normalizeDefinitionForComparison(sourceDefinition)) ===
                stableStringify(normalizeDefinitionForComparison(canonicalDefinition))
        ) {
            shared.add(name);
        }
    }

    let changed = true;
    while (changed) {
        changed = false;
        for (const name of [...shared]) {
            const refs = new Set([
                ...collectLocalDefinitionRefNames(sourceDefinitions[name]),
                ...collectLocalDefinitionRefNames(canonicalDefinitions[name]),
            ]);
            for (const refName of refs) {
                if (refName !== name && !shared.has(refName)) {
                    shared.delete(name);
                    changed = true;
                    break;
                }
            }
        }
    }

    return shared;
}

export function collectReachableDefinitionNames(
    schema: Record<string, unknown>,
    rootDefinitionNames: Iterable<string> = ["SessionEvent"]
): Set<string> {
    const definitions = collectDefinitions(schema);
    const reachable = new Set<string>();
    const visiting = new Set<string>();

    const visitDefinition = (name: string): void => {
        if (reachable.has(name) || visiting.has(name)) return;
        const definition = definitions[name];
        if (definition === undefined) return;

        visiting.add(name);
        reachable.add(name);
        visitSchema(definition);
        visiting.delete(name);
    };

    const visitSchema = (value: unknown): void => {
        if (!value || typeof value !== "object") return;
        if (Array.isArray(value)) {
            for (const item of value) visitSchema(item);
            return;
        }

        const record = value as Record<string, unknown>;
        if (typeof record.$ref === "string") {
            const localRef = parseLocalDefinitionRef(record.$ref);
            if (localRef) visitDefinition(localRef);
        }
        for (const child of Object.values(record)) visitSchema(child);
    };

    for (const rootName of rootDefinitionNames) {
        visitDefinition(rootName);
    }

    return reachable;
}

export function collectSchemaReferencedDefinitionNames(
    schemas: Iterable<JSONSchema7 | null | undefined>,
    definitionCollections: DefinitionCollections
): Set<string> {
    const definitions = collectDefinitions({
        definitions: definitionCollections.definitions ?? {},
        $defs: definitionCollections.$defs ?? {},
    });
    const reachable = new Set<string>();
    const visiting = new Set<string>();

    const visitDefinition = (name: string, ref?: string): void => {
        if (reachable.has(name) || visiting.has(name)) return;
        const definition = ref ? resolveRef(ref, definitionCollections) : definitions[name];
        if (definition === undefined || typeof definition !== "object" || definition === null) return;

        visiting.add(name);
        reachable.add(name);
        visitSchema(definition);
        visiting.delete(name);
    };

    const visitSchema = (value: unknown): void => {
        if (!value || typeof value !== "object") return;
        if (Array.isArray(value)) {
            for (const item of value) visitSchema(item);
            return;
        }

        const record = value as Record<string, unknown>;
        if (typeof record.$ref === "string") {
            const localRef = parseLocalDefinitionRef(record.$ref);
            if (localRef) visitDefinition(localRef, record.$ref);
        }
        for (const child of Object.values(record)) visitSchema(child);
    };

    for (const schema of schemas) {
        visitSchema(schema);
    }

    return reachable;
}

export function collectRpcMethodReferencedDefinitionNames(
    methods: Iterable<RpcMethod>,
    definitionCollections: DefinitionCollections
): Set<string> {
    const schemas: Array<JSONSchema7 | null | undefined> = [];
    for (const method of methods) {
        schemas.push(method.params, method.result);
    }

    return collectSchemaReferencedDefinitionNames(schemas, definitionCollections);
}

export function collectExperimentalOnlyRpcReferencedDefinitionNames(
    methods: Iterable<RpcMethod>,
    definitionCollections: DefinitionCollections
): Set<string> {
    const methodList = [...methods];
    const experimental = collectRpcMethodReferencedDefinitionNames(
        methodList.filter((method) => method.stability === "experimental"),
        definitionCollections
    );
    const nonExperimental = collectRpcMethodReferencedDefinitionNames(
        methodList.filter((method) => method.stability !== "experimental"),
        definitionCollections
    );

    for (const name of nonExperimental) {
        experimental.delete(name);
    }

    return experimental;
}

export function rewriteSharedDefinitionReferences<T>(
    schema: T,
    sharedDefinitionNames: Iterable<string>,
    externalSchemaFile: string,
    preserveDefinitions = false
): T {
    const sharedNames = new Set(sharedDefinitionNames);
    if (sharedNames.size === 0) return cloneSchemaForCodegen(schema);

    const rewriteRef = (ref: string): string => {
        const localRef = parseLocalDefinitionRef(ref);
        return localRef && sharedNames.has(localRef) ? `${externalSchemaFile}#/definitions/${localRef}` : ref;
    };

    const rewrite = (value: unknown): unknown => {
        if (Array.isArray(value)) {
            return value.map((item) => rewrite(item));
        }

        if (!value || typeof value !== "object") {
            return value;
        }

        const source = value as Record<string, unknown>;
        const result: Record<string, unknown> = {};
        for (const [childKey, childValue] of Object.entries(source)) {
            if ((childKey === "definitions" || childKey === "$defs") && childValue && typeof childValue === "object" && !Array.isArray(childValue)) {
                const definitions: Record<string, unknown> = {};
                for (const [definitionName, definitionValue] of Object.entries(childValue as Record<string, unknown>)) {
                    if (preserveDefinitions || !sharedNames.has(definitionName)) {
                        definitions[definitionName] = rewrite(definitionValue);
                    }
                }
                result[childKey] = definitions;
                continue;
            }

            result[childKey] = rewrite(childValue);
        }

        if (typeof result.$ref === "string") {
            result.$ref = rewriteRef(result.$ref);
        }

        return result;
    };

    return rewrite(schema) as T;
}

export function inlineExternalSchemaDefinitions<T>(
    schema: T,
    externalSchema: Record<string, unknown>,
    externalSchemaFile: string,
    options: { conflictingDefinitionNamePrefix?: string } = {}
): { schema: T; inlinedDefinitionNames: Set<string> } {
    const cloned = cloneSchemaForCodegen(schema) as Record<string, unknown>;
    const externalRefs = collectExternalSchemaRefNames(cloned).get(externalSchemaFile);
    if (!externalRefs || externalRefs.size === 0) {
        return { schema: cloned as T, inlinedDefinitionNames: new Set<string>() };
    }

    const externalDefinitions = collectDefinitions(externalSchema);
    const reachableDefinitions = collectReachableDefinitionNames(externalSchema, externalRefs);
    const inlinedDefinitionNames = new Set<string>();
    const targetDefinitions = {
        ...((cloned.definitions ?? {}) as Record<string, JSONSchema7Definition>),
    };
    const nameMap = new Map<string, string>();
    const usedNames = new Set([...Object.keys(targetDefinitions), ...reachableDefinitions]);

    for (const name of [...reachableDefinitions].sort()) {
        const definition = externalDefinitions[name];
        if (definition === undefined) continue;

        const existing = targetDefinitions[name];
        if (
            existing !== undefined &&
            stableStringify(normalizeDefinitionForComparison(existing)) !==
                stableStringify(normalizeDefinitionForComparison(definition))
        ) {
            if (!options.conflictingDefinitionNamePrefix) {
                throw new Error(
                    `Cannot inline ${externalSchemaFile}#/definitions/${name}; api.schema.json already defines a different schema with that name.`
                );
            }

            let renamed = `${options.conflictingDefinitionNamePrefix}${name}`;
            let suffix = 2;
            while (usedNames.has(renamed)) {
                renamed = `${options.conflictingDefinitionNamePrefix}${name}${suffix++}`;
            }
            usedNames.add(renamed);
            nameMap.set(name, renamed);
        } else {
            nameMap.set(name, name);
        }
    }

    const rewriteInlinedRefs = (value: unknown): unknown => {
        if (Array.isArray(value)) {
            return value.map((item) => rewriteInlinedRefs(item));
        }

        if (!value || typeof value !== "object") {
            return value;
        }

        const result: Record<string, unknown> = {};
        for (const [key, child] of Object.entries(value as Record<string, unknown>)) {
            result[key] = rewriteInlinedRefs(child);
        }

        if (typeof result.$ref === "string") {
            const localRef = parseLocalDefinitionRef(result.$ref);
            const externalRef = parseExternalSchemaRef(result.$ref);
            const mappedName =
                localRef ? nameMap.get(localRef) :
                externalRef?.schemaFile === externalSchemaFile ? nameMap.get(externalRef.definitionName) :
                undefined;
            if (mappedName) {
                result.$ref = `#/definitions/${mappedName}`;
            }
        }

        return result;
    };

    for (const name of [...reachableDefinitions].sort()) {
        const definition = externalDefinitions[name];
        const targetName = nameMap.get(name);
        if (definition === undefined || !targetName) continue;

        targetDefinitions[targetName] = rewriteInlinedRefs(cloneSchemaForCodegen(definition)) as JSONSchema7Definition;
        inlinedDefinitionNames.add(targetName);
    }

    cloned.definitions = targetDefinitions;

    const rewrite = (value: unknown): unknown => {
        if (Array.isArray(value)) {
            return value.map((item) => rewrite(item));
        }

        if (!value || typeof value !== "object") {
            return value;
        }

        const result: Record<string, unknown> = {};
        for (const [key, child] of Object.entries(value as Record<string, unknown>)) {
            result[key] = rewrite(child);
        }

        if (typeof result.$ref === "string") {
            const externalRef = parseExternalSchemaRef(result.$ref);
            const targetName = externalRef?.schemaFile === externalSchemaFile ? nameMap.get(externalRef.definitionName) : undefined;
            if (targetName) {
                result.$ref = `#/definitions/${targetName}`;
            }
        }

        return result;
    };

    return { schema: rewrite(cloned) as T, inlinedDefinitionNames };
}

function normalizeDefinitionForComparison(definition: JSONSchema7Definition): unknown {
    if (Array.isArray(definition)) {
        return definition.map((item) =>
            typeof item === "object" && item !== null ? normalizeDefinitionForComparison(item as JSONSchema7Definition) : item
        );
    }

    if (!definition || typeof definition !== "object") {
        return definition;
    }

    const result: Record<string, unknown> = {};
    for (const [key, value] of Object.entries(definition as Record<string, unknown>)) {
        if (key === "description" || key === "markdownDescription" || key === "x-enumDescriptions") {
            continue;
        } else if (key === "$ref" && typeof value === "string") {
            const localRef = parseLocalDefinitionRef(value);
            result[key] = localRef ? `#/definitions/${localRef}` : value;
        } else if (Array.isArray(value)) {
            result[key] = value.map((item) =>
                typeof item === "object" && item !== null ? normalizeDefinitionForComparison(item as JSONSchema7Definition) : item
            );
        } else if (value && typeof value === "object") {
            result[key] = normalizeDefinitionForComparison(value as JSONSchema7Definition);
        } else {
            result[key] = value;
        }
    }
    return result;
}

function collectLocalDefinitionRefNames(value: unknown): Set<string> {
    const refs = new Set<string>();

    const visit = (node: unknown): void => {
        if (!node || typeof node !== "object") return;
        if (Array.isArray(node)) {
            for (const item of node) visit(item);
            return;
        }

        const record = node as Record<string, unknown>;
        if (typeof record.$ref === "string") {
            const localRef = parseLocalDefinitionRef(record.$ref);
            if (localRef) refs.add(localRef);
        }
        for (const child of Object.values(record)) visit(child);
    };

    visit(value);
    return refs;
}

function parseLocalDefinitionRef(ref: string): string | undefined {
    const match = ref.match(/^#\/(?:definitions|\$defs)\/(.+)$/);
    return match?.[1];
}

export function withSharedDefinitions<T extends JSONSchema7>(
    schema: T,
    definitions: DefinitionCollections
): SchemaWithSharedDefinitions<T> {
    const legacyDefinitions = { ...(definitions.definitions ?? {}) };
    const draft2019Definitions = { ...(definitions.$defs ?? {}) };

    const sharedLegacyDefinitions =
        Object.keys(legacyDefinitions).length > 0 ? legacyDefinitions : { ...draft2019Definitions };
    const sharedDraftDefinitions =
        Object.keys(draft2019Definitions).length > 0 ? draft2019Definitions : { ...legacyDefinitions };

    return {
        ...schema,
        definitions: sharedLegacyDefinitions,
        $defs: sharedDraftDefinitions,
    };
}
