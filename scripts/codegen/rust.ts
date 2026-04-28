/**
 * Rust code generator for the Copilot protocol JSON Schemas.
 *
 * Reads api.schema.json and session-events.schema.json, emits idiomatic Rust
 * types to rust/src/generated/.
 *
 * Usage: npx tsx scripts/codegen/rust.ts
 */

import { execFile } from "child_process";
import fs from "fs/promises";
import path from "path";
import { promisify } from "util";
import type { JSONSchema7, JSONSchema7Definition } from "json-schema";
import {
	type ApiSchema,
	type DefinitionCollections,
	EXCLUDED_EVENT_TYPES,
	REPO_ROOT,
	type RpcMethod,
	collectDefinitionCollections,
	collectDefinitions,
	getApiSchemaPath,
	getRpcSchemaTypeName,
	getSessionEventsSchemaPath,
	isObjectSchema,
	isRpcMethod,
	isSchemaDeprecated,
	isVoidSchema,
	postProcessSchema,
	refTypeName,
	resolveObjectSchema,
	resolveRef,
	resolveSchema,
} from "./utils.js";

const execFileAsync = promisify(execFile);

const GENERATED_DIR = path.join(REPO_ROOT, "rust/src/generated");

/**
 * JSON property names that should be emitted as a hand-authored newtype rather
 * than `String`. The newtype is `#[serde(transparent)]`, so the wire format is
 * unchanged. Add new entries sparingly — these only fire when a schema field
 * has type `string` and an exact-match name in this map.
 */
const STRING_NEWTYPE_OVERRIDES: Record<string, string> = {
	sessionId: "SessionId",
	remoteSessionId: "SessionId",
	requestId: "RequestId",
};

// ── Naming helpers ──────────────────────────────────────────────────────────

function toPascalCase(s: string): string {
	return s
		.split(/[._\-\s]+/)
		.map((w) => w.charAt(0).toUpperCase() + w.slice(1))
		.join("");
}

function toSnakeCase(s: string): string {
	return s
		.replace(/([A-Z])/g, "_$1")
		.replace(/^_/, "")
		.replace(/[.\-\s]+/g, "_")
		.toLowerCase()
		.replace(/_+/g, "_");
}

/** Convert a JSON property name (camelCase) to a Rust field name (snake_case). */
function toRustFieldName(jsonName: string): string {
	return toSnakeCase(jsonName);
}

/** Convert snake_case back to camelCase (matches serde's rename_all = "camelCase"). */
function snakeToCamelCase(snake: string): string {
	return snake.replace(/_([a-z0-9])/g, (_, c: string) => c.toUpperCase());
}

/**
 * Rust reserved keywords that need raw identifier syntax (r#).
 */
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
	"unsafe",
	"use",
	"where",
	"while",
	"yield",
]);

function safeRustFieldName(name: string): string {
	const snake = toRustFieldName(name);
	return RUST_KEYWORDS.has(snake) ? `r#${snake}` : snake;
}

// ── Codegen context ─────────────────────────────────────────────────────────

interface RustCodegenCtx {
	/** Accumulated struct definitions. */
	structs: string[];
	/** Accumulated enum definitions. */
	enums: string[];
	/** Track generated type names to avoid duplicates. */
	generatedNames: Set<string>;
	/** Schema definitions for $ref resolution. */
	definitions?: DefinitionCollections;
}

function stripOption(typeName: string): string {
	return typeName.startsWith("Option<") && typeName.endsWith(">")
		? typeName.slice("Option<".length, -1)
		: typeName;
}

function getUnionVariants(schema: JSONSchema7): JSONSchema7[] | null {
	if (schema.anyOf) return schema.anyOf as JSONSchema7[];
	if (schema.oneOf) return schema.oneOf as JSONSchema7[];
	return null;
}

