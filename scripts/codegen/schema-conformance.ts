import fs from "fs/promises";
import type { JSONSchema7 } from "json-schema";
import path from "path";

import { COPILOT_CLI_VERSION } from "../../nodejs/src/cliVersion.js";
import {
    analyseDiscriminatedUnion,
    analyseNestedClosedUnionResult,
} from "./schema-unions.js";
import { getApiSchemaPath, REPO_ROOT } from "./utils.js";

const FORBIDDEN_CANDIDATE_FIELDS = ["card", "cardData", "rawCard"];

function assert(condition: unknown, message: string): asserts condition {
    if (!condition) throw new Error(`Schema conformance failed: ${message}`);
}

function referencedDefinitionNames(schema: JSONSchema7): string[] {
    return (((schema.anyOf ?? schema.oneOf) as JSONSchema7[]) ?? []).map(
        (variant) => variant.$ref?.split("/").at(-1) ?? "",
    );
}

function collectRpcMethods(
    node: unknown,
): Array<{ rpcMethod: string; result?: JSONSchema7 }> {
    if (!node || typeof node !== "object") return [];
    if (
        "rpcMethod" in node &&
        typeof (node as { rpcMethod?: unknown }).rpcMethod === "string"
    ) {
        return [node as { rpcMethod: string; result?: JSONSchema7 }];
    }
    return Object.values(node).flatMap(collectRpcMethods);
}

const schemaPath = await getApiSchemaPath();
const packageRoot = path.dirname(path.dirname(schemaPath));
const javaCodegenPackageJson = JSON.parse(
    await fs.readFile(
        path.join(REPO_ROOT, "java/scripts/codegen/package.json"),
        "utf8",
    ),
) as { dependencies?: Record<string, string> };
const javaCodegenPackageVersion =
    javaCodegenPackageJson.dependencies?.["@github/copilot"];
const packageJson = JSON.parse(
    await fs.readFile(path.join(packageRoot, "package.json"), "utf8"),
) as { version?: string };
const schema = JSON.parse(await fs.readFile(schemaPath, "utf8")) as {
    definitions: Record<string, JSONSchema7>;
    server?: {
        catalog?: {
            search?: {
                rpcMethod?: string;
                params?: { $ref?: string };
                result?: { $ref?: string };
            };
        };
    };
};

assert(
    /^\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?$/.test(COPILOT_CLI_VERSION),
    "nodejs/src/cliVersion.ts must pin an exact Copilot CLI version",
);
assert(
    packageJson.version === COPILOT_CLI_VERSION,
    `expected Copilot CLI ${COPILOT_CLI_VERSION}, received ${packageJson.version ?? "unknown"}`,
);
assert(
    javaCodegenPackageVersion === COPILOT_CLI_VERSION,
    `java/scripts/codegen must pin @github/copilot exactly to ${COPILOT_CLI_VERSION}`,
);
assert(
    schema.server?.catalog?.search?.rpcMethod === "catalog.search",
    "catalog.search is missing",
);
assert(
    schema.server.catalog.search.params?.$ref ===
        "#/definitions/CatalogSearchRequest",
    "catalog.search request is not typed",
);
assert(
    schema.server.catalog.search.result?.$ref ===
        "#/definitions/CatalogSearchResult",
    "catalog.search result is not typed",
);

const candidateVariants = referencedDefinitionNames(
    schema.definitions.CatalogCandidate,
);
assert(
    candidateVariants.join(",") ===
        "CatalogMcpServerCandidate,CatalogAiSkillCandidate",
    `unexpected candidate variants: ${candidateVariants.join(", ")}`,
);
const sourceVariants = referencedDefinitionNames(
    schema.definitions.CatalogCandidateSource,
);
assert(
    sourceVariants.join(",") ===
        "CatalogCandidateSourceUrl,CatalogCandidateSourceEmbedded",
    `unexpected candidate source variants: ${sourceVariants.join(", ")}`,
);

const resolveVariant = (variant: JSONSchema7): JSONSchema7 | undefined => {
    const name = variant.$ref?.match(/^#\/definitions\/([^/]+)$/)?.[1];
    return name ? schema.definitions[name] : variant;
};
for (const name of [
    "CatalogCandidate",
    "CatalogCandidateSource",
    "CatalogSearchResult",
]) {
    const analysis = analyseDiscriminatedUnion(
        schema.definitions[name],
        resolveVariant,
    );
    assert(analysis !== undefined, `${name} must remain a discriminated union`);
    assert(
        analysis.unknownVariantPolicy === "reject",
        `${name} variants must remain closed to unknown payload fields`,
    );
}

const selectedNestedUnionMethods = collectRpcMethods(schema)
    .map((method) => ({
        method,
        analysis: analyseNestedClosedUnionResult(
            method.result,
            schema.definitions,
        ),
    }))
    .filter((entry) => entry.analysis !== undefined);
assert(
    selectedNestedUnionMethods.map(({ method }) => method.rpcMethod).join(",") ===
        "catalog.search",
    `unexpected nested-union result methods: ${selectedNestedUnionMethods
        .map(({ method }) => method.rpcMethod)
        .join(", ")}`,
);
assert(
    [...selectedNestedUnionMethods[0].analysis!.unionDefinitionNames]
        .sort()
        .join(",") ===
        "CatalogCandidate,CatalogCandidateSource,CatalogSearchResult",
    "nested-union policy graph must remain limited to the proven catalogue result unions",
);

for (const name of candidateVariants) {
    const properties = schema.definitions[name]?.properties;
    assert(
        properties?.handle?.type === "string",
        `${name}.handle must remain an opaque string`,
    );
    for (const field of FORBIDDEN_CANDIDATE_FIELDS) {
        assert(
            !(field in properties),
            `${name} exposes forbidden raw card field ${field}`,
        );
    }
}

const resultVariants = new Set(
    referencedDefinitionNames(schema.definitions.CatalogSearchResult),
);
for (const name of [
    "CatalogSearchSucceeded",
    "CatalogAuthenticationRequiredError",
    "CatalogNetworkFailureError",
    "CatalogContractViolationError",
    "CatalogUnavailableError",
]) {
    assert(resultVariants.has(name), `CatalogSearchResult is missing ${name}`);
}

console.log(`Schema conformance: Copilot CLI ${packageJson.version}`);
