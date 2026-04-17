/*---------------------------------------------------------------------------------------------
 *  Copyright (c) Microsoft Corporation. All rights reserved.
 *--------------------------------------------------------------------------------------------*/

/**
 * Rust code generator for session-events and RPC types.
 *
 * Reads the same JSON schemas used by the other language generators and emits
 * Rust source files with serde derives.
 */

import fs from "fs/promises";
import path from "path";
import { execFile } from "child_process";
import { promisify } from "util";
import type { JSONSchema7, JSONSchema7Definition } from "json-schema";
import { fileURLToPath } from "url";
import {
  cloneSchemaForCodegen,
  getApiSchemaPath,
  getRpcSchemaTypeName,
  getSessionEventsSchemaPath,
  hoistTitledSchemas,
  isObjectSchema,
  isVoidSchema,
  isRpcMethod,
  isNodeFullyExperimental,
  isNodeFullyDeprecated,
  isSchemaDeprecated,
  postProcessSchema,
  writeGeneratedFile,
  collectDefinitionCollections,
  hasSchemaPayload,
  refTypeName,
  resolveObjectSchema,
  resolveSchema,
  withSharedDefinitions,
  EXCLUDED_EVENT_TYPES,
  type ApiSchema,
  type DefinitionCollections,
  type RpcMethod,
} from "./utils.js";

// ── Rust naming utilities ───────────────────────────────────────────────────

/** Convert a camelCase or dot.separated string to snake_case. */
function toSnakeCase(s: string): string {
  return s
    .replace(/([a-z0-9])([A-Z])/g, "$1_$2")
    .replace(/[._-]/g, "_")
    .toLowerCase();
}

/** Convert a string to PascalCase (for Rust type names). */
function toPascalCase(s: string): string {
  return s
    .split(/[._-]/)
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1))
    .join("");
}

/** Convert a string to SCREAMING_SNAKE_CASE (for Rust constants). */
function toScreamingSnakeCase(s: string): string {
  return toSnakeCase(s).toUpperCase();
}

/** Reserved Rust keywords that need a `r#` prefix when used as field names. */
const RUST_KEYWORDS = new Set([
  "as",
  "async",
  "await",
  "break",
  "const",
  "continue",
  "crate",
  "dyn",
  "else",
  "enum",
  "extern",
  "false",
  "fn",
  "for",
  "if",
  "impl",
  "in",
  "let",
  "loop",
  "match",
  "mod",
  "move",
  "mut",
  "pub",
  "ref",
  "return",
  "self",
  "Self",
  "static",
  "struct",
  "super",
  "trait",
  "true",
  "type",
  "union",
  "unsafe",
  "use",
  "where",
  "while",
  "yield",
]);

/** Escape a Rust field name if it's a keyword. */
function rustFieldName(name: string): string {
  const snake = toSnakeCase(name);
  return RUST_KEYWORDS.has(snake) ? `r#${snake}` : snake;
}

function isNamedRustObjectSchema(
  schema: JSONSchema7 | undefined,
): schema is JSONSchema7 {
  return (
    !!schema &&
    schema.type === "object" &&
    (schema.properties !== undefined || schema.additionalProperties === false)
  );
}

function emitDeprecatedAttribute(
  lines: string[],
  deprecated: boolean,
  indent = "",
): void {
  if (deprecated) {
    lines.push(`${indent}#[deprecated]`);
  }
}

// ── Rust type resolution ────────────────────────────────────────────────────

interface RustCodegenCtx {
  structs: string[];
  enums: string[];
  generatedNames: Set<string>;
  definitions: DefinitionCollections;
}

/**
 * Resolve a JSON schema to its Rust type annotation.
 * Returns the Rust type string (e.g. "String", "Vec<String>", "Option<f64>").
 */