function tryEmitRustDiscriminatedUnion(
	schema: JSONSchema7,
	parentTypeName: string,
	jsonPropName: string,
	ctx: RustCodegenCtx,
): string | null {
	const variants = getUnionVariants(schema);
	if (!variants) return null;

	const nonNull = variants.filter((variant) => variant.type !== "null");
	if (nonNull.length <= 1) return null;

	const enumName =
		(typeof schema.title === "string" && schema.title) ||
		parentTypeName + toPascalCase(jsonPropName);

	const resolvedVariants = nonNull.map((variant) => {
		if (variant.$ref && typeof variant.$ref === "string") {
			const resolved = resolveRef(variant.$ref, ctx.definitions);
			return {
				schema: (resolved ?? variant) as JSONSchema7,
				typeName: toPascalCase(refTypeName(variant.$ref, ctx.definitions)),
			};
		}

		const resolved =
			resolveObjectSchema(variant, ctx.definitions) ??
			resolveSchema(variant, ctx.definitions) ??
			variant;
		const kindConst = (resolved.properties?.kind as JSONSchema7 | undefined)
			?.const;
		const typeName =
			(typeof resolved.title === "string" && resolved.title) ||
			(typeof kindConst === "string"
				? `${enumName}${toPascalCase(kindConst)}`
				: `${enumName}Variant`);

		return {
			schema: resolved as JSONSchema7,
			typeName,
		};
	});

	const isDiscriminated = resolvedVariants.every(
		({ schema: variantSchema }) => {
			if (!isObjectSchema(variantSchema) || !variantSchema.properties)
				return false;
			const kind = variantSchema.properties.kind as JSONSchema7 | undefined;
			return typeof kind?.const === "string";
		},
	);
	if (!isDiscriminated) return null;

	if (ctx.generatedNames.has(enumName)) {
		return enumName;
	}
	ctx.generatedNames.add(enumName);

	for (const { schema: variantSchema, typeName } of resolvedVariants) {
		if (isObjectSchema(variantSchema)) {
			emitRustStruct(typeName, variantSchema, ctx);
		}
	}

	const lines: string[] = [];
	if (schema.description) {
		for (const line of schema.description.split(/\r?\n/)) {
			lines.push(`/// ${line}`);
		}
	}
	lines.push("#[derive(Debug, Clone, Serialize, Deserialize)]");
	lines.push("#[serde(untagged)]");
	lines.push(`pub enum ${enumName} {`);

	for (const { schema: variantSchema, typeName } of resolvedVariants) {
		const kind = ((variantSchema.properties?.kind as JSONSchema7 | undefined)
			?.const ?? typeName) as string;
		lines.push(`    ${toPascalCase(kind)}(${stripOption(typeName)}),`);
	}

	lines.push("}");
	ctx.enums.push(lines.join("\n"));
	return enumName;
}

function makeCtx(definitions?: DefinitionCollections): RustCodegenCtx {
	return {
		structs: [],
		enums: [],
		generatedNames: new Set(),
		definitions,
	};
}

// ── Type resolution ─────────────────────────────────────────────────────────

/**
 * Map a JSON Schema to a Rust type string. Emits nested type definitions as
 * side effects into ctx.
 */
