/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Java code generator for session-events and RPC types.
 * Generates Java source files under src/generated/java/ from JSON Schema files.
 */

import fs from "fs/promises";
import path from "path";
import { fileURLToPath } from "url";
import type { JSONSchema7 } from "json-schema";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

/** Root of the copilot-sdk-java repo */
const REPO_ROOT = path.resolve(__dirname, "../..");

/** Event types to exclude from generation (internal/legacy types) */
const EXCLUDED_EVENT_TYPES = new Set(["session.import_legacy"]);

const AUTO_GENERATED_HEADER = `// AUTO-GENERATED FILE - DO NOT EDIT`;
const GENERATED_FROM_SESSION_EVENTS = `// Generated from: session-events.schema.json`;
const GENERATED_FROM_API = `// Generated from: api.schema.json`;
const GENERATED_ANNOTATION = `@javax.annotation.processing.Generated("copilot-sdk-codegen")`;
const COPYRIGHT = `/*---------------------------------------------------------------------------------------------\n *  Copyright (c) Microsoft Corporation. All rights reserved.\n *--------------------------------------------------------------------------------------------*/`;

// ── Naming utilities ─────────────────────────────────────────────────────────

function toPascalCase(name: string): string {
    return name.split(/[-_.]/).map((p) => p.charAt(0).toUpperCase() + p.slice(1)).join("");
}

function toJavaClassName(typeName: string): string {
    return typeName.split(/[._]/).map((p) => p.charAt(0).toUpperCase() + p.slice(1)).join("");
}

function toCamelCase(name: string): string {
    const pascal = toPascalCase(name);
    return pascal.charAt(0).toLowerCase() + pascal.slice(1);
}

function toEnumConstant(value: string): string {
    return value.toUpperCase().replace(/[-. /:]/g, "_").replace(/^_+/, "").replace(/_+/g, "_");
}

// ── Schema path resolution ───────────────────────────────────────────────────

async function getSessionEventsSchemaPath(): Promise<string> {
    const candidates = [
        path.join(REPO_ROOT, "scripts/codegen/node_modules/@github/copilot/schemas/session-events.schema.json"),
        path.join(REPO_ROOT, "nodejs/node_modules/@github/copilot/schemas/session-events.schema.json"),
    ];
    for (const p of candidates) {
        try {
            await fs.access(p);
            return p;
        } catch {
            // try next
        }
    }
    throw new Error("session-events.schema.json not found. Run 'npm ci' in scripts/codegen first.");
}

async function getApiSchemaPath(): Promise<string> {
    const candidates = [
        path.join(REPO_ROOT, "scripts/codegen/node_modules/@github/copilot/schemas/api.schema.json"),
        path.join(REPO_ROOT, "nodejs/node_modules/@github/copilot/schemas/api.schema.json"),
    ];
    for (const p of candidates) {
        try {
            await fs.access(p);
            return p;
        } catch {
            // try next
        }
    }
    throw new Error("api.schema.json not found. Run 'npm ci' in scripts/codegen first.");
}

// ── File writing ─────────────────────────────────────────────────────────────

async function writeGeneratedFile(relativePath: string, content: string): Promise<string> {
    const fullPath = path.join(REPO_ROOT, relativePath);
    await fs.mkdir(path.dirname(fullPath), { recursive: true });
    await fs.writeFile(fullPath, content, "utf-8");
    console.log(`  ✓ ${relativePath}`);
    return fullPath;
}

// ── Java type mapping ─────────────────────────────────────────────────────────

interface JavaTypeResult {
    javaType: string;
    imports: Set<string>;
}

// Module-level state for $ref resolution during codegen.
// Set before each schema generation pass; used by schemaTypeToJava and helpers.
let currentDefinitions: Record<string, JSONSchema7> = {};
const pendingStandaloneTypes = new Map<string, JSONSchema7>();

/**
 * Resolve a $ref in a JSON Schema against the current definitions.
 * Returns the resolved schema, or the original if no $ref is present.
 */
