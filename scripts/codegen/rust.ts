/**
 * Rust code generator for the Copilot protocol JSON Schemas.
 *
 * Reads api.schema.json and session-events.schema.json, emits idiomatic Rust
 * types to rust/src/generated/.
 *
 * Usage:
 *   npx tsx scripts/codegen/rust.ts
 *   npx tsx scripts/codegen/rust.ts <apiSchemaPath>
 *   npx tsx scripts/codegen/rust.ts <sessionEventsSchemaPath> <apiSchemaPath>
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
	collectReachableDefinitionNames,
	findSharedSchemaDefinitions,
	getApiSchemaPath,
	getNullableInner,
	getRpcSchemaTypeName,
	getSessionEventsSchemaPath,
	isObjectSchema,
	isRpcMethod,
	isSchemaDeprecated,
	isSchemaExperimental,
	isVoidSchema,
	parseExternalSchemaRef,
	postProcessSchema,
	refTypeName,
	resolveObjectSchema,
	resolveRef,
	resolveSchema,
	rewriteSharedDefinitionReferences,
	stripBooleanLiterals,
} from "./utils.js";

const execFileAsync = promisify(execFile);

const GENERATED_DIR = path.join(REPO_ROOT, "rust/src/generated");

const EXTERNAL_SCHEMA_RUST_MODULE: Record<string, string> = {
	"session-events.schema.json": "super::session_events",
};

const EXTERNAL_SCHEMA_RUST_TYPE_MODULE: Record<string, Record<string, string>> = {
	"session-events.schema.json": {
		SessionEvent: "crate::types",
	},
};

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
	const name = s
		.split(/[^A-Za-z0-9]+/)
		.filter(Boolean)
		.map((w) => w.charAt(0).toUpperCase() + w.slice(1))
		.join("");
	if (!name) return "Value";
	return /^[0-9]/.test(name) ? `Value${name}` : name;
}

function toRustPascalIdentifier(value: string, fallback: string): string {
	let identifier = toPascalCase(value);
	if (!identifier) {
		identifier = fallback;
	} else if (!/^[A-Za-z_]/.test(identifier)) {
		identifier = `${fallback}${identifier}`;
	}

	return RUST_KEYWORDS.has(identifier) ? `${identifier}Value` : identifier;
}

function uniqueRustPascalIdentifier(
	value: string,
	used: Set<string>,
	fallback: string,
	reserved: Set<string> = new Set(),
): string {
	const identifier = toRustPascalIdentifier(value, fallback);
	if (used.has(identifier) || reserved.has(identifier)) {
		throw new Error(
			`Generated Rust enum variant identifier "${identifier}" is not unique for value "${value}". Add an explicit naming rule instead of stabilizing an arbitrary public variant name.`,
		);
	}
	used.add(identifier);
	return identifier;
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
	/**
	 * Generated type names that do not (and cannot trivially) implement
	 * `Default` — currently `#[serde(untagged)]` enums of distinct payload
	 * structs. Structs with a *required* field of one of these types must
	 * also skip the `Default` derive; their names are added here on emission
	 * so the property propagates transitively.
	 */
	nonDefaultableTypes: Set<string>;
	/** Schema definitions for $ref resolution. */
	definitions?: DefinitionCollections;
	/** When set, only these const-valued properties are accepted as union discriminators. */
	unionDiscriminatorProperties?: Set<string>;
	/** Whether unions without a const-valued discriminator should be emitted. */
	allowUntaggedUnions: boolean;
	/** Specific union type names allowed even when their discriminator is not generally allowed. */
	allowedUnionTypeNames: Set<string>;
	/** External schema references that are actually emitted in generated types. */
	externalTypeRefs: Map<string, Set<string>>;
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

interface RustUnionVariant {
	schema: JSONSchema7;
	typeName: string;
}