function resolveRustType(
	propSchema: JSONSchema7,
	parentTypeName: string,
	jsonPropName: string,
	isRequired: boolean,
	ctx: RustCodegenCtx,
): string {
	const nestedName = parentTypeName + toPascalCase(jsonPropName);

	// $ref — resolve and recurse
	if (propSchema.$ref && typeof propSchema.$ref === "string") {
		const typeName = toPascalCase(
			refTypeName(propSchema.$ref, ctx.definitions),
		);
		const resolved = resolveRef(propSchema.$ref, ctx.definitions);
		if (resolved) {
			if (resolved.enum) {
				emitRustStringEnum(
					typeName,
					resolved.enum as string[],
					ctx,
					resolved.description,
				);
				return wrapOption(typeName, isRequired);
			}
			if (isObjectSchema(resolved)) {
				emitRustStruct(typeName, resolved, ctx);
				return wrapOption(typeName, isRequired);
			}
			return resolveRustType(
				resolved,
				parentTypeName,
				jsonPropName,
				isRequired,
				ctx,
			);
		}
		return wrapOption(typeName, isRequired);
	}

	// anyOf — nullable pattern or union
	if (propSchema.anyOf) {
		const discriminatedUnion = tryEmitRustDiscriminatedUnion(
			propSchema,
			parentTypeName,
			jsonPropName,
			ctx,
		);
		if (discriminatedUnion) {
			return wrapOption(discriminatedUnion, isRequired);
		}

		const nonNull = (propSchema.anyOf as JSONSchema7[]).filter(
			(s) => s.type !== "null",
		);
		const hasNull = (propSchema.anyOf as JSONSchema7[]).some(
			(s) => s.type === "null",
		);

		if (nonNull.length === 1) {
			const innerType = resolveRustType(
				nonNull[0],
				parentTypeName,
				jsonPropName,
				true,
				ctx,
			);
			if (isRequired && !hasNull) return innerType;
			return wrapOption(innerType, false);
		}

		if (nonNull.length > 1) {
			// Multi-type union — use serde_json::Value as escape hatch
			return wrapOption("serde_json::Value", isRequired);
		}
	}

	// oneOf — treat like anyOf for now
	if (propSchema.oneOf) {
		const discriminatedUnion = tryEmitRustDiscriminatedUnion(
			propSchema,
			parentTypeName,
			jsonPropName,
			ctx,
		);
		if (discriminatedUnion) {
			return wrapOption(discriminatedUnion, isRequired);
		}

		const nonNull = (propSchema.oneOf as JSONSchema7[]).filter(
			(s) => s.type !== "null",
		);
		if (nonNull.length === 1) {
			const innerType = resolveRustType(
				nonNull[0],
				parentTypeName,
				jsonPropName,
				true,
				ctx,
			);
			return wrapOption(innerType, isRequired);
		}
		return wrapOption("serde_json::Value", isRequired);
	}

	// allOf — merge and treat as object
	if (propSchema.allOf) {
		const merged = resolveObjectSchema(propSchema, ctx.definitions);
		if (merged && isObjectSchema(merged)) {
			const structName = (propSchema.title as string) || nestedName;
			emitRustStruct(structName, merged, ctx);
			return wrapOption(structName, isRequired);
		}
	}

	// enum
	if (propSchema.enum && Array.isArray(propSchema.enum)) {
		const enumName = (propSchema.title as string) || nestedName;
		emitRustStringEnum(
			enumName,
			propSchema.enum as string[],
			ctx,
			propSchema.description,
		);
		return wrapOption(enumName, isRequired);
	}

	// const — just a string
	if (propSchema.const !== undefined) {
		if (typeof propSchema.const === "string") {
			const enumName = (propSchema.title as string) || nestedName;
			emitRustConstStringEnum(
				enumName,
				propSchema.const,
				ctx,
				propSchema.description,
			);
			return wrapOption(enumName, isRequired);
		}
		return wrapOption("serde_json::Value", isRequired);
	}

	const schemaType = propSchema.type;

	// Type arrays like ["string", "null"]
	if (Array.isArray(schemaType)) {
		const nonNullTypes = (schemaType as string[]).filter((t) => t !== "null");
		if (nonNullTypes.length === 1) {
			const inner = resolveRustType(
				{ ...propSchema, type: nonNullTypes[0] as JSONSchema7["type"] },
				parentTypeName,
				jsonPropName,
				true,
				ctx,
			);
			return wrapOption(inner, false);
		}
		return wrapOption("serde_json::Value", isRequired);
	}

	// Primitive types
	if (schemaType === "string") {
		const newtype = STRING_NEWTYPE_OVERRIDES[jsonPropName];
		if (newtype) return wrapOption(newtype, isRequired);
		return wrapOption("String", isRequired);
	}
	if (schemaType === "number") return wrapOption("f64", isRequired);
	if (schemaType === "integer") return wrapOption("i64", isRequired);
	if (schemaType === "boolean") return wrapOption("bool", isRequired);

	// Array
	if (schemaType === "array") {
		const items = propSchema.items as JSONSchema7 | undefined;
		if (items) {
			const itemType = resolveRustType(
				items,
				parentTypeName,
				`${jsonPropName}Item`,
				true,
				ctx,
			);
			return wrapOption(`Vec<${itemType}>`, isRequired);
		}
		return wrapOption("Vec<serde_json::Value>", isRequired);
	}

	// Object
	if (schemaType === "object" || (propSchema.properties && !schemaType)) {
		if (
			propSchema.properties &&
			Object.keys(propSchema.properties).length > 0
		) {
			const structName = (propSchema.title as string) || nestedName;
			emitRustStruct(structName, propSchema, ctx);
			return wrapOption(structName, isRequired);
		}
		if (propSchema.additionalProperties) {
			if (
				typeof propSchema.additionalProperties === "object" &&
				Object.keys(propSchema.additionalProperties as Record<string, unknown>)
					.length > 0
			) {
				const ap = propSchema.additionalProperties as JSONSchema7;
				if (ap.type === "object" && ap.properties) {
					const valueName = (ap.title as string) || `${nestedName}Value`;
					emitRustStruct(valueName, ap, ctx);
					return wrapOption(`HashMap<String, ${valueName}>`, isRequired);
				}
				const valueType = resolveRustType(
					ap,
					parentTypeName,
					`${jsonPropName}Value`,
					true,
					ctx,
				);
				return wrapOption(`HashMap<String, ${valueType}>`, isRequired);
			}
			return wrapOption("HashMap<String, serde_json::Value>", isRequired);
		}
		return wrapOption("serde_json::Value", isRequired);
	}

	// Fallback
	return wrapOption("serde_json::Value", isRequired);
}

