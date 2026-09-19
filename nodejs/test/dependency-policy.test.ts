import { describe, expect, it } from "vitest";
import {
    assertExactProductionDependencies,
    assertMinimumPublicationAge,
    loadNpmPublicationTimes,
    MINIMUM_DEPENDENCY_AGE_MS,
    parseResolvedProductionDependencies,
    type ProductionDependency,
    type RegistryRequest,
} from "../scripts/dependency-policy.js";

const dependency: ProductionDependency = {
    name: "koffi",
    version: "3.2.1",
};
const publishedAt = "2026-09-04T07:39:01.277Z";

describe("production dependency policy", () => {
    it("accepts exact dependencies and optional dependencies", () => {
        expect(
            assertExactProductionDependencies({
                dependencies: { koffi: "3.2.1" },
                optionalDependencies: { "optional-package": "1.0.0-beta.1" },
            })
        ).toEqual([
            dependency,
            {
                name: "optional-package",
                version: "1.0.0-beta.1",
            },
        ]);
    });

    it.each(["^3.2.1", "~3.2.1", ">=3.2.1", "latest", "file:../package"])(
        "rejects non-exact requirement %s",
        (version) => {
            expect(() =>
                assertExactProductionDependencies({ dependencies: { koffi: version } })
            ).toThrow(`dependencies.koffi must use an exact SemVer version; found '${version}'`);
        }
    );

    it("accepts a version at the exact seven-day boundary", () => {
        const now = Date.parse(publishedAt) + MINIMUM_DEPENDENCY_AGE_MS;
        expect(() =>
            assertMinimumPublicationAge([dependency], new Map([["koffi@3.2.1", publishedAt]]), now)
        ).not.toThrow();
    });

    it("rejects a version newer than seven full days", () => {
        const now = Date.parse(publishedAt) + MINIMUM_DEPENDENCY_AGE_MS - 1;
        expect(() =>
            assertMinimumPublicationAge([dependency], new Map([["koffi@3.2.1", publishedAt]]), now)
        ).toThrow("is not eligible until 2026-09-11T07:39:01.277Z");
    });

    it("exempts GitHub packages from the publication cooldown", () => {
        expect(() =>
            assertMinimumPublicationAge(
                [
                    {
                        name: "@github/copilot",
                        version: "1.0.0",
                    },
                ],
                new Map(),
                Date.parse("2026-09-16T00:00:00Z")
            )
        ).not.toThrow();
    });

    it("loads authoritative publication times from the npm registry", async () => {
        const requestedUrls: string[] = [];
        const request: RegistryRequest = async (url) => {
            requestedUrls.push(url);
            return {
                ok: true,
                status: 200,
                statusText: "OK",
                json: async () => ({
                    time: {
                        "3.2.1": publishedAt,
                    },
                }),
            };
        };

        await expect(loadNpmPublicationTimes([dependency], request)).resolves.toEqual(
            new Map([["koffi@3.2.1", publishedAt]])
        );
        expect(requestedUrls).toEqual(["https://registry.npmjs.org/koffi"]);
    });

    it("surfaces npm metadata request failures", async () => {
        const request: RegistryRequest = async () => ({
            ok: false,
            status: 503,
            statusText: "Service Unavailable",
            json: async () => ({}),
        });

        await expect(loadNpmPublicationTimes([dependency], request)).rejects.toThrow(
            "npm metadata request for koffi failed: 503 Service Unavailable"
        );
    });

    it("reads the complete resolved production graph from package-lock v3", () => {
        expect(
            parseResolvedProductionDependencies({
                lockfileVersion: 3,
                packages: {
                    "": {
                        dependencies: { koffi: "3.2.1" },
                        devDependencies: { eslint: "^9.0.0" },
                    },
                    "node_modules/koffi": { version: "3.2.1" },
                    "node_modules/@koromix/koffi-linux-x64": {
                        version: "3.2.1",
                        optional: true,
                    },
                    "node_modules/shared-optional": {
                        version: "2.0.0",
                        devOptional: true,
                    },
                    "node_modules/eslint": { version: "9.0.0", dev: true },
                    "node_modules/dev-optional": {
                        version: "1.0.0",
                        dev: true,
                        optional: true,
                    },
                    "node_modules/local-package": { link: true },
                    "node_modules/bundled-package": {
                        version: "1.0.0",
                        inBundle: true,
                    },
                    "node_modules/parent/node_modules/nested": { version: "4.0.0" },
                },
            })
        ).toEqual([
            { name: "koffi", version: "3.2.1" },
            {
                name: "@koromix/koffi-linux-x64",
                version: "3.2.1",
            },
            { name: "shared-optional", version: "2.0.0" },
            { name: "nested", version: "4.0.0" },
        ]);
    });

    it("keeps distinct resolved versions and removes duplicate package/version pairs", () => {
        expect(
            parseResolvedProductionDependencies({
                lockfileVersion: 3,
                packages: {
                    "": {},
                    "node_modules/example": { version: "1.0.0" },
                    "node_modules/parent/node_modules/example": { version: "1.0.0" },
                    "node_modules/other/node_modules/example": { version: "2.0.0" },
                },
            })
        ).toEqual([
            { name: "example", version: "1.0.0" },
            { name: "example", version: "2.0.0" },
        ]);
    });

    it("rejects unsupported production lockfile entries", () => {
        expect(() =>
            parseResolvedProductionDependencies({
                lockfileVersion: 3,
                packages: {
                    "": {},
                    "packages/example": { version: "1.0.0" },
                },
            })
        ).toThrow("Unsupported production package-lock path 'packages/example'");
        expect(() =>
            parseResolvedProductionDependencies({
                lockfileVersion: 3,
                packages: {
                    "": {},
                    "node_modules/example": { version: "github:owner/example" },
                },
            })
        ).toThrow("Resolved production dependency example must use an exact SemVer version");
    });
});