function resolveRustType(
  schema: JSONSchema7 | JSONSchema7Definition | undefined,
  ctx: RustCodegenCtx,
  propName?: string,
  parentName?: string,
): string {
  if (schema === undefined || schema === true)
    return "serde_json::Value".toString();
  if (schema === false) return "serde_json::Value";

  const s = schema as JSONSchema7;

  // Handle $ref
  if (s.$ref) {
    const typeName = refTypeName(s.$ref, ctx.definitions);
    const rustTypeName = typeName
      ? toPascalCase(typeName)
      : "serde_json::Value";
    const resolved = resolveSchema(s, ctx.definitions);
    if (resolved && resolved !== s) {
      if (
        resolved.enum &&
        Array.isArray(resolved.enum) &&
        resolved.enum.every((value) => typeof value === "string")
      ) {
        emitStringEnum(
          ctx,
          rustTypeName,
          resolved.enum as string[],
          resolved.description,
          isSchemaDeprecated(resolved),
        );
        return rustTypeName;
      }

      const resolvedObject = resolveObjectSchema(s, ctx.definitions);
      if (isNamedRustObjectSchema(resolvedObject)) {
        emitStruct(
          ctx,
          rustTypeName,
          resolvedObject,
          resolvedObject.description,
          isSchemaDeprecated(resolvedObject),
        );
        return rustTypeName;
      }

      return resolveRustType(resolved, ctx, propName, parentName);
    }
    return rustTypeName;
  }

  // Handle oneOf / anyOf (union types)
  if (s.oneOf || s.anyOf) {
    const variants = (s.oneOf || s.anyOf) as JSONSchema7[];
    // If it's a nullable type (oneOf with null), unwrap
    const nonNull = variants.filter(
      (v) => typeof v === "object" && v.type !== "null",
    );
    if (nonNull.length === 1 && variants.length === 2) {
      return resolveRustType(nonNull[0], ctx, propName, parentName);
    }
    // For complex unions, use serde_json::Value
    return "serde_json::Value";
  }

  // Handle allOf (intersection): if it resolves to an object schema, generate
  // a struct from the merged properties. Otherwise fall back to the first
  // variant. This mirrors the behaviour of the other language generators
  // which use resolveObjectSchema for full allOf merging.
  if (s.allOf) {
    const merged = resolveObjectSchema(s, ctx.definitions);
    if (merged && merged.type === "object") {
      return resolveRustType(merged, ctx, propName, parentName);
    }
    return resolveRustType(
      (s.allOf as JSONSchema7[])[0],
      ctx,
      propName,
      parentName,
    );
  }

  // Handle enum (string enum)
  if (s.enum && s.type === "string") {
    // Generate a named enum if we have a good name
    if (propName && parentName) {
      const enumName = toPascalCase(parentName) + toPascalCase(propName);
      if (!ctx.generatedNames.has(enumName)) {
        emitStringEnum(
          ctx,
          enumName,
          s.enum as string[],
          s.description,
          isSchemaDeprecated(s),
        );
      }
      return enumName;
    }
    return "String";
  }

  // Handle const (literal string)
  if (s.const !== undefined) return "String";

  // Handle type
  if (Array.isArray(s.type)) {
    const nonNull = (s.type as string[]).filter((t) => t !== "null");
    if (nonNull.length === 1) {
      return resolveRustPrimitive(nonNull[0], s, ctx, propName, parentName);
    }
    return "serde_json::Value";
  }

  if (typeof s.type === "string") {
    return resolveRustPrimitive(s.type, s, ctx, propName, parentName);
  }

  return "serde_json::Value";
}

function resolveRustPrimitive(
  type: string,
  schema: JSONSchema7,
  ctx: RustCodegenCtx,
  propName?: string,
  parentName?: string,
): string {
  switch (type) {
    case "string":
      // `format: "date-time"` and friends could map to richer types
      // (chrono / uuid / url) but those are opt-in dependencies — keep them
      // as `String` for now to mirror Go's posture.
      return "String";
    case "number":
      // The schema convention for `format: "duration"` is "value in
      // milliseconds" (see comment in scripts/codegen/csharp.ts). Map to
      // `std::time::Duration` so callers don't have to remember the unit;
      // a serde helper handles the wire conversion.
      if (schema.format === "duration") return "std::time::Duration";
      return "f64";
    case "integer":
      if (schema.format === "duration") return "std::time::Duration";
      return "i64";
    case "boolean":
      return "bool";
    case "null":
      return "()";
    case "array":
      if (schema.items) {
        const itemType = resolveRustType(
          schema.items as JSONSchema7,
          ctx,
          propName,
          parentName,
        );
        return `Vec<${itemType}>`;
      }
      return "Vec<serde_json::Value>";
    case "object":
      if (
        schema.additionalProperties &&
        typeof schema.additionalProperties === "object"
      ) {
        const valType = resolveRustType(
          schema.additionalProperties as JSONSchema7,
          ctx,
          propName,
          parentName,
        );
        return `std::collections::HashMap<String, ${valType}>`;
      }
      if (schema.properties || schema.additionalProperties === false) {
        // Inline object — generate a named struct
        if (propName && parentName) {
          const structName = toPascalCase(parentName) + toPascalCase(propName);
          if (!ctx.generatedNames.has(structName)) {
            emitStruct(
              ctx,
              structName,
              schema,
              schema.description,
              isSchemaDeprecated(schema),
            );
          }
          return structName;
        }
      }
      return "serde_json::Value";
    default:
      return "serde_json::Value";
  }
}

// ── Emitters ────────────────────────────────────────────────────────────────

function emitStringEnum(
  ctx: RustCodegenCtx,
  name: string,
  values: string[],
  description?: string,
  deprecated = false,
): void {
  if (ctx.generatedNames.has(name)) return;
  ctx.generatedNames.add(name);

  const lines: string[] = [];
  if (description) {
    lines.push(`/// ${description}`);
  }
  emitDeprecatedAttribute(lines, deprecated);
  lines.push(
    `#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]`,
  );
  lines.push(`pub enum ${name} {`);
  for (const value of values) {
    const variant = toPascalCase(value.replace(/[^a-zA-Z0-9._-]/g, "_"));
    if (variant !== value) {
      lines.push(`    #[serde(rename = ${JSON.stringify(value)})]`);
    }
    lines.push(`    ${variant},`);
  }
  // Allow unknown variants
  lines.push(`    /// Unknown variant not yet covered by the SDK.`);
  lines.push(`    #[default]`);
  lines.push(`    #[serde(other)]`);
  lines.push(`    Unknown,`);
  lines.push(`}`);
  lines.push(``);
  ctx.enums.push(lines.join("\n"));
}