function wrapOption(rustType: string, isRequired: boolean): string {
	if (isRequired) return rustType;
	// Don't double-wrap Option, Vec, or HashMap (they're already nullable-ish)
	if (
		rustType.startsWith("Option<") ||
		rustType.startsWith("Vec<") ||
		rustType.startsWith("HashMap<")
	) {
		return rustType;
	}
	return `Option<${rustType}>`;
}

// ── Struct emission ─────────────────────────────────────────────────────────

function emitRustStruct(
	typeName: string,
	schema: JSONSchema7,
	ctx: RustCodegenCtx,
	description?: string,
): void {
	if (ctx.generatedNames.has(typeName)) return;
	ctx.generatedNames.add(typeName);

	const required = new Set(schema.required || []);
	const lines: string[] = [];
	const desc = description || schema.description;
	if (desc) {
		for (const line of desc.split(/\r?\n/)) {
			lines.push(`/// ${line}`);
		}
	}
	if (isSchemaDeprecated(schema)) {
		lines.push("#[deprecated]");
	}
	lines.push("#[derive(Debug, Clone, Serialize, Deserialize)]");
	lines.push(`#[serde(rename_all = "camelCase")]`);
	lines.push(`pub struct ${typeName} {`);

	for (const [propName, propSchema] of Object.entries(
		schema.properties || {},
	)) {
		if (typeof propSchema !== "object") continue;
		const prop = propSchema as JSONSchema7;
		const isReq = required.has(propName);
		const rustField = safeRustFieldName(propName);
		const rustType = resolveRustType(prop, typeName, propName, isReq, ctx);

		if (prop.description) {
			for (const line of prop.description.split(/\r?\n/)) {
				lines.push(`    /// ${line}`);
			}
		}
		if (isSchemaDeprecated(prop)) {
			lines.push("    #[deprecated]");
		}

		// Determine if an explicit rename is needed. `rename_all = "camelCase"` on
		// the struct converts snake_case fields to camelCase automatically, so we
		// only need an explicit rename when that automatic conversion doesn't produce
		// the original JSON property name.
		const snakeField = toRustFieldName(propName);
		const autoRename = snakeToCamelCase(snakeField);
		const needsRename = autoRename !== propName;
		const isOptionType = rustType.startsWith("Option<");
		const needsSkip = !isReq && isOptionType;

		if (needsSkip && needsRename) {
			lines.push(
				`    #[serde(rename = "${propName}", skip_serializing_if = "Option::is_none")]`,
			);
		} else if (needsSkip) {
			lines.push(`    #[serde(skip_serializing_if = "Option::is_none")]`);
		} else if (!isReq && !isOptionType && needsRename) {
			lines.push(`    #[serde(rename = "${propName}", default)]`);
		} else if (!isReq && !isOptionType) {
			lines.push("    #[serde(default)]");
		} else if (needsRename) {
			lines.push(`    #[serde(rename = "${propName}")]`);
		}

		lines.push(`    pub ${rustField}: ${rustType},`);
	}

	lines.push("}");
	ctx.structs.push(lines.join("\n"));
}