function findRustDiscriminator(variants: RustUnionVariant[]): string | null {
	const first = variants[0]?.schema;
	if (!isObjectSchema(first) || !first.properties) return null;

	for (const [propName, propSchema] of Object.entries(first.properties).sort(
		([a], [b]) => a.localeCompare(b),
	)) {
		if (typeof propSchema !== "object") continue;
		if ((propSchema as JSONSchema7).const === undefined) continue;

		const values = new Set<string>();
		let isValid = true;
		for (const { schema } of variants) {
			if (!isObjectSchema(schema) || !schema.properties) {
				isValid = false;
				break;
			}
			const candidate = schema.properties[propName];
			if (typeof candidate !== "object") {
				isValid = false;
				break;
			}
			const value = (candidate as JSONSchema7).const;
			if (value === undefined) {
				isValid = false;
				break;
			}
			const key = String(value);
			if (values.has(key)) {
				isValid = false;
				break;
			}
			values.add(key);
		}
		if (isValid) return propName;
	}

	return null;
}

function tryEmitRustUnion(
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

	const resolvedVariants: RustUnionVariant[] = [];
	for (let i = 0; i < nonNull.length; i++) {
		const variant = nonNull[i];
		if (variant.$ref && typeof variant.$ref === "string") {
			const resolved = resolveRef(variant.$ref, ctx.definitions);
			if (resolved && !isObjectSchema(resolved)) return null;
			resolvedVariants.push({
				schema: (resolved ?? variant) as JSONSchema7,
				typeName: rustRefTypeName(variant.$ref, ctx.definitions),
			});
			continue;
		}

		const resolved =
			resolveObjectSchema(variant, ctx.definitions) ??
			resolveSchema(variant, ctx.definitions) ??
			variant;
		if (!isObjectSchema(resolved)) return null;
		const discriminatorValue = Object.values(resolved.properties ?? {}).find(
			(prop) => typeof prop === "object" && (prop as JSONSchema7).const !== undefined,
		) as JSONSchema7 | undefined;
		const typeName =
			(typeof resolved.title === "string" && resolved.title) ||
			(discriminatorValue?.const !== undefined
				? `${enumName}${toPascalCase(String(discriminatorValue.const))}`
				: `${enumName}Variant${i + 1}`);

		resolvedVariants.push({
			schema: resolved as JSONSchema7,
			typeName,
		});
	}

	const discriminator = findRustDiscriminator(resolvedVariants);
	const isAllowedUnionType = ctx.allowedUnionTypeNames.has(enumName);
	if (discriminator) {
		if (
			ctx.unionDiscriminatorProperties &&
			!ctx.unionDiscriminatorProperties.has(discriminator) &&
			!isAllowedUnionType
		) {
			return null;
		}
	} else if (!ctx.allowUntaggedUnions || !isAllowedUnionType) {
		return null;
	}

	if (ctx.generatedNames.has(enumName)) {
		return enumName;
	}
	ctx.generatedNames.add(enumName);
	// Untagged enums of distinct payload structs have no obvious default
	// variant; structs with a required field of this type will also skip
	// the `Default` derive.
	ctx.nonDefaultableTypes.add(enumName);

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
	pushRustExperimentalDocs(lines, isSchemaExperimental(schema));
	lines.push("#[derive(Debug, Clone, Serialize, Deserialize)]");
	lines.push("#[serde(untagged)]");
	lines.push(`pub enum ${enumName} {`);

	const usedVariantNames = new Set<string>();
	for (const { schema: variantSchema, typeName } of resolvedVariants) {
		const discriminatorValue =
			discriminator && isObjectSchema(variantSchema)
				? (variantSchema.properties?.[discriminator] as JSONSchema7 | undefined)
						?.const
				: undefined;
		const variantName = uniqueRustPascalIdentifier(
			discriminatorValue === undefined ? typeName : String(discriminatorValue),
			usedVariantNames,
			"Variant",
		);
		lines.push(`    ${variantName}(${stripOption(typeName)}),`);
	}

	lines.push("}");
	ctx.enums.push(lines.join("\n"));
	return enumName;
}

function recordExternalRustTypeRef(ref: string, ctx: RustCodegenCtx): void {
	const externalRef = parseExternalSchemaRef(ref);
	if (!externalRef) return;

	let typeNames = ctx.externalTypeRefs.get(externalRef.schemaFile);
	if (!typeNames) {
		typeNames = new Set<string>();
		ctx.externalTypeRefs.set(externalRef.schemaFile, typeNames);
	}
	typeNames.add(externalRef.definitionName);
}