function emitStruct(
  ctx: RustCodegenCtx,
  name: string,
  schema: JSONSchema7,
  description?: string,
  deprecated = false,
): void {
  if (ctx.generatedNames.has(name)) return;
  ctx.generatedNames.add(name);

  const lines: string[] = [];
  if (description) {
    for (const line of description.split("\n")) {
      lines.push(`/// ${line}`);
    }
  }
  emitDeprecatedAttribute(lines, deprecated || isSchemaDeprecated(schema));
  lines.push(`#[derive(Debug, Clone, Default, Serialize, Deserialize)]`);
  lines.push(`#[serde(rename_all = "camelCase")]`);
  lines.push(`pub struct ${name} {`);

  const required = new Set(schema.required || []);
  const props = schema.properties || {};

  for (const [propKey, propSchema] of Object.entries(props)) {
    if (typeof propSchema === "boolean") continue;
    const prop = propSchema as JSONSchema7;
    const fieldName = rustFieldName(propKey);
    const isRequired = required.has(propKey);
    let rustType = resolveRustType(prop, ctx, propKey, name);

    const isDuration =
      prop.format === "duration" &&
      (prop.type === "integer" || prop.type === "number");
    const durationSerdeBase =
      prop.type === "number" ? "millis_f64" : "millis";
    const durationSerdeMod = isRequired
      ? durationSerdeBase
      : `${durationSerdeBase}_opt`;

    // Add doc comment
    if (prop.description) {
      for (const line of prop.description.split("\n")) {
        lines.push(`    /// ${line}`);
      }
    }
    emitDeprecatedAttribute(lines, isSchemaDeprecated(prop), "    ");

    // Handle serde rename if snake_case differs from JSON key
    const expectedSnake = toSnakeCase(propKey);
    // serde(rename_all = "camelCase") handles most cases, but explicit rename
    // is needed when the key doesn't round-trip through camelCase → snake_case
    if (fieldName.startsWith("r#")) {
      lines.push(`    #[serde(rename = ${JSON.stringify(propKey)})]`);
    }

    if (isRequired) {
      if (isDuration) {
        lines.push(
          `    #[serde(with = "crate::duration_serde::${durationSerdeMod}")]`,
        );
      }
      lines.push(`    pub ${fieldName}: ${rustType},`);
    } else {
      if (isDuration) {
        lines.push(
          `    #[serde(default, with = "crate::duration_serde::${durationSerdeMod}", skip_serializing_if = "Option::is_none")]`,
        );
      } else {
        lines.push(`    #[serde(skip_serializing_if = "Option::is_none")]`);
      }
      lines.push(`    pub ${fieldName}: Option<${rustType}>,`);
    }
  }

  lines.push(`}`);
  lines.push(``);
  ctx.structs.push(lines.join("\n"));
}

function emitTypeAlias(
  ctx: RustCodegenCtx,
  name: string,
  targetType: string,
  description?: string,
  deprecated = false,
): void {
  if (ctx.generatedNames.has(name)) return;
  ctx.generatedNames.add(name);

  const lines: string[] = [];
  if (description) {
    for (const line of description.split("\n")) {
      lines.push(`/// ${line}`);
    }
  }
  emitDeprecatedAttribute(lines, deprecated);
  lines.push(`pub type ${name} = ${targetType};`);
  lines.push(``);
  ctx.structs.push(lines.join("\n"));
}

function resolveRustAliasTarget(
  schema: JSONSchema7,
  ctx: RustCodegenCtx,
  name: string,
): string {
  if (schema.type === "array" && schema.items) {
    const itemType = resolveRustType(
      schema.items as JSONSchema7,
      ctx,
      "Item",
      name,
    );
    return `Vec<${itemType}>`;
  }

  if (
    schema.type === "object" &&
    schema.additionalProperties &&
    typeof schema.additionalProperties === "object"
  ) {
    const valueType = resolveRustType(
      schema.additionalProperties as JSONSchema7,
      ctx,
      "Value",
      name,
    );
    return `std::collections::HashMap<String, ${valueType}>`;
  }

  return resolveRustType(schema, ctx, undefined, name);
}

// ── Session Events Generation ───────────────────────────────────────────────

interface EventVariant {
  typeName: string; // e.g. "session.start"
  variantName: string; // e.g. "SessionStart"
  dataStructName: string; // e.g. "SessionStartData"
  dataSchema: JSONSchema7;
  description?: string;
  deprecated: boolean;
  dataDeprecated: boolean;
}

function extractEventVariants(schema: JSONSchema7): EventVariant[] {
  const variants: EventVariant[] = [];
  const definitionCollections = collectDefinitionCollections(
    schema as Record<string, unknown>,
  );

  // The schema root has "$ref": "#/definitions/SessionEvent".
  // Resolve it, then use anyOf (or oneOf as fallback).
  const sessionEvent =
    resolveSchema(
      { $ref: "#/definitions/SessionEvent" },
      definitionCollections,
    ) ??
    resolveSchema({ $ref: "#/$defs/SessionEvent" }, definitionCollections) ??
    schema;

  const unionVariants = (sessionEvent.anyOf ?? sessionEvent.oneOf) as
    | JSONSchema7[]
    | undefined;
  if (!unionVariants) return variants;

  for (const variant of unionVariants) {
    const resolved =
      resolveObjectSchema(variant as JSONSchema7, definitionCollections) ??
      resolveSchema(variant as JSONSchema7, definitionCollections) ??
      (variant as JSONSchema7);
    if (typeof resolved !== "object" || !resolved.properties) continue;

    const typeSchema = resolved.properties.type as JSONSchema7;
    if (!typeSchema || !typeSchema.const) continue;

    const typeName = typeSchema.const as string;
    if (EXCLUDED_EVENT_TYPES.has(typeName)) continue;

    const dataSchema = (resolved.properties.data as JSONSchema7) || {
      type: "object",
    };
    const resolvedDataSchema =
      resolveSchema(dataSchema, definitionCollections) ?? dataSchema;
    const variantName = toPascalCase(typeName);
    const dataStructName = variantName + "Data";

    variants.push({
      typeName,
      variantName,
      dataStructName,
      dataSchema: resolvedDataSchema,
      description: resolved.description ?? resolvedDataSchema.description,
      deprecated: isSchemaDeprecated(resolved),
      dataDeprecated: isSchemaDeprecated(resolvedDataSchema),
    });
  }

  return variants;
}

