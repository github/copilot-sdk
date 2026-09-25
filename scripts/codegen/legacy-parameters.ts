/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Language-neutral `x-legacy-parameters` handling shared by every SDK generator.
 *
 * A request schema opts in by listing its original parameter set. Each generator keeps
 * that published API with its own ordering and construction conventions, and exposes the
 * full current schema through an extensible entry point where the language needs one.
 * Future optional properties are added to the schema without changing the annotation.
 *
 * A response record can opt in too. Its legacy list names the properties the published
 * record had, in schema property order, so languages with positional construction can keep
 * that constructor while the record gains the new fields.
 *
 * This module is dependency-free so generators outside `src/sdk/scripts/codegen`, such
 * as the Java generator, can import it directly.
 */

export const LEGACY_PARAMETERS_KEY = "x-legacy-parameters";

interface LegacyParameterSchema {
    properties?: Record<string, unknown>;
    required?: readonly string[];
    [LEGACY_PARAMETERS_KEY]?: unknown;
}

export interface LegacyParameters {
    /** The original parameter set, in declared order. */
    readonly legacy: readonly string[];
    /** Properties added after the legacy API, in schema property order. All are optional. */
    readonly additions: readonly string[];
    /** Schema-required properties, excluding implicit ones. */
    readonly required: ReadonlySet<string>;
}

export interface LegacyParameterOptions {
    /** Properties the SDK supplies itself, such as a session-scoped `sessionId`. */
    readonly implicit?: readonly string[];
    /** Whether callers may omit the whole request. Optional requests cannot opt in. */
    readonly optional?: boolean;
    /** Whether the request schema accepts null. Nullable requests cannot opt in. */
    readonly nullable?: boolean;
    /**
     * Whether the legacy list must follow schema property order, as a positional record
     * constructor requires.
     */
    readonly ordered?: boolean;
}

/** True for the `anyOf: [{ not: {} }, …]` shape of a request callers may omit entirely. */
export function isOmittableRequest(schema: unknown): boolean {
    const anyOf = (schema as { anyOf?: unknown } | null | undefined)?.anyOf;
    return (
        Array.isArray(anyOf) &&
        anyOf.some((variant) => {
            const not = (variant as { not?: unknown } | null)?.not;
            return typeof not === "object" && not !== null && Object.keys(not).length === 0;
        })
    );
}

export function hasLegacyParameters(schema: unknown): boolean {
    return typeof schema === "object" && schema !== null && LEGACY_PARAMETERS_KEY in schema;
}

/**
 * Reads and validates a request schema's legacy parameter set.
 *
 * Returns `undefined` when the schema has not opted in, so unmarked APIs keep their
 * existing generation. Throws when the metadata would omit a required input, name an
 * unknown property, or otherwise fail to preserve the original API.
 */
export function readLegacyParameters(
    schema: unknown,
    owner: string,
    options: LegacyParameterOptions = {}
): LegacyParameters | undefined {
    if (!hasLegacyParameters(schema)) return undefined;
    const request = schema as LegacyParameterSchema;
    const invalid = (reason: string): never => {
        throw new Error(`Invalid ${LEGACY_PARAMETERS_KEY} for ${owner}: ${reason}`);
    };
    const names = request[LEGACY_PARAMETERS_KEY];
    if (!Array.isArray(names) || !names.every((name): name is string => typeof name === "string")) {
        return invalid("expected an array of property names");
    }
    if (options.optional) return invalid("optional requests cannot declare legacy parameters");
    if (options.nullable) return invalid("nullable requests cannot declare legacy parameters");
    if (new Set(names).size !== names.length) return invalid("duplicate property names");

    const implicit = new Set(options.implicit ?? []);
    const properties = Object.keys(request.properties ?? {}).filter((name) => !implicit.has(name));
    const known = new Set(properties);
    for (const name of names) {
        if (implicit.has(name)) return invalid(`implicit property ${name} cannot be a legacy parameter`);
        if (!known.has(name)) return invalid(`unknown property ${name}`);
    }

    const required = new Set((request.required ?? []).filter((name) => !implicit.has(name)));
    const legacy = new Set(names);
    for (const name of required) {
        if (!legacy.has(name)) return invalid(`required property ${name} must be a legacy parameter`);
    }
    const additions = properties.filter((name) => !legacy.has(name));
    if (additions.length === 0) return invalid("expected at least one property added after the legacy API");
    if (options.ordered && properties.filter((name) => legacy.has(name)).some((name, index) => name !== names[index])) {
        return invalid("legacy parameters must follow schema property order");
    }

    return { legacy: names, additions, required };
}

