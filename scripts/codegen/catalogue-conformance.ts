import fs from "fs/promises";
import path from "path";

import { getApiSchemaPath, REPO_ROOT } from "./utils.js";

const FORBIDDEN_CANDIDATE_FIELDS = ["card", "cardData", "rawCard"];

function assert(condition: unknown, message: string): asserts condition {
    if (!condition) throw new Error(`Catalogue schema conformance failed: ${message}`);
}

function referencedDefinitionNames(schema: { anyOf?: Array<{ $ref?: string }> }): string[] {
    return (schema.anyOf ?? []).map((variant) => variant.$ref?.split("/").at(-1) ?? "");
}

const schemaPath = await getApiSchemaPath();
const packageRoot = path.dirname(path.dirname(schemaPath));
const sdkPackageLock = JSON.parse(
    await fs.readFile(path.join(REPO_ROOT, "nodejs/package-lock.json"), "utf8")
) as { packages?: Record<string, { version?: string }> };
const expectedPackageVersion =
    sdkPackageLock.packages?.["node_modules/@github/copilot"]?.version;
const packageJson = JSON.parse(
    await fs.readFile(path.join(packageRoot, "package.json"), "utf8")
) as { version?: string };
const schema = JSON.parse(await fs.readFile(schemaPath, "utf8")) as {
    definitions: Record<
        string,
        {
            anyOf?: Array<{ $ref?: string }>;
            properties?: Record<string, { type?: string }>;
        }
    >;
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
    expectedPackageVersion !== undefined
        && /^\d+\.\d+\.\d+(?:-[0-9A-Za-z.-]+)?$/.test(expectedPackageVersion),
    "nodejs/package-lock.json must pin @github/copilot to an exact version"
);
assert(
    packageJson.version === expectedPackageVersion,
    `expected @github/copilot ${expectedPackageVersion}, received ${packageJson.version ?? "unknown"}`
);
assert(
    schema.server?.catalog?.search?.rpcMethod === "catalog.search",
    "catalog.search is missing"
);
assert(
    schema.server.catalog.search.params?.$ref === "#/definitions/CatalogSearchRequest",
    "catalog.search request is not typed"
);
assert(
    schema.server.catalog.search.result?.$ref === "#/definitions/CatalogSearchResult",
    "catalog.search result is not typed"
);

const candidateVariants = referencedDefinitionNames(schema.definitions.CatalogCandidate);
assert(
    candidateVariants.join(",") === "CatalogMcpServerCandidate,CatalogAiSkillCandidate",
    `unexpected candidate variants: ${candidateVariants.join(", ")}`
);
const sourceVariants = referencedDefinitionNames(schema.definitions.CatalogCandidateSource);
assert(
    sourceVariants.join(",") === "CatalogCandidateSourceUrl,CatalogCandidateSourceEmbedded",
    `unexpected candidate source variants: ${sourceVariants.join(", ")}`
);

for (const name of candidateVariants) {
    const properties = schema.definitions[name]?.properties;
    assert(properties?.handle?.type === "string", `${name}.handle must remain an opaque string`);
    for (const field of FORBIDDEN_CANDIDATE_FIELDS) {
        assert(!(field in properties), `${name} exposes forbidden raw card field ${field}`);
    }
}

const resultVariants = new Set(
    referencedDefinitionNames(schema.definitions.CatalogSearchResult)
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

console.log(`Catalogue schema conformance: @github/copilot ${packageJson.version}`);