function generateSessionEventsCode(schema: JSONSchema7): string {
  const definitionCollections = collectDefinitionCollections(
    schema as Record<string, unknown>,
  );
  const ctx: RustCodegenCtx = {
    structs: [],
    enums: [],
    generatedNames: new Set(),
    definitions: definitionCollections,
  };

  const variants = extractEventVariants(schema);

  // Track which variants have data vs void payloads
  const variantHasData: Map<string, boolean> = new Map();

  // Emit data structs for each event variant (before enums so names are in ctx)
  for (const v of variants) {
    const resolved =
      resolveSchema(v.dataSchema, ctx.definitions) ?? v.dataSchema;
    if (isNamedRustObjectSchema(resolved)) {
      emitStruct(
        ctx,
        v.dataStructName,
        resolved,
        v.description,
        v.dataDeprecated,
      );
      variantHasData.set(v.variantName, true);
    } else if (resolved.type === "string") {
      emitTypeAlias(
        ctx,
        v.dataStructName,
        "String",
        v.description ?? v.typeName,
        v.dataDeprecated,
      );
      variantHasData.set(v.variantName, true);
    } else if (isVoidSchema(resolved)) {
      variantHasData.set(v.variantName, false);
    } else {
      emitTypeAlias(
        ctx,
        v.dataStructName,
        "serde_json::Value",
        v.description ?? v.typeName,
        v.dataDeprecated,
      );
      variantHasData.set(v.variantName, true);
    }
  }

  // ── SessionEventType enum (custom Serialize/Deserialize to preserve unknown strings) ──

  const eventTypeEnumLines: string[] = [];
  eventTypeEnumLines.push(`/// All known session event type strings.`);
  eventTypeEnumLines.push(`#[derive(Debug, Clone, PartialEq, Eq, Hash)]`);
  eventTypeEnumLines.push(`pub enum SessionEventType {`);
  for (const v of variants) {
    if (v.description) {
      eventTypeEnumLines.push(`    /// ${v.description.split("\n")[0]}`);
    }
    if (v.deprecated) {
      eventTypeEnumLines.push(`    #[deprecated]`);
    }
    eventTypeEnumLines.push(`    ${v.variantName},`);
  }
  eventTypeEnumLines.push(
    `    /// Unknown event type not yet covered by the SDK.`,
  );
  eventTypeEnumLines.push(
    `    /// The original type string is preserved for round-tripping.`,
  );
  eventTypeEnumLines.push(`    Unknown(String),`);
  eventTypeEnumLines.push(`}`);
  eventTypeEnumLines.push(``);

  // Custom Serialize for SessionEventType
  eventTypeEnumLines.push(`impl serde::Serialize for SessionEventType {`);
  eventTypeEnumLines.push(
    `    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>`,
  );
  eventTypeEnumLines.push(`    where`);
  eventTypeEnumLines.push(`        S: serde::Serializer,`);
  eventTypeEnumLines.push(`    {`);
  eventTypeEnumLines.push(`        serializer.serialize_str(self.as_str())`);
  eventTypeEnumLines.push(`    }`);
  eventTypeEnumLines.push(`}`);
  eventTypeEnumLines.push(``);

  // Custom Deserialize for SessionEventType
  eventTypeEnumLines.push(
    `impl<'de> serde::Deserialize<'de> for SessionEventType {`,
  );
  eventTypeEnumLines.push(
    `    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>`,
  );
  eventTypeEnumLines.push(`    where`);
  eventTypeEnumLines.push(`        D: serde::Deserializer<'de>,`);
  eventTypeEnumLines.push(`    {`);
  eventTypeEnumLines.push(
    `        let s = String::deserialize(deserializer)?;`,
  );
  eventTypeEnumLines.push(`        Ok(Self::parse_type(&s))`);
  eventTypeEnumLines.push(`    }`);
  eventTypeEnumLines.push(`}`);
  eventTypeEnumLines.push(``);

  // as_str() method
  eventTypeEnumLines.push(`impl SessionEventType {`);
  eventTypeEnumLines.push(
    `    /// Returns the wire-format string for this event type.`,
  );
  eventTypeEnumLines.push(`    pub fn as_str(&self) -> &str {`);
  eventTypeEnumLines.push(`        match self {`);
  for (const v of variants) {
    eventTypeEnumLines.push(
      `            Self::${v.variantName} => ${JSON.stringify(v.typeName)},`,
    );
  }
  eventTypeEnumLines.push(`            Self::Unknown(s) => s.as_str(),`);
  eventTypeEnumLines.push(`        }`);
  eventTypeEnumLines.push(`    }`);
  eventTypeEnumLines.push(``);
  eventTypeEnumLines.push(
    `    /// Parses a wire-format string into a typed event type.`,
  );
  eventTypeEnumLines.push(`    pub fn parse_type(s: &str) -> Self {`);
  eventTypeEnumLines.push(`        match s {`);
  for (const v of variants) {
    eventTypeEnumLines.push(
      `            ${JSON.stringify(v.typeName)} => Self::${v.variantName},`,
    );
  }
  eventTypeEnumLines.push(
    `            other => Self::Unknown(other.to_owned()),`,
  );
  eventTypeEnumLines.push(`        }`);
  eventTypeEnumLines.push(`    }`);
  eventTypeEnumLines.push(`}`);
  eventTypeEnumLines.push(``);

  // impl FromStr for SessionEventType
  eventTypeEnumLines.push(`impl std::str::FromStr for SessionEventType {`);
  eventTypeEnumLines.push(`    type Err = std::convert::Infallible;`);
  eventTypeEnumLines.push(``);
  eventTypeEnumLines.push(
    `    fn from_str(s: &str) -> Result<Self, Self::Err> {`,
  );
  eventTypeEnumLines.push(`        Ok(Self::parse_type(s))`);
  eventTypeEnumLines.push(`    }`);
  eventTypeEnumLines.push(`}`);
  eventTypeEnumLines.push(``);

  // Display for SessionEventType — outputs the wire-format string
  eventTypeEnumLines.push(`impl std::fmt::Display for SessionEventType {`);
  eventTypeEnumLines.push(
    `    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {`,
  );
  eventTypeEnumLines.push(`        f.write_str(self.as_str())`);
  eventTypeEnumLines.push(`    }`);
  eventTypeEnumLines.push(`}`);
  eventTypeEnumLines.push(``);

  // ── SessionEventData enum (no serde derives — serialized/deserialized via SessionEvent) ──

  const dataEnumLines: string[] = [];
  dataEnumLines.push(`/// Typed session event data payload.`);
  dataEnumLines.push(`///`);
  dataEnumLines.push(
    "/// Each variant corresponds to a [`SessionEventType`] and carries the",
  );
  dataEnumLines.push(
    `/// typed data struct for that event. Unknown or new event types are`,
  );
  dataEnumLines.push(`/// captured as raw JSON in the \`Unknown\` variant.`);
  dataEnumLines.push(`#[derive(Debug, Clone)]`);
  dataEnumLines.push(`pub enum SessionEventData {`);
  for (const v of variants) {
    if (v.description) {
      dataEnumLines.push(`    /// ${v.description.split("\n")[0]}`);
    }
    if (v.deprecated) {
      dataEnumLines.push(`    #[deprecated]`);
    }
    if (variantHasData.get(v.variantName)) {
      dataEnumLines.push(`    ${v.variantName}(${v.dataStructName}),`);
    } else {
      dataEnumLines.push(`    ${v.variantName},`);
    }
  }
  dataEnumLines.push(
    `    /// Unknown event type — data is preserved as raw JSON.`,
  );
  dataEnumLines.push(`    Unknown(serde_json::Value),`);
  dataEnumLines.push(`}`);
  dataEnumLines.push(``);

  // Custom Serialize for SessionEventData
  dataEnumLines.push(`impl serde::Serialize for SessionEventData {`);
  dataEnumLines.push(
    `    fn serialize<S>(&self, serializer: S) -> Result<S::Ok, S::Error>`,
  );
  dataEnumLines.push(`    where`);
  dataEnumLines.push(`        S: serde::Serializer,`);
  dataEnumLines.push(`    {`);
  dataEnumLines.push(`        match self {`);
  for (const v of variants) {
    if (variantHasData.get(v.variantName)) {
      dataEnumLines.push(
        `            Self::${v.variantName}(d) => d.serialize(serializer),`,
      );
    } else {
      dataEnumLines.push(`            Self::${v.variantName} => {`);
      dataEnumLines.push(`                use serde::ser::SerializeMap;`);
      dataEnumLines.push(
        `                serializer.serialize_map(Some(0))?.end()`,
      );
      dataEnumLines.push(`            }`);
    }
  }
  dataEnumLines.push(
    `            Self::Unknown(v) => v.serialize(serializer),`,
  );
  dataEnumLines.push(`        }`);
  dataEnumLines.push(`    }`);
  dataEnumLines.push(`}`);
  dataEnumLines.push(``);

  // Display for SessionEventData — serializes to JSON for logging/debugging
  dataEnumLines.push(`impl std::fmt::Display for SessionEventData {`);
  dataEnumLines.push(
    `    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {`,
  );
  dataEnumLines.push(`        match serde_json::to_string(self) {`);
  dataEnumLines.push(`            Ok(s) => f.write_str(&s),`);
  dataEnumLines.push(`            Err(_) => f.write_str("{}"),`);
  dataEnumLines.push(`        }`);
  dataEnumLines.push(`    }`);
  dataEnumLines.push(`}`);
  dataEnumLines.push(``);

  // ── SessionEvent struct ──

  const eventStructLines: string[] = [];
  eventStructLines.push(`/// A single event in a session's timeline.`);
  eventStructLines.push(`///`);
  eventStructLines.push(
    `/// Events form a linked chain via \`parent_id\`. The \`event_type\` field`,
  );
  eventStructLines.push(
    `/// is a typed enum that identifies the kind of event, and \`data\` carries`,
  );
  eventStructLines.push(`/// the corresponding typed payload.`);
  eventStructLines.push(`#[derive(Debug, Clone, serde::Serialize)]`);
  eventStructLines.push(`#[serde(rename_all = "camelCase")]`);
  eventStructLines.push(`pub struct SessionEvent {`);
  eventStructLines.push(`    /// Unique event ID (UUID v4).`);
  eventStructLines.push(`    pub id: String,`);
  eventStructLines.push(`    /// ISO 8601 timestamp.`);
  eventStructLines.push(`    pub timestamp: String,`);
  eventStructLines.push(`    /// ID of the preceding event in the chain.`);
  eventStructLines.push(`    pub parent_id: Option<String>,`);
  eventStructLines.push(
    `    /// Transient events that are not persisted to disk.`,
  );
  eventStructLines.push(
    `    #[serde(skip_serializing_if = "Option::is_none")]`,
  );
  eventStructLines.push(`    pub ephemeral: Option<bool>,`);
  eventStructLines.push(`    /// Event type discriminator.`);
  eventStructLines.push(`    #[serde(rename = "type")]`);
  eventStructLines.push(`    pub event_type: SessionEventType,`);
  eventStructLines.push(`    /// Typed event-specific data payload.`);
  eventStructLines.push(`    pub data: SessionEventData,`);
  eventStructLines.push(`}`);
  eventStructLines.push(``);

  // Custom Deserialize for SessionEvent
  eventStructLines.push(`impl<'de> serde::Deserialize<'de> for SessionEvent {`);
  eventStructLines.push(
    `    fn deserialize<D>(deserializer: D) -> Result<Self, D::Error>`,
  );
  eventStructLines.push(`    where`);
  eventStructLines.push(`        D: serde::Deserializer<'de>,`);
  eventStructLines.push(`    {`);
  eventStructLines.push(`        #[derive(serde::Deserialize)]`);
  eventStructLines.push(`        #[serde(rename_all = "camelCase")]`);
  eventStructLines.push(`        struct RawEvent {`);
  eventStructLines.push(`            id: String,`);
  eventStructLines.push(`            timestamp: String,`);
  eventStructLines.push(`            parent_id: Option<String>,`);
  eventStructLines.push(`            #[serde(default)]`);
  eventStructLines.push(`            ephemeral: Option<bool>,`);
  eventStructLines.push(`            #[serde(rename = "type")]`);
  eventStructLines.push(`            event_type: SessionEventType,`);
  eventStructLines.push(`            #[serde(default)]`);
  eventStructLines.push(`            data: serde_json::Value,`);
  eventStructLines.push(`        }`);
  eventStructLines.push(``);
  eventStructLines.push(
    `        let raw = RawEvent::deserialize(deserializer)?;`,
  );
  eventStructLines.push(`        let data = match &raw.event_type {`);
  for (const v of variants) {
    if (variantHasData.get(v.variantName)) {
      eventStructLines.push(
        `            SessionEventType::${v.variantName} => {`,
      );
      eventStructLines.push(`                serde_json::from_value(raw.data)`);
      eventStructLines.push(
        `                    .map(SessionEventData::${v.variantName})`,
      );
      eventStructLines.push(
        `                    .map_err(serde::de::Error::custom)?`,
      );
      eventStructLines.push(`            }`);
    } else {
      eventStructLines.push(
        `            SessionEventType::${v.variantName} => SessionEventData::${v.variantName},`,
      );
    }
  }
  eventStructLines.push(
    `            SessionEventType::Unknown(_) => SessionEventData::Unknown(raw.data),`,
  );
  eventStructLines.push(`        };`);
  eventStructLines.push(``);
  eventStructLines.push(`        Ok(SessionEvent {`);
  eventStructLines.push(`            id: raw.id,`);
  eventStructLines.push(`            timestamp: raw.timestamp,`);
  eventStructLines.push(`            parent_id: raw.parent_id,`);
  eventStructLines.push(`            ephemeral: raw.ephemeral,`);
  eventStructLines.push(`            event_type: raw.event_type,`);
  eventStructLines.push(`            data,`);
  eventStructLines.push(`        })`);
  eventStructLines.push(`    }`);
  eventStructLines.push(`}`);
  eventStructLines.push(``);

  // Assemble the output
  const header = `// AUTO-GENERATED FILE — DO NOT EDIT
// Generated from: session-events.schema.json
//
// Run \`cd scripts/codegen && npm run generate:rust\` to regenerate.

#![allow(deprecated)]
use serde::{Deserialize, Serialize};
`;

  return [
    header,
    ...ctx.enums,
    eventTypeEnumLines.join("\n"),
    ...ctx.structs,
    dataEnumLines.join("\n"),
    eventStructLines.join("\n"),
  ].join("\n");
}