// ── Enum emission ───────────────────────────────────────────────────────────

function emitRustStringEnum(
	enumName: string,
	values: string[],
	ctx: RustCodegenCtx,
	description?: string,
): void {
	if (ctx.generatedNames.has(enumName)) return;
	ctx.generatedNames.add(enumName);

	const lines: string[] = [];
	if (description) {
		for (const line of description.split(/\r?\n/)) {
			lines.push(`/// ${line}`);
		}
	}
	lines.push("#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]");
	lines.push(`pub enum ${enumName} {`);

	for (const value of values) {
		const variantName = toPascalCase(value);
		if (variantName !== value) {
			lines.push(`    #[serde(rename = "${value}")]`);
		}
		lines.push(`    ${variantName},`);
	}

	// Add a catch-all for forward compatibility
	lines.push("    /// Unknown variant for forward compatibility.");
	lines.push("    #[serde(other)]");
	lines.push("    Unknown,");

	lines.push("}");
	ctx.enums.push(lines.join("\n"));
}

function emitRustConstStringEnum(
	enumName: string,
	value: string,
	ctx: RustCodegenCtx,
	description?: string,
): void {
	if (ctx.generatedNames.has(enumName)) return;
	ctx.generatedNames.add(enumName);

	const lines: string[] = [];
	if (description) {
		for (const line of description.split(/\r?\n/)) {
			lines.push(`/// ${line}`);
		}
	}
	lines.push("#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]");
	lines.push(`pub enum ${enumName} {`);
	const variantName = toPascalCase(value);
	if (variantName !== value) {
		lines.push(`    #[serde(rename = "${value}")]`);
	}
	lines.push(`    ${variantName},`);
	lines.push("}");
	ctx.enums.push(lines.join("\n"));
}

// ── Session events generation ───────────────────────────────────────────────

interface EventVariant {
	/** The event type string, e.g. "session.start" */
	typeName: string;
	/** PascalCase variant name, e.g. "SessionStart" */
	variantName: string;
	/** Data struct name, e.g. "SessionStartData" */
	dataClassName: string;
	/** Schema for the data field */
	dataSchema: JSONSchema7;
	/** Description of the event */
	description?: string;
}

function extractEventVariants(schema: JSONSchema7): EventVariant[] {
	const definitionCollections = collectDefinitionCollections(
		schema as Record<string, unknown>,
	);
	const sessionEvent =
		resolveSchema(
			{ $ref: "#/definitions/SessionEvent" },
			definitionCollections,
		) ?? resolveSchema({ $ref: "#/$defs/SessionEvent" }, definitionCollections);
	if (!sessionEvent?.anyOf)
		throw new Error("Schema must have SessionEvent definition with anyOf");

	return (sessionEvent.anyOf as JSONSchema7[])
		.map((variant) => {
			const resolvedVariant =
				resolveObjectSchema(variant as JSONSchema7, definitionCollections) ??
				resolveSchema(variant as JSONSchema7, definitionCollections) ??
				(variant as JSONSchema7);
			if (typeof resolvedVariant !== "object" || !resolvedVariant.properties) {
				throw new Error("Invalid variant");
			}
			const typeSchema = resolvedVariant.properties.type as JSONSchema7;
			const typeName = typeSchema?.const as string;
			if (!typeName) throw new Error("Variant must have type.const");

			const dataSchema =
				resolveObjectSchema(
					resolvedVariant.properties.data as JSONSchema7,
					definitionCollections,
				) ??
				resolveSchema(
					resolvedVariant.properties.data as JSONSchema7,
					definitionCollections,
				) ??
				((resolvedVariant.properties.data as JSONSchema7) || {});

			return {
				typeName,
				variantName: toPascalCase(typeName),
				dataClassName: `${toPascalCase(typeName)}Data`,
				dataSchema,
				description: resolvedVariant.description || dataSchema.description,
			};
		})
		.filter((v) => !EXCLUDED_EVENT_TYPES.has(v.typeName));
}