function makeCtx(
	definitions?: DefinitionCollections,
	options: {
		unionDiscriminatorProperties?: Set<string> | null;
		allowUntaggedUnions?: boolean;
		allowedUnionTypeNames?: Iterable<string>;
	} = {},
): RustCodegenCtx {
	return {
		structs: [],
		enums: [],
		generatedNames: new Set(),
		nonDefaultableTypes: new Set(),
		definitions,
		unionDiscriminatorProperties:
			options.unionDiscriminatorProperties === null
				? undefined
				: (options.unionDiscriminatorProperties ?? new Set(["kind"])),
		allowUntaggedUnions: options.allowUntaggedUnions ?? false,
		allowedUnionTypeNames: new Set(options.allowedUnionTypeNames ?? []),
		externalTypeRefs: new Map(),
	};
}

function pushRustExperimentalDocs(
	lines: string[],
	experimental: boolean,
	indent = "",
): void {
	if (!experimental) return;
	lines.push(`${indent}///`);
	lines.push(`${indent}/// <div class="warning">`);
	lines.push(`${indent}///`);
	lines.push(
		`${indent}/// **Experimental.** This type is part of an experimental wire-protocol surface`,
	);
	lines.push(
		`${indent}/// and may change or be removed in future SDK or CLI releases.`,
	);
	lines.push(`${indent}///`);
	lines.push(`${indent}/// </div>`);
}

// ── Type resolution ─────────────────────────────────────────────────────────