// ── RPC Types Generation ────────────────────────────────────────────────────

let rpcDefinitions: DefinitionCollections = { definitions: {}, $defs: {} };

function rustRequestFallbackName(method: RpcMethod): string {
  return toPascalCase(method.rpcMethod) + "Params";
}

function rustResultTypeName(method: RpcMethod): string {
  const resultSchema =
    resolveSchema(method.result, rpcDefinitions) ?? method.result ?? undefined;
  return getRpcSchemaTypeName(
    resultSchema,
    toPascalCase(method.rpcMethod) + "Result",
  );
}

function rustParamsTypeName(method: RpcMethod): string {
  const fallback = rustRequestFallbackName(method);
  const resolvedParams =
    resolveObjectSchema(method.params, rpcDefinitions) ??
    resolveSchema(method.params, rpcDefinitions) ??
    method.params ??
    undefined;
  if (method.rpcMethod.startsWith("session.") && method.params?.$ref) {
    return fallback;
  }
  return getRpcSchemaTypeName(resolvedParams, fallback);
}

function collectRpcMethods(node: Record<string, unknown>): RpcMethod[] {
  const results: RpcMethod[] = [];
  for (const value of Object.values(node)) {
    if (isRpcMethod(value)) {
      results.push(value);
    } else if (typeof value === "object" && value !== null) {
      results.push(...collectRpcMethods(value as Record<string, unknown>));
    }
  }
  return results;
}