function generateSessionEventsCode(schema: JSONSchema7): string {
	const variants = extractEventVariants(schema);
	const ctx = makeCtx(
		collectDefinitionCollections(schema as Record<string, unknown>),
	);

	// Generate per-event data structs
	for (const variant of variants) {
		emitRustStruct(
			variant.dataClassName,
			variant.dataSchema,
			ctx,
			variant.description,
		);
	}

	// Build the SessionEventType enum
	const typeEnumLines: string[] = [];
	typeEnumLines.push("/// Identifies the kind of session event.");
	typeEnumLines.push(
		"#[derive(Debug, Clone, PartialEq, Eq, Hash, Serialize, Deserialize)]",
	);
	typeEnumLines.push("pub enum SessionEventType {");
	for (const variant of variants) {
		typeEnumLines.push(`    #[serde(rename = "${variant.typeName}")]`);
		typeEnumLines.push(`    ${variant.variantName},`);
	}
	typeEnumLines.push("    /// Unknown event type for forward compatibility.");
	typeEnumLines.push("    #[serde(other)]");
	typeEnumLines.push("    Unknown,");
	typeEnumLines.push("}");

	// Build the SessionEventData enum (adjacently tagged by type/data)
	const dataEnumLines: string[] = [];
	dataEnumLines.push(
		"/// Typed session event data, discriminated by the event `type` field.",
	);
	dataEnumLines.push("///");
	dataEnumLines.push(
		"/// Use with [`TypedSessionEvent`] for fully typed event handling.",
	);
	dataEnumLines.push("#[derive(Debug, Clone, Serialize, Deserialize)]");
	dataEnumLines.push(`#[serde(tag = "type", content = "data")]`);
	dataEnumLines.push("pub enum SessionEventData {");
	for (const variant of variants) {
		dataEnumLines.push(`    #[serde(rename = "${variant.typeName}")]`);
		dataEnumLines.push(`    ${variant.variantName}(${variant.dataClassName}),`);
	}
	dataEnumLines.push("}");

	// Build TypedSessionEvent that combines common fields with typed data
	const typedEventLines: string[] = [];
	typedEventLines.push("/// A session event with typed data payload.");
	typedEventLines.push("///");
	typedEventLines.push(
		"/// The common event fields (id, timestamp, parentId, ephemeral) are",
	);
	typedEventLines.push(
		"/// available directly. The event-specific data is in the `payload` field",
	);
	typedEventLines.push("/// as a [`SessionEventData`] enum.");
	typedEventLines.push("#[derive(Debug, Clone, Serialize, Deserialize)]");
	typedEventLines.push(`#[serde(rename_all = "camelCase")]`);
	typedEventLines.push("pub struct TypedSessionEvent {");
	typedEventLines.push("    /// Unique event identifier (UUID v4).");
	typedEventLines.push("    pub id: String,");
	typedEventLines.push(
		"    /// ISO 8601 timestamp when the event was created.",
	);
	typedEventLines.push("    pub timestamp: String,");
	typedEventLines.push("    /// ID of the preceding event in the chain.");
	typedEventLines.push(`    #[serde(skip_serializing_if = "Option::is_none")]`);
	typedEventLines.push("    pub parent_id: Option<String>,");
	typedEventLines.push(
		"    /// When true, the event is transient and not persisted.",
	);
	typedEventLines.push(`    #[serde(skip_serializing_if = "Option::is_none")]`);
	typedEventLines.push("    pub ephemeral: Option<bool>,");
	typedEventLines.push(
		"    /// The typed event payload (discriminated by event type).",
	);
	typedEventLines.push("    #[serde(flatten)]");
	typedEventLines.push("    pub payload: SessionEventData,");
	typedEventLines.push("}");

	// Assemble file
	const out: string[] = [];
	out.push(
		"//! Auto-generated from session-events.schema.json — do not edit manually.",
	);
	out.push("");
	out.push("use std::collections::HashMap;");
	out.push("");
	out.push("use serde::{Deserialize, Serialize};");
	out.push("");
	out.push("use crate::types::{RequestId, SessionId};");
	out.push("");

	// SessionEventType enum
	out.push(typeEnumLines.join("\n"));
	out.push("");

	// SessionEventData enum
	out.push(dataEnumLines.join("\n"));
	out.push("");

	// TypedSessionEvent struct
	out.push(typedEventLines.join("\n"));
	out.push("");

	// Per-event data structs
	for (const block of ctx.structs) {
		out.push(block);
		out.push("");
	}

	// Supporting enums
	for (const block of ctx.enums) {
		out.push(block);
		out.push("");
	}

	return out.join("\n");
}