/** Only requests the SDK sends can preserve a legacy API; handler payloads cannot opt in. */
export function rejectLegacyParameters(schema: unknown, owner: string): void {
    if (hasLegacyParameters(schema)) {
        throw new Error(`Invalid ${LEGACY_PARAMETERS_KEY} for ${owner}: only server and session requests are supported`);
    }
}

/** The schema sections that can carry requests, keyed as in `api.schema.json`. */
export interface LegacyRequestSections<Node> {
    server?: Node;
    session?: Node;
    clientSession?: Node;
    clientGlobal?: Node;
}

/**
 * Validates every request's `x-legacy-parameters` for generators whose projection needs no
 * extra API: added inputs are optional fields of the same request type.
 */
export function validateLegacyRequests<Node, Method extends { rpcMethod: string; params?: unknown }>(
    sections: LegacyRequestSections<Node>,
    collect: (node: Node) => Method[],
    params: (method: Method) => unknown,
    nullable: (method: Method) => boolean
): void {
    for (const [section, implicit] of [["server", []], ["session", ["sessionId"]]] as const) {
        const node = sections[section];
        if (!node) continue;
        for (const method of collect(node)) {
            readLegacyParameters(params(method), method.rpcMethod, {
                implicit,
                optional: isOmittableRequest(method.params),
                nullable: nullable(method),
            });
        }
    }
    for (const section of ["clientSession", "clientGlobal"] as const) {
        const node = sections[section];
        if (!node) continue;
        for (const method of collect(node)) rejectLegacyParameters(params(method), method.rpcMethod);
    }
}

/**
 * Validates `x-legacy-parameters` on every shared definition, including response records
 * that no request path reads. Generators whose projection of an added field needs no extra
 * API call this so malformed metadata fails identically in every language.
 */
export function validateLegacyDefinitions(collections: {
    definitions?: Record<string, unknown>;
    $defs?: Record<string, unknown>;
}): void {
    for (const definitions of [collections.definitions, collections.$defs]) {
        for (const [name, schema] of Object.entries(definitions ?? {})) {
            readLegacyParameters(schema, name, { implicit: ["sessionId"] });
        }
    }
}

/**
 * Property marker for a field that was published as untyped JSON. Generators whose
 * published projection was untyped keep it untyped; generators that already published a
 * typed projection keep that. Today only Rust projects differently because of it: Java
 * already publishes such fields as `List<Object>`, and C#, Go, Python and TypeScript
 * already publish the typed shape.
 */
export const LEGACY_UNTYPED_KEY = "x-legacy-untyped";

/**
 * Reads the property-level `x-legacy-untyped` marker: an existing field that was
 * published as untyped JSON stays untyped, with its typed schema exposed only as a
 * standalone type. Only `true` is valid.
 */
export function readLegacyUntyped(propertySchema: unknown, owner: string): boolean {
    if (typeof propertySchema !== "object" || propertySchema === null) return false;
    if (!(LEGACY_UNTYPED_KEY in propertySchema)) return false;
    if ((propertySchema as Record<string, unknown>)[LEGACY_UNTYPED_KEY] !== true) {
        throw new Error(`${owner}: ${LEGACY_UNTYPED_KEY} must be true`);
    }
    return true;
}

/** Validates every property-level `x-legacy-untyped` marker in a schema document. */
export function validateLegacyUntypedMarkers(root: unknown, owner = "schema"): void {
    const visit = (node: unknown, path: string): void => {
        if (Array.isArray(node)) {
            node.forEach((entry, index) => visit(entry, `${path}[${index}]`));
            return;
        }
        if (typeof node !== "object" || node === null) return;
        const record = node as Record<string, unknown>;
        const properties = record.properties;
        if (typeof properties === "object" && properties !== null && !Array.isArray(properties)) {
            for (const [name, property] of Object.entries(properties)) {
                readLegacyUntyped(property, `${path}.${name}`);
            }
        }
        for (const [key, value] of Object.entries(record)) {
            if (key !== LEGACY_UNTYPED_KEY) visit(value, `${path}/${key}`);
        }
    };
    visit(root, owner);
}