function generateRpcCode(schema: ApiSchema): string {
  rpcDefinitions = collectDefinitionCollections(
    schema as Record<string, unknown>,
  );
  const ctx: RustCodegenCtx = {
    structs: [],
    enums: [],
    generatedNames: new Set(),
    definitions: rpcDefinitions,
  };

  const allMethods = [
    ...collectRpcMethods(
      ((schema as Record<string, unknown>).server as Record<string, unknown>) ||
        {},
    ),
    ...collectRpcMethods(
      ((schema as Record<string, unknown>).session as Record<
        string,
        unknown
      >) || {},
    ),
    ...collectRpcMethods(
      ((schema as Record<string, unknown>).clientSession as Record<
        string,
        unknown
      >) || {},
    ),
  ];

  const combinedSchema = withSharedDefinitions(
    { $schema: "http://json-schema.org/draft-07/schema#" },
    rpcDefinitions,
  );

  // Collect all named types
  const rootDefinitions: Record<string, JSONSchema7> = {};
  const deprecatedRootNames = new Set<string>();
  for (const method of allMethods) {
    const resultSchema =
      resolveSchema(method.result, rpcDefinitions) ??
      method.result ??
      undefined;
    if (!isVoidSchema(resultSchema)) {
      const name = rustResultTypeName(method);
      rootDefinitions[name] = (resultSchema as JSONSchema7) ?? {
        type: "object",
      };
      if (method.deprecated && !method.result?.$ref) {
        deprecatedRootNames.add(name);
      }
    }

    const resolvedParams =
      resolveObjectSchema(method.params, rpcDefinitions) ??
      resolveSchema(method.params, rpcDefinitions) ??
      method.params ??
      undefined;

    if (method.params && hasSchemaPayload(resolvedParams)) {
      if (
        method.rpcMethod.startsWith("session.") &&
        resolvedParams?.properties
      ) {
        const filtered: JSONSchema7 = {
          ...resolvedParams,
          properties: Object.fromEntries(
            Object.entries(resolvedParams.properties).filter(
              ([k]) => k !== "sessionId",
            ),
          ),
          required: resolvedParams.required?.filter((r) => r !== "sessionId"),
        };
        if (hasSchemaPayload(filtered)) {
          const name = rustParamsTypeName(method);
          rootDefinitions[name] = filtered;
          if (method.deprecated && !method.params?.$ref) {
            deprecatedRootNames.add(name);
          }
        }
      } else {
        const name = rustParamsTypeName(method);
        rootDefinitions[name] = (resolvedParams as JSONSchema7) ?? {
          type: "object",
        };
        if (method.deprecated && !method.params?.$ref) {
          deprecatedRootNames.add(name);
        }
      }
    }
  }

  // Also hoist shared definitions that are referenced
  const { sharedDefinitions } = hoistTitledSchemas(
    (combinedSchema.definitions ?? {}) as Record<string, JSONSchema7>,
  );
  for (const [name, def] of Object.entries({
    ...sharedDefinitions,
    ...rootDefinitions,
  })) {
    if (typeof def !== "object") continue;
    const schema = def as JSONSchema7;
    const deprecated =
      deprecatedRootNames.has(name) || isSchemaDeprecated(schema);
    if (isNamedRustObjectSchema(schema)) {
      emitStruct(
        ctx,
        toPascalCase(name),
        schema,
        schema.description,
        deprecated,
      );
    } else if (
      schema.type === "string" &&
      schema.enum &&
      Array.isArray(schema.enum) &&
      schema.enum.every((value) => typeof value === "string")
    ) {
      emitStringEnum(
        ctx,
        toPascalCase(name),
        schema.enum as string[],
        schema.description,
        deprecated,
      );
    } else if (!isVoidSchema(schema)) {
      emitTypeAlias(
        ctx,
        toPascalCase(name),
        resolveRustAliasTarget(schema, ctx, toPascalCase(name)),
        schema.description,
        deprecated,
      );
    }
  }

  // Emit a module-level constant table of RPC method names
  const methodLines: string[] = [];
  methodLines.push(`/// RPC method name constants.`);
  methodLines.push(`pub mod methods {`);
  for (const method of allMethods) {
    const constName = toScreamingSnakeCase(method.rpcMethod);
    const desc = method.description || method.rpcMethod;
    const isDeprecated = isSchemaDeprecated(method);
    const isExperimental = method.stability === "experimental";
    if (isDeprecated) methodLines.push(`    #[deprecated]`);
    if (isExperimental) {
      methodLines.push(`    /// (Experimental) ${desc}`);
    } else {
      methodLines.push(`    /// ${desc}`);
    }
    const value = JSON.stringify(method.rpcMethod);
    const line = `    pub const ${constName}: &str = ${value};`;
    if (line.length > 99) {
      methodLines.push(`    pub const ${constName}: &str =`);
      methodLines.push(`        ${value};`);
    } else {
      methodLines.push(line);
    }
  }
  methodLines.push(`}`);
  methodLines.push(``);

  const header = `// AUTO-GENERATED FILE — DO NOT EDIT
// Generated from: api.schema.json
//
// Run \`cd scripts/codegen && npm run generate:rust\` to regenerate.

#![allow(clippy::derivable_impls)]
#![allow(deprecated)]
use serde::{Deserialize, Serialize};
`;

  return [header, ...ctx.enums, ...ctx.structs, methodLines.join("\n")].join(
    "\n",
  );
}