// ── API types generation ────────────────────────────────────────────────────

function collectRpcMethods(
	node: Record<string, unknown>,
	prefix = "",
): RpcMethod[] {
	const methods: RpcMethod[] = [];
	for (const [key, value] of Object.entries(node)) {
		if (isRpcMethod(value)) {
			methods.push(value);
		} else if (typeof value === "object" && value !== null) {
			methods.push(
				...collectRpcMethods(
					value as Record<string, unknown>,
					prefix ? `${prefix}.${key}` : key,
				),
			);
		}
	}
	return methods;
}

function rustParamsTypeName(method: RpcMethod): string {
	return getRpcSchemaTypeName(
		method.params,
		`${toPascalCase(method.rpcMethod)}Params`,
	);
}

function rustResultTypeName(method: RpcMethod): string {
	return getRpcSchemaTypeName(
		method.result,
		`${toPascalCase(method.rpcMethod)}Result`,
	);
}

function generateApiTypesCode(apiSchema: ApiSchema): string {
	const definitions = collectDefinitions(apiSchema as Record<string, unknown>);
	const defCollections = collectDefinitionCollections(
		apiSchema as Record<string, unknown>,
	);
	const ctx = makeCtx(defCollections);

	// Generate shared definitions (structs & enums)
	for (const [name, def] of Object.entries(definitions)) {
		if (typeof def !== "object" || def === null) continue;
		const schema = def as JSONSchema7;

		if (schema.enum && Array.isArray(schema.enum)) {
			emitRustStringEnum(
				name,
				schema.enum as string[],
				ctx,
				schema.description,
			);
		} else if (isObjectSchema(schema)) {
			emitRustStruct(name, schema, ctx, schema.description);
		}
	}

	// Collect all RPC methods and generate request/response types
	const allMethods: RpcMethod[] = [];
	for (const group of [
		apiSchema.server,
		apiSchema.session,
		apiSchema.clientSession,
	]) {
		if (group) {
			allMethods.push(...collectRpcMethods(group as Record<string, unknown>));
		}
	}

	// RPC method name constants
	const methodConstLines: string[] = [];
	methodConstLines.push("/// JSON-RPC method name constants.");
	methodConstLines.push("pub mod rpc_methods {");

	for (const method of allMethods) {
		const constName = method.rpcMethod.replace(/\./g, "_").toUpperCase();
		methodConstLines.push(`    /// \`${method.rpcMethod}\``);
		methodConstLines.push(
			`    pub const ${constName}: &str = "${method.rpcMethod}";`,
		);
	}
	methodConstLines.push("}");

	// Generate param/result types for each method
	for (const method of allMethods) {
		if (
			method.params &&
			isObjectSchema(method.params) &&
			!isVoidSchema(method.params)
		) {
			const paramsName = rustParamsTypeName(method);
			emitRustStruct(paramsName, method.params, ctx, method.params.description);
		}
		if (method.result && !isVoidSchema(method.result)) {
			const resultName = rustResultTypeName(method);
			const resolved = resolveSchema(method.result, defCollections);
			if (resolved) {
				if (resolved.enum && Array.isArray(resolved.enum)) {
					// Already generated from definitions
				} else if (isObjectSchema(resolved)) {
					emitRustStruct(resultName, resolved, ctx, resolved.description);
				}
			}
		}
	}

	// Assemble file
	const out: string[] = [];
	out.push("//! Auto-generated from api.schema.json — do not edit manually.");
	out.push("");
	out.push("#![allow(clippy::large_enum_variant)]");
	out.push("");
	out.push("use std::collections::HashMap;");
	out.push("");
	out.push("use serde::{Deserialize, Serialize};");
	out.push("");
	out.push("use crate::types::{RequestId, SessionId};");
	out.push("");

	// Method constants
	out.push(methodConstLines.join("\n"));
	out.push("");

	// Shared definition types first, then RPC types
	for (const block of ctx.structs) {
		out.push(block);
		out.push("");
	}

	for (const block of ctx.enums) {
		out.push(block);
		out.push("");
	}

	return out.join("\n");
}

