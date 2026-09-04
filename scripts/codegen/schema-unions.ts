import type { JSONSchema7 } from "json-schema";

export type SchemaDiscriminatorValue = string | number | boolean | null;
export type UnknownVariantPolicy = "preserve" | "reject";

export interface SchemaDiscriminatedUnionVariant {
    source: JSONSchema7;
    schema: JSONSchema7;
    discriminatorValues: SchemaDiscriminatorValue[];
}

export interface SchemaDiscriminatorMapping {
    value: SchemaDiscriminatorValue;
    variants: SchemaDiscriminatedUnionVariant[];
}

export interface SchemaDiscriminatedUnion {
    property: string;
    variants: SchemaDiscriminatedUnionVariant[];
    mapping: SchemaDiscriminatorMapping[];
    unknownVariantPolicy: UnknownVariantPolicy;
}

export type SchemaVariantResolver = (
    schema: JSONSchema7,
) => JSONSchema7 | undefined;

function isDiscriminatorValue(
    value: unknown,
): value is SchemaDiscriminatorValue {
    return (
        value === null || ["string", "number", "boolean"].includes(typeof value)
    );
}

function discriminatorValues(
    schema: JSONSchema7,
): SchemaDiscriminatorValue[] | undefined {
    if (isDiscriminatorValue(schema.const)) {
        return [schema.const];
    }
    if (
        Array.isArray(schema.enum) &&
        schema.enum.length > 0 &&
        schema.enum.every(isDiscriminatorValue)
    ) {
        return [...new Set(schema.enum)];
    }
    return undefined;
}

export function schemaDiscriminatorValueKey(
    value: SchemaDiscriminatorValue,
): string {
    return `${typeof value}:${JSON.stringify(value)}`;
}

/**
 * Derive discriminator and unknown-value handling from JSON Schema alone.
 *
 * A union is closed when every resolved variant rejects additional properties.
 * Language emitters use that policy to reject unknown or missing discriminators
 * while retaining their idiomatic generated representation.
 */
export function analyseDiscriminatedUnionVariants(
    sources: JSONSchema7[],
    resolveVariant: SchemaVariantResolver = (schema) => schema,
): SchemaDiscriminatedUnion | undefined {
    if (sources.length < 2) return undefined;

    const resolved = sources.map((source) => resolveVariant(source));
    if (resolved.some((schema) => !schema?.properties)) return undefined;

    const schemas = resolved as JSONSchema7[];
    for (const property of Object.keys(schemas[0].properties ?? {}).sort()) {
        const variants: SchemaDiscriminatedUnionVariant[] = [];
        const mapping = new Map<
            string,
            {
                value: SchemaDiscriminatorValue;
                variants: SchemaDiscriminatedUnionVariant[];
            }
        >();
        let valid = true;

        for (let index = 0; index < schemas.length; index++) {
            const schema = schemas[index];
            const propertySchema = schema.properties?.[property];
            if (
                !(schema.required ?? []).includes(property) ||
                !propertySchema ||
                typeof propertySchema !== "object"
            ) {
                valid = false;
                break;
            }

            const values = discriminatorValues(propertySchema as JSONSchema7);
            if (!values) {
                valid = false;
                break;
            }

            const variant = {
                source: sources[index],
                schema,
                discriminatorValues: values,
            };
            variants.push(variant);
            for (const value of values) {
                const key = schemaDiscriminatorValueKey(value);
                const entry = mapping.get(key) ?? { value, variants: [] };
                entry.variants.push(variant);
                mapping.set(key, entry);
            }
        }

        if (valid && variants.length === schemas.length && mapping.size > 0) {
            return {
                property,
                variants,
                mapping: [...mapping.values()],
                unknownVariantPolicy: schemas.every(
                    (schema) => schema.additionalProperties === false,
                )
                    ? "reject"
                    : "preserve",
            };
        }
    }

    return undefined;
}

export function analyseDiscriminatedUnion(
    schema: JSONSchema7,
    resolveVariant: SchemaVariantResolver = (variant) => variant,
): SchemaDiscriminatedUnion | undefined {
    const variants = schema.anyOf ?? schema.oneOf;
    if (!Array.isArray(variants)) return undefined;
    return analyseDiscriminatedUnionVariants(
        variants as JSONSchema7[],
        resolveVariant,
    );
}