// ── Main ────────────────────────────────────────────────────────────────────

const execFileAsync = promisify(execFile);

async function rustfmt(filePath: string): Promise<void> {
  try {
    await execFileAsync("rustfmt", [filePath]);
  } catch {
    // rustfmt not available — generated code is still valid, just not formatted
  }
}

async function generateSessionEvents(schemaPath?: string): Promise<void> {
  console.log("Rust: generating session-events...");
  const resolvedPath = schemaPath ?? (await getSessionEventsSchemaPath());
  const schema = JSON.parse(
    await fs.readFile(resolvedPath, "utf-8"),
  ) as JSONSchema7;
  const processed = postProcessSchema(schema);
  const code = generateSessionEventsCode(processed);

  const outPath = await writeGeneratedFile(
    "rust/src/generated/session_events.rs",
    code,
  );
  await rustfmt(outPath);
  console.log(`  ✓ ${outPath}`);
}

async function generateRpc(schemaPath?: string): Promise<void> {
  console.log("Rust: generating RPC types...");
  const resolvedPath = schemaPath ?? (await getApiSchemaPath());
  const schema = cloneSchemaForCodegen(
    JSON.parse(await fs.readFile(resolvedPath, "utf-8")) as ApiSchema,
  );
  const code = generateRpcCode(schema);

  const outPath = await writeGeneratedFile("rust/src/generated/rpc.rs", code);
  await rustfmt(outPath);
  console.log(`  ✓ ${outPath}`);
}