// ── mod.rs generation ───────────────────────────────────────────────────────

function generateModRs(): string {
	const lines: string[] = [];
	lines.push("//! Auto-generated protocol types — do not edit manually.");
	lines.push("//!");
	lines.push(
		"//! Generated from the Copilot protocol JSON Schemas by `scripts/codegen/rust.ts`.",
	);
	lines.push("#![allow(missing_docs)]");
	lines.push("#![allow(rustdoc::bare_urls)]");
	lines.push("");
	lines.push("pub mod api_types;");
	lines.push("pub mod session_events;");
	lines.push("");
	lines.push(
		"// Re-export session event types at the module root — no conflicts with",
	);
	lines.push(
		"// hand-written types. API types are kept namespaced under `api_types::`",
	);
	lines.push(
		"// because some names (Tool, ModelCapabilities, etc.) overlap with the",
	);
	lines.push("// hand-written SDK API types in `types.rs`.");
	lines.push("pub use session_events::*;");
	lines.push("");
	return lines.join("\n");
}

// ── Format with rustfmt ─────────────────────────────────────────────────────

async function rustfmt(filePath: string): Promise<void> {
	try {
		await execFileAsync("rustfmt", ["--edition", "2021", filePath]);
	} catch (e: unknown) {
		const error = e as { stderr?: string };
		console.warn(
			`rustfmt warning for ${path.basename(filePath)}: ${error.stderr || e}`,
		);
	}
}

// ── Main ────────────────────────────────────────────────────────────────────

async function generate(): Promise<void> {
	console.log("Loading schemas...");

	const sessionEventsSchemaPath = await getSessionEventsSchemaPath();
	const apiSchemaPath = await getApiSchemaPath(process.argv[2]);

	const sessionEventsRaw = JSON.parse(
		await fs.readFile(sessionEventsSchemaPath, "utf-8"),
	);
	const apiRaw = JSON.parse(
		await fs.readFile(apiSchemaPath, "utf-8"),
	) as ApiSchema;

	const sessionEventsSchema = postProcessSchema(
		sessionEventsRaw as JSONSchema7,
	);
	const apiSchema = postProcessSchema(
		apiRaw as JSONSchema7,
	) as unknown as ApiSchema;

	// Ensure output directory exists
	await fs.mkdir(GENERATED_DIR, { recursive: true });

	// Generate session events
	console.log("Generating session_events.rs...");
	const sessionEventsCode = generateSessionEventsCode(sessionEventsSchema);
	const sessionEventsPath = path.join(GENERATED_DIR, "session_events.rs");
	await fs.writeFile(sessionEventsPath, sessionEventsCode, "utf-8");
	await rustfmt(sessionEventsPath);

	// Generate API types
	console.log("Generating api_types.rs...");
	const apiTypesCode = generateApiTypesCode(apiSchema);
	const apiTypesPath = path.join(GENERATED_DIR, "api_types.rs");
	await fs.writeFile(apiTypesPath, apiTypesCode, "utf-8");
	await rustfmt(apiTypesPath);

	// Generate mod.rs
	console.log("Generating mod.rs...");
	const modRsCode = generateModRs();
	const modRsPath = path.join(GENERATED_DIR, "mod.rs");
	await fs.writeFile(modRsPath, modRsCode, "utf-8");
	await rustfmt(modRsPath);

	console.log(`Done! Generated files in ${GENERATED_DIR}`);
}

generate().catch((err) => {
	console.error("Code generation failed:", err);
	process.exit(1);
});