function resolveRef(schema: JSONSchema7 | undefined): JSONSchema7 | undefined {
    if (!schema) return schema;
    if (schema.$ref) {
        const name = schema.$ref.replace(/^#\/definitions\//, "");
        const resolved = currentDefinitions[name];
        if (!resolved) {
            console.warn(`[codegen] Unresolved $ref: ${schema.$ref}`);
            return schema;
        }
        return resolved;
    }
    return schema;
}

function schemaTypeToJava(
    schema: JSONSchema7,
    required: boolean,
    context: string,
    propName: string,
    nestedTypes: Map<string, JavaClassDef>
): JavaTypeResult {
    const imports = new Set<string>();

    // Resolve $ref first — register standalone types for generation
    if (schema.$ref) {
        const name = schema.$ref.replace(/^#\/definitions\//, "");
        const resolved = currentDefinitions[name];
        if (resolved) {
            // Enum or object types → register for standalone generation, return ref name
            if ((resolved.type === "string" && resolved.enum) ||
                (resolved.type === "object" && resolved.properties)) {
                pendingStandaloneTypes.set(name, resolved);
                return { javaType: name, imports };
            }
            // Other types (primitives, arrays, maps, anyOf unions) → resolve and recurse
            return schemaTypeToJava(resolved, required, context, propName, nestedTypes);
        }
        // Unresolved $ref — return name as-is
        console.warn(`[codegen] Unresolved $ref: ${schema.$ref}`);
        return { javaType: name, imports };
    }

    if (schema.anyOf) {
        const hasNull = schema.anyOf.some((s) => typeof s === "object" && (s as JSONSchema7).type === "null");
        const nonNull = schema.anyOf.filter((s) => typeof s === "object" && (s as JSONSchema7).type !== "null");
        if (nonNull.length === 1) {
            const result = schemaTypeToJava(nonNull[0] as JSONSchema7, required && !hasNull, context, propName, nestedTypes);
            return result;
        }
        // Multi-branch anyOf: fall through to Object, matching the C# generator's
        // behavior.  Java has no union types, so Object is the correct erasure for
        // anyOf[string, object] and similar multi-variant schemas.
        console.warn(`[codegen] ${context}.${propName}: anyOf with ${nonNull.length} non-null branches — falling back to Object`);
        return { javaType: "Object", imports };
    }

    if (schema.type === "string") {
        if (schema.format === "uuid") {
            imports.add("java.util.UUID");
            return { javaType: "UUID", imports };
        }
        if (schema.format === "date-time") {
            imports.add("java.time.OffsetDateTime");
            return { javaType: "OffsetDateTime", imports };
        }
        if (schema.enum && Array.isArray(schema.enum)) {
            const enumName = `${context}${toPascalCase(propName)}`;
            nestedTypes.set(enumName, {
                kind: "enum",
                name: enumName,
                values: schema.enum as string[],
                description: schema.description,
            });
            return { javaType: enumName, imports };
        }
        return { javaType: "String", imports };
    }

    if (Array.isArray(schema.type)) {
        const nonNullTypes = schema.type.filter((t) => t !== "null");
        if (nonNullTypes.length === 1) {
            const baseSchema = { ...schema, type: nonNullTypes[0] };
            return schemaTypeToJava(baseSchema as JSONSchema7, required, context, propName, nestedTypes);
        }
    }

    if (schema.type === "integer") {
        // JSON Schema "integer" maps to Long (boxed — always used for records).
        // Use primitive long for required fields in mutable-bean contexts if needed.
        return { javaType: required ? "long" : "Long", imports };
    }

    if (schema.type === "number") {
        return { javaType: required ? "double" : "Double", imports };
    }

    if (schema.type === "boolean") {
        return { javaType: required ? "boolean" : "Boolean", imports };
    }

    if (schema.type === "array") {
        const items = schema.items as JSONSchema7 | undefined;
        if (items) {
            // Always pass required=false so primitives are boxed (List<Long>, not List<long>)
            const itemResult = schemaTypeToJava(items, false, context, propName + "Item", nestedTypes);
            imports.add("java.util.List");
            for (const imp of itemResult.imports) imports.add(imp);
            return { javaType: `List<${itemResult.javaType}>`, imports };
        }
        imports.add("java.util.List");
        console.warn(`[codegen] ${context}.${propName}: array without typed items — falling back to List<Object>`);
        return { javaType: "List<Object>", imports };
    }

    if (schema.type === "object") {
        if (schema.properties && Object.keys(schema.properties).length > 0) {
            const nestedName = `${context}${toPascalCase(propName)}`;
            if (!nestedTypes.has(nestedName)) {
                nestedTypes.set(nestedName, {
                    kind: "class",
                    name: nestedName,
                    schema,
                    description: schema.description,
                });
            }
            return { javaType: nestedName, imports };
        }
        if (schema.additionalProperties) {
            const valueSchema = typeof schema.additionalProperties === "object"
                ? schema.additionalProperties as JSONSchema7
                : { type: "object" } as JSONSchema7;
            // Always pass required=false so primitives are boxed (Map<String, Long>, not Map<String, long>)
            const valueResult = schemaTypeToJava(valueSchema, false, context, propName + "Value", nestedTypes);
            imports.add("java.util.Map");
            for (const imp of valueResult.imports) imports.add(imp);
            return { javaType: `Map<String, ${valueResult.javaType}>`, imports };
        }
        imports.add("java.util.Map");
        console.warn(`[codegen] ${context}.${propName}: object without typed properties or additionalProperties — falling back to Map<String, Object>`);
        return { javaType: "Map<String, Object>", imports };
    }

    console.warn(`[codegen] ${context}.${propName}: unrecognized schema (type=${JSON.stringify(schema.type)}) — falling back to Object`);
    return { javaType: "Object", imports };
}

// ── Class definitions ─────────────────────────────────────────────────────────

interface JavaClassDef {
    kind: "class" | "enum";
    name: string;
    description?: string;
    schema?: JSONSchema7;
    values?: string[];  // for enum
}

// ── Session Events codegen ────────────────────────────────────────────────────

interface EventVariant {
    typeName: string;
    className: string;
    dataSchema: JSONSchema7 | null;
    description?: string;
}

function extractEventVariants(schema: JSONSchema7): EventVariant[] {
    const definitions = schema.definitions as Record<string, JSONSchema7>;
    const sessionEvent = definitions?.SessionEvent;
    if (!sessionEvent?.anyOf) throw new Error("Schema must have SessionEvent definition with anyOf");

    return (sessionEvent.anyOf as JSONSchema7[])
        .map((variant) => {
            // Resolve $ref if present (1.0.35+ schema uses $ref to named definitions)
            let resolved = variant;
            if (variant.$ref) {
                const refName = variant.$ref.replace(/^#\/definitions\//, "");
                resolved = definitions[refName];
                if (!resolved) throw new Error(`Unresolved $ref: ${variant.$ref}`);
            }
            const typeSchema = resolved.properties?.type as JSONSchema7;
            const typeName = typeSchema?.const as string;
            if (!typeName) throw new Error("Variant must have type.const");
            const baseName = toJavaClassName(typeName);
            let dataSchema = resolved.properties?.data as JSONSchema7 | undefined;
            // Resolve $ref on data schema if present
            if (dataSchema?.$ref) {
                const dataRefName = dataSchema.$ref.replace(/^#\/definitions\//, "");
                dataSchema = definitions[dataRefName];
            }
            return {
                typeName,
                className: `${baseName}Event`,
                dataSchema: dataSchema ?? null,
                description: resolved.description,
            };
        })
        .filter((v) => !EXCLUDED_EVENT_TYPES.has(v.typeName));
}

async function generateSessionEvents(schemaPath: string): Promise<void> {
    console.log("\n📋 Generating session event classes...");
    const schemaContent = await fs.readFile(schemaPath, "utf-8");
    const schema = JSON.parse(schemaContent) as JSONSchema7;

    // Set module-level definitions for $ref resolution
    currentDefinitions = (schema.definitions ?? {}) as Record<string, JSONSchema7>;
    pendingStandaloneTypes.clear();

    const variants = extractEventVariants(schema);
    const packageName = "com.github.copilot.sdk.generated";
    const packageDir = `src/generated/java/com/github/copilot/sdk/generated`;

    // Generate base SessionEvent class
    await generateSessionEventBaseClass(variants, packageName, packageDir);

    // Generate one class file per event variant
    for (const variant of variants) {
        await generateEventVariantClass(variant, packageName, packageDir);
    }

    // Generate standalone types discovered via $ref resolution
    await generatePendingStandaloneTypes(packageName, packageDir, GENERATED_FROM_SESSION_EVENTS);

    console.log(`✅ Generated ${variants.length + 1} session event files`);
}

async function generateSessionEventBaseClass(
    variants: EventVariant[],
    packageName: string,
    packageDir: string
): Promise<void> {
    const lines: string[] = [];
    lines.push(COPYRIGHT);
    lines.push("");
    lines.push(AUTO_GENERATED_HEADER);
    lines.push(GENERATED_FROM_SESSION_EVENTS);
    lines.push("");
    lines.push(`package ${packageName};`);
    lines.push("");
    lines.push(`import com.fasterxml.jackson.annotation.JsonIgnoreProperties;`);
    lines.push(`import com.fasterxml.jackson.annotation.JsonInclude;`);
    lines.push(`import com.fasterxml.jackson.annotation.JsonProperty;`);
    lines.push(`import com.fasterxml.jackson.annotation.JsonSubTypes;`);
    lines.push(`import com.fasterxml.jackson.annotation.JsonTypeInfo;`);
    lines.push(`import java.time.OffsetDateTime;`);
    lines.push(`import java.util.UUID;`);
    lines.push(`import javax.annotation.processing.Generated;`);
    lines.push("");
    lines.push(`/**`);
    lines.push(` * Base class for all generated session events.`);
    lines.push(` *`);
    lines.push(` * @since 1.0.0`);
    lines.push(` */`);
    lines.push(`@JsonIgnoreProperties(ignoreUnknown = true)`);
    lines.push(`@JsonInclude(JsonInclude.Include.NON_NULL)`);
    lines.push(`@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", visible = true, defaultImpl = UnknownSessionEvent.class)`);
    lines.push(`@JsonSubTypes({`);
    for (let i = 0; i < variants.length; i++) {
        const v = variants[i];
        const comma = i < variants.length - 1 ? "," : "";
        lines.push(`    @JsonSubTypes.Type(value = ${v.className}.class, name = "${v.typeName}")${comma}`);
    }
    lines.push(`})`);
    lines.push(GENERATED_ANNOTATION);

    // Build the permits clause (all variant classes + UnknownSessionEvent last)
    const allPermitted = [...variants.map((v) => v.className), "UnknownSessionEvent"];
    lines.push(`public abstract sealed class SessionEvent permits`);
    for (let i = 0; i < allPermitted.length; i++) {
        const comma = i < allPermitted.length - 1 ? "," : " {";
        lines.push(`        ${allPermitted[i]}${comma}`);
    }
    lines.push("");
    lines.push(`    /** Unique event identifier (UUID v4), generated when the event is emitted. */`);
    lines.push(`    @JsonProperty("id")`);
    lines.push(`    private UUID id;`);
    lines.push("");
    lines.push(`    /** ISO 8601 timestamp when the event was created. */`);
    lines.push(`    @JsonProperty("timestamp")`);
    lines.push(`    private OffsetDateTime timestamp;`);
    lines.push("");
    lines.push(`    /** ID of the chronologically preceding event in the session. Null for the first event. */`);
    lines.push(`    @JsonProperty("parentId")`);
    lines.push(`    private UUID parentId;`);
    lines.push("");
    lines.push(`    /** When true, the event is transient and not persisted to the session event log on disk. */`);
    lines.push(`    @JsonProperty("ephemeral")`);
    lines.push(`    private Boolean ephemeral;`);
    lines.push("");
    lines.push(`    /**`);
    lines.push(`     * Returns the event-type discriminator string (e.g., {@code "session.idle"}).`);
    lines.push(`     *`);
    lines.push(`     * @return the event type`);
    lines.push(`     */`);
    lines.push(`    public abstract String getType();`);
    lines.push("");
    lines.push(`    public UUID getId() { return id; }`);
    lines.push(`    public void setId(UUID id) { this.id = id; }`);
    lines.push("");
    lines.push(`    public OffsetDateTime getTimestamp() { return timestamp; }`);
    lines.push(`    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }`);
    lines.push("");
    lines.push(`    public UUID getParentId() { return parentId; }`);
    lines.push(`    public void setParentId(UUID parentId) { this.parentId = parentId; }`);
    lines.push("");
    lines.push(`    public Boolean getEphemeral() { return ephemeral; }`);
    lines.push(`    public void setEphemeral(Boolean ephemeral) { this.ephemeral = ephemeral; }`);
    lines.push(`}`);
    lines.push("");

    await writeGeneratedFile(`${packageDir}/SessionEvent.java`, lines.join("\n"));

    // Also generate the UnknownSessionEvent fallback
    await generateUnknownEventClass(packageName, packageDir);
}

async function generateUnknownEventClass(packageName: string, packageDir: string): Promise<void> {
    const lines: string[] = [];
    lines.push(COPYRIGHT);
    lines.push("");
    lines.push(AUTO_GENERATED_HEADER);
    lines.push(GENERATED_FROM_SESSION_EVENTS);
    lines.push("");
    lines.push(`package ${packageName};`);
    lines.push("");
    lines.push(`import com.fasterxml.jackson.annotation.JsonIgnoreProperties;`);
    lines.push(`import com.fasterxml.jackson.annotation.JsonProperty;`);
    lines.push(`import javax.annotation.processing.Generated;`);
    lines.push("");
    lines.push(`/**`);
    lines.push(` * Fallback for event types not yet known to this SDK version.`);
    lines.push(` * <p>`);
    lines.push(` * {@link #getType()} returns the original type string from the JSON payload,`);
    lines.push(` * preserving forward compatibility with event types introduced by newer CLI versions.`);
    lines.push(` *`);
    lines.push(` * @since 1.0.0`);
    lines.push(` */`);
    lines.push(`@JsonIgnoreProperties(ignoreUnknown = true)`);
    lines.push(GENERATED_ANNOTATION);
    lines.push(`public final class UnknownSessionEvent extends SessionEvent {`);
    lines.push("");
    lines.push(`    @JsonProperty("type")`);
    lines.push(`    private String type = "unknown";`);
    lines.push("");
    lines.push(`    @Override`);
    lines.push(`    public String getType() { return type; }`);
    lines.push(`}`);
    lines.push("");

    await writeGeneratedFile(`${packageDir}/UnknownSessionEvent.java`, lines.join("\n"));
}

/** Render a nested type (enum or record) indented at the given level. */
function renderNestedType(nested: JavaClassDef, indentLevel: number, nestedTypes: Map<string, JavaClassDef>, allImports: Set<string>): string[] {
    const ind = "    ".repeat(indentLevel);
    const lines: string[] = [];

    if (nested.kind === "enum") {
        lines.push("");
        if (nested.description) {
            lines.push(`${ind}/** ${nested.description} */`);
        }
        lines.push(`${ind}public enum ${nested.name} {`);
        for (let i = 0; i < (nested.values || []).length; i++) {
            const v = nested.values![i];
            const comma = i < nested.values!.length - 1 ? "," : ";";
            lines.push(`${ind}    /** The {@code ${v}} variant. */`);
            lines.push(`${ind}    ${toEnumConstant(v)}("${v}")${comma}`);
        }
        lines.push("");
        lines.push(`${ind}    private final String value;`);
        lines.push(`${ind}    ${nested.name}(String value) { this.value = value; }`);
        lines.push(`${ind}    @com.fasterxml.jackson.annotation.JsonValue`);
        lines.push(`${ind}    public String getValue() { return value; }`);
        lines.push(`${ind}    @com.fasterxml.jackson.annotation.JsonCreator`);
        lines.push(`${ind}    public static ${nested.name} fromValue(String value) {`);
        lines.push(`${ind}        for (${nested.name} v : values()) {`);
        lines.push(`${ind}            if (v.value.equals(value)) return v;`);
        lines.push(`${ind}        }`);
        lines.push(`${ind}        throw new IllegalArgumentException("Unknown ${nested.name} value: " + value);`);
        lines.push(`${ind}    }`);
        lines.push(`${ind}}`);
    } else if (nested.kind === "class" && nested.schema?.properties) {
        const localNestedTypes = new Map<string, JavaClassDef>();
        const fields: { jsonName: string; javaName: string; javaType: string; description?: string }[] = [];

        for (const [propName, propSchema] of Object.entries(nested.schema.properties)) {
            if (typeof propSchema !== "object") continue;
            const prop = propSchema as JSONSchema7;
            // Record components are always boxed (nullable by design).
            const result = schemaTypeToJava(prop, false, nested.name, propName, localNestedTypes);
            for (const imp of result.imports) allImports.add(imp);
            fields.push({ jsonName: propName, javaName: toCamelCase(propName), javaType: result.javaType, description: prop.description });
        }

        lines.push("");
        if (nested.description) {
            lines.push(`${ind}/** ${nested.description} */`);
        }
        lines.push(`${ind}@JsonIgnoreProperties(ignoreUnknown = true)`);
        lines.push(`${ind}@JsonInclude(JsonInclude.Include.NON_NULL)`);
        if (fields.length === 0) {
            lines.push(`${ind}public record ${nested.name}() {`);
        } else {
            lines.push(`${ind}public record ${nested.name}(`);
            for (let i = 0; i < fields.length; i++) {
                const f = fields[i];
                const comma = i < fields.length - 1 ? "," : "";
                if (f.description) lines.push(`${ind}    /** ${f.description} */`);
                lines.push(`${ind}    @JsonProperty("${f.jsonName}") ${f.javaType} ${f.javaName}${comma}`);
            }
            lines.push(`${ind}) {`);
        }
        // Render any further nested types inside this record
        for (const [, localNested] of localNestedTypes) {
            lines.push(...renderNestedType(localNested, indentLevel + 1, nestedTypes, allImports));
        }
        if (lines[lines.length - 1] !== "") lines.push("");
        lines.pop(); // remove trailing blank before closing brace
        lines.push(`${ind}}`);
    }

    return lines;
}

async function generateEventVariantClass(
    variant: EventVariant,
    packageName: string,
    packageDir: string
): Promise<void> {
    const lines: string[] = [];
    const allImports = new Set<string>([
        "com.fasterxml.jackson.annotation.JsonIgnoreProperties",
        "com.fasterxml.jackson.annotation.JsonProperty",
        "com.fasterxml.jackson.annotation.JsonInclude",
        "javax.annotation.processing.Generated",
    ]);
    const nestedTypes = new Map<string, JavaClassDef>();

    // Collect data record fields
    interface FieldInfo {
        jsonName: string;
        javaName: string;
        javaType: string;
        description?: string;
    }

    const dataFields: FieldInfo[] = [];

    if (variant.dataSchema?.properties) {
        for (const [propName, propSchema] of Object.entries(variant.dataSchema.properties)) {
            if (typeof propSchema !== "object") continue;
            const prop = propSchema as JSONSchema7;
            // Record components are always boxed (nullable by design).
            const result = schemaTypeToJava(prop, false, `${variant.className}Data`, propName, nestedTypes);
            for (const imp of result.imports) allImports.add(imp);
            dataFields.push({
                jsonName: propName,
                javaName: toCamelCase(propName),
                javaType: result.javaType,
                description: prop.description,
            });
        }
    }

    // Whether a data record should be emitted (always when dataSchema is present)
    const hasDataSchema = variant.dataSchema !== null;

    // Build the file
    lines.push(COPYRIGHT);
    lines.push("");
    lines.push(AUTO_GENERATED_HEADER);
    lines.push(GENERATED_FROM_SESSION_EVENTS);
    lines.push("");
    lines.push(`package ${packageName};`);
    lines.push("");

    // Placeholder for imports
    const importPlaceholderIdx = lines.length;
    lines.push("__IMPORTS__");
    lines.push("");

    if (variant.description) {
        lines.push(`/**`);
        lines.push(` * ${variant.description}`);
        lines.push(` *`);
        lines.push(` * @since 1.0.0`);
        lines.push(` */`);
    } else {
        lines.push(`/**`);
        lines.push(` * The {@code ${variant.typeName}} session event.`);
        lines.push(` *`);
        lines.push(` * @since 1.0.0`);
        lines.push(` */`);
    }
    lines.push(`@JsonIgnoreProperties(ignoreUnknown = true)`);
    lines.push(`@JsonInclude(JsonInclude.Include.NON_NULL)`);
    lines.push(GENERATED_ANNOTATION);
    lines.push(`public final class ${variant.className} extends SessionEvent {`);
    lines.push("");
    lines.push(`    @Override`);
    lines.push(`    public String getType() { return "${variant.typeName}"; }`);

    if (hasDataSchema) {
        lines.push("");
        lines.push(`    @JsonProperty("data")`);
        lines.push(`    private ${variant.className}Data data;`);
        lines.push("");
        lines.push(`    public ${variant.className}Data getData() { return data; }`);
        lines.push(`    public void setData(${variant.className}Data data) { this.data = data; }`);
        lines.push("");
        // Generate data inner record
        lines.push(`    /** Data payload for {@link ${variant.className}}. */`);
        lines.push(`    @JsonIgnoreProperties(ignoreUnknown = true)`);
        lines.push(`    @JsonInclude(JsonInclude.Include.NON_NULL)`);
        if (dataFields.length === 0) {
            lines.push(`    public record ${variant.className}Data() {`);
        } else {
            lines.push(`    public record ${variant.className}Data(`);
            for (let i = 0; i < dataFields.length; i++) {
                const field = dataFields[i];
                const comma = i < dataFields.length - 1 ? "," : "";
                if (field.description) {
                    lines.push(`        /** ${field.description} */`);
                }
                lines.push(`        @JsonProperty("${field.jsonName}") ${field.javaType} ${field.javaName}${comma}`);
            }
            lines.push(`    ) {`);
        }
        // Render nested types inside Data record
        for (const [, nested] of nestedTypes) {
            lines.push(...renderNestedType(nested, 2, nestedTypes, allImports));
        }
        if (nestedTypes.size > 0 && lines[lines.length - 1] === "") lines.pop();
        lines.push(`    }`);
    }

    lines.push(`}`);
    lines.push("");

    // Replace import placeholder
    const sortedImports = [...allImports].sort();
    const importLines = sortedImports.map((i) => `import ${i};`).join("\n");
    lines[importPlaceholderIdx] = importLines;

    await writeGeneratedFile(`${packageDir}/${variant.className}.java`, lines.join("\n"));
}

// ── Standalone $ref type generation ──────────────────────────────────────────

/**
 * Generate all pending standalone types discovered via $ref resolution.
 * Iterates until no new types are discovered (handles transitive $ref chains).
 */
async function generatePendingStandaloneTypes(
    packageName: string,
    packageDir: string,
    headerComment: string
): Promise<void> {
    const generated = new Set<string>();

    while (true) {
        const batch: [string, JSONSchema7][] = [];
        for (const [name, schema] of pendingStandaloneTypes) {
            if (!generated.has(name)) {
                batch.push([name, schema]);
                generated.add(name);
            }
        }
        pendingStandaloneTypes.clear();

        if (batch.length === 0) break;

        for (const [name, schema] of batch) {
            if (schema.type === "string" && schema.enum) {
                await generateStandaloneEnum(name, schema, packageName, packageDir, headerComment);
            } else if (schema.type === "object" && schema.properties) {
                await generateStandaloneRecord(name, schema, packageName, packageDir, headerComment);
            } else {
                console.warn(`[codegen] Cannot generate standalone type for ${name}: type=${schema.type}`);
            }
        }
        // Generating records may have discovered more $ref targets — loop again
    }
}

async function generateStandaloneEnum(
    name: string,
    schema: JSONSchema7,
    packageName: string,
    packageDir: string,
    headerComment: string
): Promise<void> {
    const values = schema.enum as string[];
    const lines: string[] = [];
    lines.push(COPYRIGHT);
    lines.push("");
    lines.push(AUTO_GENERATED_HEADER);
    lines.push(headerComment);
    lines.push("");
    lines.push(`package ${packageName};`);
    lines.push("");
    lines.push(`import javax.annotation.processing.Generated;`);
    lines.push("");
    if (schema.description) {
        lines.push(`/**`);
        lines.push(` * ${schema.description}`);
        lines.push(` *`);
        lines.push(` * @since 1.0.0`);
        lines.push(` */`);
    }
    lines.push(GENERATED_ANNOTATION);
    lines.push(`public enum ${name} {`);
    for (let i = 0; i < values.length; i++) {
        const v = values[i];
        const comma = i < values.length - 1 ? "," : ";";
        lines.push(`    /** The {@code ${v}} variant. */`);
        lines.push(`    ${toEnumConstant(v)}("${v}")${comma}`);
    }
    lines.push("");
    lines.push(`    private final String value;`);
    lines.push(`    ${name}(String value) { this.value = value; }`);
    lines.push(`    @com.fasterxml.jackson.annotation.JsonValue`);
    lines.push(`    public String getValue() { return value; }`);
    lines.push(`    @com.fasterxml.jackson.annotation.JsonCreator`);
    lines.push(`    public static ${name} fromValue(String value) {`);
    lines.push(`        for (${name} v : values()) {`);
    lines.push(`            if (v.value.equals(value)) return v;`);
    lines.push(`        }`);
    lines.push(`        throw new IllegalArgumentException("Unknown ${name} value: " + value);`);
    lines.push(`    }`);
    lines.push(`}`);
    lines.push("");

    await writeGeneratedFile(`${packageDir}/${name}.java`, lines.join("\n"));
}

async function generateStandaloneRecord(
    name: string,
    schema: JSONSchema7,
    packageName: string,
    packageDir: string,
    headerComment: string
): Promise<void> {
    const nestedTypes = new Map<string, { code: string }>();
    const { code, imports } = generateRpcClass(name, schema, nestedTypes, packageName);

    const lines: string[] = [];
    lines.push(COPYRIGHT);
    lines.push("");
    lines.push(AUTO_GENERATED_HEADER);
    lines.push(headerComment);
    lines.push("");
    lines.push(`package ${packageName};`);
    lines.push("");

    const allImports = new Set<string>([
        "com.fasterxml.jackson.annotation.JsonIgnoreProperties",
        "com.fasterxml.jackson.annotation.JsonProperty",
        "com.fasterxml.jackson.annotation.JsonInclude",
        "javax.annotation.processing.Generated",
        ...imports,
    ]);
    const sortedImports = [...allImports].sort();
    for (const imp of sortedImports) {
        lines.push(`import ${imp};`);
    }
    lines.push("");

    if (schema.description) {
        lines.push(`/**`);
        lines.push(` * ${schema.description}`);
        lines.push(` *`);
        lines.push(` * @since 1.0.0`);
        lines.push(` */`);
    }
    lines.push(GENERATED_ANNOTATION);
    lines.push(code);
    lines.push("");

    await writeGeneratedFile(`${packageDir}/${name}.java`, lines.join("\n"));
}

// ── RPC types codegen ─────────────────────────────────────────────────────────

interface RpcMethod {
    rpcMethod: string;
    params: JSONSchema7 | null;
    result: JSONSchema7 | null;
    stability?: string;
}

function isRpcMethod(node: unknown): node is RpcMethod {
    return typeof node === "object" && node !== null && "rpcMethod" in node;
}

function collectRpcMethods(node: Record<string, unknown>): [string, RpcMethod][] {
    const results: [string, RpcMethod][] = [];
    for (const [key, value] of Object.entries(node)) {
        if (isRpcMethod(value)) {
            results.push([key, value]);
        } else if (typeof value === "object" && value !== null) {
            results.push(...collectRpcMethods(value as Record<string, unknown>));
        }
    }
    return results;
}

/** Convert an RPC method name to a Java class name prefix (e.g., "models.list" -> "ModelsList") */
function rpcMethodToClassName(rpcMethod: string): string {
    return rpcMethod.split(/[._-]/).map((p) => p.charAt(0).toUpperCase() + p.slice(1)).join("");
}

/** Generate a Java record for a JSON Schema object type. Returns the class content. */
function generateRpcClass(
    className: string,
    schema: JSONSchema7,
    _nestedTypes: Map<string, { code: string }>,
    _packageName: string,
    visibility: "public" | "internal" = "public"
): { code: string; imports: Set<string> } {
    const imports = new Set<string>();
    const localNestedTypes = new Map<string, JavaClassDef>();
    const lines: string[] = [];
    const visModifier = visibility === "public" ? "public " : "";

    const properties = Object.entries(schema.properties || {});
    const fields = properties.flatMap(([propName, propSchema]) => {
        if (typeof propSchema !== "object") return [];
        const prop = propSchema as JSONSchema7;
        // Record components are always boxed (nullable by design).
        const result = schemaTypeToJava(prop, false, className, propName, localNestedTypes);
        for (const imp of result.imports) imports.add(imp);
        return [{ propName, javaName: toCamelCase(propName), javaType: result.javaType, description: prop.description }];
    });

    lines.push(`@JsonInclude(JsonInclude.Include.NON_NULL)`);
    lines.push(`@JsonIgnoreProperties(ignoreUnknown = true)`);
    if (fields.length === 0) {
        lines.push(`${visModifier}record ${className}() {`);
    } else {
        lines.push(`${visModifier}record ${className}(`);
        for (let i = 0; i < fields.length; i++) {
            const f = fields[i];
            const comma = i < fields.length - 1 ? "," : "";
            if (f.description) {
                lines.push(`    /** ${f.description} */`);
            }
            lines.push(`    @JsonProperty("${f.propName}") ${f.javaType} ${f.javaName}${comma}`);
        }
        lines.push(`) {`);
    }

    // Add nested types as nested records/enums inside this record
    for (const [, nested] of localNestedTypes) {
        lines.push(...renderNestedType(nested, 1, new Map(), imports));
    }

    if (localNestedTypes.size > 0 && lines[lines.length - 1] === "") lines.pop();
    lines.push(`}`);

    return { code: lines.join("\n"), imports };
}

async function generateRpcTypes(schemaPath: string): Promise<void> {
    console.log("\n🔌 Generating RPC types...");
    const schemaContent = await fs.readFile(schemaPath, "utf-8");
    const schema = JSON.parse(schemaContent) as Record<string, unknown> & {
        server?: Record<string, unknown>;
        session?: Record<string, unknown>;
        clientSession?: Record<string, unknown>;
        definitions?: Record<string, JSONSchema7>;
    };

    // Set module-level definitions for $ref resolution
    currentDefinitions = (schema.definitions ?? {}) as Record<string, JSONSchema7>;
    pendingStandaloneTypes.clear();

    const packageName = "com.github.copilot.sdk.generated.rpc";
    const packageDir = `src/generated/java/com/github/copilot/sdk/generated/rpc`;

    // Collect all RPC methods from all sections
    const sections: [string, Record<string, unknown>][] = [];
    if (schema.server) sections.push(["server", schema.server]);
    if (schema.session) sections.push(["session", schema.session]);
    if (schema.clientSession) sections.push(["clientSession", schema.clientSession]);

    const generatedClasses = new Map<string, boolean>();
    const allFiles: string[] = [];

    for (const [, sectionNode] of sections) {
        const methods = collectRpcMethods(sectionNode);
        for (const [, method] of methods) {
            const className = rpcMethodToClassName(method.rpcMethod);

            // Generate params class — resolve $ref if params is a reference
            let paramsSchema = method.params as JSONSchema7 | null;
            if (paramsSchema?.$ref) paramsSchema = resolveRef(paramsSchema) as JSONSchema7;
            if (paramsSchema && typeof paramsSchema === "object" && paramsSchema.properties) {
                const paramsClassName = `${className}Params`;
                if (!generatedClasses.has(paramsClassName)) {
                    generatedClasses.set(paramsClassName, true);
                    allFiles.push(await generateRpcDataClass(paramsClassName, paramsSchema, packageName, packageDir, method.rpcMethod, "params"));
                }
            }

            // Generate result class — resolve $ref if result is a reference
            let resultSchema = method.result as JSONSchema7 | null;
            if (resultSchema?.$ref) resultSchema = resolveRef(resultSchema) as JSONSchema7;
            if (resultSchema && typeof resultSchema === "object" && resultSchema.properties) {
                const resultClassName = `${className}Result`;
                if (!generatedClasses.has(resultClassName)) {
                    generatedClasses.set(resultClassName, true);
                    allFiles.push(await generateRpcDataClass(resultClassName, resultSchema, packageName, packageDir, method.rpcMethod, "result"));
                }
            }
        }
    }

    // Generate standalone types discovered via $ref resolution
    await generatePendingStandaloneTypes(packageName, packageDir, GENERATED_FROM_API);

    console.log(`✅ Generated ${allFiles.length} RPC type files`);
}

async function generateRpcDataClass(
    className: string,
    schema: JSONSchema7,
    packageName: string,
    packageDir: string,
    rpcMethod: string,
    kind: "params" | "result"
): Promise<string> {
    const nestedTypes = new Map<string, { code: string }>();
    const { code, imports } = generateRpcClass(className, schema, nestedTypes, packageName);

    const lines: string[] = [];
    lines.push(COPYRIGHT);
    lines.push("");
    lines.push(AUTO_GENERATED_HEADER);
    lines.push(GENERATED_FROM_API);
    lines.push("");
    lines.push(`package ${packageName};`);
    lines.push("");

    const allImports = new Set<string>([
        "com.fasterxml.jackson.annotation.JsonIgnoreProperties",
        "com.fasterxml.jackson.annotation.JsonProperty",
        "com.fasterxml.jackson.annotation.JsonInclude",
        "javax.annotation.processing.Generated",
        ...imports,
    ]);
    const sortedImports = [...allImports].sort();
    for (const imp of sortedImports) {
        lines.push(`import ${imp};`);
    }
    lines.push("");

    if (schema.description) {
        lines.push(`/**`);
        lines.push(` * ${schema.description}`);
        lines.push(` *`);
        lines.push(` * @since 1.0.0`);
        lines.push(` */`);
    } else {
        lines.push(`/**`);
        lines.push(` * ${kind === "params" ? "Request parameters" : "Result"} for the {@code ${rpcMethod}} RPC method.`);
        lines.push(` *`);
        lines.push(` * @since 1.0.0`);
        lines.push(` */`);
    }
    lines.push(GENERATED_ANNOTATION);
    lines.push(code);
    lines.push("");

    await writeGeneratedFile(`${packageDir}/${className}.java`, lines.join("\n"));
    return className;
}

// ── RPC wrapper generation ───────────────────────────────────────────────────

/** A single RPC method node parsed from the schema */
interface RpcMethodNode {
    rpcMethod: string;
    stability: string;
    params: JSONSchema7 | null;
    result: JSONSchema7 | null;
}

/** Namespace tree node: holds direct methods and sub-namespace trees */
interface NamespaceTree {
    methods: Map<string, RpcMethodNode>;   // leaf method name -> info
    subspaces: Map<string, NamespaceTree>; // sub-namespace name -> tree
}

/** Build a namespace tree by recursively walking a schema section object */
function buildNamespaceTree(node: Record<string, unknown>): NamespaceTree {
    const tree: NamespaceTree = { methods: new Map(), subspaces: new Map() };
    for (const [key, value] of Object.entries(node)) {
        if (typeof value !== "object" || value === null) continue;
        const obj = value as Record<string, unknown>;
        if ("rpcMethod" in obj) {
            tree.methods.set(key, {
                rpcMethod: String(obj.rpcMethod),
                stability: String(obj.stability ?? "stable"),
                params: (obj.params as JSONSchema7) ?? null,
                result: (obj.result as JSONSchema7) ?? null,
            });
        } else {
            const child = buildNamespaceTree(obj);
            // Only add non-empty sub-trees
            if (child.methods.size > 0 || child.subspaces.size > 0) {
                tree.subspaces.set(key, child);
            }
        }
    }
    return tree;
}

/**
 * Derive the Java class name for an API namespace class.
 * e.g., prefix="Server", path=["mcp","config"] → "ServerMcpConfigApi"
 */
function apiClassName(prefix: string, path: string[]): string {
    const parts = [prefix, ...path].map((p) => p.charAt(0).toUpperCase() + p.slice(1));
    return parts.join("") + "Api";
}

/**
 * Derive the result class name for an RPC method.
 * If the result schema has no properties we use Void; if no result schema we also use Void.
 */
function wrapperResultClassName(method: RpcMethodNode): string {
    let result = method.result;
    if (result?.$ref) result = resolveRef(result) as JSONSchema7;
    if (
        result &&
        typeof result === "object" &&
        result.properties &&
        Object.keys(result.properties).length > 0
    ) {
        return rpcMethodToClassName(method.rpcMethod) + "Result";
    }
    return "Void";
}

/**
 * Return the params class name if the method has a params schema with properties
 * other than sessionId (i.e. there are user-supplied parameters).
 */
function wrapperParamsClassName(method: RpcMethodNode): string | null {
    let params = method.params;
    if (params?.$ref) params = resolveRef(params) as JSONSchema7;
    if (!params || typeof params !== "object") return null;
    const props = params.properties ?? {};
    const userProps = Object.keys(props).filter((k) => k !== "sessionId");
    if (userProps.length === 0) return null;
    return rpcMethodToClassName(method.rpcMethod) + "Params";
}

/** True if the method's params schema contains a "sessionId" property */
function methodHasSessionId(method: RpcMethodNode): boolean {
    let params = method.params;
    if (params?.$ref) params = resolveRef(params) as JSONSchema7;
    return !!params?.properties && "sessionId" in params.properties;
}

/**
 * Generate the Java source for a single method in a wrapper API class.
 * Returns the Java source lines and whether an ObjectMapper is required.
 */
function generateApiMethod(
    key: string,
    method: RpcMethodNode,
    isSession: boolean,
    sessionIdExpr: string
): { lines: string[]; needsMapper: boolean } {
    const resultClass = wrapperResultClassName(method);
    const paramsClass = wrapperParamsClassName(method);
    const hasSessionId = methodHasSessionId(method);
    const hasExtraParams = paramsClass !== null;
    let needsMapper = false;

    const lines: string[] = [];

    // Javadoc
    const description = (method.params as JSONSchema7 | null)?.description
        ?? (method.result as JSONSchema7 | null)?.description
        ?? `Invokes {@code ${method.rpcMethod}}.`;
    lines.push(`    /**`);
    lines.push(`     * ${description}`);
    if (isSession && hasExtraParams && hasSessionId) {
        lines.push(`     * <p>`);
        lines.push(`     * Note: the {@code sessionId} field in the params record is overridden`);
        lines.push(`     * by the session-scoped wrapper; any value provided is ignored.`);
    }
    if (method.stability === "experimental") {
        lines.push(`     *`);
        lines.push(`     * @apiNote This method is experimental and may change in a future version.`);
    }
    lines.push(`     * @since 1.0.0`);
    lines.push(`     */`);

    // Signature
    if (hasExtraParams) {
        lines.push(`    public CompletableFuture<${resultClass}> ${key}(${paramsClass} params) {`);
    } else {
        lines.push(`    public CompletableFuture<${resultClass}> ${key}() {`);
    }

    // Body
    if (isSession) {
        if (hasExtraParams) {
            // Merge sessionId into the params using Jackson ObjectNode
            needsMapper = true;
            lines.push(`        com.fasterxml.jackson.databind.node.ObjectNode _p = MAPPER.valueToTree(params);`);
            lines.push(`        _p.put("sessionId", ${sessionIdExpr});`);
            lines.push(`        return caller.invoke("${method.rpcMethod}", _p, ${resultClass}.class);`);
        } else if (hasSessionId) {
            lines.push(`        return caller.invoke("${method.rpcMethod}", java.util.Map.of("sessionId", ${sessionIdExpr}), ${resultClass}.class);`);
        } else {
            lines.push(`        return caller.invoke("${method.rpcMethod}", java.util.Map.of(), ${resultClass}.class);`);
        }
    } else {
        // Server-side: pass params directly (or empty map if no params)
        if (hasExtraParams) {
            lines.push(`        return caller.invoke("${method.rpcMethod}", params, ${resultClass}.class);`);
        } else {
            lines.push(`        return caller.invoke("${method.rpcMethod}", java.util.Map.of(), ${resultClass}.class);`);
        }
    }

    lines.push(`    }`);
    lines.push(``);

    return { lines, needsMapper };
}

/**
 * Generate a Java source file for a single namespace API class.
 * Returns the generated class name and whether a mapper static field is needed.
 */
async function generateNamespaceApiFile(
    prefix: string,
    namespacePath: string[],
    tree: NamespaceTree,
    isSession: boolean,
    packageName: string,
    packageDir: string
): Promise<string> {
    const className = apiClassName(prefix, namespacePath);
    const sessionIdExpr = "this.sessionId";

    const classLines: string[] = [];
    const allImports = new Set<string>([
        "java.util.concurrent.CompletableFuture",
        "javax.annotation.processing.Generated",
    ]);
    let needsMapper = false;

    // Generate sub-namespace fields
    const subFields: string[] = [];
    const subInits: string[] = [];
    for (const [subKey, subTree] of tree.subspaces) {
        const subClass = apiClassName(prefix, [...namespacePath, subKey]);
        subFields.push(`    /** API methods for the {@code ${[...namespacePath, subKey].join(".")}} sub-namespace. */`);
        subFields.push(`    public final ${subClass} ${subKey};`);
        if (isSession) {
            subInits.push(`        this.${subKey} = new ${subClass}(caller, sessionId);`);
        } else {
            subInits.push(`        this.${subKey} = new ${subClass}(caller);`);
        }
        // Recursively generate sub-namespace files
        await generateNamespaceApiFile(prefix, [...namespacePath, subKey], subTree, isSession, packageName, packageDir);
    }

    // Collect result/param imports and generate methods
    const methodLines: string[] = [];
    for (const [key, method] of tree.methods) {
        const resultClass = wrapperResultClassName(method);
        const paramsClass = wrapperParamsClassName(method);
        if (resultClass !== "Void") allImports.add(`${packageName}.${resultClass}`);
        if (paramsClass) allImports.add(`${packageName}.${paramsClass}`);

        const { lines, needsMapper: nm } = generateApiMethod(key, method, isSession, sessionIdExpr);
        methodLines.push(...lines);
        if (nm) needsMapper = true;
    }

    // Build class body
    const qualifiedNs = namespacePath.length > 0 ? namespacePath.join(".") : prefix.toLowerCase();
    classLines.push(COPYRIGHT);
    classLines.push(``);
    classLines.push(AUTO_GENERATED_HEADER);
    classLines.push(GENERATED_FROM_API);
    classLines.push(``);
    classLines.push(`package ${packageName};`);
    classLines.push(``);

    // Add imports (skip same-package imports)
    const sortedImports = [...allImports].filter(imp => !imp.startsWith(packageName + ".")).sort();
    for (const imp of sortedImports) {
        classLines.push(`import ${imp};`);
    }
    classLines.push(``);

    // Javadoc for class
    classLines.push(`/**`);
    classLines.push(` * API methods for the {@code ${qualifiedNs}} namespace.`);
    classLines.push(` *`);
    classLines.push(` * @since 1.0.0`);
    classLines.push(` */`);
    classLines.push(GENERATED_ANNOTATION);
    classLines.push(`public final class ${className} {`);
    classLines.push(``);
    if (needsMapper) {
        classLines.push(`    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;`);
        classLines.push(``);
    }
    classLines.push(`    private final RpcCaller caller;`);
    if (isSession) {
        classLines.push(`    private final String sessionId;`);
    }

    // Sub-namespace fields
    if (subFields.length > 0) {
        classLines.push(``);
        classLines.push(...subFields);
    }

    // Constructor
    classLines.push(``);
    if (isSession) {
        classLines.push(`    /** @param caller the RPC transport function */`);
        classLines.push(`    ${className}(RpcCaller caller, String sessionId) {`);
        classLines.push(`        this.caller = caller;`);
        classLines.push(`        this.sessionId = sessionId;`);
    } else {
        classLines.push(`    /** @param caller the RPC transport function */`);
        classLines.push(`    ${className}(RpcCaller caller) {`);
        classLines.push(`        this.caller = caller;`);
    }
    for (const init of subInits) {
        classLines.push(init);
    }
    classLines.push(`    }`);
    classLines.push(``);

    // Methods
    classLines.push(...methodLines);

    classLines.push(`}`);
    classLines.push(``);

    await writeGeneratedFile(`${packageDir}/${className}.java`, classLines.join("\n"));
    return className;
}

/**
 * Generate ServerRpc.java or SessionRpc.java — the top-level wrapper class.
 */
async function generateRpcRootFile(
    sectionName: string, // "server" | "session"
    tree: NamespaceTree,
    isSession: boolean,
    packageName: string,
    packageDir: string
): Promise<void> {
    const prefix = sectionName === "server" ? "Server" : "Session";
    const rootClassName = prefix + "Rpc";
    const sessionIdExpr = "this.sessionId";

    const classLines: string[] = [];
    const allImports = new Set<string>([
        "java.util.concurrent.CompletableFuture",
        "javax.annotation.processing.Generated",
    ]);
    let needsMapper = false;

    // Sub-namespace fields and init lines
    const subFields: string[] = [];
    const subInits: string[] = [];
    for (const [nsKey, nsTree] of tree.subspaces) {
        const nsClass = apiClassName(prefix, [nsKey]);
        subFields.push(`    /** API methods for the {@code ${nsKey}} namespace. */`);
        subFields.push(`    public final ${nsClass} ${nsKey};`);
        if (isSession) {
            subInits.push(`        this.${nsKey} = new ${nsClass}(caller, sessionId);`);
        } else {
            subInits.push(`        this.${nsKey} = new ${nsClass}(caller);`);
        }
        // Generate the namespace API class file (recursively)
        await generateNamespaceApiFile(prefix, [nsKey], nsTree, isSession, packageName, packageDir);
    }

    // Collect result/param imports and generate top-level method bodies
    const methodLines: string[] = [];
    for (const [key, method] of tree.methods) {
        const resultClass = wrapperResultClassName(method);
        const paramsClass = wrapperParamsClassName(method);
        if (resultClass !== "Void") allImports.add(`${packageName}.${resultClass}`);
        if (paramsClass) allImports.add(`${packageName}.${paramsClass}`);

        const { lines, needsMapper: nm } = generateApiMethod(key, method, isSession, sessionIdExpr);
        methodLines.push(...lines);
        if (nm) needsMapper = true;
    }

    // Build file content
    classLines.push(COPYRIGHT);
    classLines.push(``);
    classLines.push(AUTO_GENERATED_HEADER);
    classLines.push(GENERATED_FROM_API);
    classLines.push(``);
    classLines.push(`package ${packageName};`);
    classLines.push(``);

    const sortedImports = [...allImports].filter(imp => !imp.startsWith(packageName + ".")).sort();
    for (const imp of sortedImports) {
        classLines.push(`import ${imp};`);
    }
    classLines.push(``);

    classLines.push(`/**`);
    if (isSession) {
        classLines.push(` * Typed client for session-scoped RPC methods.`);
        classLines.push(` * <p>`);
        classLines.push(` * Provides strongly-typed access to all session-level API namespaces.`);
        classLines.push(` * The {@code sessionId} is injected automatically into every call.`);
        classLines.push(` * <p>`);
        classLines.push(` * Obtain an instance by calling {@code new SessionRpc(caller, sessionId)}.`);
    } else {
        classLines.push(` * Typed client for server-level RPC methods.`);
        classLines.push(` * <p>`);
        classLines.push(` * Provides strongly-typed access to all server-level API namespaces.`);
        classLines.push(` * <p>`);
        classLines.push(` * Obtain an instance by calling {@code new ServerRpc(caller)}.`);
    }
    classLines.push(` *`);
    classLines.push(` * @since 1.0.0`);
    classLines.push(` */`);
    classLines.push(GENERATED_ANNOTATION);
    classLines.push(`public final class ${rootClassName} {`);
    classLines.push(``);
    if (needsMapper) {
        classLines.push(`    private static final com.fasterxml.jackson.databind.ObjectMapper MAPPER = RpcMapper.INSTANCE;`);
        classLines.push(``);
    }
    classLines.push(`    private final RpcCaller caller;`);
    if (isSession) {
        classLines.push(`    private final String sessionId;`);
    }
    if (subFields.length > 0) {
        classLines.push(``);
        classLines.push(...subFields);
    }
    classLines.push(``);

    // Constructor
    if (isSession) {
        classLines.push(`    /**`);
        classLines.push(`     * Creates a new session RPC client.`);
        classLines.push(`     *`);
        classLines.push(`     * @param caller    the RPC transport function (e.g., {@code jsonRpcClient::invoke})`);
        classLines.push(`     * @param sessionId the session ID to inject into every request`);
        classLines.push(`     */`);
        classLines.push(`    public ${rootClassName}(RpcCaller caller, String sessionId) {`);
        classLines.push(`        this.caller = caller;`);
        classLines.push(`        this.sessionId = sessionId;`);
    } else {
        classLines.push(`    /**`);
        classLines.push(`     * Creates a new server RPC client.`);
        classLines.push(`     *`);
        classLines.push(`     * @param caller the RPC transport function (e.g., {@code jsonRpcClient::invoke})`);
        classLines.push(`     */`);
        classLines.push(`    public ${rootClassName}(RpcCaller caller) {`);
        classLines.push(`        this.caller = caller;`);
    }
    for (const init of subInits) {
        classLines.push(init);
    }
    classLines.push(`    }`);
    classLines.push(``);

    // Top-level methods
    classLines.push(...methodLines);

    classLines.push(`}`);
    classLines.push(``);

    await writeGeneratedFile(`${packageDir}/${rootClassName}.java`, classLines.join("\n"));
}

/** Generate the RpcCaller functional interface */
async function generateRpcCallerInterface(packageName: string, packageDir: string): Promise<void> {
    const lines: string[] = [];
    lines.push(COPYRIGHT);
    lines.push(``);
    lines.push(AUTO_GENERATED_HEADER);
    lines.push(GENERATED_FROM_API);
    lines.push(``);
    lines.push(`package ${packageName};`);
    lines.push(``);
    lines.push(`import java.util.concurrent.CompletableFuture;`);
    lines.push(`import javax.annotation.processing.Generated;`);
    lines.push(``);
    lines.push(`/**`);
    lines.push(` * Interface for invoking JSON-RPC methods with typed responses.`);
    lines.push(` * <p>`);
    lines.push(` * Implementations delegate to the underlying transport layer`);
    lines.push(` * (e.g., a {@code JsonRpcClient} instance). A method reference is typically the clearest`);
    lines.push(` * way to adapt a generic {@code invoke} method to this interface:`);
    lines.push(` * <pre>{@code`);
    lines.push(` * RpcCaller caller = jsonRpcClient::invoke;`);
    lines.push(` * }</pre>`);
    lines.push(` *`);
    lines.push(` * @since 1.0.0`);
    lines.push(` */`);
    lines.push(GENERATED_ANNOTATION);
    lines.push(`public interface RpcCaller {`);
    lines.push(``);
    lines.push(`    /**`);
    lines.push(`     * Invokes a JSON-RPC method and returns a future for the typed response.`);
    lines.push(`     *`);
    lines.push(`     * @param <T>        the expected response type`);
    lines.push(`     * @param method     the JSON-RPC method name`);
    lines.push(`     * @param params     the request parameters (may be a {@code Map}, DTO record, or {@code JsonNode})`);
    lines.push(`     * @param resultType the {@link Class} of the expected response type`);
    lines.push(`     * @return a {@link CompletableFuture} that completes with the deserialized result`);
    lines.push(`     */`);
    lines.push(`    <T> CompletableFuture<T> invoke(String method, Object params, Class<T> resultType);`);
    lines.push(`}`);
    lines.push(``);

    await writeGeneratedFile(`${packageDir}/RpcCaller.java`, lines.join("\n"));
}

/**
 * Generate RpcMapper.java — a package-private holder for the shared ObjectMapper used
 * when merging sessionId into session API call params.  All session API classes that
 * need an ObjectMapper reference this single instance instead of instantiating their own.
 */
async function generateRpcMapperClass(packageName: string, packageDir: string): Promise<void> {
    const lines: string[] = [];
    lines.push(COPYRIGHT);
    lines.push(``);
    lines.push(AUTO_GENERATED_HEADER);
    lines.push(GENERATED_FROM_API);
    lines.push(``);
    lines.push(`package ${packageName};`);
    lines.push(``);
    lines.push(`import javax.annotation.processing.Generated;`);
    lines.push(``);
    lines.push(`/**`);
    lines.push(` * Package-private holder for the shared {@link com.fasterxml.jackson.databind.ObjectMapper}`);
    lines.push(` * used by session API classes when merging {@code sessionId} into call parameters.`);
    lines.push(` * <p>`);
    lines.push(` * {@link com.fasterxml.jackson.databind.ObjectMapper} is thread-safe and expensive to`);
    lines.push(` * instantiate, so a single shared instance is used across all generated API classes.`);
    lines.push(` * The configuration mirrors {@code JsonRpcClient}'s mapper (JavaTimeModule, lenient`);
    lines.push(` * unknown-property handling, ISO date format, NON_NULL inclusion).`);
    lines.push(` *`);
    lines.push(` * @since 1.0.0`);
    lines.push(` */`);
    lines.push(GENERATED_ANNOTATION);
    lines.push(`final class RpcMapper {`);
    lines.push(``);
    lines.push(`    static final com.fasterxml.jackson.databind.ObjectMapper INSTANCE = createMapper();`);
    lines.push(``);
    lines.push(`    private static com.fasterxml.jackson.databind.ObjectMapper createMapper() {`);
    lines.push(`        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();`);
    lines.push(`        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());`);
    lines.push(`        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);`);
    lines.push(`        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);`);
    lines.push(`        mapper.setDefaultPropertyInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);`);
    lines.push(`        return mapper;`);
    lines.push(`    }`);
    lines.push(``);
    lines.push(`    private RpcMapper() {}`);
    lines.push(`}`);
    lines.push(``);

    await writeGeneratedFile(`${packageDir}/RpcMapper.java`, lines.join("\n"));
}

/** Main entry point for RPC wrapper generation */
async function generateRpcWrappers(schemaPath: string): Promise<void> {
    console.log("\n🔧 Generating RPC wrapper classes...");

    const schemaContent = await fs.readFile(schemaPath, "utf-8");
    const schema = JSON.parse(schemaContent) as {
        server?: Record<string, unknown>;
        session?: Record<string, unknown>;
        clientSession?: Record<string, unknown>;
        definitions?: Record<string, JSONSchema7>;
    };

    // Set module-level definitions for $ref resolution in wrapper helpers
    currentDefinitions = (schema.definitions ?? {}) as Record<string, JSONSchema7>;

    const packageName = "com.github.copilot.sdk.generated.rpc";
    const packageDir = `src/generated/java/com/github/copilot/sdk/generated/rpc`;

    // RpcCaller interface and shared ObjectMapper holder
    await generateRpcCallerInterface(packageName, packageDir);
    await generateRpcMapperClass(packageName, packageDir);

    // Server-side wrappers
    if (schema.server) {
        const serverTree = buildNamespaceTree(schema.server);
        await generateRpcRootFile("server", serverTree, false, packageName, packageDir);
    }

    // Session-side wrappers
    if (schema.session) {
        const sessionTree = buildNamespaceTree(schema.session);
        await generateRpcRootFile("session", sessionTree, true, packageName, packageDir);
    }

    console.log(`✅ RPC wrapper classes generated`);
}

// ── Main entry point ──────────────────────────────────────────────────────────

async function main(): Promise<void> {
    console.log("🚀 Java SDK code generator");
    console.log("============================");

    const sessionEventsSchemaPath = await getSessionEventsSchemaPath();
    console.log(`📄 Session events schema: ${sessionEventsSchemaPath}`);
    const apiSchemaPath = await getApiSchemaPath();
    console.log(`📄 API schema: ${apiSchemaPath}`);

    await generateSessionEvents(sessionEventsSchemaPath);
    await generateRpcTypes(apiSchemaPath);
    await generateRpcWrappers(apiSchemaPath);

    console.log("\n✅ Java code generation complete!");
}

main().catch((err) => {
    console.error("❌ Code generation failed:", err);
    process.exit(1);
});