function rustRefTypeName(ref: string, definitions?: DefinitionCollections): string {
	const externalRef = parseExternalSchemaRef(ref);
	return toPascalCase(externalRef?.definitionName ?? refTypeName(ref, definitions));
}

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
		recordExternalRustTypeRef(propSchema.$ref, ctx);
		const typeName = rustRefTypeName(propSchema.$ref, ctx.definitions);
		const resolved = resolveRef(propSchema.$ref, ctx.definitions);
		if (resolved) {
			if (resolved.enum) {
				emitRustStringEnum(
					typeName,
					resolved.enum as string[],
					ctx,
					resolved.description,
					isSchemaExperimental(resolved),
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
		const unionType = tryEmitRustUnion(
			propSchema,
			parentTypeName,
			jsonPropName,
			ctx,
		);
		if (unionType) {
			return wrapOption(unionType, isRequired);
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
		const unionType = tryEmitRustUnion(
			propSchema,
			parentTypeName,
			jsonPropName,
			ctx,
		);
		if (unionType) {
			return wrapOption(unionType, isRequired);
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
			isSchemaExperimental(propSchema),
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
	pushRustExperimentalDocs(lines, isSchemaExperimental(schema));
	if (isSchemaDeprecated(schema)) {
		lines.push("#[deprecated]");
	}

	// Resolve field types up-front so we can decide whether `Default` can be
	// derived. A required field whose bare type is non-default-able (e.g. an
	// untagged enum, or another struct that already opted out) blocks the
	// derive and propagates the opt-out to this struct.
	interface FieldInfo {
		propName: string;
		prop: JSONSchema7;
		isReq: boolean;
		rustField: string;
		rustType: string;
	}
	const fields: FieldInfo[] = [];
	for (const [propName, propSchema] of Object.entries(
		schema.properties || {},
	)) {
		if (typeof propSchema !== "object") continue;
		const prop = propSchema as JSONSchema7;
		const isReq = required.has(propName);
		const rustField = safeRustFieldName(propName);
		const rustType = resolveRustType(prop, typeName, propName, isReq, ctx);
		fields.push({ propName, prop, isReq, rustField, rustType });
	}

	const blocksDefault = fields.some(
		(f) => f.isReq && ctx.nonDefaultableTypes.has(f.rustType),
	);
	if (blocksDefault) {
		ctx.nonDefaultableTypes.add(typeName);
		lines.push("#[derive(Debug, Clone, Serialize, Deserialize)]");
	} else {
		lines.push("#[derive(Debug, Clone, Default, Serialize, Deserialize)]");
	}
	lines.push(`#[serde(rename_all = "camelCase")]`);
	lines.push(`pub struct ${typeName} {`);

	for (const { propName, prop, isReq, rustField, rustType } of fields) {
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
	experimental = false,
): void {
	if (ctx.generatedNames.has(enumName)) return;
	ctx.generatedNames.add(enumName);

	const lines: string[] = [];
	if (description) {
		for (const line of description.split(/\r?\n/)) {
			lines.push(`/// ${line}`);
		}
	}
	pushRustExperimentalDocs(lines, experimental);
	lines.push(
		"#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]",
	);
	lines.push(`pub enum ${enumName} {`);

	const usedVariantNames = new Set<string>();
	const reservedVariantNames = new Set(["Unknown"]);
	for (const value of values) {
		const variantName = uniqueRustPascalIdentifier(
			value,
			usedVariantNames,
			"Value",
			reservedVariantNames,
		);
		if (variantName !== value) {
			lines.push(`    #[serde(rename = "${value}")]`);
		}
		lines.push(`    ${variantName},`);
	}

	// Add a catch-all for forward compatibility. This is also the `Default`
	// variant — for wire-protocol enums an unknown/sentinel value is the only
	// safe default.
	lines.push("    /// Unknown variant for forward compatibility.");
	lines.push("    #[default]");
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
	lines.push(
		"#[derive(Debug, Clone, Default, PartialEq, Eq, Serialize, Deserialize)]",
	);
	lines.push(`pub enum ${enumName} {`);
	const variantName = toRustPascalIdentifier(value, "Value");
	if (variantName !== value) {
		lines.push(`    #[serde(rename = "${value}")]`);
	}
	lines.push("    #[default]");
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
	/** Whether the event definition is experimental. */
	eventExperimental: boolean;
	/** Whether the event data definition is experimental. */
	dataExperimental: boolean;
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
				eventExperimental: isSchemaExperimental(resolvedVariant),
				dataExperimental: isSchemaExperimental(dataSchema),
			};
		})
		.filter((v) => !EXCLUDED_EVENT_TYPES.has(v.typeName));
}

function generateSessionEventsCode(schema: JSONSchema7): string {
	const variants = extractEventVariants(schema);
	const ctx = makeCtx(
		collectDefinitionCollections(schema as Record<string, unknown>),
		{
			allowUntaggedUnions: true,
			allowedUnionTypeNames: [
				"ToolExecutionCompleteContent",
				"ToolExecutionCompleteContentResourceDetails",
			],
		},
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
		"#[derive(Debug, Clone, Default, PartialEq, Eq, Hash, Serialize, Deserialize)]",
	);
	typeEnumLines.push("pub enum SessionEventType {");
	for (const variant of variants) {
		pushRustExperimentalDocs(
			typeEnumLines,
			variant.eventExperimental,
			"    ",
		);
		typeEnumLines.push(`    #[serde(rename = "${variant.typeName}")]`);
		typeEnumLines.push(`    ${variant.variantName},`);
	}
	typeEnumLines.push("    /// Unknown event type for forward compatibility.");
	typeEnumLines.push("    #[default]");
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
		pushRustExperimentalDocs(
			dataEnumLines,
			variant.dataExperimental,
			"    ",
		);
		dataEnumLines.push(`    #[serde(rename = "${variant.typeName}")]`);
		dataEnumLines.push(`    ${variant.variantName}(${variant.dataClassName}),`);
	}
	dataEnumLines.push("}");

	// Build TypedSessionEvent that combines common fields with typed data
	const typedEventLines: string[] = [];
	typedEventLines.push("/// A session event with typed data payload.");
	typedEventLines.push("///");
	typedEventLines.push(
		"/// The common event fields (id, timestamp, parentId, ephemeral, agentId)",
	);
	typedEventLines.push(
		"/// are available directly. The event-specific data is in the `payload`",
	);
	typedEventLines.push("/// field as a [`SessionEventData`] enum.");
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
		"    /// Sub-agent instance identifier. Absent for events from the root /",
	);
	typedEventLines.push(
		"    /// main agent and session-level events.",
	);
	typedEventLines.push(`    #[serde(skip_serializing_if = "Option::is_none")]`);
	typedEventLines.push("    pub agent_id: Option<String>,");
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

function rustParamsTypeName(method: RpcMethod, ctx: RustCodegenCtx): string {
	if (method.params?.$ref && parseExternalSchemaRef(method.params.$ref)) {
		recordExternalRustTypeRef(method.params.$ref, ctx);
		return rustRefTypeName(method.params.$ref);
	}
	return getRpcSchemaTypeName(
		method.params,
		`${toPascalCase(method.rpcMethod)}Params`,
	);
}

function rustResultTypeName(method: RpcMethod, ctx: RustCodegenCtx): string {
	if (method.result?.$ref && parseExternalSchemaRef(method.result.$ref)) {
		recordExternalRustTypeRef(method.result.$ref, ctx);
		return rustRefTypeName(method.result.$ref);
	}
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
				isSchemaExperimental(schema),
			);
		} else if (getUnionVariants(schema)) {
			tryEmitRustUnion(schema, name, "", ctx);
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
			const paramsName = rustParamsTypeName(method, ctx);
			emitRustStruct(paramsName, method.params, ctx, method.params.description);
		}
		if (method.result && !isVoidSchema(method.result)) {
			const resultName = rustResultTypeName(method, ctx);
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
	const externalImports = new Map<string, Set<string>>();
	for (const [schemaFile, typeNames] of ctx.externalTypeRefs) {
		const defaultModule = EXTERNAL_SCHEMA_RUST_MODULE[schemaFile];
		const typeModules = EXTERNAL_SCHEMA_RUST_TYPE_MODULE[schemaFile] ?? {};
		for (const typeName of typeNames) {
			const module = typeModules[typeName] ?? defaultModule;
			if (!module) continue;
			let names = externalImports.get(module);
			if (!names) {
				names = new Set<string>();
				externalImports.set(module, names);
			}
			names.add(typeName);
		}
	}
	for (const [module, typeNames] of [...externalImports].sort(([left], [right]) =>
		left.localeCompare(right),
	)) {
		out.push(`use ${module}::{${[...typeNames].sort().join(", ")}};`);
	}
	if (externalImports.size > 0) {
		out.push("");
	}
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

// ── Typed RPC namespace generation ──────────────────────────────────────────

interface NamespaceNode {
	name: string;
	typeName: string;
	methods: RpcMethod[];
	children: Map<string, NamespaceNode>;
}

function newNamespaceNode(name: string, typeName: string): NamespaceNode {
	return { name, typeName, methods: [], children: new Map() };
}

/**
 * Build a namespace tree from a list of methods. `groupOf(method)` returns the
 * dotted group path (e.g. "mcp.config" for "mcp.config.list" / "workspaces"
 * for "workspaces.listFiles"); the last segment of `rpcMethod` is the leaf
 * method name.
 */
function buildNamespaceTree(
	rootTypeName: string,
	methods: RpcMethod[],
	stripPrefix: string,
): NamespaceNode {
	const root = newNamespaceNode("", rootTypeName);
	for (const method of methods) {
		const trimmed = stripPrefix && method.rpcMethod.startsWith(stripPrefix)
			? method.rpcMethod.slice(stripPrefix.length)
			: method.rpcMethod;
		const segments = trimmed.split(".");
		const groupSegments = segments.slice(0, -1);
		let node = root;
		for (const seg of groupSegments) {
			let child = node.children.get(seg);
			if (!child) {
				const childTypeName = `${node.typeName}${toPascalCase(seg)}`;
				child = newNamespaceNode(seg, childTypeName);
				node.children.set(seg, child);
			}
			node = child;
		}
		node.methods.push(method);
	}
	return root;
}

/**
 * Determine if a method has typed params. Returns `{ hasParams, typeName }`.
 * Handles `$ref`-based, title-bearing, and inline params uniformly:
 *
 *   - Resolves `$ref` to its definition.
 *   - For session methods, ignores `sessionId` (the namespace injects it).
 *   - Returns `hasParams=false` when the resolved property set (after the
 *     sessionId filter for session methods) is empty.
 *   - The type name comes from `$ref` (preferred), then the resolved
 *     definition's `title`, then the inline params `title`.
 */
function getMethodParamsInfo(
	method: RpcMethod,
	defCollections: DefinitionCollections,
	isSession: boolean,
): { hasParams: boolean; optional: boolean; typeName: string | null } {
	if (!method.params) return { hasParams: false, optional: false, typeName: null };
	const inline = method.params as JSONSchema7 & { $ref?: string };
	const resolved = resolveObjectSchema(inline, defCollections) ??
		resolveSchema(inline, defCollections);
	if (!resolved) return { hasParams: false, optional: false, typeName: null };

	let typeName: string | null = null;
	if (typeof inline.$ref === "string") {
		typeName = refTypeName(inline.$ref, defCollections);
	} else if (typeof resolved.title === "string") {
		typeName = resolved.title;
	} else if (typeof inline.title === "string") {
		typeName = inline.title;
	}

	const allProps = Object.keys(resolved.properties || {});
	const props = isSession
		? allProps.filter((p) => p !== "sessionId")
		: allProps;
	if (props.length === 0) return { hasParams: false, optional: false, typeName: null };
	if (!typeName) return { hasParams: false, optional: false, typeName: null };
	const required = new Set(resolved.required || []);
	const hasRequiredParams = props.some((p) => required.has(p));
	const optional = !!getNullableInner(inline) && !hasRequiredParams;
	return { hasParams: true, optional, typeName };
}

function rpcMethodConstName(method: RpcMethod): string {
	return method.rpcMethod.replace(/\./g, "_").toUpperCase();
}

function emitNamespaceStruct(
	out: string[],
	node: NamespaceNode,
	holderType: string,
	holderField: string,
	isSession: boolean,
	defCollections: DefinitionCollections,
	docPrefix: string,
): void {
	const lifetimes = "<'a>";
	out.push(`/// ${docPrefix}`);
	out.push(`#[derive(Clone, Copy)]`);
	out.push(`pub struct ${node.typeName}${lifetimes} {`);
	out.push(`    pub(crate) ${holderField}: &'a ${holderType},`);
	out.push(`}`);
	out.push("");

	out.push(`impl${lifetimes} ${node.typeName}${lifetimes} {`);

	// Sub-namespace accessors
	const childNames = Array.from(node.children.keys()).sort();
	for (const childName of childNames) {
		const child = node.children.get(childName)!;
		const accessor = toSnakeCase(childName);
		const desc = isSession
			? `\`session.${accessorPath(node, childName, isSession)}.*\``
			: `\`${accessorPath(node, childName, isSession)}.*\``;
		out.push(`    /// ${desc} sub-namespace.`);
		out.push(
			`    pub fn ${accessor}(&self) -> ${child.typeName}<'a> {`,
		);
		out.push(`        ${child.typeName} { ${holderField}: self.${holderField} }`);
		out.push(`    }`);
		out.push("");
	}

	// Leaf methods
	for (const method of node.methods) {
		emitNamespaceMethod(out, method, holderField, isSession, defCollections);
	}

	out.push(`}`);
	out.push("");

	// Recursively emit child structs
	for (const childName of childNames) {
		const child = node.children.get(childName)!;
		const childDoc = isSession
			? `\`session.${accessorPath(node, childName, isSession)}.*\` RPCs.`
			: `\`${accessorPath(node, childName, isSession)}.*\` RPCs.`;
		emitNamespaceStruct(
			out,
			child,
			holderType,
			holderField,
			isSession,
			defCollections,
			childDoc,
		);
	}
}

function accessorPath(parent: NamespaceNode, child: string, _isSession: boolean): string {
	// Build wire-style dotted path from the namespace tree's "name" chain plus child.
	// `parent.name === ""` for root; we accumulate by retrieving parent name only.
	// (We don't track full ancestry here; this is just for doc strings — we
	// fall back to the child name alone when at the root.)
	if (!parent.name) return child;
	return `${parent.name}.${child}`;
}

function getResultTypeName(
	method: RpcMethod,
	defCollections: DefinitionCollections,
): string | null {
	const result = method.result as (JSONSchema7 & { $ref?: string }) | null;
	if (!result || isVoidSchema(result)) return null;
	if (typeof result.$ref === "string") {
		return refTypeName(result.$ref, defCollections);
	}
	if (typeof result.title === "string") return result.title;
	return `${toPascalCase(method.rpcMethod)}Result`;
}

function pushNamespaceMethodBody(
	out: string[],
	constName: string,
	isSession: boolean,
	hasParams: boolean,
	resultIsVoid: boolean,
): void {
	// Build the params Value sent over the wire.
	if (isSession) {
		if (hasParams) {
			out.push(`        let mut wire_params = serde_json::to_value(params)?;`);
			out.push(
				`        wire_params["sessionId"] = serde_json::Value::String(self.session.id().to_string());`,
			);
		} else {
			out.push(
				`        let wire_params = serde_json::json!({ "sessionId": self.session.id() });`,
			);
		}
		out.push(
			`        let _value = self.session.client().call(rpc_methods::${constName}, Some(wire_params)).await?;`,
		);
	} else {
		if (hasParams) {
			out.push(`        let wire_params = serde_json::to_value(params)?;`);
		} else {
			out.push(`        let wire_params = serde_json::json!({});`);
		}
		out.push(
			`        let _value = self.client.call(rpc_methods::${constName}, Some(wire_params)).await?;`,
		);
	}

	if (resultIsVoid) {
		out.push(`        Ok(())`);
	} else {
		out.push(`        Ok(serde_json::from_value(_value)?)`);
	}
	out.push(`    }`);
}

function emitNamespaceMethod(
	out: string[],
	method: RpcMethod,
	holderField: string,
	isSession: boolean,
	defCollections: DefinitionCollections,
): void {
	const wireMethod = method.rpcMethod;
	const constName = rpcMethodConstName(method);
	const lastSegment = wireMethod.split(".").pop()!;
	const fnName = toSnakeCase(lastSegment);

	const paramsInfo = getMethodParamsInfo(method, defCollections, isSession);
	const hasParams = paramsInfo.hasParams;
	const paramsTypeName = paramsInfo.typeName;

	const resultTypeName = getResultTypeName(method, defCollections);
	const returnType = resultTypeName ? resultTypeName : "()";
	const resultIsVoid = resultTypeName === null;

	const docs: string[] = [];
	docs.push(`    /// Wire method: \`${wireMethod}\`.`);
	if (method.deprecated) docs.push(`    #[deprecated]`);
	const stability = method.stability;
	if (stability === "experimental") {
		docs.push(`    ///`);
		docs.push(
			`    /// <div class="warning">`,
		);
		docs.push(
			`    ///`,
		);
		docs.push(
			`    /// **Experimental.** This API is part of an experimental wire-protocol surface`,
		);
		docs.push(
			`    /// and may change or be removed in future SDK or CLI releases. Pin both the`,
		);
		docs.push(
			`    /// SDK and CLI versions if your code depends on it.`,
		);
		docs.push(
			`    ///`,
		);
		docs.push(
			`    /// </div>`,
		);
	} else if (stability && stability !== "stable") {
		docs.push(`    /// Stability: \`${stability}\`.`);
	}

	const paramArg = hasParams ? `, params: ${paramsTypeName}` : "";

	out.push(...docs);
	if (hasParams && paramsInfo.optional) {
		out.push(
			`    pub async fn ${fnName}(&self) -> Result<${returnType}, Error> {`,
		);
		pushNamespaceMethodBody(out, constName, isSession, false, resultIsVoid);
		out.push("");
		out.push(...docs);
		out.push(
			`    pub async fn ${fnName}_with_params(&self, params: ${paramsTypeName}) -> Result<${returnType}, Error> {`,
		);
		pushNamespaceMethodBody(out, constName, isSession, true, resultIsVoid);
		out.push("");
		return;
	}

	out.push(
		`    pub async fn ${fnName}(&self${paramArg}) -> Result<${returnType}, Error> {`,
	);
	pushNamespaceMethodBody(out, constName, isSession, hasParams, resultIsVoid);
	out.push("");
}

function generateRpcCode(apiSchema: ApiSchema): string {
	const defCollections = collectDefinitionCollections(
		apiSchema as unknown as Record<string, unknown>,
	);

	const serverMethods = apiSchema.server
		? collectRpcMethods(apiSchema.server as Record<string, unknown>)
		: [];
	const sessionMethods = apiSchema.session
		? collectRpcMethods(apiSchema.session as Record<string, unknown>)
		: [];

	const clientRoot = buildNamespaceTree("ClientRpc", serverMethods, "");
	const sessionRoot = buildNamespaceTree(
		"SessionRpc",
		sessionMethods,
		"session.",
	);

	const out: string[] = [];
	out.push(
		"//! Auto-generated typed JSON-RPC namespace — do not edit manually.",
	);
	out.push("//!");
	out.push(
		"//! Generated from `api.schema.json` by `scripts/codegen/rust.ts`. The",
	);
	out.push(
		"//! [`ClientRpc`] and [`SessionRpc`] view structs let callers reach every",
	);
	out.push(
		"//! protocol method through a typed namespace tree, so wire method names",
	);
	out.push(
		"//! and request/response shapes live in exactly one place — this file.",
	);
	out.push("");
	out.push("#![allow(missing_docs)]");
	out.push("#![allow(clippy::too_many_arguments)]");
	out.push("");
	out.push("use super::api_types::{rpc_methods, *};");
	out.push("use crate::session::Session;");
	out.push("use crate::{Client, Error};");
	out.push("");

	emitNamespaceStruct(
		out,
		clientRoot,
		"Client",
		"client",
		false,
		defCollections,
		"Typed view over the [`Client`]'s server-level RPC namespace.",
	);
	emitNamespaceStruct(
		out,
		sessionRoot,
		"Session",
		"session",
		true,
		defCollections,
		"Typed view over a [`Session`]'s RPC namespace.",
	);

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
	lines.push("pub mod rpc;");
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

function parseSchemaArgs(): {
	sessionEventsSchemaPath?: string;
	apiSchemaPath?: string;
} {
	const [firstArg, secondArg] = process.argv.slice(2);
	if (secondArg) {
		return {
			sessionEventsSchemaPath: firstArg,
			apiSchemaPath: secondArg,
		};
	}

	return {
		apiSchemaPath: firstArg,
	};
}

async function generate(): Promise<void> {
	console.log("Loading schemas...");

	const schemaArgs = parseSchemaArgs();
	const sessionEventsSchemaPath =
		schemaArgs.sessionEventsSchemaPath || (await getSessionEventsSchemaPath());
	const apiSchemaPath = await getApiSchemaPath(schemaArgs.apiSchemaPath);

	const sessionEventsRaw = JSON.parse(
		await fs.readFile(sessionEventsSchemaPath, "utf-8"),
	);
	const apiRaw = JSON.parse(
		await fs.readFile(apiSchemaPath, "utf-8"),
	) as ApiSchema;

	const sessionEventsSchema = postProcessSchema(
		stripBooleanLiterals(sessionEventsRaw) as JSONSchema7,
	);
	const apiSchema = postProcessSchema(
		stripBooleanLiterals(apiRaw) as JSONSchema7,
	) as unknown as ApiSchema;

	// Ensure output directory exists
	await fs.mkdir(GENERATED_DIR, { recursive: true });

	// Generate session events
	console.log("Generating session_events.rs...");
	const sessionEventsCode = generateSessionEventsCode(sessionEventsSchema);
	const sharedDefinitions = findSharedSchemaDefinitions(
		apiSchema as unknown as Record<string, unknown>,
		sessionEventsSchema as unknown as Record<string, unknown>,
	);
	const reachableDefinitions = collectReachableDefinitionNames(
		sessionEventsSchema as unknown as Record<string, unknown>,
	);
	for (const name of [...sharedDefinitions]) {
		const declarationPattern = new RegExp(`\\bpub\\s+(?:struct|enum)\\s+${name}\\b`);
		if (!reachableDefinitions.has(name) || !declarationPattern.test(sessionEventsCode)) {
			sharedDefinitions.delete(name);
		}
	}
	const apiSchemaForGeneration = rewriteSharedDefinitionReferences(
		apiSchema,
		sharedDefinitions,
		"session-events.schema.json",
	);
	const sessionEventsPath = path.join(GENERATED_DIR, "session_events.rs");
	await fs.writeFile(sessionEventsPath, sessionEventsCode, "utf-8");
	await rustfmt(sessionEventsPath);

	// Generate API types
	console.log("Generating api_types.rs...");
	const apiTypesCode = generateApiTypesCode(apiSchemaForGeneration);
	const apiTypesPath = path.join(GENERATED_DIR, "api_types.rs");
	await fs.writeFile(apiTypesPath, apiTypesCode, "utf-8");
	await rustfmt(apiTypesPath);

	// Generate typed RPC namespace
	console.log("Generating rpc.rs...");
	const rpcCode = generateRpcCode(apiSchemaForGeneration);
	const rpcPath = path.join(GENERATED_DIR, "rpc.rs");
	await fs.writeFile(rpcPath, rpcCode, "utf-8");
	await rustfmt(rpcPath);

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
