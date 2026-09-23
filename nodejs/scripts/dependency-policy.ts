import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import * as semver from "semver";

const NPM_REGISTRY_URL = "https://registry.npmjs.org";

export const MINIMUM_DEPENDENCY_AGE_MS = 7 * 24 * 60 * 60 * 1000;

export interface ProductionDependencyManifest {
    dependencies?: Record<string, string>;
    optionalDependencies?: Record<string, string>;
}

export interface ProductionDependency {
    name: string;
    version: string;
}

export interface RegistryResponse {
    json(): Promise<unknown>;
    ok: boolean;
    status: number;
    statusText: string;
}

export type RegistryRequest = (url: string) => Promise<RegistryResponse>;

function isRecord(value: unknown): value is Record<string, unknown> {
    return typeof value === "object" && value !== null && !Array.isArray(value);
}

function dependencyKey(dependency: ProductionDependency): string {
    return `${dependency.name}@${dependency.version}`;
}

function readDependencySection(
    manifest: Record<string, unknown>,
    section: "dependencies" | "optionalDependencies"
): Record<string, string> | undefined {
    const value = manifest[section];
    if (value === undefined) {
        return undefined;
    }
    assert(isRecord(value), `${section} must be a JSON object`);
    const dependencies: Record<string, string> = {};
    for (const [name, version] of Object.entries(value)) {
        assert(typeof version === "string", `${section}.${name} must be a string`);
        dependencies[name] = version;
    }
    return dependencies;
}

export function readProductionDependencyManifest(path: string): ProductionDependencyManifest {
    const parsed: unknown = JSON.parse(readFileSync(path, "utf8"));
    assert(isRecord(parsed), "package.json must contain a JSON object");
    return {
        dependencies: readDependencySection(parsed, "dependencies"),
        optionalDependencies: readDependencySection(parsed, "optionalDependencies"),
    };
}

export function assertExactProductionDependencies(
    manifest: ProductionDependencyManifest
): ProductionDependency[] {
    const dependencies: ProductionDependency[] = [];
    for (const section of ["dependencies", "optionalDependencies"] as const) {
        for (const [name, version] of Object.entries(manifest[section] ?? {})) {
            assert.equal(
                semver.valid(version),
                version,
                `${section}.${name} must use an exact SemVer version; found '${version}'`
            );
            dependencies.push({ name, version });
        }
    }
    return dependencies;
}

function packageNameFromLockfilePath(path: string): string | undefined {
    return /(?:^|\/)node_modules\/((?:@[^/]+\/)?[^/]+)$/.exec(path)?.[1];
}

export function parseResolvedProductionDependencies(lockfile: unknown): ProductionDependency[] {
    assert(isRecord(lockfile), "package-lock.json must contain a JSON object");
    assert.equal(lockfile.lockfileVersion, 3, "package-lock.json must use lockfileVersion 3");
    assert(isRecord(lockfile.packages), "package-lock.json must contain a packages object");

    const dependencies = new Map<string, ProductionDependency>();
    for (const [path, value] of Object.entries(lockfile.packages)) {
        if (path === "") {
            continue;
        }
        assert(isRecord(value), `Package-lock entry '${path}' must be a JSON object`);
        // npm marks dev=true only when a package is strictly dev-only. Optional and
        // devOptional packages reachable from production dependencies remain included.
        if (value.dev === true || value.link === true || value.inBundle === true) {
            continue;
        }
        const name = packageNameFromLockfilePath(path);
        assert(name, `Unsupported production package-lock path '${path}'`);
        const version = value.version;
        assert(
            typeof version === "string" && semver.valid(version) === version,
            `Resolved production dependency ${name} must use an exact SemVer version`
        );
        dependencies.set(`${name}@${version}`, { name, version });
    }
    return [...dependencies.values()];
}

export function readResolvedProductionDependencies(path: string): ProductionDependency[] {
    return parseResolvedProductionDependencies(JSON.parse(readFileSync(path, "utf8")) as unknown);
}

export function requiresPublicationCooldown(dependencyName: string): boolean {
    return !dependencyName.startsWith("@github/");
}

export async function loadNpmPublicationTimes(
    dependencies: readonly ProductionDependency[],
    request: RegistryRequest = (url) => fetch(url)
): Promise<Map<string, string>> {
    const externalDependencies = dependencies.filter((dependency) =>
        requiresPublicationCooldown(dependency.name)
    );
    const packageNames = [...new Set(externalDependencies.map((dependency) => dependency.name))];
    const metadata = new Map<string, Record<string, unknown>>();

    await Promise.all(
        packageNames.map(async (packageName) => {
            const response = await request(
                `${NPM_REGISTRY_URL}/${encodeURIComponent(packageName)}`
            );
            assert(
                response.ok,
                `npm metadata request for ${packageName} failed: ${response.status} ${response.statusText}`
            );
            const body = await response.json();
            assert(isRecord(body), `npm metadata for ${packageName} must be a JSON object`);
            const times = body.time;
            assert(isRecord(times), `npm metadata for ${packageName} is missing publication times`);
            metadata.set(packageName, times);
        })
    );

    return new Map(
        externalDependencies.map((dependency) => {
            const publishedAt = metadata.get(dependency.name)?.[dependency.version];
            assert(
                typeof publishedAt === "string",
                `npm metadata is missing a publication time for ${dependencyKey(dependency)}`
            );
            return [dependencyKey(dependency), publishedAt];
        })
    );
}

export function assertMinimumPublicationAge(
    dependencies: readonly ProductionDependency[],
    publicationTimes: ReadonlyMap<string, string>,
    now = Date.now()
): void {
    assert(Number.isFinite(now), "Dependency policy verification time must be finite");

    for (const dependency of dependencies) {
        if (!requiresPublicationCooldown(dependency.name)) {
            continue;
        }
        const key = dependencyKey(dependency);
        const publishedAt = publicationTimes.get(key);
        assert(publishedAt, `Missing npm publication time for ${key}`);
        const publishedAtMs = Date.parse(publishedAt);
        assert(Number.isFinite(publishedAtMs), `Invalid npm publication time for ${key}`);
        const eligibleAtMs = publishedAtMs + MINIMUM_DEPENDENCY_AGE_MS;
        assert(
            now >= eligibleAtMs,
            `${key} was published at ${publishedAt} and is not eligible until ${new Date(
                eligibleAtMs
            ).toISOString()}`
        );
    }
}