async function updateGeneratedMod(): Promise<void> {
  console.log("Rust: updating generated/mod.rs...");
  // Re-read the existing mod.rs to preserve the protocol version constant,
  // then add re-exports for the generated modules.
  const __filename = fileURLToPath(import.meta.url);
  const __dirname = path.dirname(__filename);
  const repoRoot = path.resolve(__dirname, "../..");
  const modPath = path.join(repoRoot, "rust/src/generated/mod.rs");

  let existing = "";
  try {
    existing = await fs.readFile(modPath, "utf-8");
  } catch {
    // File doesn't exist yet
  }

  // Keep everything from the file that was written by update-protocol-version.ts,
  // stripping any previously-appended module declarations (which we'll re-add below).
  const lines = existing.split("\n");
  const newLines: string[] = [];
  let foundProtocolVersion = false;

  for (const line of lines) {
    if (line.includes("SDK_PROTOCOL_VERSION")) {
      foundProtocolVersion = true;
    }
    // Stop before any previously-generated module re-exports
    if (line.startsWith("// The modules below are generated")) {
      break;
    }
    if (
      line.startsWith("pub mod session_events;") ||
      line.startsWith("pub mod rpc;")
    ) {
      break;
    }
    newLines.push(line);
  }

  // Trim trailing blank lines so we get a clean boundary
  while (newLines.length > 0 && newLines[newLines.length - 1].trim() === "") {
    newLines.pop();
  }

  if (!foundProtocolVersion) {
    // Fallback: write a fresh file
    newLines.length = 0;
    newLines.push(
      `// Code generated by update-protocol-version.ts. DO NOT EDIT.`,
    );
    newLines.push(``);
    newLines.push(`/// The SDK protocol version.`);
    newLines.push(
      `/// This must match the version expected by the copilot-agent-runtime server.`,
    );
    newLines.push(`pub const SDK_PROTOCOL_VERSION: u32 = 3;`);
    newLines.push(``);
    newLines.push(`/// Gets the SDK protocol version.`);
    newLines.push(`pub fn get_sdk_protocol_version() -> u32 {`);
    newLines.push(`    SDK_PROTOCOL_VERSION`);
    newLines.push(`}`);
  }

  // Add the module re-exports
  newLines.push(``);
  newLines.push(
    `// The modules below are generated by scripts/codegen/rust.ts`,
  );
  newLines.push(`pub mod rpc;`);
  newLines.push(`pub mod session_events;`);
  newLines.push(``);

  await fs.writeFile(modPath, newLines.join("\n"));
  console.log(`  ✓ rust/src/generated/mod.rs`);
}

async function generate(
  sessionSchemaPath?: string,
  apiSchemaPath?: string,
): Promise<void> {
  await generateSessionEvents(sessionSchemaPath);
  await generateRpc(apiSchemaPath);
  await updateGeneratedMod();
  console.log("Rust: done!");
}

const [sessionSchemaArg, apiSchemaArg] = process.argv.slice(2);
generate(sessionSchemaArg, apiSchemaArg).catch((err) => {
  console.error("Rust codegen failed:", err);
  process.exit(1);
});
